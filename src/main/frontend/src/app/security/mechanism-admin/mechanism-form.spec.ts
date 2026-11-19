import { mechanismForm, toMechanismDefinition } from "./mechanism-form";
import { SecurityMechanismDefinition } from "../security.models";

describe('mechanismForm', () => {
  const definition: SecurityMechanismDefinition = {
    id: 'form-login', name: 'Formulario', description: 'Login con sesión', role: 'BROWSER_LOGIN',
    reactiveSupported: true, requiresIssuer: false, managesUsers: true, conflictsWith: ['jwt', 'http-basic'],
    dependencies: [{ name: 'Spring Security', groupId: 'org.springframework.boot', artifactId: 'spring-boot-starter-security',
      version: null, kind: 'LIBRARY', mavenScope: 'COMPILE', gradleScope: 'IMPLEMENTATION', includedByDefault: true, reactive: false }],
  };

  it('un método nuevo empieza inválido hasta tener id, nombre y coordenadas', () => {
    expect(mechanismForm().valid).toBe(false);
  });

  it('solo la autenticación de la API y el login pueden necesitar emisor', () => {
    const form = mechanismForm(definition);
    form.controls.requiresIssuer.setValue(true);
    expect(form.hasError('issuerRole')).toBe(false);

    form.controls.role.setValue('ADD_ON');
    expect(form.hasError('issuerRole')).toBe(true);
  });

  it('convierte el formulario sin cambios y con los choques ordenados', () => {
    const form = mechanismForm(definition);
    form.controls.name.setValue('  Formulario  ');

    expect(toMechanismDefinition(form)).toEqual({ ...definition, conflictsWith: ['http-basic', 'jwt'] });
  });
});