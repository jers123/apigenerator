import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CATEGORY_LABELS, GroupForm } from './group-form';
import { DependencyCategory } from '../catalog.models';
import { DependencyRows } from '../dependency-rows/dependency-rows';

/**
 * Editor de un grupo de dependencias y de sus artefactos.
 *
 * No guarda nada: edita el formulario que recibe, y quien lo usa decide
 * cuándo enviarlo. Sirve para los grupos independientes y para el grupo de
 * driver de un motor de base de datos.
 */
@Component({
  imports: [ReactiveFormsModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatSelectModule, DependencyRows],
  selector: 'app-group-editor',
  styleUrl: './group-editor.css',
  templateUrl: './group-editor.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupEditor {
  /** Formulario a editar; se crea con `groupForm`. */
  readonly form = input.required<GroupForm>();

  /** Categorías permitidas; si es una sola, se muestra fija. */
  readonly categories = input.required<readonly DependencyCategory[]>();

  /** Si el id no se puede cambiar, como al editar un grupo existente. */
  readonly idLocked = input(false);

  /** Nombre legible de cada categoría. */
  protected readonly categoryLabels = CATEGORY_LABELS;
}