package com.cfs.xnews.provenance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExtractionRunRepository
        extends JpaRepository<ExtractionRun, Long> {

    // Safe under concurrency: a second insert of the same configuration is
    // a no-op, and the caller then reads the existing row.
    @Modifying
    @Query(
            value = """
            INSERT INTO extraction_runs (kind, model, prompt_version, code_version)
            VALUES (:kind, :model, :promptVersion, :codeVersion)
            ON CONFLICT (kind, model, prompt_version, code_version) DO NOTHING
            """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("kind") String kind,
            @Param("model") String model,
            @Param("promptVersion") String promptVersion,
            @Param("codeVersion") String codeVersion
    );

    @Query("""
        SELECT r.id FROM ExtractionRun r
        WHERE r.kind = :kind AND r.model = :model
          AND r.promptVersion = :promptVersion AND r.codeVersion = :codeVersion
        """)
    Optional<Long> findIdByConfiguration(
            @Param("kind") String kind,
            @Param("model") String model,
            @Param("promptVersion") String promptVersion,
            @Param("codeVersion") String codeVersion
    );
}
