package com.cfs.xnews.processing;

public final class VectorUtils {

    private VectorUtils() {
    }

    public static String toPgVector(
            float[] embedding
    ) {

        StringBuilder builder =
                new StringBuilder("[");

        for (
                int i = 0;
                i < embedding.length;
                i++
        ) {

            if (i > 0) {
                builder.append(",");
            }

            builder.append(
                    embedding[i]
            );
        }

        builder.append("]");

        return builder.toString();
    }
}