import { applyEngine, applyProvider, basePackageOf, chooseIssuerType, enginesWithoutSequences, jdbcUrlPreview, modelDependencyGroupIds, projectForm, setConnectionEnabled, setIssuerEnabled, toArchitectureRequest, toBuildRequest, toConnectionRequest, toMetadataRequest, toProjectRequest, toSecurityRequest } from './project-form';
import { DatabaseEngine } from '../database/database.models';
import { Attribute, DomainModel, EMPTY_DOMAIN_MODEL, Entity, ModelDependencyGroups } from '../model/model.models';
import { addEntity, NO_CONSTRAINTS } from '../model/model-form';

describe('project-form', () => {
  it('empieza con valores válidos y su paquete base', () => {
    const form = projectForm();

    expect(form.valid).toBe(true);
    expect(basePackageOf(form.controls.basics)).toBe('com.example.demo');
  });

  it('el paquete base quita los guiones del artifactId, como el backend', () => {
    const form = projectForm();
    form.controls.basics.patchValue({ groupId: 'org.jers', artifactId: 'api-clientes' });

    expect(basePackageOf(form.controls.basics)).toBe('org.jers.apiclientes');
  });

  it('sin groupId o artifactId válidos no hay paquete base', () => {
    const form = projectForm();
    form.controls.basics.controls.groupId.setValue('Org.Jers');

    expect(form.controls.basics.controls.groupId.hasError('pattern')).toBe(true);
    expect(basePackageOf(form.controls.basics)).toBeNull();
  });

  it('el artifactId usa el mismo formato que los ids del catálogo', () => {
    const artifactId = projectForm().controls.basics.controls.artifactId;

    artifactId.setValue('API Clientes');
    expect(artifactId.valid).toBe(false);

    artifactId.setValue('api-clientes');
    expect(artifactId.valid).toBe(true);
  });

  it('el puerto es obligatorio y debe estar entre 1 y 65535', () => {
    const port = projectForm().controls.basics.controls.serverPort;

    port.setValue(null);
    expect(port.hasError('required')).toBe(true);

    port.setValue(70_000);
    expect(port.hasError('max')).toBe(true);
  });

  it('convierte los pasos en las partes de la petición', () => {
    const form = projectForm();
    form.controls.basics.patchValue({ name: '  API  ', description: ' Gestión ' });
    form.controls.architecture.patchValue({ style: 'HEXAGONAL', layout: 'MULTI_MODULE', patterns: ['CQRS'] });

    expect(toMetadataRequest(form.controls.basics)).toEqual({
      groupId: 'com.example', artifactId: 'demo', name: 'API', description: 'Gestión',
    });
    expect(toBuildRequest(form.controls.basics)).toEqual({
      buildTool: 'MAVEN', javaVersion: 25, packaging: 'JAR', reactive: false, serverPort: 8080,
    });
    expect(toArchitectureRequest(form.controls.architecture)).toEqual({
      style: 'HEXAGONAL', layout: 'MULTI_MODULE', patterns: ['CQRS'],
    });
  });

  const postgresql: DatabaseEngine = {
    id: 'postgresql', label: 'PostgreSQL', driverGroupId: 'postgresql-driver', embedded: false, defaultPort: 5432,
    defaultUsername: 'postgres', jdbcUrlTemplate: 'jdbc:postgresql://{host}:{port}/{database}',
    kind: 'RELATIONAL', supportsSequences: true, exampleUrl: 'jdbc:postgresql://localhost:5432/mi_base',
  };
  const h2: DatabaseEngine = {
    id: 'h2', label: 'H2', driverGroupId: 'h2-database', embedded: true, defaultPort: null,
    defaultUsername: 'sa', jdbcUrlTemplate: 'jdbc:h2:mem:{database}',
    kind: 'RELATIONAL', supportsSequences: true, exampleUrl: 'jdbc:h2:mem:mi_base',
  };
  const mysql: DatabaseEngine = {
    id: 'mysql', label: 'MySQL', driverGroupId: 'mysql-driver', embedded: false, defaultPort: 3306,
    defaultUsername: 'root', jdbcUrlTemplate: 'jdbc:mysql://{host}:{port}/{database}',
    kind: 'RELATIONAL', supportsSequences: false, exampleUrl: 'jdbc:mysql://localhost:3306/mi_base',
  };

  it('sin bases de datos el paso es válido y no envía conexiones', () => {
    const databases = projectForm().controls.databases;

    expect(databases.valid).toBe(true);
    expect(toConnectionRequest(databases.controls.database)).toBeNull();
    expect(toConnectionRequest(databases.controls.testDatabase)).toBeNull();
  });

  it('una base activada sin motor no es válida', () => {
    const connection = projectForm().controls.databases.controls.database;

    setConnectionEnabled(connection, true, undefined);

    expect(connection.controls.engineId.hasError('required')).toBe(true);
  });

  it('elegir un motor con servidor copia su puerto y usuario y arma la URL', () => {
    const connection = projectForm().controls.databases.controls.database;
    setConnectionEnabled(connection, true, undefined);

    applyEngine(connection, postgresql);

    expect(connection.valid).toBe(true);
    expect(connection.getRawValue()).toMatchObject({ engineId: 'postgresql', port: 5432, username: 'postgres' });
    expect(jdbcUrlPreview(connection, postgresql)).toBe('jdbc:postgresql://localhost:5432/app');
  });

  it('un motor embebido no envía servidor ni puerto', () => {
    const connection = projectForm().controls.databases.controls.database;
    setConnectionEnabled(connection, true, undefined);

    applyEngine(connection, h2);

    expect(connection.valid).toBe(true);
    expect(toConnectionRequest(connection)).toMatchObject({ engineId: 'h2', host: null, port: null });
  });

  it('al volver a activar una base embebida, servidor y puerto siguen ocultos', () => {
    const connection = projectForm().controls.databases.controls.database;
    setConnectionEnabled(connection, true, undefined);
    applyEngine(connection, h2);

    setConnectionEnabled(connection, false, h2);
    setConnectionEnabled(connection, true, h2);

    expect(connection.controls.host.disabled).toBe(true);
    expect(connection.controls.port.disabled).toBe(true);
  });

  it('el nombre de la base debe empezar por letra', () => {
    const connection = projectForm().controls.databases.controls.database;
    setConnectionEnabled(connection, true, undefined);

    connection.controls.databaseName.setValue('1clientes');

    expect(connection.controls.databaseName.hasError('pattern')).toBe(true);
  });

  it('arma la petición completa con los valores iniciales', () => {
    expect(toProjectRequest(projectForm())).toEqual({
      metadata: { groupId: 'com.example', artifactId: 'demo', name: 'Demo', description: '' },
      build: { buildTool: 'MAVEN', javaVersion: 25, packaging: 'JAR', reactive: false, serverPort: 8080 },
      architecture: { style: 'LAYERED', layout: 'SINGLE_MODULE', patterns: [] },
      options: { javadoc: true, unitTests: true, configFormat: 'YAML' },
      dependencyGroupIds: [],
      database: null,
      testDatabase: null,
      versions: {},
      security: { apiAuthenticationId: null, browserLoginId: null, addOnIds: [], issuer: null },
      model: EMPTY_DOMAIN_MODEL,
    });
  });

  it('la petición lleva el modelo del paso Modelo', () => {
    const form = projectForm();
    addEntity(form.controls.model).controls.name.setValue('Cliente');

    expect(toProjectRequest(form).model.entities.map((entity) => entity.name)).toEqual(['Cliente']);
  });

  it('con base de pruebas pide pruebas aunque la opción esté apagada', () => {
    const form = projectForm();
    const { database, testDatabase } = form.controls.databases.controls;
    setConnectionEnabled(database, true, undefined);
    applyEngine(database, postgresql);
    setConnectionEnabled(testDatabase, true, undefined);
    applyEngine(testDatabase, h2);
    form.controls.options.controls.unitTests.setValue(false);

    const request = toProjectRequest(form);

    expect(request.options.unitTests).toBe(true);
    expect(request.database).toMatchObject({ engineId: 'postgresql' });
    expect(request.testDatabase).toMatchObject({ engineId: 'h2', host: null });
  });

  it('el emisor solo cuenta habilitado; el servidor propio no lleva proveedor', () => {
    const security = projectForm().controls.security;
    security.controls.apiAuthenticationId.setValue('oauth2-resource-server');

    setIssuerEnabled(security.controls.issuer, true);
    expect(security.valid).toBe(false);

    chooseIssuerType(security.controls.issuer, 'OWN_SERVER');
    expect(security.valid).toBe(true);
    expect(toSecurityRequest(security).issuer).toEqual({ type: 'OWN_SERVER', providerId: null, values: {} });

    setIssuerEnabled(security.controls.issuer, false);
    expect(toSecurityRequest(security).issuer).toBeNull();
  });

  it('con proveedor exige cada dato y los envía recortados', () => {
    const security = projectForm().controls.security;
    const issuer = security.controls.issuer;
    security.controls.apiAuthenticationId.setValue('oauth2-resource-server');
    setIssuerEnabled(issuer, true);
    chooseIssuerType(issuer, 'PROVIDER');
    applyProvider(issuer, {
      id: 'auth0', name: 'Auth0', description: '', hosting: 'CLOUD', issuerUriTemplate: 'https://{domain}/',
      parameters: [{ key: 'domain', label: 'Dominio', example: '' }],
      supportsResourceServer: true, supportsLogin: true, documentationUrl: null,
    });
    expect(security.valid).toBe(false);

    issuer.controls.values.controls['domain'].setValue('  t.us.auth0.com ');

    expect(security.valid).toBe(true);
    expect(toSecurityRequest(security).issuer).toEqual({
      type: 'PROVIDER', providerId: 'auth0', values: { domain: 't.us.auth0.com' },
    });
  });

  describe('grupos que agrega el modelo', () => {
    const groups: ModelDependencyGroups = { persistenceGroupId: 'spring-data', validationGroupId: 'validation' };
    const optional: Attribute = {
      id: 'a-1', name: 'nombre', columnName: null, type: { scalar: 'STRING', enumId: null },
      required: false, unique: false, constraints: NO_CONSTRAINTS,
    };
    const entity: Entity = {
      id: 'e-1', name: 'Etiqueta', tableName: null, resourcePath: null, sequenceName: null, superclassId: null,
      attributes: [optional], relations: [],
    };
    const withEntity: DomainModel = { ...EMPTY_DOMAIN_MODEL, entities: [entity] };

    it('sin entidades no agrega nada', () => {
      expect(modelDependencyGroupIds(EMPTY_DOMAIN_MODEL, true, groups)).toEqual([]);
    });

    it('con entidades agrega persistencia solo si hay base de desarrollo', () => {
      expect(modelDependencyGroupIds(withEntity, true, groups)).toEqual(['spring-data']);
      expect(modelDependencyGroupIds(withEntity, false, groups)).toEqual([]);
    });

    it('un atributo obligatorio o con restricciones agrega validación, también en superclases', () => {
      const required: DomainModel = { ...withEntity, entities: [{ ...entity, attributes: [{ ...optional, required: true }] }] };
      const inSuperclass: DomainModel = {
        ...withEntity,
        superclasses: [{
          id: 's-1', name: 'Base', parentId: null,
          attributes: [{ ...optional, id: 'a-2', constraints: { ...NO_CONSTRAINTS, maxLength: 50 } }],
        }],
      };

      expect(modelDependencyGroupIds(required, false, groups)).toEqual(['validation']);
      expect(modelDependencyGroupIds(inSuperclass, true, groups)).toEqual(['spring-data', 'validation']);
    });

    it('una relación obligatoria agrega validación', () => {
      const model: DomainModel = {
        ...withEntity,
        entities: [{
          ...entity,
          relations: [{
            id: 'r-1', name: 'padre', targetEntityId: 'e-1', cardinality: 'MANY_TO_ONE', required: true,
            inverseName: null, joinColumn: null, joinTable: null,
          }],
        }],
      };

      expect(modelDependencyGroupIds(model, false, groups)).toEqual(['validation']);
    });

    it('mientras no llegan los ids del servidor no agrega nada', () => {
      expect(modelDependencyGroupIds(withEntity, true, null)).toEqual([]);
    });
  });

  it('lista los motores elegidos que no tienen secuencias', () => {
    const databases = projectForm().controls.databases;
    const engines = [postgresql, h2, mysql];
    expect(enginesWithoutSequences(databases, engines)).toEqual([]);

    setConnectionEnabled(databases.controls.database, true, undefined);
    applyEngine(databases.controls.database, mysql);
    setConnectionEnabled(databases.controls.testDatabase, true, undefined);
    applyEngine(databases.controls.testDatabase, h2);

    expect(enginesWithoutSequences(databases, engines)).toEqual(['MySQL']);

    setConnectionEnabled(databases.controls.database, false, mysql);
    expect(enginesWithoutSequences(databases, engines)).toEqual([]);
  });
});