import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PendingProfileChangesService } from '../../../core/services/pending-profile-changes.service';
import { ConfirmDialogComponent } from '../../../shared/ui/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, ConfirmDialogComponent],
  template: `
    <header class="topbar card">
      <div class="topbar__left">
        <img src="assets/images/al-toro-logo-vector.svg" alt="Al Toro Gastrobar" class="topbar-logo" />
        <span class="topbar-user" *ngIf="authService.currentUser()?.fullName as fullName">{{ fullName }}</span>
      </div>

      <button type="button" class="btn-secondary" (click)="onLogout()">Cerrar sesión</button>
    </header>

    <app-confirm-dialog
      [open]="showUnsavedDialog()"
      title="Cambios sin guardar"
      message="Tienes cambios sin guardar. ¿Estás seguro de que deseas salir?"
      cancelLabel="No, continuar editando"
      confirmLabel="Sí, salir"
      (cancel)="showUnsavedDialog.set(false)"
      (confirm)="onConfirmUnsavedExit()"
    ></app-confirm-dialog>

    <app-confirm-dialog
      [open]="showLogoutDialog()"
      title="Cerrar sesión"
      message="¿Deseas cerrar sesión?"
      cancelLabel="No"
      confirmLabel="Sí, cerrar sesión"
      (cancel)="showLogoutDialog.set(false)"
      (confirm)="onConfirmLogout()"
    ></app-confirm-dialog>
  `,
  styleUrls: ['./topbar.component.scss']
})
export class TopbarComponent {
  readonly showLogoutDialog = signal(false);
  readonly showUnsavedDialog = signal(false);

  constructor(
    public readonly authService: AuthService,
    private readonly router: Router,
    private readonly pendingChangesService: PendingProfileChangesService
  ) {}

  onLogout(): void {
    if (this.router.url.startsWith('/app/profile') && this.pendingChangesService.hasUnsavedChanges()) {
      this.showUnsavedDialog.set(true);
      return;
    }

    this.showLogoutDialog.set(true);
  }

  onConfirmUnsavedExit(): void {
    this.showUnsavedDialog.set(false);
    this.pendingChangesService.skipNextPrompt();
    this.pendingChangesService.setHasUnsavedChanges(false);
    this.showLogoutDialog.set(true);
  }

  onConfirmLogout(): void {
    this.showLogoutDialog.set(false);

    this.authService.logout().subscribe({
      next: () => void this.router.navigateByUrl('/auth/login', { replaceUrl: true }),
      error: () => void this.router.navigateByUrl('/auth/login', { replaceUrl: true })
    });
  }
}