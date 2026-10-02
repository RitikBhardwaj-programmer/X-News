package com.cfs.xnews.processing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatcherModeTest {

    @Test
    void parsesTheConfiguredModeIgnoringCaseAndSpaces() {

        assertThat(MatcherMode.parse("v1")).isEqualTo(MatcherMode.V1);
        assertThat(MatcherMode.parse(" Shadow ")).isEqualTo(MatcherMode.SHADOW);
        assertThat(MatcherMode.parse("V2")).isEqualTo(MatcherMode.V2);
    }

    @Test
    void rejectsAnUnknownModeInsteadOfGuessing() {

        assertThatThrownBy(() -> MatcherMode.parse("shadw"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("v1, shadow or v2");
        assertThatThrownBy(() -> MatcherMode.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decisionModeIsShadowOrLive() {

        assertThat(MatcherMode.SHADOW.decisionMode()).isEqualTo("shadow");
        assertThat(MatcherMode.V2.decisionMode()).isEqualTo("live");
    }
}
