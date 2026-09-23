package com.cfs.xnews.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RunningMeanCentroidStrategyTest {

    private final RunningMeanCentroidStrategy strategy = new RunningMeanCentroidStrategy();

    @Test
    void newEventCentroidIsTheNormalizedFirstEmbedding() {

        float[] centroid = strategy.update(null, 0, new float[]{3f, 4f});

        assertThat(centroid[0]).isCloseTo(0.6f, within(1e-6f));
        assertThat(centroid[1]).isCloseTo(0.8f, within(1e-6f));
    }

    @Test
    void existingCentroidIsWeightedByMemberCount() {

        // 3 articles pointing along x, one new article along y:
        // (1*3 + 0, 0*3 + 1) = (3, 1) -> normalized
        float[] centroid = strategy.update(new float[]{1f, 0f}, 3, new float[]{0f, 1f});

        double norm = Math.sqrt(9 + 1);

        assertThat(centroid[0]).isCloseTo((float) (3 / norm), within(1e-6f));
        assertThat(centroid[1]).isCloseTo((float) (1 / norm), within(1e-6f));
    }

    @Test
    void resultIsAlwaysUnitLength() {

        float[] centroid = strategy.update(new float[]{0.6f, 0.8f}, 7, new float[]{-2f, 5f});

        double squares = 0;

        for (float value : centroid) {
            squares += value * value;
        }

        assertThat(Math.sqrt(squares)).isCloseTo(1.0, within(1e-6));
    }
}
