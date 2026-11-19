import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { rxResource } from '@angular/core/rxjs-interop';
import { ConnectionFields } from '../connection-fields/connection-fields';
import { applyEngine, ConnectionForm, DatabasesForm, setConnectionEnabled } from '../project-form';
import { DatabaseApi } from '../../database/database-api';
import { DatabaseEngine } from '../../database/database.models';
import { problemMessage } from '../../shared/problem-detail';

/**
 * Paso "Bases de datos": desarrollo y pruebas, ambas opcionales. La de
 * pruebas solo existe si hay de desarrollo.
 */
@Component({
  imports: [MatProgressBarModule, MatSlideToggleModule, ConnectionFields],
  selector: 'app-database-step',
  styleUrl: './database-step.css',
  templateUrl: './database-step.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DatabaseStep {
  /** Formulario del paso. */
  readonly form = input.required<DatabasesForm>();

  /** Cliente de la API de motores. */
  private readonly databaseApi = inject(DatabaseApi);

  /** Motores registrados. */
  protected readonly engines = rxResource({ stream: () => this.databaseApi.list() });

  /** Motores para los selectores, o vacía mientras carga. */
  protected readonly engineList = computed<readonly DatabaseEngine[]>(() =>
    this.engines.hasValue() ? this.engines.value() : [],
  );

  /** Mensaje si los motores no se pudieron cargar. */
  protected readonly loadError = computed(() => {
    const error = this.engines.error();
    return error ? problemMessage(error) : null;
  });

  /**
   * Activa o desactiva la base de desarrollo. Sin ella tampoco hay base de pruebas.
   * @param enabled si se usa
   */
  protected toggleDatabase(enabled: boolean): void {
    const controls = this.form().controls;
    controls.useDatabase.setValue(enabled);
    this.toggleConnection(controls.database, enabled);
    if (!enabled) {
      this.toggleTestDatabase(false);
    }
  }

  /**
   * Activa o desactiva la base de pruebas; solo se activa si hay base de desarrollo.
   * @param enabled si se usa
   */
  protected toggleTestDatabase(enabled: boolean): void {
    const controls = this.form().controls;
    const allowed = enabled && controls.useDatabase.value;
    controls.useTestDatabase.setValue(allowed);
    this.toggleConnection(controls.testDatabase, allowed);
  }

  /**
   * Habilita o deshabilita una conexión y, si se habilita sin motor, propone
   * el primero de la lista.
   * @param connection conexión
   * @param enabled si se usa
   */
  private toggleConnection(connection: ConnectionForm, enabled: boolean): void {
    const engine = this.engineList().find((candidate) => candidate.id === connection.controls.engineId.value);
    setConnectionEnabled(connection, enabled, engine);
    const first = this.engineList().at(0);
    if (enabled && !engine && first) {
      applyEngine(connection, first);
    }
  }
}