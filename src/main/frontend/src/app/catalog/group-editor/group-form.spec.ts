import { coordinatesOf, dependencyForm, groupForm, toGroupDefinition } from './group-form';
import { DependencyDefinition, GroupDefinition } from "../catalog.models";

describe('group-form', () => {
  const lombok: DependencyDefinition = {
    name: 'Lombok', groupId: 'org.projectlombok', artifactId: 'lombok', version: '1.18.40', kind: 'LIBRARY',
    mavenScope: 'PROVIDED', gradleScope: 'COMPILE_ONLY', includedByDefault: true, reactive: false,
  };
  const group: GroupDefinition = {
    id: 'lombok', name: 'Lombok', category: 'OTHER', selectedByDefault: true, requiresDatabase: false, dependencies: [lombok],
  };

  it('un grupo existente vuelve a la API exactamente igual', () => {
    expect(toGroupDefinition(groupForm('SPRING', group))).toEqual(group);
  });

  it('un grupo nuevo empieza con una dependencia en blanco y es inválido hasta llenarlo', () => {
    const form = groupForm('SPRING');

    expect(form.controls.category.value).toBe('SPRING');
    expect(form.controls.dependencies.length).toBe(1);
    expect(form.valid).toBe(false);
  });

  it('el id debe tener formato de slug', () => {
    const form = groupForm('SPRING', group);

    form.controls.id.setValue('Spring Web');
    expect(form.controls.id.hasError('pattern')).toBe(true);

    form.controls.id.setValue('spring-web');
    expect(form.controls.id.valid).toBe(true);
  });

  it('los textos con solo espacios cuentan como vacíos', () => {
    const form = groupForm('SPRING', group);

    form.controls.name.setValue('   ');

    expect(form.controls.name.hasError('required')).toBe(true);
  });

  it('una librería exige los dos ámbitos; un plugin no', () => {
    const row = dependencyForm(lombok);

    row.controls.gradleScope.setValue(null);
    expect(row.hasError('scopesRequired')).toBe(true);

    row.controls.kind.setValue('PLUGIN');
    expect(row.hasError('scopesRequired')).toBe(false);
  });

  it('una dependencia reactiva no puede ir incluida por defecto', () => {
    const row = dependencyForm(lombok);

    row.controls.reactive.setValue(true);

    expect(row.hasError('reactiveByDefault')).toBe(true);
  });

  it('el grupo exige al menos una dependencia', () => {
    const form = groupForm('SPRING', group);

    form.controls.dependencies.removeAt(0);

    expect(form.controls.dependencies.hasError('atLeastOne')).toBe(true);
  });

  it('al convertir recorta textos, deja null la versión vacía y quita ámbitos que no son de librería', () => {
    const form = groupForm('SPRING', group);
    const row = form.controls.dependencies.at(0);
    row.patchValue({ name: '  Lombok  ', version: '  ', kind: 'PLUGIN' });

    const [dependency] = toGroupDefinition(form).dependencies;

    expect(dependency.name).toBe('Lombok');
    expect(dependency.version).toBeNull();
    expect(dependency.mavenScope).toBeNull();
    expect(dependency.gradleScope).toBeNull();
  });

  it('solo da coordenadas cuando groupId y artifactId están completos', () => {
    const row = dependencyForm();
    expect(coordinatesOf(row)).toBeNull();

    row.patchValue({ groupId: ' org.projectlombok ', artifactId: 'lombok' });
    expect(coordinatesOf(row)).toEqual({ groupId: 'org.projectlombok', artifactId: 'lombok' });
  });

  it('conserva la marca "necesita base de datos" al editar un grupo', () => {
    const form = groupForm('SPRING', { ...group, requiresDatabase: true });

    expect(toGroupDefinition(form).requiresDatabase).toBe(true);
  });
});