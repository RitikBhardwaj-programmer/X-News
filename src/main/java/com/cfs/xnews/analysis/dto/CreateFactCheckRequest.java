package com.cfs.xnews.analysis.dto;

public record CreateFactCheckRequest(
        String agency,
        String claim,
        String verdict,
        String explanation,
        String sourceUrl
) {
}