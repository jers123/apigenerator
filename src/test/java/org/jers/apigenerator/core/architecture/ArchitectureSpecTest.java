package org.jers.apigenerator.core.architecture;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchitectureSpecTest {

    @Test
    void nativePatternsAreIncludedEvenIfNotChosen() {
        ArchitectureSpec spec = new ArchitectureSpec(ArchitectureStyle.HEXAGONAL, ModuleLayout.SINGLE_MODULE, Set.of());

        assertThat(spec.patterns()).contains(DesignPattern.SEPARATED_INTERFACE, DesignPattern.ADAPTER,
                DesignPattern.REPOSITORY, DesignPattern.DOMAIN_MODEL);
    }

    @Test
    void chosenCompatiblePatternsAreKeptInCatalogOrder() {
        ArchitectureSpec spec = new ArchitectureSpec(ArchitectureStyle.HEXAGONAL, ModuleLayout.SINGLE_MODULE,
                Set.of(DesignPattern.STRATEGY, DesignPattern.CQRS));

        assertThat(spec.patterns()).containsSubsequence(DesignPattern.CQRS, DesignPattern.STRATEGY);
    }

    @Test
    void aContraindicatedPatternIsRejectedWithItsReason() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ArchitectureSpec(ArchitectureStyle.LAYERED, ModuleLayout.SINGLE_MODULE,
                        Set.of(DesignPattern.CQRS)))
                .withMessageContaining("CQRS")
                .withMessageContaining("único modelo");
    }

    @Test
    void aPatternThatDoesNotApplyIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ArchitectureSpec(ArchitectureStyle.LAYERED, ModuleLayout.SINGLE_MODULE,
                        Set.of(DesignPattern.API_GATEWAY)))
                .withMessageContaining("API Gateway");
    }

    @Test
    void verticalSliceCannotBeSplitIntoModules() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ArchitectureSpec(ArchitectureStyle.VERTICAL_SLICE, ModuleLayout.MULTI_MODULE,
                        Set.of()))
                .withMessageContaining("Vertical Slice");
    }

    @Test
    void stylesWithLayersCanBeSplitIntoModules() {
        for (ArchitectureStyle style : Set.of(ArchitectureStyle.LAYERED, ArchitectureStyle.HEXAGONAL,
                ArchitectureStyle.ONION, ArchitectureStyle.CLEAN, ArchitectureStyle.MODULITH)) {
            assertThat(new ArchitectureSpec(style, ModuleLayout.MULTI_MODULE, Set.of()).layout())
                    .isEqualTo(ModuleLayout.MULTI_MODULE);
        }
    }

    @Test
    void patternsCannotBeChangedFromOutside() {
        ArchitectureSpec spec = new ArchitectureSpec(ArchitectureStyle.LAYERED, ModuleLayout.SINGLE_MODULE, Set.of());

        assertThatThrownBy(() -> spec.patterns().add(DesignPattern.STRATEGY))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}