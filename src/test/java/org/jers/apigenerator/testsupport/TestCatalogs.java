package org.jers.apigenerator.testsupport;

import org.jers.apigenerator.core.catalog.Dependency;
import org.jers.apigenerator.core.catalog.DependencyCatalog;
import org.jers.apigenerator.core.catalog.DependencyCategory;
import org.jers.apigenerator.core.catalog.DependencyGroup;
import org.jers.apigenerator.core.catalog.DependencyKind;
import org.jers.apigenerator.core.catalog.GradleScope;
import org.jers.apigenerator.core.catalog.MavenScope;

import java.util.List;
import java.util.Locale;

/** Catálogos pequeños y predecibles para tests de cualquier capa. */
public final class TestCatalogs {

    private TestCatalogs() {
    }

    /** Maven, Gradle, Spring Web (MVC/WebFlux), Lombok y PostgreSQL (JDBC/R2DBC). */
    public static DependencyCatalog small() {
        return new DependencyCatalog(List.of(
                group("maven", "Maven", DependencyCategory.MAVEN, base("Maven Plugin")),
                group("gradle", "Gradle", DependencyCategory.GRADLE, base("Gradle Launcher")),
                group("spring-web", "Spring Web", DependencyCategory.SPRING, base("WebMVC"), reactive("WebFlux")),
                group("lombok", "Lombok", DependencyCategory.OTHER, base("Lombok")),
                group("postgresql", "PostgreSQL Driver", DependencyCategory.DATABASE,
                        base("PostgreSQL JDBC"), reactive("PostgreSQL R2DBC"))));
    }

    private static Dependency base(final String name) {
        return library(name, true, false);
    }

    private static Dependency reactive(final String name) {
        return library(name, false, true);
    }

    private static Dependency library(final String name, final boolean includedByDefault, final boolean reactive) {
        return new Dependency(name, "org.example", name.toLowerCase(Locale.ROOT).replace(' ', '-'), null,
                DependencyKind.LIBRARY, MavenScope.COMPILE, GradleScope.IMPLEMENTATION,
                includedByDefault, reactive);
    }

    private static DependencyGroup group(final String id, final String name, final DependencyCategory category,
                                         final Dependency... dependencies) {
        return new DependencyGroup(id, name, category, List.of(dependencies), false);
    }

    /**
     * Catálogo para planificar proyectos: Maven (herramienta 3.10.0 y plugin de Boot), parent de
     * Spring Boot 4.1.2, Spring Web, SpringDoc 3.2.0, Lombok sin versión, Spring Data (necesita
     * base de datos), Validation y driver de PostgreSQL.
     */
    public static DependencyCatalog project() {
        return new DependencyCatalog(List.of(
                new DependencyGroup("maven", "Maven", DependencyCategory.MAVEN, List.of(
                        artifact("Apache Maven", "org.apache.maven", "apache-maven", "3.10.0",
                                DependencyKind.BUILD_TOOL),
                        artifact("Spring Boot Maven Plugin", "org.springframework.boot", "spring-boot-maven-plugin",
                                null, DependencyKind.PLUGIN)), false),
                new DependencyGroup("spring-boot", "Spring Boot", DependencyCategory.SPRING, List.of(
                        artifact("Spring Boot Starter Parent", "org.springframework.boot",
                                "spring-boot-starter-parent", "4.1.2", DependencyKind.PARENT)), true),
                new DependencyGroup("spring-web", "Spring Web", DependencyCategory.SPRING, List.of(
                        library("Spring Web MVC", "org.springframework.boot", "spring-boot-starter-webmvc", null)),
                        true),
                new DependencyGroup("springdoc", "SpringDoc", DependencyCategory.OTHER, List.of(
                        library("Swagger UI", "org.springdoc", "springdoc-openapi-starter-webmvc-ui", "3.2.0")),
                        false),
                new DependencyGroup("lombok", "Lombok", DependencyCategory.OTHER, List.of(
                        library("Lombok", "org.projectlombok", "lombok", null)), false),
                new DependencyGroup("spring-data", "Spring Data", DependencyCategory.SPRING, List.of(
                        library("Spring Data JPA", "org.springframework.boot", "spring-boot-starter-data-jpa", null)),
                        false, true),
                new DependencyGroup("validation", "Validation", DependencyCategory.SPRING, List.of(
                        library("Validation", "org.springframework.boot", "spring-boot-starter-validation", null)),
                        false),
                new DependencyGroup("postgresql-driver", "PostgreSQL Driver", DependencyCategory.DATABASE, List.of(
                        library("PostgreSQL", "org.postgresql", "postgresql", null)), false)));
    }

    private static Dependency artifact(final String name, final String groupId, final String artifactId,
                                       final String version, final DependencyKind kind) {
        return new Dependency(name, groupId, artifactId, version, kind, null, null, true, false);
    }

    private static Dependency library(final String name, final String groupId, final String artifactId,
                                      final String version) {
        return new Dependency(name, groupId, artifactId, version, DependencyKind.LIBRARY,
                MavenScope.COMPILE, GradleScope.IMPLEMENTATION, true, false);
    }
}