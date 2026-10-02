package com.cfs.xnews.provenance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the extraction run (provenance record) for a configuration,
 * creating it on first use. The code version is the deployed image tag
 * (XNEWS_CODE_VERSION, set at image build), or "local" on a developer
 * machine.
 */
@Service
public class ExtractionRunService {

    public static final String NO_PROMPT = "n/a";

    static final int MAX_MODEL_CHARS = 150;

    private final ExtractionRunRepository repository;
    private final String codeVersion;

    // Run ids never change once created, so they are cached per configuration.
    private final Map<String, Long> cache = new ConcurrentHashMap<>();

    public ExtractionRunService(
            ExtractionRunRepository repository,

            @Value("${xnews.code-version:local}")
            String codeVersion
    ) {
        this.repository = repository;
        this.codeVersion = codeVersion;
    }

    public String codeVersion() {
        return codeVersion;
    }

    // Its own transaction: the run row commits even if the caller (an
    // article or an analysis) later rolls back, so a cached id always
    // points at an existing row.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long runId(String kind, String model, String promptVersion) {

        String safeModel = model == null || model.isBlank()
                ? "unknown"
                : model.length() <= MAX_MODEL_CHARS ? model : model.substring(0, MAX_MODEL_CHARS);
        String key = kind + "|" + safeModel + "|" + promptVersion;

        Long cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        repository.insertIfAbsent(kind, safeModel, promptVersion, codeVersion);

        Long id = repository
                .findIdByConfiguration(kind, safeModel, promptVersion, codeVersion)
                .orElseThrow(() -> new IllegalStateException("Extraction run missing after insert: " + key));

        cache.put(key, id);

        return id;
    }
}
