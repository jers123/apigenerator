import { TestBed } from "@angular/core/testing";
import { ConnectionFields } from "./connection-fields";
import { ConnectionForm, projectForm, setConnectionEnabled } from "../project-form";
import { DatabaseEngine } from "../../database/database.models";

describe('ConnectionFields', () => {
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

  /** Conexión de desarrollo habilitada y sin motor, como la deja el paso al activarla. */
  function enabledConnection(): ConnectionForm {
    const connection = projectForm().controls.databases.controls.database;
    setConnectionEnabled(connection, true, undefined);
    return connection;
  }

  /** Crea el componente con sus tres inputs obligatorios. */
  async function render(connection: ConnectionForm) {
    const fixture = TestBed.createComponent(ConnectionFields);
    fixture.componentRef.setInput('connection', connection);
    fixture.componentRef.setInput('engines', [postgresql, h2]);
    fixture.componentRef.setInput('testIdPrefix', 'database');
    await fixture.whenStable();
    return fixture;
  }

  const page = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;

  async function chooseEngine(fixture: { whenStable(): Promise<unknown> }, label: string): Promise<void> {
    document.querySelector<HTMLElement>('[data-testid="database-engine"] .mat-mdc-select-trigger')!.click();
    await fixture.whenStable();
    [...document.querySelectorAll<HTMLElement>('mat-option')].find((o) => o.textContent?.includes(label))!.click();
    await fixture.whenStable();
  }

  it('sin motor elegido no muestra URL JDBC', async () => {
    const fixture = await render(enabledConnection());

    expect(page(fixture).querySelector('[data-testid="database-url"]')).toBeNull();
  });

  it('al elegir un motor con servidor copia sus valores y muestra la URL', async () => {
    const connection = enabledConnection();
    const fixture = await render(connection);

    await chooseEngine(fixture, 'PostgreSQL');

    expect(connection.getRawValue()).toMatchObject({ engineId: 'postgresql', port: 5432, username: 'postgres' });
    expect(page(fixture).querySelector('[data-testid="database-host"]')).not.toBeNull();
    expect(page(fixture).querySelector('[data-testid="database-url"]')?.textContent)
      .toContain('jdbc:postgresql://localhost:5432/app');
  });

  it('al elegir un motor embebido oculta servidor y puerto', async () => {
    const connection = enabledConnection();
    const fixture = await render(connection);

    await chooseEngine(fixture, 'H2');

    expect(page(fixture).querySelector('[data-testid="database-host"]')).toBeNull();
    expect(page(fixture).querySelector('[data-testid="database-port"]')).toBeNull();
    expect(page(fixture).querySelector('[data-testid="database-url"]')?.textContent).toContain('jdbc:h2:mem:app');
  });

  it('marca embebido en el nombre del motor dentro del selector', async () => {
    const fixture = await render(enabledConnection());

    document.querySelector<HTMLElement>('[data-testid="database-engine"] .mat-mdc-select-trigger')!.click();
    await fixture.whenStable();

    expect([...document.querySelectorAll('mat-option')].map((o) => o.textContent?.trim()))
      .toEqual(['PostgreSQL', 'H2 (embebida)']);
  });
});