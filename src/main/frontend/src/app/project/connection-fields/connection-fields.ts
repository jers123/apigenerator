import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { applyEngine, ConnectionForm, jdbcUrlPreview } from "../project-form";
import { DatabaseEngine } from "../../database/database.models";

/** Campos de una conexión a base de datos, con la URL JDBC que resultará. */
@Component({
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  selector: 'app-connection-fields',
  styleUrl: './connection-fields.css',
  templateUrl: './connection-fields.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConnectionFields {
  /** Conexión a editar; debe estar habilitada. */
  readonly connection = input.required<ConnectionForm>();

  /** Motores que se pueden elegir. */
  readonly engines = input.required<readonly DatabaseEngine[]>();

  /** Prefijo de los `data-testid`, para distinguir desarrollo de pruebas. */
  readonly testIdPrefix = input.required<string>();

  /**
   * Motor elegido.
   * @returns el motor, o `undefined` si aún no hay uno
   */
  protected engine(): DatabaseEngine | undefined {
    return this.engines().find((engine) => engine.id === this.connection().controls.engineId.value);
  }

  /**
   * Cambia de motor y copia sus valores por defecto.
   * @param id id del motor elegido
   */
  protected chooseEngine(id: string): void {
    const engine = this.engines().find((candidate) => candidate.id === id);
    if (engine) {
      applyEngine(this.connection(), engine);
    }
  }

  /**
   * URL JDBC con los valores escritos.
   * @returns la URL, o `null` si no hay motor
   */
  protected preview(): string | null {
    return jdbcUrlPreview(this.connection(), this.engine());
  }
}