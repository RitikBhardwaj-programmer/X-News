package com.cfs.xnews.jev.dto;

import java.util.List;
import java.util.Map;

public record JevQuestion(
        String type,
        String instructions,
        Object criteria
) {

    public static JevQuestion choice(String instructions, Map<String, String> options) {
        return new JevQuestion("choice", instructions, options);
    }

    public static JevQuestion noul(String instructions, String trueDescription, String falseDescription) {
        return new JevQuestion(
                "noul",
                instructions,
                Map.of("true", trueDescription, "false", falseDescription)
        );
    }

    public static JevQuestion score(String instructions, List<String> levels) {
        return new JevQuestion("score", instructions, levels);
    }
}
