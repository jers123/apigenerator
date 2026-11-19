import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { SecurityAdmin } from './security-admin';

describe('SecurityAdmin', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('muestra las pestañas Métodos y Proveedores y solo carga la abierta', async () => {
    const fixture = TestBed.createComponent(SecurityAdmin);
    TestBed.tick();
    await new Promise((resolve) => setTimeout(resolve));
    TestBed.tick();

    const tabs = [...(fixture.nativeElement as HTMLElement).querySelectorAll('[role="tab"]')]
      .map((tab) => tab.textContent?.trim());
    expect(tabs).toEqual(['Métodos', 'Proveedores']);

    http.expectOne('/api/v1/security/mechanisms').flush([]);
    http.expectNone('/api/v1/security/identity-providers');
  });
});