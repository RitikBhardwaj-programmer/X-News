package com.cfs.xnews.jev.dto;

import java.util.Map;

public record JevRequest(
        String state,
        String model,
        Map<String, JevQuestion> questions
) {

    public static JevRequest of(String state, Map<String, JevQuestion> questions) {
        return new JevRequest(state, "jev-latest", questions);
    }
}
