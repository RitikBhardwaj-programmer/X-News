package com.cfs.xnews.event;

import org.springframework.stereotype.Component;

/**
 * new_centroid = normalize(centroid * n + embedding). Dividing by (n + 1)
 * as well would not change the result, since normalizing removes any scale.
 *
 * A plain running mean under-represents how a long-running story drifts over
 * time; that is why this sits behind {@link CentroidUpdateStrategy} so a
 * recency-weighted version can replace it without touching the callers.
 */
@Component
public class RunningMeanCentroidStrategy implements CentroidUpdateStrategy {

    @Override
    public float[] update(
            float[] currentCentroid,
            int currentMemberCount,
            float[] newEmbedding
    ) {

        float[] sum = new float[newEmbedding.length];

        boolean hasHistory =
                currentCentroid != null && currentMemberCount > 0;

        for (int i = 0; i < newEmbedding.length; i++) {

            sum[i] = hasHistory
                    ? currentCentroid[i] * currentMemberCount + newEmbedding[i]
                    : newEmbedding[i];
        }

        return normalize(sum);
    }

    private float[] normalize(float[] vector) {

        double squares = 0;

        for (float value : vector) {
            squares += (double) value * value;
        }

        double norm = Math.sqrt(squares);

        if (norm == 0) {
            return vector;
        }

        float[] result = new float[vector.length];

        for (int i = 0; i < vector.length; i++) {
            result[i] = (float) (vector[i] / norm);
        }

        return result;
    }
}
