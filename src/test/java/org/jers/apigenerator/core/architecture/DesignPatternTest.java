package org.jers.apigenerator.core.architecture;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DesignPatternTest {

    @Test
    void everyPatternHasACompatibilityForEveryStyle() {
        for (DesignPattern pattern : DesignPattern.values()) {
            for (ArchitectureStyle style : ArchitectureStyle.values()) {
                assertThat(pattern.compatibilityWith(style)).as("%s con %s", pattern, style).isNotNull();
            }
        }
    }

    @Test
    void hexagonalOnionAndCleanShareTheSameCompatibility() {
        assertThat(DesignPattern.values()).allSatisfy(pattern -> {
            assertThat(pattern.compatibilityWith(ArchitectureStyle.ONION))
                    .isEqualTo(pattern.compatibilityWith(ArchitectureStyle.HEXAGONAL));
            assertThat(pattern.compatibilityWith(ArchitectureStyle.CLEAN))
                    .isEqualTo(pattern.compatibilityWith(ArchitectureStyle.HEXAGONAL));
        });
    }

    @Test
    void everyPatternThatCannotBeChosenSomewhereExplainsWhy() {
        assertThat(DesignPattern.values())
                .filteredOn(pattern -> Arrays.stream(ArchitectureStyle.values())
                        .anyMatch(style -> !pattern.compatibilityWith(style).selectable()))
                .allSatisfy(pattern -> assertThat(pattern.getReason()).as("%s", pattern).isNotBlank());
    }

    @Test
    void springDataDirectlyIsContraindicatedInHexagonal() {
        assertThat(DesignPattern.SPRING_DATA_REPOSITORY.compatibilityWith(ArchitectureStyle.HEXAGONAL))
                .isEqualTo(Compatibility.CONTRAINDICATED);
        assertThat(DesignPattern.SPRING_DATA_REPOSITORY.compatibilityWith(ArchitectureStyle.LAYERED))
                .isEqualTo(Compatibility.NATIVE);
    }

    @Test
    void layeredRequiresTheClassicCrudPatterns() {
        assertThat(DesignPattern.nativeTo(ArchitectureStyle.LAYERED)).containsExactly(
                DesignPattern.TRANSACTION_SCRIPT, DesignPattern.SERVICE_LAYER, DesignPattern.SPRING_DATA_REPOSITORY,
                DesignPattern.DATA_MAPPER, DesignPattern.DTO);
    }
}