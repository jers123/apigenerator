import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { DependencyStep } from './dependency-step';
import { projectForm } from "../project-form";
import { Catalog, Dependency, DependencyCategory, DependencyDefinition, DependencyGroup } from "../../catalog/catalog.models";

describe('DependencyStep', () => {
  let http: HttpTestingController;

  /** Dependencia de prueba; por defecto, una librería que no sigue a Spring Boot y sin versión. */
  const dep = (name: string, extra: Partial<Dependency> = {}): Dependency => ({
    name, groupId: 'g', artifactId: name.toLowerCase(), version: null, kind: 'LIBRARY',
    mavenScope: 'COMPILE', gradleScope: 'IMPLEMENTATION', includedByDefault: true, reactive: false,
    followsSpringBoot: false, ...extra,
  });

  const group = (id: string, name: string, category: DependencyCategory, deps: Dependency[],
                 extra: Partial<DependencyGroup> = {}): DependencyGroup => ({
    id, name, category, categoryLabel: category, selectedByDefault: false, requiresDatabase: false,
    standardDependencies: deps, reactiveDependencies: deps, ...extra,
  });

  const webmvc = dep('WebMVC', { followsSpringBoot: true });
  const webflux = dep('WebFlux', { followsSpringBoot: true, includedByDefault: false, reactive: true });
  const lombok = dep('Lombok');

  const catalog: Catalog = {
    groups: [
      group('maven', 'Maven', 'MAVEN', [dep('Plugin')], { selectedByDefault: true }),
      group('spring-web', 'Spring Web', 'SPRING', [webmvc], { selectedByDefault: true, reactiveDependencies: [webflux] }),
      group('spring-data', 'Spring Data', 'SPRING', [dep('JPA', { followsSpringBoot: true })], { requiresDatabase: true }),
      group('lombok', 'Lombok', 'OTHER', [lombok]),
      group('postgresql-driver', 'PostgreSQL Driver', 'DATABASE', [dep('PostgreSQL')]),
      group('old-security', 'Seguridad vieja', 'OTHER', [
        dep('Security', { groupId: 'org.springframework.boot', artifactId: 'spring-boot-starter-security' }),
      ]),
    ],
  };

  /** Librería de seguridad tal como llega del paso "Seguridad". */
  const securityLibrary = (name: string, artifactId: string): DependencyDefinition => ({
    name, groupId: 'org.springframework.boot', artifactId, version: null, kind: 'LIBRARY',
    mavenScope: 'COMPILE', gradleScope: 'IMPLEMENTATION', includedByDefault: true, reactive: false,
  });

  const SECURITY_ARTIFACTS = [
    'org.springframework.boot:spring-boot-starter-security',
    'org.springframework.boot:spring-boot-starter-security-test',
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Deja terminar las microtareas de los resource y ejecuta effects y detección de cambios. */
  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    TestBed.tick();
  }

  /** Crea el paso con sus entradas y responde la carga del catálogo. */
  async function render(options: {
    driver?: string | null;
    reactive?: boolean;
    securityArtifacts?: string[];
    securityDependencies?: DependencyDefinition[];
    modelGroupIds?: string[];
  } = {}) {
    const form = projectForm().controls.dependencies;
    const fixture = TestBed.createComponent(DependencyStep);
    fixture.componentRef.setInput('form', form);
    fixture.componentRef.setInput('buildTool', 'MAVEN');
    fixture.componentRef.setInput('reactive', options.reactive ?? false);
    fixture.componentRef.setInput('databaseDriver', options.driver ?? null);
    fixture.componentRef.setInput('securityArtifacts', options.securityArtifacts ?? []);
    fixture.componentRef.setInput('securityDependencies', options.securityDependencies ?? []);
    fixture.componentRef.setInput('modelGroupIds', options.modelGroupIds ?? []);
    TestBed.tick();
    http.expectOne('/api/v1/catalog').flush(catalog);
    await settle();
    return { fixture, form };
  }

  const resolveCall = () => http.expectOne('/api/v1/catalog/resolve');
  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const checkbox = (fixture: { nativeElement: unknown }, id: string) =>
    page(fixture).querySelector<HTMLInputElement>(`[data-testid="group-${id}"] input[type="checkbox"]`)!;
  const text = (fixture: { nativeElement: unknown }, testId: string) =>
    page(fixture).querySelector(`[data-testid="${testId}"]`)?.textContent ?? '';

  it('resuelve con los grupos por defecto y los guarda en el formulario', async () => {
    const { form } = await render();

    const call = resolveCall();
    expect(call.request.body).toEqual({ groupIds: ['spring-web'], buildTool: 'MAVEN', reactive: false });
    call.flush({ groupIds: ['maven', 'spring-web'], dependencies: [webmvc] });
    await settle();

    expect(form.controls.groupIds.value).toEqual(['spring-web']);
    expect(form.controls.versions.value).toEqual({});
  });

  it('marca y bloquea el driver de la base elegida, sin guardarlo en el formulario', async () => {
    const { fixture, form } = await render({ driver: 'postgresql-driver' });

    const call = resolveCall();
    expect(call.request.body).toMatchObject({ groupIds: ['postgresql-driver', 'spring-web'] });
    call.flush({ groupIds: ['postgresql-driver', 'spring-web'], dependencies: [webmvc] });
    await settle();

    expect(checkbox(fixture, 'postgresql-driver').checked).toBe(true);
    expect(checkbox(fixture, 'postgresql-driver').disabled).toBe(true);
    expect(text(fixture, 'group-postgresql-driver')).toContain('Incluido por la base de datos elegida');
    expect(checkbox(fixture, 'spring-data').disabled).toBe(false);
    expect(form.controls.groupIds.value).toEqual(['spring-web']);
  });

  it('sin base de datos bloquea los grupos que la necesitan y explica por qué', async () => {
    const { fixture } = await render();
    resolveCall().flush({ groupIds: [], dependencies: [] });
    await settle();

    expect(checkbox(fixture, 'spring-data').disabled).toBe(true);
    expect(text(fixture, 'group-spring-data')).toContain('Necesita una base de datos de desarrollo');
    expect(checkbox(fixture, 'postgresql-driver').checked).toBe(false);
  });

  it('usa el modo reactivo del paso Proyecto y vuelve a resolver si cambia', async () => {
    const { fixture } = await render({ reactive: true });
    const first = resolveCall();
    expect(first.request.body).toMatchObject({ reactive: true });
    first.flush({ groupIds: [], dependencies: [] });
    await settle();
    expect(text(fixture, 'group-spring-web')).toContain('WebFlux');

    fixture.componentRef.setInput('reactive', false);
    await settle();

    const second = resolveCall();
    expect(second.request.body).toMatchObject({ reactive: false });
    second.flush({ groupIds: [], dependencies: [] });
    await settle();
  });

  it('ofrece "Gestionada" cuando el catálogo no fija versión y guarda solo lo que cambia el usuario', async () => {
    const { fixture, form } = await render();
    resolveCall().flush({ groupIds: ['spring-web'], dependencies: [webmvc] });
    await settle();

    checkbox(fixture, 'lombok').click();
    await settle();
    const call = resolveCall();
    expect(call.request.body).toMatchObject({ groupIds: ['lombok', 'spring-web'] });
    call.flush({ groupIds: ['lombok', 'spring-web'], dependencies: [webmvc, lombok] });
    await settle();
    http.expectOne('/api/v1/versions').flush({
      artifacts: [{ groupId: 'g', artifactId: 'lombok', latest: '1.18.42', versions: ['1.18.42', '1.18.40'], available: true }],
    });
    await fixture.whenStable();

    expect(text(fixture, 'version-lombok')).toContain('Gestionada por Spring Boot');
    expect(form.controls.versions.value).toEqual({});

    document.querySelector<HTMLElement>('[data-testid="version-lombok"] .mat-mdc-select-trigger')!.click();
    await fixture.whenStable();
    const options = [...document.querySelectorAll<HTMLElement>('mat-option')];
    expect(options.map((o) => o.textContent?.trim())).toEqual(['Gestionada por Spring Boot', '1.18.42', '1.18.40']);
    options[1].click();
    await fixture.whenStable();

    expect(form.controls.versions.value).toEqual({ 'g:lombok': '1.18.42' });
  });

  it('al desmarcar un grupo descarta la versión elegida para sus artefactos', async () => {
    const { fixture, form } = await render();
    resolveCall().flush({ groupIds: ['spring-web'], dependencies: [webmvc] });
    await settle();
    checkbox(fixture, 'lombok').click();
    await settle();
    resolveCall().flush({ groupIds: ['lombok', 'spring-web'], dependencies: [webmvc, lombok] });
    await settle();
    http.expectOne('/api/v1/versions').flush({
      artifacts: [{ groupId: 'g', artifactId: 'lombok', latest: '1.18.42', versions: ['1.18.42'], available: true }],
    });
    await fixture.whenStable();
    document.querySelector<HTMLElement>('[data-testid="version-lombok"] .mat-mdc-select-trigger')!.click();
    await fixture.whenStable();
    [...document.querySelectorAll<HTMLElement>('mat-option')].find((o) => o.textContent?.trim() === '1.18.42')!.click();
    await fixture.whenStable();
    expect(form.controls.versions.value).toEqual({ 'g:lombok': '1.18.42' });

    checkbox(fixture, 'lombok').click();
    await settle();
    resolveCall().flush({ groupIds: ['spring-web'], dependencies: [webmvc] });
    await settle();

    expect(form.controls.groupIds.value).toEqual(['spring-web']);
    expect(form.controls.versions.value).toEqual({});
  });

  it('muestra el detalle del ProblemDetail cuando el servidor rechaza la selección', async () => {
    const { fixture } = await render();
    resolveCall().flush(
      { title: 'Grupo de dependencias inexistente', status: 400, detail: 'Grupos inexistentes en el catálogo: [kafka]' },
      { status: 400, statusText: 'Bad Request' },
    );
    await settle();

    expect(text(fixture, 'error')).toContain('kafka');
  });

  it('avisa, sin romperse, si el backend no responde al cargar el catálogo', async () => {
    const fixture = TestBed.createComponent(DependencyStep);
    fixture.componentRef.setInput('form', projectForm().controls.dependencies);
    fixture.componentRef.setInput('buildTool', 'MAVEN');
    fixture.componentRef.setInput('reactive', false);
    TestBed.tick();
    http.expectOne('/api/v1/catalog').error(new ProgressEvent('error'), { status: 0 });
    await settle();

    expect(text(fixture, 'error')).toContain('backend');
  });

  it('bloquea los grupos del catálogo que traen librerías de seguridad', async () => {
    const { fixture } = await render({ securityArtifacts: SECURITY_ARTIFACTS });
    resolveCall().flush({ groupIds: ['spring-web'], dependencies: [webmvc] });
    await settle();

    expect(checkbox(fixture, 'old-security').disabled).toBe(true);
    expect(text(fixture, 'group-old-security')).toContain('se eligen en «Seguridad»');
  });

  it('sin métodos de seguridad muestra el grupo Spring Security desmarcado y bloqueado', async () => {
    const { fixture } = await render({ securityArtifacts: SECURITY_ARTIFACTS });
    resolveCall().flush({ groupIds: ['spring-web'], dependencies: [webmvc] });
    await settle();

    expect(checkbox(fixture, 'security').checked).toBe(false);
    expect(checkbox(fixture, 'security').disabled).toBe(true);
    expect(text(fixture, 'group-security')).toContain('Se agrega al elegir un método en «Seguridad»');
    expect(page(fixture).querySelector('[data-testid="security-dependencies"]')).toBeNull();
  });

  it('con métodos de seguridad marca y bloquea el grupo una sola vez, con sus artefactos', async () => {
    const { fixture } = await render({
      securityArtifacts: SECURITY_ARTIFACTS,
      securityDependencies: [
        securityLibrary('Spring Security', 'spring-boot-starter-security'),
        securityLibrary('Spring Security Test', 'spring-boot-starter-security-test'),
      ],
    });
    resolveCall().flush({ groupIds: ['spring-web'], dependencies: [webmvc] });
    await settle();

    expect(page(fixture).querySelectorAll('[data-testid="group-security"]').length).toBe(1);
    expect(checkbox(fixture, 'security').checked).toBe(true);
    expect(checkbox(fixture, 'security').disabled).toBe(true);
    expect(text(fixture, 'group-security')).toContain('Incluido por la seguridad elegida');
    expect(text(fixture, 'group-security')).toContain('Spring Security Test');
    expect(text(fixture, 'security-dependencies')).toContain('Spring Security');
  });

  it('marca y bloquea los grupos del modelo, los resuelve y no los guarda en el formulario', async () => {
    const { fixture, form } = await render({ driver: 'postgresql-driver', modelGroupIds: ['spring-data'] });

    const call = resolveCall();
    expect(call.request.body).toMatchObject({ groupIds: ['postgresql-driver', 'spring-data', 'spring-web'] });
    call.flush({ groupIds: ['postgresql-driver', 'spring-data', 'spring-web'], dependencies: [webmvc] });
    await settle();

    expect(checkbox(fixture, 'spring-data').checked).toBe(true);
    expect(checkbox(fixture, 'spring-data').disabled).toBe(true);
    expect(text(fixture, 'group-spring-data')).toContain('Incluido por el modelo');
    expect(form.controls.groupIds.value).toEqual(['spring-web']);
  });
});