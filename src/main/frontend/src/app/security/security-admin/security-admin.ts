import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { MechanismAdmin } from '../mechanism-admin/mechanism-admin';
import { ProviderAdmin } from '../provider-admin/provider-admin';

/**
 * Pantalla de seguridad: una pestaña para los métodos y otra para los
 * proveedores de identidad. Cada pestaña carga sus datos al abrirse.
 */
@Component({
  imports: [MatTabsModule, MechanismAdmin, ProviderAdmin],
  selector: 'app-security-admin',
  styleUrl: './security-admin.css',
  templateUrl: './security-admin.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecurityAdmin {}
