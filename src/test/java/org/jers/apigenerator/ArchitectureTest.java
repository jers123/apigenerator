package org.jers.apigenerator;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

public class ArchitectureTest {

    private static final String ROOT = "org.jers.apigenerator";
    private static final String CORE = ROOT + ".core..";
    private static final String APPLICATION = ROOT + ".application..";
    private static final String INFRASTRUCTURE = ROOT + ".infrastructure..";
    private static final String WEB = ROOT + ".web..";
    private static final String CONFIG = ROOT + ".config..";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void coreDoesNotDependOnFrameworks() {
        noClasses().that().resideInAPackage(CORE)
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "tools.jackson..",
                        "com.fasterxml.jackson..", "jakarta..")
                .because("el núcleo es Java puro y se prueba sin levantar Spring")
                .check(CLASSES);
    }

    @Test
    void coreDoesNotDependOnOuterLayers() {
        noClasses().that().resideInAPackage(CORE)
                .should().dependOnClassesThat()
                .resideInAnyPackage(APPLICATION, INFRASTRUCTURE, WEB, CONFIG)
                .because("las dependencias apuntan hacia el núcleo, nunca hacia afuera")
                .check(CLASSES);
    }

    @Test
    void applicationDoesNotDependOnAdapters() {
        noClasses().that().resideInAPackage(APPLICATION)
                .should().dependOnClassesThat()
                .resideInAnyPackage(WEB, INFRASTRUCTURE)
                .because("los casos de uso solo conocen el núcleo y sus puertos")
                .check(CLASSES);
    }

    @Test
    void infrastructureDoesNotDependOnWeb() {
        noClasses().that().resideInAPackage(INFRASTRUCTURE)
                .should().dependOnClassesThat().resideInAPackage(WEB)
                .because("los adaptadores de salida no conocen la API REST")
                .check(CLASSES);
    }

    @Test
    void webDoesNotDependOnInfrastructure() {
        noClasses().that().resideInAPackage(WEB)
                .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
                .because("la web entra por la capa de aplicación, no por los adaptadores")
                .check(CLASSES);
    }

    @Test
    void packagesAreFreeOfCycles() {
        slices().matching(ROOT + ".(*)..")
                .should().beFreeOfCycles()
                .check(CLASSES);
    }
}