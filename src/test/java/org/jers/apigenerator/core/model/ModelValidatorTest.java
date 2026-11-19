package org.jers.apigenerator.core.model;

import org.jers.apigenerator.testsupport.TestModels;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelValidatorTest {

	private static final ModelValidator VALIDATOR = new ModelValidator();

	// ── Ayudas ───────────────────────────────────────────────────────

	private static Attribute attribute(final String id, final String name, final ScalarType type) {
		return TestModels.attribute(id, name, type, false);
	}

	private static Attribute constrained(final String id, final ScalarType type, final AttributeConstraints c) {
		return new Attribute(id, "campo", null, AttributeType.of(type), false, false, c);
	}

	private static AttributeConstraints length(final Integer min, final Integer max) {
		return new AttributeConstraints(min, max, null, null, null, null, null, false, null);
	}

	private static AttributeConstraints range(final String min, final String max) {
		return new AttributeConstraints(null, null, min == null ? null : new BigDecimal(min),
				max == null ? null : new BigDecimal(max), null, null, null, false, null);
	}

	private static Entity entity(final String id, final String name, final List<Attribute> attributes,
								 final Relation... relations) {
		return new Entity(id, name, null, null, null, attributes, List.of(relations));
	}

	private static Entity withName(final String id, final String name) {
		return entity(id, name, List.of(attribute(id + "-a", "nombre", ScalarType.STRING)));
	}

	private static Entity child(final String id, final String name, final String superclassId,
								final Attribute... attributes) {
		return new Entity(id, name, null, null, null, superclassId, List.of(attributes), List.of());
	}

	private static Superclass superclass(final String id, final String name, final String parentId,
										 final Attribute... attributes) {
		return new Superclass(id, name, parentId, List.of(attributes));
	}

	private static Relation relation(final String id, final String name, final String target,
									 final Cardinality cardinality, final boolean required, final String inverse) {
		return new Relation(id, name, target, cardinality, required, inverse, null, null);
	}

	private static DomainModel model(final Entity... entities) {
		return new DomainModel(null, null, List.of(entities));
	}

	private static DomainModel tree(final IdPlacement placement, final AuditFields audit,
									final List<Superclass> superclasses, final Entity... entities) {
		return new DomainModel(new IdSettings(ScalarType.LONG, IdGeneration.IDENTITY, placement), audit,
				superclasses, List.of(entities));
	}

	private static ModelValidation validate(final DomainModel model) {
		return VALIDATOR.validate(model);
	}

	private static void assertError(final DomainModel model, final String elementId, final String fragment) {
		assertThat(validate(model).errors())
				.as("debería haber un error en %s con «%s»", elementId, fragment)
				.anySatisfy(issue -> {
					assertThat(issue.elementId()).isEqualTo(elementId);
					assertThat(issue.message()).contains(fragment);
				});
	}

	private static void assertWarning(final DomainModel model, final String elementId, final String fragment) {
		assertThat(validate(model).warnings())
				.anySatisfy(issue -> {
					assertThat(issue.elementId()).isEqualTo(elementId);
					assertThat(issue.message()).contains(fragment);
				});
	}

	// ── Modelo válido ────────────────────────────────────────────────

	@Test
	void aValidModelHasNoIssues() {
		assertThat(validate(TestModels.clientsAndOrders()).issues()).isEmpty();
	}

	@Test
	void anEmptyModelIsValid() {
		assertThat(validate(DomainModel.empty()).issues()).isEmpty();
	}

	@Test
	void theInheritanceTreeIsValid() {
		assertThat(validate(TestModels.inheritance()).issues()).isEmpty();
	}

	// ── Id ───────────────────────────────────────────────────────────

	@Test
	void theIdTypeMustSuitItsGeneration() {
		DomainModel model = new DomainModel(new IdSettings(ScalarType.UUID, IdGeneration.IDENTITY), null, List.of());

		assertError(model, null, "IDENTITY");
	}

	@Test
	void someTypesCannotBeIds() {
		DomainModel model = new DomainModel(new IdSettings(ScalarType.BOOLEAN, IdGeneration.ASSIGNED), null,
				List.of());

		assertError(model, null, "no puede ser id");
	}

	@Test
	void internalIdsCannotRepeat() {
		assertError(model(withName("e-1", "Cliente"), withName("e-1", "Pedido")), "e-1", "se repite");
		assertError(tree(IdPlacement.ENTITY, null, List.of(superclass("e-1", "Base", null)),
				child("e-1", "Cliente", null, attribute("a-1", "nombre", ScalarType.STRING))), "e-1", "se repite");
	}

	@Test
	void theIdInTheRootNeedsSuperclassesAndEveryEntityMustInherit() {
		DomainModel model = tree(IdPlacement.ROOT_SUPERCLASS, null, List.of(), withName("e-1", "Cliente"));

		assertError(model, null, "no tiene superclases");
		assertError(model, "e-1", "Debe heredar de una superclase");
	}

	@Test
	void theIdInEachEntityAllowsEntitiesWithoutSuperclass() {
		DomainModel model = tree(IdPlacement.ENTITY, null,
				List.of(superclass("s-1", "Base", null, attribute("a-1", "activo", ScalarType.BOOLEAN))),
				child("e-1", "Cliente", "s-1", attribute("a-2", "nombre", ScalarType.STRING)),
				withName("e-2", "Pedido"));

		assertThat(validate(model).hasErrors()).isFalse();
	}

	// ── Entidades ────────────────────────────────────────────────────

	@Test
	void entityNamesArePascalCaseAndRequired() {
		assertError(model(withName("e-1", "cliente")), "e-1", "PascalCase");
		assertError(model(withName("e-1", "Detalle Pedido")), "e-1", "PascalCase");
		assertError(model(withName("e-1", "")), "e-1", "obligatorio");
	}

	@Test
	void entityNamesCannotClashWithClassesOfTheGeneratedCode() {
		assertError(model(withName("e-1", "String")), "e-1", "choca");
	}

	@Test
	void entityNamesTablesAndPathsCannotRepeat() {
		assertError(model(withName("e-1", "Cliente"), withName("e-2", "Cliente")), "e-2", "igual que Cliente");

		Entity clients = new Entity("e-2", "Comprador", "cliente", null, null,
				List.of(attribute("a-2", "nombre", ScalarType.STRING)), List.of());
		assertError(model(withName("e-1", "Cliente"), clients), "e-2", "la tabla «cliente»");
	}

	@Test
	void anEntityWithoutAttributesOrRelationsOnlyWarns() {
		DomainModel model = model(entity("e-1", "Etiqueta", List.of()));

		assertWarning(model, "e-1", "solo tendrá id");
		assertThat(validate(model).hasErrors()).isFalse();
	}

	@Test
	void writtenTablesMustBeSnakeCaseAndShort() {
		Entity spaces = new Entity("e-1", "Cliente", "Clientes Tbl", null, null,
				List.of(attribute("a-1", "nombre", ScalarType.STRING)), List.of());
		Entity tooLong = new Entity("e-2", "Pedido", "p".repeat(64), null, null,
				List.of(attribute("a-2", "fecha", ScalarType.LOCAL_DATE)), List.of());

		assertError(model(spaces), "e-1", "snake_case");
		assertError(model(tooLong), "e-2", "63");
	}

	@Test
	void sqlReservedTablesOnlyWarn() {
		DomainModel model = model(withName("e-1", "Order"));

		assertWarning(model, "e-1", "palabra reservada en SQL");
		assertThat(validate(model).hasErrors()).isFalse();
	}

	@Test
	void writtenPathsMustBeKebabCaseFromTheRoot() {
		Entity entity = new Entity("e-1", "Cliente", null, "/Clientes", null,
				List.of(attribute("a-1", "nombre", ScalarType.STRING)), List.of());

		assertError(model(entity), "e-1", "La ruta");
	}

	@Test
	void sequencesAreCheckedOnlyWithTheSequenceStrategy() {
		Entity entity = new Entity("e-1", "Cliente", null, null, "Mala Seq",
				List.of(attribute("a-1", "nombre", ScalarType.STRING)), List.of());

		assertThat(validate(model(entity)).hasErrors()).isFalse();
		assertError(new DomainModel(new IdSettings(ScalarType.LONG, IdGeneration.SEQUENCE), null, List.of(entity)),
				"e-1", "La secuencia");
	}

	// ── Atributos ────────────────────────────────────────────────────

	@Test
	void attributeNamesAreCamelCaseNotKeywordsAndNotId() {
		assertError(model(entity("e-1", "Cliente", List.of(attribute("a-1", "Fecha Nac", ScalarType.STRING)))),
				"a-1", "camelCase");
		assertError(model(entity("e-1", "Cliente", List.of(attribute("a-1", "class", ScalarType.STRING)))),
				"a-1", "palabra reservada de Java");
		assertError(model(entity("e-1", "Cliente", List.of(attribute("a-1", "id", ScalarType.LONG)))),
				"a-1", "lo agrega el modelo");
	}

	@Test
	void enumsAreNotAvailableYet() {
		Attribute status = new Attribute("a-1", "estado", null, new AttributeType(null, "en-estado"), false, false,
				null);

		assertError(model(entity("e-1", "Pedido", List.of(status))), "a-1", "enums");
	}

	@Test
	void constraintsMustSuitTheType() {
		assertError(model(entity("e-1", "X", List.of(constrained("a-1", ScalarType.INTEGER, length(1, 5))))),
				"a-1", "solo aplican a textos");
		assertError(model(entity("e-1", "X", List.of(constrained("a-1", ScalarType.STRING, range("1", null))))),
				"a-1", "solo aplican a números");
		assertError(model(entity("e-1", "X", List.of(constrained("a-1", ScalarType.LONG,
				new AttributeConstraints(null, null, null, null, 5, 2, null, false, null))))), "a-1", "BigDecimal");
		assertError(model(entity("e-1", "X", List.of(constrained("a-1", ScalarType.LOCAL_TIME,
						new AttributeConstraints(null, null, null, null, null, null, null, false, TemporalRule.PAST))))),
				"a-1", "fechas e instantes");
	}

	@Test
	void constraintsMustBeCoherent() {
		assertError(model(entity("e-1", "X", List.of(constrained("a-1", ScalarType.STRING, length(10, 5))))),
				"a-1", "mayor que la máxima");
		assertError(model(entity("e-1", "X", List.of(constrained("a-1", ScalarType.BIG_DECIMAL,
				range("100", "10"))))), "a-1", "mayor que el máximo");
		assertError(model(entity("e-1", "X", List.of(constrained("a-1", ScalarType.INTEGER, range("1.5", null))))),
				"a-1", "no llevan decimales");
		assertError(model(entity("e-1", "X", List.of(constrained("a-1", ScalarType.STRING,
						new AttributeConstraints(null, null, null, null, null, null, "[a-", false, null))))),
				"a-1", "expresión regular");
	}

	// ── Relaciones ───────────────────────────────────────────────────

	@Test
	void theTargetMustExist() {
		Entity order = entity("e-1", "Pedido", List.of(attribute("a-1", "fecha", ScalarType.LOCAL_DATE)),
				relation("r-1", "cliente", "e-nada", Cardinality.MANY_TO_ONE, false, null));

		assertError(model(order), "r-1", "no existe");
	}

	@Test
	void collectionsCannotBeRequired() {
		Entity client = entity("e-1", "Cliente", List.of(attribute("a-1", "nombre", ScalarType.STRING)),
				relation("r-1", "pedidos", "e-2", Cardinality.ONE_TO_MANY, true, null));

		assertError(model(client, withName("e-2", "Pedido")), "r-1", "no puede ser obligatoria");
	}

	@Test
	void namesCannotRepeatInsideAnEntity() {
		Entity order = entity("e-2", "Pedido", List.of(attribute("a-2", "cliente", ScalarType.STRING)),
				relation("r-1", "cliente", "e-1", Cardinality.MANY_TO_ONE, false, null));

		assertError(model(withName("e-1", "Cliente"), order), "r-1", "se repite en Pedido");
	}

	@Test
	void anInverseCannotClashWithANameOfTheTarget() {
		Entity client = entity("e-1", "Cliente", List.of(attribute("a-1", "pedidos", ScalarType.INTEGER)));
		Entity order = entity("e-2", "Pedido", List.of(attribute("a-2", "fecha", ScalarType.LOCAL_DATE)),
				relation("r-1", "cliente", "e-1", Cardinality.MANY_TO_ONE, false, "pedidos"));

		assertError(model(client, order), "r-1", "«pedidos» se repite en Cliente");
	}

	@Test
	void aSelfRelationCannotNameItsInverseLikeItself() {
		Entity employee = entity("e-1", "Empleado", List.of(attribute("a-1", "nombre", ScalarType.STRING)),
				relation("r-1", "jefe", "e-1", Cardinality.MANY_TO_ONE, false, "jefe"));

		assertError(model(employee), "r-1", "igual que la relación");
	}

	@Test
	void aJoinTableOutsideManyToManyOnlyWarns() {
		Relation relation = new Relation("r-1", "cliente", "e-1", Cardinality.MANY_TO_ONE, false, null, null,
				"tabla_x");
		Entity order = entity("e-2", "Pedido", List.of(attribute("a-2", "fecha", ScalarType.LOCAL_DATE)), relation);

		assertWarning(model(withName("e-1", "Cliente"), order), "r-1", "solo se usa en relaciones N‑N");
	}

	// ── Tablas y columnas ────────────────────────────────────────────

	@Test
	void anAttributeCannotReuseTheColumnOfAForeignKey() {
		Entity order = entity("e-2", "Pedido", List.of(attribute("a-2", "clienteId", ScalarType.LONG)),
				relation("r-1", "cliente", "e-1", Cardinality.MANY_TO_ONE, false, null));

		assertError(model(withName("e-1", "Cliente"), order), "r-1", "«cliente_id» se repite en la tabla pedido");
	}

	@Test
	void aJoinTableCannotReuseTheTableOfAnEntity() {
		Entity client = entity("e-1", "Cliente", List.of(attribute("a-1", "nombre", ScalarType.STRING)),
				relation("r-1", "favoritos", "e-2", Cardinality.MANY_TO_MANY, false, null));

		assertError(model(client, withName("e-2", "Producto"), withName("e-3", "ClienteProducto")),
				"r-1", "ya la usa la entidad ClienteProducto");
	}

	// ── Auditoría ────────────────────────────────────────────────────

	@Test
	void entitiesCannotRepeatTheAuditFields() {
		Entity client = entity("e-1", "Cliente", List.of(attribute("a-1", "createdAt", ScalarType.INSTANT)));

		assertError(new DomainModel(null, new AuditFields(true, false), List.of(client)), "a-1",
				"ya lo usa la auditoría");
	}

	@Test
	void withoutAuditThoseNamesAreFree() {
		Entity client = entity("e-1", "Cliente", List.of(attribute("a-1", "createdAt", ScalarType.INSTANT)));

		assertThat(validate(model(client)).hasErrors()).isFalse();
	}

	// ── Superclases ──────────────────────────────────────────────────

	@Test
	void superclassNamesFollowTheClassRules() {
		Entity client = child("e-1", "Cliente", "s-1", attribute("a-2", "nombre", ScalarType.STRING));

		assertError(tree(IdPlacement.ENTITY, null, List.of(superclass("s-1", "auditable", null)), client),
				"s-1", "PascalCase");
		assertError(tree(IdPlacement.ENTITY, null, List.of(superclass("s-1", "", null)), client),
				"s-1", "obligatorio");
		assertError(tree(IdPlacement.ENTITY, null, List.of(superclass("s-1", "Object", null)), client),
				"s-1", "choca");
	}

	@Test
	void superclassesAndEntitiesCannotShareNames() {
		DomainModel model = tree(IdPlacement.ENTITY, null, List.of(superclass("s-1", "Cliente", null)),
				child("e-1", "Cliente", "s-1", attribute("a-1", "nombre", ScalarType.STRING)));

		assertError(model, "e-1", "igual que la superclase Cliente");
	}

	@Test
	void parentsAndChosenSuperclassesMustExist() {
		DomainModel model = tree(IdPlacement.ENTITY, null, List.of(superclass("s-1", "Base", "s-nada")),
				child("e-1", "Cliente", "s-1", attribute("a-1", "nombre", ScalarType.STRING)),
				child("e-2", "Pedido", "s-nada", attribute("a-2", "fecha", ScalarType.LOCAL_DATE)));

		assertError(model, "s-1", "padre no existe");
		assertError(model, "e-2", "La superclase elegida no existe");
	}

	@Test
	void theInheritanceCannotFormACycle() {
		DomainModel model = tree(IdPlacement.ENTITY, null,
				List.of(superclass("s-a", "Base", "s-b"), superclass("s-b", "Otra", "s-a")),
				child("e-1", "Cliente", "s-a", attribute("a-1", "nombre", ScalarType.STRING)));

		assertError(model, "s-a", "ciclo");
		assertError(model, "s-b", "ciclo");
	}

	@Test
	void anAttributeCannotRepeatOneInheritedAlongTheChain() {
		DomainModel model = tree(IdPlacement.ROOT_SUPERCLASS, null,
				List.of(superclass("s-a", "Auditable", null, attribute("a-1", "activo", ScalarType.BOOLEAN)),
						superclass("s-b", "Documento", "s-a", attribute("a-2", "activo", ScalarType.BOOLEAN))),
				child("e-1", "Factura", "s-b", attribute("a-3", "activo", ScalarType.BOOLEAN)));

		assertError(model, "a-2", "«activo» se repite en Documento: ya lo usa el atributo activo de Auditable");
		assertError(model, "a-3", "«activo» se repite en Factura: ya lo usa el atributo activo de Auditable");
	}

	@Test
	void aSuperclassCannotRepeatTheAuditFields() {
		DomainModel model = tree(IdPlacement.ROOT_SUPERCLASS, new AuditFields(true, false),
				List.of(superclass("s-1", "Auditable", null, attribute("a-1", "createdAt", ScalarType.INSTANT))),
				child("e-1", "Cliente", "s-1", attribute("a-2", "nombre", ScalarType.STRING)));

		assertError(model, "a-1", "ya lo usa la auditoría");
	}

	@Test
	void anEntityCannotReuseAnInheritedColumn() {
		Attribute state = new Attribute("a-2", "estado", "activo", AttributeType.of(ScalarType.STRING), false,
				false, null);
		DomainModel model = tree(IdPlacement.ROOT_SUPERCLASS, null,
				List.of(superclass("s-1", "Auditable", null, attribute("a-1", "activo", ScalarType.BOOLEAN))),
				child("e-1", "Factura", "s-1", state));

		assertError(model, "a-2", "«activo» se repite en la tabla factura: ya la usa el atributo activo de Auditable");
	}

	@Test
	void aSuperclassThatNoEntityUsesOnlyWarns() {
		DomainModel model = tree(IdPlacement.ENTITY, null,
				List.of(superclass("s-1", "Base", null, attribute("a-1", "activo", ScalarType.BOOLEAN))),
				withName("e-1", "Cliente"));

		assertWarning(model, "s-1", "Ninguna entidad hereda de ella");
		assertThat(validate(model).hasErrors()).isFalse();
	}
}