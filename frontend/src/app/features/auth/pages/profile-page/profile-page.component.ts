import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, OnDestroy, effect, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { Observable, Subject, takeUntil } from 'rxjs';
import { UpdateProfileRequest } from '../../../../core/models/auth.models';
import { AuthService } from '../../../../core/services/auth.service';
import { PendingProfileChangesService } from '../../../../core/services/pending-profile-changes.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { RoleChipComponent } from '../../../../shared/ui/role-chip/role-chip.component';

interface ProfileSnapshot {
  fullName: string;
  email: string;
  phone: string;
  address: string;
}

const noWhitespaceValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '');
  return value.trim().length === 0 && value.length > 0 ? { whitespace: true } : null;
};

const fullNameFormatValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '').trim();

  if (!value) {
    return null;
  }

  return /^[\p{L}\s]+$/u.test(value) ? null : { invalidNameFormat: true };
};

const passwordComplexityValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '');

  if (!value) {
    return null;
  }

  if (value.length < 8) {
    return { invalidPasswordComplexity: true };
  }

  const hasUppercase = /[A-Z]/.test(value);
  const hasLowercase = /[a-z]/.test(value);
  const hasNumber = /\d/.test(value);
  const hasSpecialChar = /[^A-Za-z0-9]/.test(value);

  return hasUppercase && hasLowercase && hasNumber && hasSpecialChar
    ? null
    : { invalidPasswordComplexity: true };
};

const confirmPasswordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const newPassword = String(control.get('newPassword')?.value ?? '');
  const confirmNewPassword = String(control.get('confirmNewPassword')?.value ?? '');

  if (!newPassword && !confirmNewPassword) {
    return null;
  }

  return newPassword === confirmNewPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, RoleChipComponent, ConfirmDialogComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Mi perfil" subtitle="Modifica tus datos y credenciales de acceso"></app-page-header>

      <article class="card profile-card">
        <app-role-chip [role]="authService.currentUser()?.role ?? 'CLIENTE'"></app-role-chip>

        <form class="form-grid form-compact profile-form" [formGroup]="profileForm" (ngSubmit)="onSubmit()" novalidate>
          <label class="form-label">
            <span>Nombre completo *</span>
            <input class="input-field" formControlName="fullName" [class.input-error]="showNameAnyError()" />
            <p class="error-text" *ngIf="showNameRequiredError()">Este campo es obligatorio</p>
            <p class="error-text" *ngIf="showNameWhitespaceError()">Este campo no puede estar vacío</p>
            <p class="error-text" *ngIf="showNameFormatError()">El nombre solo puede contener letras y espacios</p>
          </label>

          <label class="form-label">
            <span>Correo electrónico *</span>
            <input
              class="input-field"
              type="email"
              formControlName="email"
              (input)="onEmailInput()"
              [class.input-error]="showEmailAnyError()"
            />
            <p class="error-text" *ngIf="showEmailRequiredError()">Este campo es obligatorio</p>
            <p class="error-text" *ngIf="showEmailWhitespaceError()">Este campo no puede estar vacío</p>
            <p class="error-text" *ngIf="showEmailFormatError()">Ingresa un correo electrónico válido (ej. nombre&#64;dominio.com)</p>
            <p class="error-text" *ngIf="showEmailDuplicateError()">
              Este correo electrónico ya está en uso por otro usuario. Por favor utiliza otro
            </p>
          </label>

          <label class="form-label">
            <span>Teléfono *</span>
            <input
              class="input-field"
              formControlName="phone"
              maxlength="10"
              (input)="onPhoneInput()"
              [class.input-error]="showPhoneAnyError()"
            />
            <p class="error-text" *ngIf="showPhoneRequiredError()">Este campo es obligatorio</p>
            <p class="error-text" *ngIf="showPhoneWhitespaceError()">Este campo no puede estar vacío</p>
            <p class="error-text" *ngIf="showPhoneFormatError()">Ingresa un número de teléfono válido de 10 dígitos</p>
            <p class="error-text" *ngIf="showPhoneDuplicateError()">
              Este número de teléfono ya está en uso por otro usuario. Por favor utiliza otro
            </p>
          </label>

          <label class="form-label">
            <span>Dirección</span>
            <input class="input-field" formControlName="address" />
          </label>

          <button class="btn-secondary" type="button" (click)="togglePasswordChange()">
            {{ changingPassword() ? 'Ocultar cambio de contraseña' : 'Cambiar contraseña' }}
          </button>

          <section class="password-fields" *ngIf="changingPassword()">
            <label class="form-label">
              <span>Contraseña actual *</span>
              <input
                class="input-field"
                type="password"
                formControlName="currentPassword"
                (input)="onCurrentPasswordInput()"
                [class.input-error]="showCurrentPasswordAnyError()"
              />
              <p class="error-text" *ngIf="showCurrentPasswordRequiredError()">Este campo es obligatorio</p>
              <p class="error-text" *ngIf="showCurrentPasswordWhitespaceError()">Este campo no puede estar vacío</p>
              <p class="error-text" *ngIf="showCurrentPasswordInvalidError()">La contraseña actual no es correcta</p>
            </label>

            <label class="form-label">
              <span>Nueva contraseña *</span>
              <input
                class="input-field"
                type="password"
                formControlName="newPassword"
                [class.input-error]="showNewPasswordAnyError()"
              />
              <p class="error-text" *ngIf="showNewPasswordRequiredError()">Este campo es obligatorio</p>
              <p class="error-text" *ngIf="showNewPasswordWhitespaceError()">Este campo no puede estar vacío</p>
              <p class="error-text" *ngIf="showNewPasswordComplexityError()">
                La contraseña debe tener al menos 8 caracteres, incluir mayuscula, minuscula y caracter especial
              </p>
            </label>

            <label class="form-label">
              <span>Confirmar nueva contraseña *</span>
              <input
                class="input-field"
                type="password"
                formControlName="confirmNewPassword"
                [class.input-error]="showConfirmPasswordAnyError()"
              />
              <p class="error-text" *ngIf="showConfirmPasswordRequiredError()">Este campo es obligatorio</p>
              <p class="error-text" *ngIf="showConfirmPasswordWhitespaceError()">Este campo no puede estar vacío</p>
              <p class="error-text" *ngIf="showConfirmPasswordMismatchError()">Las contraseñas no coinciden</p>
            </label>
          </section>

          <div class="action-row">
            <button class="btn-secondary" type="button" (click)="onCancel()">Cancelar</button>
            <button class="btn-primary" type="submit" [disabled]="loading()">
              {{ loading() ? 'Guardando...' : 'Guardar cambios' }}
            </button>
          </div>

          <p class="form-success" *ngIf="successMessage()">{{ successMessage() }}</p>
          <p class="form-error" *ngIf="formMessage()">{{ formMessage() }}</p>
        </form>
      </article>

      <app-confirm-dialog
        [open]="showCancelDialog()"
        title="Cancelar modificación"
        message="¿Estás seguro de que deseas cancelar los cambios?"
        cancelLabel="No, continuar editando"
        confirmLabel="Sí, cancelar"
        (cancel)="showCancelDialog.set(false)"
        (confirm)="onConfirmCancel()"
      ></app-confirm-dialog>

      <app-confirm-dialog
        [open]="showNavigateAwayDialog()"
        title="Cambios sin guardar"
        message="Tienes cambios sin guardar. ¿Estás seguro de que deseas salir?"
        cancelLabel="No, seguir editando"
        confirmLabel="Sí, salir"
        (cancel)="onNavigateAwayCancel()"
        (confirm)="onNavigateAwayConfirm()"
      ></app-confirm-dialog>
    </section>
  `,
  styles: [
    `
      .profile-card {
        padding: 1rem;
        max-width: 700px;
      }

      .profile-form {
        margin-top: 0.8rem;
      }

      .password-fields {
        display: grid;
        gap: 0.52rem;
      }

      .action-row {
        display: grid;
        gap: 0.45rem;
        grid-template-columns: 1fr 1fr;
      }

      .form-success,
      .form-error {
        margin: 0;
        padding: 0.55rem 0.7rem;
        border-radius: 8px;
        font-size: 0.84rem;
      }

      .form-success {
        border: 1px solid rgba(30, 120, 75, 0.35);
        background: rgba(30, 120, 75, 0.09);
        color: #14553a;
      }

      .form-error {
        border: 1px solid var(--primary);
        background: rgba(211, 47, 47, 0.1);
        color: #4d3323;
      }

      @media (max-width: 640px) {
        .action-row {
          grid-template-columns: 1fr;
        }
      }
    `
  ]
})
export class ProfilePageComponent implements OnDestroy {
  readonly loading = signal(false);
  readonly changingPassword = signal(false);
  readonly showCancelDialog = signal(false);
  readonly showNavigateAwayDialog = signal(false);

  private navigateAwaySubject: Subject<boolean> | null = null;
  readonly formMessage = signal('');
  readonly successMessage = signal('');

  private readonly destroy$ = new Subject<void>();
  private initialSnapshot: ProfileSnapshot | null = null;

  readonly profileForm = this.formBuilder.nonNullable.group(
    {
      fullName: ['', [Validators.required, noWhitespaceValidator, fullNameFormatValidator]],
      email: ['', [Validators.required, noWhitespaceValidator, Validators.email]],
      phone: ['', [Validators.required, noWhitespaceValidator, Validators.pattern(/^\d{10}$/)]],
      address: [''],
      currentPassword: [''],
      newPassword: ['', [passwordComplexityValidator]],
      confirmNewPassword: ['']
    },
    { validators: [confirmPasswordMatchValidator] }
  );

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly router: Router,
    private readonly pendingChangesService: PendingProfileChangesService,
    public readonly authService: AuthService
  ) {
    effect(
      () => {
        const user = this.authService.currentUser();
        if (!user) {
          return;
        }

        // Load full profile from backend (includes phone and address)
        this.authService.getMyProfile().subscribe({
          next: (profile) => {
            this.profileForm.patchValue(
              {
                fullName: profile.nombre,
                email: profile.email,
                phone: profile.telefono ?? '',
                address: profile.direccion ?? ''
              },
              { emitEvent: false }
            );

            this.disablePasswordChangeSection();
            this.initialSnapshot = this.captureSnapshot();
            this.profileForm.markAsPristine();
            this.profileForm.markAsUntouched();
            this.pendingChangesService.setHasUnsavedChanges(false);
          },
          error: () => {
            // Fallback to currentUser data
            this.profileForm.patchValue(
              {
                fullName: user.fullName,
                email: user.email,
                phone: user.phone ?? ''
              },
              { emitEvent: false }
            );

            this.disablePasswordChangeSection();
            this.initialSnapshot = this.captureSnapshot();
            this.profileForm.markAsPristine();
            this.profileForm.markAsUntouched();
            this.pendingChangesService.setHasUnsavedChanges(false);
          }
        });
      });

    this.profileForm.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.syncPendingState();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.pendingChangesService.setHasUnsavedChanges(false);
    if (this.navigateAwaySubject) {
      this.navigateAwaySubject.next(true);
      this.navigateAwaySubject.complete();
      this.navigateAwaySubject = null;
    }
  }

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (!this.hasUnsavedChanges()) {
      return;
    }

    event.preventDefault();
    event.returnValue = true;
  }

  canDeactivate(): boolean | Observable<boolean> {
    if (this.pendingChangesService.consumeSkipNextPrompt()) {
      return true;
    }

    if (!this.hasUnsavedChanges()) {
      return true;
    }

    this.navigateAwaySubject = new Subject<boolean>();
    this.showNavigateAwayDialog.set(true);
    return this.navigateAwaySubject.asObservable();
  }

  onNavigateAwayConfirm(): void {
    this.showNavigateAwayDialog.set(false);
    this.navigateAwaySubject?.next(true);
    this.navigateAwaySubject?.complete();
    this.navigateAwaySubject = null;
  }

  onNavigateAwayCancel(): void {
    this.showNavigateAwayDialog.set(false);
    this.navigateAwaySubject?.next(false);
    this.navigateAwaySubject?.complete();
    this.navigateAwaySubject = null;
  }

  onPhoneInput(): void {
    const control = this.profileForm.controls.phone;
    const digitsOnly = control.value.replace(/\D/g, '').slice(0, 10);

    if (digitsOnly !== control.value) {
      control.setValue(digitsOnly);
    }

    this.clearControlError(control, 'duplicatePhone');
  }

  onEmailInput(): void {
    this.clearControlError(this.profileForm.controls.email, 'duplicateEmail');
  }

  onCurrentPasswordInput(): void {
    this.clearControlError(this.profileForm.controls.currentPassword, 'invalidCurrentPassword');
  }

  togglePasswordChange(): void {
    this.successMessage.set('');
    this.formMessage.set('');

    if (this.changingPassword()) {
      this.disablePasswordChangeSection();
    } else {
      this.enablePasswordChangeSection();
    }

    this.syncPendingState();
  }

  onCancel(): void {
    this.formMessage.set('');
    this.successMessage.set('');

    if (!this.hasUnsavedChanges()) {
      this.navigateToDashboard();
      return;
    }

    this.showCancelDialog.set(true);
  }

  onConfirmCancel(): void {
    this.showCancelDialog.set(false);
    this.pendingChangesService.skipNextPrompt();
    this.pendingChangesService.setHasUnsavedChanges(false);
    this.navigateToDashboard();
  }

  onSubmit(): void {
    this.formMessage.set('');
    this.successMessage.set('');
    this.normalizeInputValues();

    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      this.formMessage.set('Revisa los campos del formulario antes de continuar.');
      return;
    }

    const formValue = this.profileForm.getRawValue();
    const isPasswordFlow = this.changingPassword();

    const payload: UpdateProfileRequest = {
      fullName: formValue.fullName,
      email: formValue.email,
      phone: formValue.phone,
      address: formValue.address
    };

    if (isPasswordFlow) {
      payload.currentPassword = formValue.currentPassword;
      payload.newPassword = formValue.newPassword;
      payload.confirmNewPassword = formValue.confirmNewPassword;
    }

    this.loading.set(true);

    if (isPasswordFlow) {
      this.authService.changePassword(
        formValue.currentPassword,
        formValue.newPassword,
        formValue.confirmNewPassword
      ).subscribe({
        next: () => {
          this.loading.set(false);
          this.disablePasswordChangeSection();
          this.profileForm.markAsPristine();
          this.successMessage.set('Contraseña actualizada correctamente');
        },
        error: (error: HttpErrorResponse) => {
          this.loading.set(false);
          const msg = (error.error?.message as string) ?? '';

          if (msg.toLowerCase().includes('actual')) {
            this.setControlError(this.profileForm.controls.currentPassword, 'invalidCurrentPassword');
            return;
          }

          if (msg.toLowerCase().includes('mayuscula') || msg.toLowerCase().includes('caracteres')) {
            this.setControlError(this.profileForm.controls.newPassword, 'invalidPasswordComplexity');
            return;
          }

          if (msg.toLowerCase().includes('coinciden')) {
            this.formMessage.set(msg);
            return;
          }

          this.formMessage.set(msg || 'No fue posible actualizar la contraseña. Intenta nuevamente.');
        }
      });
      return;
    }

    this.authService.updateProfile(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.initialSnapshot = this.captureSnapshot();
        this.pendingChangesService.setHasUnsavedChanges(false);
        this.pendingChangesService.skipNextPrompt();
        this.navigateToDashboard('Datos actualizados correctamente');
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        const code = (error.error?.code as string) ?? '';
        const msg = (error.error?.message as string) ?? '';

        if (code === 'ENT-002' && msg.toLowerCase().includes('correo')) {
          this.setControlError(this.profileForm.controls.email, 'duplicateEmail');
          return;
        }

        if (code === 'ENT-002' && msg.toLowerCase().includes('teléfono')) {
          this.setControlError(this.profileForm.controls.phone, 'duplicatePhone');
          return;
        }

        this.formMessage.set(msg || 'No fue posible actualizar tus datos. Intenta nuevamente.');
      }
    });
  }

  showNameRequiredError(): boolean {
    return this.shouldShowRequired(this.profileForm.controls.fullName);
  }

  showNameWhitespaceError(): boolean {
    const control = this.profileForm.controls.fullName;
    return this.wasInteracted(control) && control.hasError('whitespace');
  }

  showNameFormatError(): boolean {
    const control = this.profileForm.controls.fullName;
    return this.wasInteracted(control) && control.hasError('invalidNameFormat');
  }

  showNameAnyError(): boolean {
    return this.showNameRequiredError() || this.showNameWhitespaceError() || this.showNameFormatError();
  }

  showEmailRequiredError(): boolean {
    return this.shouldShowRequired(this.profileForm.controls.email);
  }

  showEmailWhitespaceError(): boolean {
    const control = this.profileForm.controls.email;
    return this.wasInteracted(control) && control.hasError('whitespace');
  }

  showEmailFormatError(): boolean {
    const control = this.profileForm.controls.email;

    if (!this.wasInteracted(control)) {
      return false;
    }

    if (control.hasError('required') || control.hasError('whitespace')) {
      return false;
    }

    return control.hasError('email');
  }

  showEmailDuplicateError(): boolean {
    const control = this.profileForm.controls.email;
    return this.wasInteracted(control) && control.hasError('duplicateEmail');
  }

  showEmailAnyError(): boolean {
    return (
      this.showEmailRequiredError() ||
      this.showEmailWhitespaceError() ||
      this.showEmailFormatError() ||
      this.showEmailDuplicateError()
    );
  }

  showPhoneRequiredError(): boolean {
    return this.shouldShowRequired(this.profileForm.controls.phone);
  }

  showPhoneWhitespaceError(): boolean {
    const control = this.profileForm.controls.phone;
    return this.wasInteracted(control) && control.hasError('whitespace');
  }

  showPhoneFormatError(): boolean {
    const control = this.profileForm.controls.phone;

    if (!this.wasInteracted(control)) {
      return false;
    }

    if (control.hasError('required') || control.hasError('whitespace')) {
      return false;
    }

    return control.hasError('pattern');
  }

  showPhoneDuplicateError(): boolean {
    const control = this.profileForm.controls.phone;
    return this.wasInteracted(control) && control.hasError('duplicatePhone');
  }

  showPhoneAnyError(): boolean {
    return (
      this.showPhoneRequiredError() ||
      this.showPhoneWhitespaceError() ||
      this.showPhoneFormatError() ||
      this.showPhoneDuplicateError()
    );
  }

  showCurrentPasswordRequiredError(): boolean {
    return this.changingPassword() && this.shouldShowRequired(this.profileForm.controls.currentPassword);
  }

  showCurrentPasswordWhitespaceError(): boolean {
    const control = this.profileForm.controls.currentPassword;
    return this.changingPassword() && this.wasInteracted(control) && control.hasError('whitespace');
  }

  showCurrentPasswordInvalidError(): boolean {
    const control = this.profileForm.controls.currentPassword;
    return this.changingPassword() && this.wasInteracted(control) && control.hasError('invalidCurrentPassword');
  }

  showCurrentPasswordAnyError(): boolean {
    return (
      this.showCurrentPasswordRequiredError() ||
      this.showCurrentPasswordWhitespaceError() ||
      this.showCurrentPasswordInvalidError()
    );
  }

  showNewPasswordRequiredError(): boolean {
    return this.changingPassword() && this.shouldShowRequired(this.profileForm.controls.newPassword);
  }

  showNewPasswordWhitespaceError(): boolean {
    const control = this.profileForm.controls.newPassword;
    return this.changingPassword() && this.wasInteracted(control) && control.hasError('whitespace');
  }

  showNewPasswordComplexityError(): boolean {
    const control = this.profileForm.controls.newPassword;

    if (!this.changingPassword() || !this.wasInteracted(control)) {
      return false;
    }

    if (control.hasError('required') || control.hasError('whitespace')) {
      return false;
    }

    return control.hasError('invalidPasswordComplexity');
  }

  showNewPasswordAnyError(): boolean {
    return (
      this.showNewPasswordRequiredError() ||
      this.showNewPasswordWhitespaceError() ||
      this.showNewPasswordComplexityError()
    );
  }

  showConfirmPasswordRequiredError(): boolean {
    return this.changingPassword() && this.shouldShowRequired(this.profileForm.controls.confirmNewPassword);
  }

  showConfirmPasswordWhitespaceError(): boolean {
    const control = this.profileForm.controls.confirmNewPassword;
    return this.changingPassword() && this.wasInteracted(control) && control.hasError('whitespace');
  }

  showConfirmPasswordMismatchError(): boolean {
    const control = this.profileForm.controls.confirmNewPassword;

    if (!this.changingPassword() || !this.wasInteracted(control)) {
      return false;
    }

    return this.profileForm.hasError('passwordMismatch');
  }

  showConfirmPasswordAnyError(): boolean {
    return (
      this.showConfirmPasswordRequiredError() ||
      this.showConfirmPasswordWhitespaceError() ||
      this.showConfirmPasswordMismatchError()
    );
  }

  private navigateToDashboard(flashMessage?: string): void {
    const role = this.authService.currentUser()?.role ?? 'CLIENTE';
    const targetRoute = role === 'CLIENTE' ? '/app/cliente' : this.authService.getLandingRouteForRole(role);

    void this.router.navigateByUrl(targetRoute, {
      state: flashMessage ? { flashMessage } : undefined
    });
  }

  private enablePasswordChangeSection(): void {
    this.changingPassword.set(true);

    this.profileForm.controls.currentPassword.setValidators([Validators.required, noWhitespaceValidator]);
    this.profileForm.controls.newPassword.setValidators([
      Validators.required,
      noWhitespaceValidator,
      passwordComplexityValidator
    ]);
    this.profileForm.controls.confirmNewPassword.setValidators([Validators.required, noWhitespaceValidator]);

    this.profileForm.controls.currentPassword.updateValueAndValidity({ emitEvent: false });
    this.profileForm.controls.newPassword.updateValueAndValidity({ emitEvent: false });
    this.profileForm.controls.confirmNewPassword.updateValueAndValidity({ emitEvent: false });
    this.profileForm.updateValueAndValidity({ emitEvent: false });
  }

  private disablePasswordChangeSection(): void {
    this.changingPassword.set(false);

    this.profileForm.patchValue(
      {
        currentPassword: '',
        newPassword: '',
        confirmNewPassword: ''
      },
      { emitEvent: false }
    );

    this.profileForm.controls.currentPassword.clearValidators();
    this.profileForm.controls.newPassword.setValidators([passwordComplexityValidator]);
    this.profileForm.controls.confirmNewPassword.clearValidators();

    this.profileForm.controls.currentPassword.setErrors(null);
    this.profileForm.controls.newPassword.setErrors(null);
    this.profileForm.controls.confirmNewPassword.setErrors(null);

    this.profileForm.controls.currentPassword.markAsPristine();
    this.profileForm.controls.newPassword.markAsPristine();
    this.profileForm.controls.confirmNewPassword.markAsPristine();

    this.profileForm.controls.currentPassword.updateValueAndValidity({ emitEvent: false });
    this.profileForm.controls.newPassword.updateValueAndValidity({ emitEvent: false });
    this.profileForm.controls.confirmNewPassword.updateValueAndValidity({ emitEvent: false });
    this.profileForm.updateValueAndValidity({ emitEvent: false });
  }

  private normalizeInputValues(): void {
    const controls = this.profileForm.controls;

    controls.fullName.setValue(controls.fullName.value.trim().replace(/\s+/g, ' '));
    controls.email.setValue(controls.email.value.trim().toLowerCase());
    controls.phone.setValue(controls.phone.value.trim());

    if (this.changingPassword()) {
      controls.currentPassword.setValue(controls.currentPassword.value.trim());
      controls.newPassword.setValue(controls.newPassword.value.trim());
      controls.confirmNewPassword.setValue(controls.confirmNewPassword.value.trim());
    }
  }

  private captureSnapshot(): ProfileSnapshot {
    const controls = this.profileForm.controls;
    return {
      fullName: controls.fullName.value.trim().replace(/\s+/g, ' '),
      email: controls.email.value.trim().toLowerCase(),
      phone: controls.phone.value.trim().replace(/\D/g, ''),
      address: controls.address.value.trim()
    };
  }

  private hasUnsavedChanges(): boolean {
    if (!this.initialSnapshot) {
      return false;
    }

    const currentSnapshot = this.captureSnapshot();
    const identityChanged =
      currentSnapshot.fullName !== this.initialSnapshot.fullName ||
      currentSnapshot.email !== this.initialSnapshot.email ||
      currentSnapshot.phone !== this.initialSnapshot.phone ||
      currentSnapshot.address !== this.initialSnapshot.address;

    const passwordChanged =
      this.changingPassword() &&
      Boolean(
        this.profileForm.controls.currentPassword.value ||
        this.profileForm.controls.newPassword.value ||
        this.profileForm.controls.confirmNewPassword.value
      );

    return identityChanged || passwordChanged;
  }

  private syncPendingState(): void {
    this.pendingChangesService.setHasUnsavedChanges(this.hasUnsavedChanges());
  }

  private setControlError(control: AbstractControl, errorKey: string): void {
    const currentErrors = control.errors ?? {};
    control.setErrors({ ...currentErrors, [errorKey]: true });
    control.markAsTouched();
  }

  private clearControlError(control: AbstractControl, errorKey: string): void {
    const currentErrors = control.errors;

    if (!currentErrors || !(errorKey in currentErrors)) {
      return;
    }

    const { [errorKey]: _removed, ...rest } = currentErrors;
    control.setErrors(Object.keys(rest).length > 0 ? rest : null);
  }

  private shouldShowRequired(control: AbstractControl): boolean {
    return this.wasInteracted(control) && control.hasError('required') && !control.hasError('whitespace');
  }

  private wasInteracted(control: AbstractControl): boolean {
    return control.touched || control.dirty;
  }
}


