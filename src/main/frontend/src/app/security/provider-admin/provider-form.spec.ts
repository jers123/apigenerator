import { addMissingParameters, issuerPreview, placeholdersOf, providerForm, toProviderDefinition } from "./provider-form";
import { IdentityProviderDefinition } from "../security.models";

describe('providerForm', () => {
  const keycloak: IdentityProviderDefinition = {
    id: 'keycloak', name: 'Keycloak', description: 'Código abierto', hosting: 'SELF_HOSTED',
    issuerUriTemplate: '{baseUrl}/realms/{realm}',
    parameters: [
      { key: 'baseUrl', label: 'URL de Keycloak', example: 'https://kc.example.com' },
      { key: 'realm', label: 'Realm', example: '' },
    ],
    supportsResourceServer: true, supportsLogin: true, documentationUrl: 'https://www.keycloak.org/documentation',
  };

  it('lee los marcadores sin repetir', () => {
    expect(placeholdersOf('https://{a}/{b}/{a}')).toEqual(['a', 'b']);
  });

  it('un proveedor de fábrica es válido', () => {
    expect(providerForm(keycloak).valid).toBe(true);
  });

  it('señala marcadores sin parámetro y parámetros sin usar', () => {
    const form = providerForm(keycloak);
    form.controls.issuerUriTemplate.setValue('https://{domain}/realms/{realm}');

    expect(form.getError('templateMismatch')).toEqual({ undeclared: ['domain'], unused: ['baseUrl'] });
  });

  it('agrega los parámetros que pide la plantilla', () => {
    const form = providerForm();
    form.controls.issuerUriTemplate.setValue('https://{domain}/');

    addMissingParameters(form);

    expect(form.controls.parameters.getRawValue()).toEqual([{ key: 'domain', label: 'domain', example: '' }]);
    expect(form.hasError('templateMismatch')).toBe(false);
  });

  it('la vista previa usa los ejemplos y deja los marcadores sin ejemplo', () => {
    expect(issuerPreview(providerForm(keycloak))).toBe('https://kc.example.com/realms/{realm}');
  });

  it('para la API exige plantilla; solo para el login no', () => {
    const form = providerForm({ ...keycloak, issuerUriTemplate: null, parameters: [] });
    expect(form.hasError('templateRequired')).toBe(true);

    form.controls.supportsResourceServer.setValue(false);
    expect(form.hasError('templateRequired')).toBe(false);
  });

  it('rechaza una plantilla http y un proveedor sin usos', () => {
    const form = providerForm(keycloak);
    form.controls.issuerUriTemplate.setValue('http://idp.local/{baseUrl}{realm}');
    form.controls.supportsResourceServer.setValue(false);
    form.controls.supportsLogin.setValue(false);

    expect(form.controls.issuerUriTemplate.hasError('insecure')).toBe(true);
    expect(form.hasError('noUse')).toBe(true);
  });

  it('envía null la plantilla y la documentación vacías', () => {
    const form = providerForm({ ...keycloak, id: 'github', issuerUriTemplate: null, parameters: [],
      supportsResourceServer: false, documentationUrl: null });

    const definition = toProviderDefinition(form);

    expect(definition.issuerUriTemplate).toBeNull();
    expect(definition.documentationUrl).toBeNull();
  });
});