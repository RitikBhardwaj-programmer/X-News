package com.cfs.xnews.processing;

import java.util.Locale;

/**
 * Which event matcher decides (ai.event-matcher.mode, V4 stage 1b).
 * <ul>
 *   <li>V1: today's centroid matcher only; no decisions are stored.</li>
 *   <li>SHADOW: v1 decides; v2 also runs, and both decisions are stored.</li>
 *   <li>V2: v2 decides; if its call fails, v1 decides instead.</li>
 * </ul>
 * Switching is config only, so rolling back is a restart with v1.
 */
public enum MatcherMode {

    V1,
    SHADOW,
    V2;

    // Fails at startup on a typo instead of silently running another mode.
    public static MatcherMode parse(String value) {

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "ai.event-matcher.mode must be v1, shadow or v2 but was: " + value
            );
        }
    }

    // The decisions table's mode column: shadow, or live once v2 decides.
    public String decisionMode() {
        return this == SHADOW ? "shadow" : "live";
    }
}
