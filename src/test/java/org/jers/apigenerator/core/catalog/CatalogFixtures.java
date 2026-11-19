package org.jers.apigenerator.core.catalog;

import java.util.Locale;

/** Objetos de prueba válidos para los tests del catálogo. */
final class CatalogFixtures {

    private CatalogFixtures() {
    }

    static Dependency library(final String name) {
        return new Dependency(name, "org.example", artifactId(name), null,
                DependencyKind.LIBRARY, MavenScope.COMPILE, GradleScope.IMPLEMENTATION,
                true, false);
    }

    static Dependency reactiveLibrary(final String name) {
        return new Dependency(name, "org.example", artifactId(name), null,
                DependencyKind.LIBRARY, MavenScope.COMPILE, GradleScope.IMPLEMENTATION,
                false, true);
    }

    private static String artifactId(final String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '-');
    }
}