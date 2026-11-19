package org.jers.apigenerator.application.project;

import lombok.RequiredArgsConstructor;
import org.jers.apigenerator.core.catalog.DependencyCatalogProvider;
import org.jers.apigenerator.core.common.DomainValidationException;
import org.jers.apigenerator.core.database.DatabaseEngine;
import org.jers.apigenerator.core.database.DatabaseEngineRepository;
import org.jers.apigenerator.core.project.ModelDependencyGroups;
import org.jers.apigenerator.core.project.ProjectPlan;
import org.jers.apigenerator.core.project.ProjectPlanner;
import org.jers.apigenerator.core.project.ProjectSpec;
import org.jers.apigenerator.core.security.IdentityProviderRepository;
import org.jers.apigenerator.core.security.SecurityCatalogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Casos de uso del proyecto a generar: resolver sus motores y calcular su plan.
 */
@Service
public class ProjectService {

    /** Fuente del catálogo vigente. */
    private final DependencyCatalogProvider catalogProvider;

    /** Motores de base de datos disponibles. */
    private final DatabaseEngineRepository engines;

    /** Métodos de seguridad disponibles. */
    private final SecurityCatalogRepository securityCatalog;

    /** Proveedores de identidad disponibles. */
    private final IdentityProviderRepository identityProviders;

    /** Grupos que el modelo agrega solo. */
    private final ModelDependencyGroups modelGroups;

    /**
     * Crea el servicio.
     *
     * @param catalogProvider   fuente del catálogo vigente
     * @param engines           motores de base de datos
     * @param securityCatalog   métodos de seguridad
     * @param identityProviders proveedores de identidad
     * @param persistenceGroup  grupo de persistencia, de {@code app.model.persistence-group}
     * @param validationGroup   grupo de validación, de {@code app.model.validation-group}
     */
    public ProjectService(final DependencyCatalogProvider catalogProvider,
                          final DatabaseEngineRepository engines,
                          final SecurityCatalogRepository securityCatalog,
                          final IdentityProviderRepository identityProviders,
                          @Value("${app.model.persistence-group:spring-data}") final String persistenceGroup,
                          @Value("${app.model.validation-group:validation}") final String validationGroup) {
        this.catalogProvider = catalogProvider;
        this.engines = engines;
        this.securityCatalog = securityCatalog;
        this.identityProviders = identityProviders;
        this.modelGroups = new ModelDependencyGroups(persistenceGroup, validationGroup);
    }

    /**
     * Busca el motor que el usuario eligió para una conexión.
     *
     * @param id id del motor
     * @return el motor
     * @throws DomainValidationException si no existe; es un dato inválido de
     *                                   la petición, no un recurso inexistente
     */
    public DatabaseEngine engine(final String id) {
        return engines.findById(id)
                .orElseThrow(() -> new DomainValidationException("No existe el motor de base de datos '" + id + "'"));
    }

    /**
     * Calcula el plan del proyecto con los catálogos vigentes.
     *
     * @param spec proyecto pedido por el usuario
     * @return plan con las dependencias finales, sus versiones y la seguridad
     */
    public ProjectPlan plan(final ProjectSpec spec) {
        return new ProjectPlanner(catalogProvider.catalog(), securityCatalog.catalog(), identityProviders.catalog(),
                modelGroups).plan(spec);
    }
}