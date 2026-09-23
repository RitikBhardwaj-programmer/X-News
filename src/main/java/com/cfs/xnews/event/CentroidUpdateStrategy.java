package com.cfs.xnews.event;

public interface CentroidUpdateStrategy {

    /**
     * @param currentCentroid the event's centroid before the new article, or null for a brand new event
     * @param currentMemberCount number of articles already counted in that centroid
     * @param newEmbedding the embedding of the article being added
     * @return the event's new, unit-length centroid
     */
    float[] update(
            float[] currentCentroid,
            int currentMemberCount,
            float[] newEmbedding
    );
}
