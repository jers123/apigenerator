import { addAttribute, addEntity, addRelation, addSuperclass, applyCardinalityRules, clearUnsupportedConstraints, compatibleGenerations, constraintRules, ID_TYPES, isToMany, modelForm, newElementId, NO_CONSTRAINTS, parentCandidates, relationsPointingTo, removeEntity, removeSuperclass, toDomainModel } from "./model-form";
import { Attribute, AttributeConstraints, DomainModel, EMPTY_DOMAIN_MODEL, ScalarType } from "./model.models";

describe('model-form', () => {
  const attribute = (
    id: string,
    name: string,
    scalar: ScalarType,
    constraints: AttributeConstraints = NO_CONSTRAINTS,
  ): Attribute => ({
    id,
    name,
    columnName: null,
    type: { scalar, enumId: null },
    required: true,
    unique: false,
    constraints,
  });

  /** Auditable ← Documento; Cliente hereda de Auditable y Pedido de Documento, con Pedido N‑1 Cliente. */
  const model: DomainModel = {
    idSettings: { type: 'LONG', generation: 'SEQUENCE', placement: 'ROOT_SUPERCLASS' },
    audit: { createdAt: true, updatedAt: false },
    superclasses: [
      { id: 's-auditable', name: 'Auditable', parentId: null, attributes: [attribute('a-activo', 'activo', 'BOOLEAN')] },
      { id: 's-documento', name: 'Documento', parentId: 's-auditable', attributes: [] },
    ],
    entities: [
      {
        id: 'e-cliente',
        name: 'Cliente',
        tableName: null,
        resourcePath: null,
        sequenceName: null,
        superclassId: 's-auditable',
        attributes: [attribute('a-nombre', 'nombre', 'STRING', { ...NO_CONSTRAINTS, maxLength: 120 })],
        relations: [],
      },
      {
        id: 'e-pedido',
        name: 'Pedido',
        tableName: 'pedidos',
        resourcePath: '/pedidos',
        sequenceName: 'pedido_seq',
        superclassId: 's-documento',
        attributes: [
          attribute('a-total', 'total', 'BIG_DECIMAL', { ...NO_CONSTRAINTS, min: 0, integerDigits: 10, fractionDigits: 2 }),
        ],
        relations: [
          {
            id: 'r-cliente',
            name: 'cliente',
            targetEntityId: 'e-cliente',
            cardinality: 'MANY_TO_ONE',
            required: true,
            inverseName: 'pedidos',
            joinColumn: null,
            joinTable: null,
          },
        ],
      },
    ],
  };

  it('un modelo existente vuelve a la API exactamente igual', () => {
    expect(toDomainModel(modelForm(model))).toEqual(model);
  });

  it('sin modelo empieza con el modelo vacío por defecto', () => {
    expect(toDomainModel(modelForm())).toEqual(EMPTY_DOMAIN_MODEL);
  });

  it('los ids nuevos llevan el prefijo de su tipo y no se repiten', () => {
    expect(newElementId('entity')).toMatch(/^e-/);
    expect(newElementId('superclass')).toMatch(/^s-/);
    expect(newElementId('attribute')).toMatch(/^a-/);
    expect(newElementId('relation')).toMatch(/^r-/);
    expect(newElementId('entity')).not.toBe(newElementId('entity'));
  });

  it('recorta los nombres y deja null los textos opcionales vacíos', () => {
    const form = modelForm(model);
    const order = form.controls.entities.at(1);
    order.patchValue({ name: '  Pedido  ', tableName: '   ', resourcePath: '', sequenceName: ' ' });
    order.controls.relations.at(0).patchValue({ inverseName: '  ' });

    const converted = toDomainModel(form).entities[1];

    expect(converted.name).toBe('Pedido');
    expect(converted.tableName).toBeNull();
    expect(converted.resourcePath).toBeNull();
    expect(converted.sequenceName).toBeNull();
    expect(converted.relations[0].inverseName).toBeNull();
  });

  it('agrega entidades, atributos y relaciones con ids propios', () => {
    const form = modelForm();
    const product = addEntity(form);
    const price = addAttribute(product.controls.attributes);
    const self = addRelation(product, product.controls.id.value);

    const [entity] = toDomainModel(form).entities;

    expect(entity.id).toMatch(/^e-/);
    expect(entity.attributes[0].id).toBe(price.controls.id.value);
    expect(entity.attributes[0].type.scalar).toBe('STRING');
    expect(entity.relations[0].id).toBe(self.controls.id.value);
    expect(entity.relations[0].targetEntityId).toBe(entity.id);
    expect(entity.relations[0].cardinality).toBe('MANY_TO_ONE');
  });

  it('al borrar una entidad también se borran las relaciones que apuntan a ella', () => {
    const form = modelForm(model);

    expect(relationsPointingTo(form, 'e-cliente')).toBe(1);
    removeEntity(form, 'e-cliente');

    const result = toDomainModel(form);
    expect(result.entities.map((entity) => entity.id)).toEqual(['e-pedido']);
    expect(result.entities[0].relations).toEqual([]);
  });

  it('borrar una entidad inexistente no cambia nada', () => {
    const form = modelForm(model);

    removeEntity(form, 'e-nada');

    expect(toDomainModel(form)).toEqual(model);
  });

  it('al borrar una superclase sus hijas y entidades heredan del padre de la borrada', () => {
    const form = modelForm(model);
    const child = addSuperclass(form, 's-documento');

    removeSuperclass(form, 's-documento');

    const result = toDomainModel(form);
    expect(result.superclasses.map((superclass) => superclass.id)).toEqual(['s-auditable', child.controls.id.value]);
    expect(result.superclasses[1].parentId).toBe('s-auditable');
    expect(result.entities[1].superclassId).toBe('s-auditable');
  });

  it('al borrar una raíz, sus hijas quedan como raíces', () => {
    const form = modelForm(model);

    removeSuperclass(form, 's-auditable');

    const result = toDomainModel(form);
    expect(result.superclasses[0].parentId).toBeNull();
    expect(result.entities[0].superclassId).toBeNull();
  });

  it('una superclase no puede elegir como padre a sí misma ni a sus descendientes', () => {
    const form = modelForm(model);
    const grandchild = addSuperclass(form, 's-documento');

    expect(parentCandidates(form, 's-auditable')).toEqual([]);
    expect(parentCandidates(form, 's-documento').map((s) => s.id)).toEqual(['s-auditable']);
    expect(parentCandidates(form, grandchild.controls.id.value).map((s) => s.id)).toEqual([
      's-auditable',
      's-documento',
    ]);
  });

  it('las restricciones permitidas dependen de la familia del tipo', () => {
    expect(constraintRules('STRING')).toEqual({ text: true, range: false, digits: false, temporal: false });
    expect(constraintRules('INTEGER')).toEqual({ text: false, range: true, digits: false, temporal: false });
    expect(constraintRules('BIG_DECIMAL')).toEqual({ text: false, range: true, digits: true, temporal: false });
    expect(constraintRules('LOCAL_DATE')).toEqual({ text: false, range: false, digits: false, temporal: true });
    expect(constraintRules('LOCAL_TIME')).toEqual({ text: false, range: false, digits: false, temporal: false });
  });

  it('al cambiar de tipo se limpian las restricciones que el nuevo no admite', () => {
    const form = modelForm(model);
    const name = form.controls.entities.at(0).controls.attributes.at(0);
    name.controls.constraints.patchValue({ pattern: '[a-z]+', email: true });

    name.controls.type.setValue('BIG_DECIMAL');
    clearUnsupportedConstraints(name);

    expect(toDomainModel(form).entities[0].attributes[0].constraints).toEqual(NO_CONSTRAINTS);
  });

  it('las restricciones que el nuevo tipo admite se conservan', () => {
    const form = modelForm(model);
    const total = form.controls.entities.at(1).controls.attributes.at(0);

    total.controls.type.setValue('INTEGER');
    clearUnsupportedConstraints(total);

    const constraints = toDomainModel(form).entities[1].attributes[0].constraints;
    expect(constraints.min).toBe(0);
    expect(constraints.integerDigits).toBeNull();
    expect(constraints.fractionDigits).toBeNull();
  });

  it('una colección deja de ser obligatoria y solo N‑N conserva la tabla intermedia', () => {
    const relation = modelForm(model).controls.entities.at(1).controls.relations.at(0);
    relation.patchValue({ joinTable: 'pedido_cliente' });

    relation.controls.cardinality.setValue('MANY_TO_MANY');
    applyCardinalityRules(relation);
    expect(relation.controls.required.value).toBe(false);
    expect(relation.controls.joinTable.value).toBe('pedido_cliente');

    relation.controls.cardinality.setValue('ONE_TO_ONE');
    applyCardinalityRules(relation);
    expect(relation.controls.joinTable.value).toBe('');
  });

  it('sabe qué cardinalidades son colecciones', () => {
    expect(isToMany('ONE_TO_MANY')).toBe(true);
    expect(isToMany('MANY_TO_MANY')).toBe(true);
    expect(isToMany('MANY_TO_ONE')).toBe(false);
    expect(isToMany('ONE_TO_ONE')).toBe(false);
  });

  it('solo ofrece como id los tipos que pueden serlo, y sus estrategias compatibles', () => {
    expect(ID_TYPES.map((type) => type.value)).toEqual(['STRING', 'INTEGER', 'LONG', 'UUID']);
    expect(compatibleGenerations('UUID').map((g) => g.value)).toEqual(['UUID', 'ASSIGNED']);
    expect(compatibleGenerations('LONG').map((g) => g.value)).toEqual(['IDENTITY', 'SEQUENCE', 'ASSIGNED']);
  });
});