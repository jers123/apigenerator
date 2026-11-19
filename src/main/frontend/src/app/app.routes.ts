import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'project' },
  {
    path: 'project',
    title: 'Nuevo proyecto · API Generator',
    loadComponent: () => import('./project/project-wizard/project-wizard').then((m) => m.ProjectWizard),
  },
  {
    path: 'groups',
    title: 'Grupos · API Generator',
    loadComponent: () => import('./catalog/group-admin/group-admin').then((m) => m.GroupAdmin),
  },
  {
    path: 'databases',
    title: 'Bases de datos · API Generator',
    loadComponent: () => import('./database/engine-admin/engine-admin').then((m) => m.EngineAdmin),
  },
  {
    path: 'security',
    title: 'Seguridad · API Generator',
    loadComponent: () => import('./security/security-admin/security-admin').then((m) => m.SecurityAdmin),
  },
  { path: '**', redirectTo: 'project' },
];