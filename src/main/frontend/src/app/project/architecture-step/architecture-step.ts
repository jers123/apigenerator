import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatRadioModule } from '@angular/material/radio';
import { ArchitectureStyle, ArchitectureStyleInfo, Compatibility, DesignPatternInfo, ModuleLayout, ModuleLayoutInfo, PatternFamily } from '../project.models';
import { ArchitectureForm } from '../project-form';
import { ProjectApi } from '../project-api';
import { problemMessage } from '../../shared/problem-detail';

/** Patrones de una misma familia, tal como se muestran. */
interface PatternSection {
  /** Familia. */
  readonly family: PatternFamily;
  /** Nombre legible de la familia. */
  readonly label: string;
  /** Patrones de la familia, en el orden del catálogo. */
  readonly patterns: readonly DesignPatternInfo[];
}

/** Texto de cada marca de compatibilidad. */
const COMPATIBILITY_LABELS: Readonly<Record<Compatibility, string>> = {
  NATIVE: 'Incluido por el estilo',
  COMPATIBLE: 'Opcional',
  CONTRAINDICATED: 'Contraindicado',
  NOT_APPLICABLE: 'No aplica',
};

/**
 * Paso "Arquitectura" del asistente: estilo, reparto en módulos y patrones.
 *
 * Muestra todos los patrones, también los que no se pueden elegir, con su
 * compatibilidad con el estilo actual. Solo guarda en el formulario los
 * opcionales que marca el usuario; los nativos los agrega el servidor.
 */
@Component({
  imports: [MatButtonToggleModule, MatCheckboxModule, MatProgressBarModule, MatRadioModule],
  selector: 'app-architecture-step',
  styleUrl: './architecture-step.css',
  templateUrl: './architecture-step.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArchitectureStep {
  /** Formulario del paso. */
  readonly form = input.required<ArchitectureForm>();

  /** Cliente de la API del proyecto. */
  private readonly projectApi = inject(ProjectApi);

  /** Texto de cada marca de compatibilidad. */
  protected readonly compatibilityLabels = COMPATIBILITY_LABELS;

  /** Catálogo de estilos y patrones. */
  protected readonly catalog = rxResource({ stream: () => this.projectApi.architectures() });

  /** Estilos, o vacía mientras carga. */
  protected readonly styles = computed<readonly ArchitectureStyleInfo[]>(() =>
    this.catalog.hasValue() ? this.catalog.value().styles : [],
  );

  /** Repartos en módulos, o vacía mientras carga. */
  protected readonly layouts = computed<readonly ModuleLayoutInfo[]>(() =>
    this.catalog.hasValue() ? this.catalog.value().layouts : [],
  );

  /** Todos los patrones, o vacía mientras carga. */
  private readonly patterns = computed<readonly DesignPatternInfo[]>(() =>
    this.catalog.hasValue() ? this.catalog.value().patterns : [],
  );

  /** Patrones agrupados por familia, en el orden del catálogo. */
  protected readonly sections = computed<readonly PatternSection[]>(() => {
    const byFamily = new Map<PatternFamily, DesignPatternInfo[]>();
    for (const pattern of this.patterns()) {
      byFamily.set(pattern.family, [...(byFamily.get(pattern.family) ?? []), pattern]);
    }
    return [...byFamily].map(([family, patterns]) => ({ family, label: patterns[0].familyLabel, patterns }));
  });

  /** Aviso de los patrones quitados al cambiar de estilo, o `null`. */
  protected readonly removedNotice = signal<string | null>(null);

  /** Mensaje si el catálogo no se pudo cargar. */
  protected readonly loadError = computed(() => {
    const error = this.catalog.error();
    return error ? problemMessage(error) : null;
  });

  /**
   * Estilo elegido.
   * @returns id del estilo del formulario
   */
  protected style(): ArchitectureStyle {
    return this.form().controls.style.value;
  }

  /**
   * Datos del estilo elegido.
   * @returns el estilo del catálogo, o `undefined` mientras carga
   */
  protected styleInfo(): ArchitectureStyleInfo | undefined {
    return this.styles().find((style) => style.id === this.style());
  }

  /**
   * Compatibilidad de un patrón con el estilo elegido.
   * @param pattern patrón del catálogo
   * @returns su marca para ese estilo
   */
  protected compatibilityOf(pattern: DesignPatternInfo): Compatibility {
    return pattern.compatibility[this.style()];
  }

  /**
   * Indica si el usuario puede marcar o desmarcar un patrón.
   * @param pattern patrón del catálogo
   * @returns `true` solo para los opcionales
   */
  protected isSelectable(pattern: DesignPatternInfo): boolean {
    return this.compatibilityOf(pattern) === 'COMPATIBLE';
  }

  /**
   * Indica si un patrón quedará en el proyecto.
   * @param pattern patrón del catálogo
   * @returns `true` si es nativo, o si es opcional y está marcado
   */
  protected isChecked(pattern: DesignPatternInfo): boolean {
    const compatibility = this.compatibilityOf(pattern);
    return (
      compatibility === 'NATIVE' ||
      (compatibility === 'COMPATIBLE' && this.form().controls.patterns.value.includes(pattern.id))
    );
  }

  /**
   * Marca o desmarca un patrón opcional.
   * @param pattern patrón del catálogo
   * @param checked nuevo estado de la casilla
   */
  protected togglePattern(pattern: DesignPatternInfo, checked: boolean): void {
    const control = this.form().controls.patterns;
    const others = control.value.filter((id) => id !== pattern.id);
    control.setValue(checked ? [...others, pattern.id] : others);
  }

  /**
   * Cambia de estilo: quita los patrones que dejan de ser opcionales, avisa
   * de los que ya no se pueden usar y vuelve a un solo módulo si el estilo no
   * admite varios.
   * @param style estilo nuevo
   */
  protected chooseStyle(style: ArchitectureStyle): void {
    const controls = this.form().controls;
    controls.style.setValue(style);

    const chosen = this.patterns().filter((pattern) => controls.patterns.value.includes(pattern.id));
    const lost = chosen.filter((pattern) => !['NATIVE', 'COMPATIBLE'].includes(pattern.compatibility[style]));
    controls.patterns.setValue(
      chosen.filter((pattern) => pattern.compatibility[style] === 'COMPATIBLE').map((pattern) => pattern.id),
    );
    this.removedNotice.set(
      lost.length > 0
        ? `Se quitaron porque no se pueden usar con este estilo: ${lost.map((pattern) => pattern.label).join(', ')}.`
        : null,
    );

    if (!this.styleInfo()?.multiModule) {
      controls.layout.setValue('SINGLE_MODULE');
    }
  }

  /**
   * Cambia el reparto en módulos.
   * @param layout reparto elegido
   */
  protected chooseLayout(layout: ModuleLayout): void {
    this.form().controls.layout.setValue(layout);
  }
}