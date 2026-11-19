import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideRouter } from '@angular/router';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('muestra la barra superior con el nombre de la herramienta', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('mat-toolbar')?.textContent).toContain('API Generator');
  });

  it('enlaza las secciones desde la barra superior', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const links = [...(fixture.nativeElement as HTMLElement).querySelectorAll('nav a')]
      .map((link) => link.getAttribute('href'));
    expect(links).toEqual(['/project', '/databases', '/groups', '/security']);
  });
});