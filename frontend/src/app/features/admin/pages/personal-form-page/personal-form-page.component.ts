import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ClienteVentasAdminService } from '../../../../core/services/cliente-ventas-admin.service';
import { PersonalAdminService, RolEmpleadoAlta } from '../../../../core/services/personal-admin.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

type RolOption = { value: RolEmpleadoAlta; label: string };

function noFutureDateValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = String(control.value ?? '').trim();
    if (!value) {
      return null;
    }

    const todayIso = new Date().toISOString().slice(0, 10);
    return value > todayIso ? { futureDate: true } : null;
  };
}

function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = String(group.get('password')?.value ?? '');
  const confirm = String(group.get('confirmPassword')?.value ?? '');
  if (!password || !confirm) {
    return null;
  }
  return password === confirm ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-personal-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, ConfirmDialogComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Nuevo empleado" subtitle="Registro de personal del sistema"></app-page-header>

      <article class="card" style="padding: 1rem; max-width: 860px;">
        <form class="form-grid form-compact" [formGroup]="form" (ngSubmit)="onSubmit()">
          <label>
            <span>Nombre</span>
            <input class="input-field" [class.input-error]="showNameRequiredError()" formControlName="fullName" />
            <small class="form-error" *ngIf="showNameRequiredError()">El nombre es obligatorio</small>
          </label>

          <label>
            <span>Correo electrónico</span>
            <input class="input-field" [class.input-error]="showEmailAnyError()" type="email" formControlName="email" />
            <small class="form-error" *ngIf="showEmailRequiredError()">El correo electrónico es obligatorio</small>
            <small class="form-error" *ngIf="showEmailFormatError()">Ingresa un correo electrónico válido</small>
            <small class="form-error" *ngIf="showEmailDuplicateError()">
              Este correo electrónico ya está registrado para otro empleado
            </small>
          </label>

          <label>
            <span>Teléfono</span>
            <input class="input-field" [class.input-error]="showPhoneAnyError()" formControlName="phone" />
            <small class="form-error" *ngIf="showPhoneRequiredError()">El teléfono es obligatorio</small>
            <small class="form-error" *ngIf="showPhoneFormatError()">
              Ingresa un número de teléfono válido (mínimo 7 dígitos)
            </small>
          </label>

          <label>
            <span>Dirección (opcional)</span>
            <input class="input-field" formControlName="address" />
          </label>

          <label>
            <span>Fecha de ingreso</span>
            <input
              class="input-field"
              [class.input-error]="showHireDateAnyError()"
              type="date"
              formControlName="hireDate"
            />
            <small class="form-error" *ngIf="showHireDateRequiredError()">La fecha de ingreso es obligatoria</small>
            <small class="form-error" *ngIf="showHireDateFutureError()">La fecha de ingreso no puede ser futura</small>
          </label>

          <label class="roles-field">
            <span>Roles</span>
            <div class="roles-grid">
              <label class="role-item" *ngFor="let role of roleOptions">
                <input
                  type="checkbox"
                  [checked]="isRoleSelected(role.value)"
                  (change)="onToggleRole(role.value, $any($event.target).checked)"
                />
                <span>{{ role.label }}</span>
              </label>
            </div>
            <small class="form-error" *ngIf="showRoleRequiredError()">El rol es obligatorio</small>
          </label>

          <label>
            <span>Foto (opcional)</span>
            <input class="input-field" type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" (change)="onPhotoSelected($event)" />
            <small class="form-error" *ngIf="photoError()">{{ photoError() }}</small>
            <small class="form-warning" *ngIf="photoWarning()">{{ photoWarning() }}</small>
          </label>

          <label>
            <span>Contraseña</span>
            <input class="input-field" [class.input-error]="showPasswordAnyError()" type="password" formControlName="password" />
            <small class="form-error" *ngIf="showPasswordRequiredError()">La contraseña es obligatoria</small>
            <small class="form-error" *ngIf="showPasswordLengthError()">La contraseña debe tener al menos 8 caracteres</small>
          </label>

          <label>
            <span>Confirmar contraseña</span>
            <input
              class="input-field"
              [class.input-error]="showConfirmPasswordAnyError()"
              type="password"
              formControlName="confirmPassword"
            />
            <small class="form-error" *ngIf="showConfirmPasswordRequiredError()">
              La confirmación de contraseña es obligatoria
            </small>
            <small class="form-error" *ngIf="showPasswordMismatchError()">Las contraseñas no coinciden</small>
          </label>

          <div class="actions-row">
            <button class="btn-secondary" type="button" (click)="onCancelCreation()" [disabled]="saving()">Cancelar</button>
            <button class="btn-primary" type="submit" [disabled]="saving()">
              {{ saving() ? 'Guardando...' : 'Guardar empleado' }}
            </button>
          </div>

          <p class="form-error" *ngIf="errorMessage()">{{ errorMessage() }}</p>
          <p class="form-success" *ngIf="successMessage()">{{ successMessage() }}</p>
        </form>
      </article>

      <app-confirm-dialog
        [open]="showCancelDialog()"
        title="Cancelar creación"
        message="¿Seguro que deseas cancelar? Los datos no guardados se perderán"
        cancelLabel="No, continuar"
        confirmLabel="Sí, cancelar"
        (cancel)="showCancelDialog.set(false)"
        (confirm)="confirmCancelCreation()"
      ></app-confirm-dialog>

      <app-confirm-dialog
        [open]="showClientEmailWarningDialog()"
        title="Correo asociado a cliente"
        message="⚠️ Este correo electrónico pertenece a un cliente registrado. ¿Deseas crear un empleado con el mismo correo?"
        cancelLabel="Cancelar"
        confirmLabel="Sí, continuar"
        (cancel)="cancelClientEmailWarning()"
        (confirm)="confirmClientEmailWarning()"
      ></app-confirm-dialog>
    </section>
  `,
  styles: [
    `
      .roles-field {
        display: grid;
        gap: 0.45rem;
      }

      .roles-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
        gap: 0.4rem 0.8rem;
      }

      .role-item {
        display: flex;
        align-items: center;
        gap: 0.4rem;
        font-size: 0.85rem;
      }

      .actions-row {
        display: flex;
        justify-content: flex-end;
        gap: 0.6rem;
      }

      .input-error {
        border-color: rgba(196, 30, 58, 0.82);
      }

      .form-error {
        color: #9f1239;
        font-size: 0.78rem;
        margin: 0;
      }

      .form-warning {
        color: #9a6700;
        font-size: 0.78rem;
        margin: 0;
      }

      .form-success {
        color: #137333;
        font-size: 0.84rem;
        margin: 0;
      }
    `,
  ],
})
export class PersonalFormPageComponent {
  readonly saving = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly photoError = signal('');
  readonly photoWarning = signal('');
  readonly showCancelDialog = signal(false);
  readonly showClientEmailWarningDialog = signal(false);

  private pendingClientConflictEmail: string | null = null;
  private confirmedClientConflictEmail: string | null = null;

  readonly roleOptions: RolOption[] = [
    { value: 'ADMIN', label: 'Administrador' },
    { value: 'MESERO', label: 'Mesero' },
    { value: 'COCINERO', label: 'Cocinero' },
    { value: 'CAJERO', label: 'Cajero' },
  ];

  readonly form = this.formBuilder.nonNullable.group(
    {
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^\d{7,}$/)]],
      address: [''],
      roles: this.formBuilder.nonNullable.control<RolEmpleadoAlta[]>([], [Validators.required]),
      hireDate: [new Date().toISOString().slice(0, 10), [Validators.required, noFutureDateValidator()]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordMatchValidator }
  );

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly router: Router,
    private readonly personalAdminService: PersonalAdminService,
    private readonly clienteVentasAdminService: ClienteVentasAdminService
  ) {}

  async onSubmit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');

    const normalizedEmail = this.form.controls.email.getRawValue().trim().toLowerCase();

    if (this.confirmedClientConflictEmail !== normalizedEmail) {
      const hasClientConflict = await this.hasExistingClienteEmail(normalizedEmail);
      if (hasClientConflict) {
        this.pendingClientConflictEmail = normalizedEmail;
        this.showClientEmailWarningDialog.set(true);
        return;
      }
    }

    this.submitCreate();
  }

  onToggleRole(role: RolEmpleadoAlta, checked: boolean): void {
    const current = this.form.controls.roles.getRawValue();
    const next = checked
      ? Array.from(new Set([...current, role]))
      : current.filter((item) => item !== role);

    this.form.controls.roles.setValue(next);
    this.form.controls.roles.markAsTouched();
  }

  isRoleSelected(role: RolEmpleadoAlta): boolean {
    return this.form.controls.roles.getRawValue().includes(role);
  }

  onCancelCreation(): void {
    this.showCancelDialog.set(true);
  }

  confirmCancelCreation(): void {
    this.showCancelDialog.set(false);
    this.router.navigate(['/app/admin/personal']);
  }

  cancelClientEmailWarning(): void {
    this.showClientEmailWarningDialog.set(false);
    this.pendingClientConflictEmail = null;
  }

  confirmClientEmailWarning(): void {
    this.showClientEmailWarningDialog.set(false);
    this.confirmedClientConflictEmail = this.pendingClientConflictEmail;
    this.pendingClientConflictEmail = null;
    this.submitCreate();
  }

  onPhotoSelected(event: Event): void {
    this.photoError.set('');
    this.photoWarning.set('');

    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.item(0);
    if (!file) {
      return;
    }

    const validType = ['image/jpeg', 'image/png'].includes(file.type);
    const maxSizeBytes = 2 * 1024 * 1024;
    if (!validType || file.size > maxSizeBytes) {
      this.photoError.set('Formato no válido. Selecciona una imagen JPG o PNG de máximo 2MB');
      if (input) {
        input.value = '';
      }
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const image = new Image();
      image.onload = () => {
        if (image.width < 200 || image.height < 200) {
          this.photoWarning.set(
            'La imagen tiene baja resolución. Se recomienda usar imágenes de al menos 500x500 píxeles'
          );
        }
      };
      image.src = String(reader.result ?? '');
    };
    reader.readAsDataURL(file);
  }

  showNameRequiredError(): boolean {
    return this.showControlError(this.form.controls.fullName, 'required');
  }

  showEmailRequiredError(): boolean {
    return this.showControlError(this.form.controls.email, 'required');
  }

  showEmailFormatError(): boolean {
    return this.showControlError(this.form.controls.email, 'email');
  }

  showEmailDuplicateError(): boolean {
    return this.showControlError(this.form.controls.email, 'duplicateEmployeeEmail');
  }

  showEmailAnyError(): boolean {
    return this.showEmailRequiredError() || this.showEmailFormatError() || this.showEmailDuplicateError();
  }

  showPhoneRequiredError(): boolean {
    return this.showControlError(this.form.controls.phone, 'required');
  }

  showPhoneFormatError(): boolean {
    return this.showControlError(this.form.controls.phone, 'pattern');
  }

  showPhoneAnyError(): boolean {
    return this.showPhoneRequiredError() || this.showPhoneFormatError();
  }

  showRoleRequiredError(): boolean {
    return this.showControlError(this.form.controls.roles, 'required');
  }

  showHireDateRequiredError(): boolean {
    return this.showControlError(this.form.controls.hireDate, 'required');
  }

  showHireDateFutureError(): boolean {
    return this.showControlError(this.form.controls.hireDate, 'futureDate');
  }

  showHireDateAnyError(): boolean {
    return this.showHireDateRequiredError() || this.showHireDateFutureError();
  }

  showPasswordRequiredError(): boolean {
    return this.showControlError(this.form.controls.password, 'required');
  }

  showPasswordLengthError(): boolean {
    return this.showControlError(this.form.controls.password, 'minlength');
  }

  showPasswordAnyError(): boolean {
    return this.showPasswordRequiredError() || this.showPasswordLengthError();
  }

  showConfirmPasswordRequiredError(): boolean {
    return this.showControlError(this.form.controls.confirmPassword, 'required');
  }

  showPasswordMismatchError(): boolean {
    return Boolean(
      this.form.hasError('passwordMismatch') &&
      (this.form.controls.confirmPassword.touched || this.form.controls.confirmPassword.dirty)
    );
  }

  showConfirmPasswordAnyError(): boolean {
    return this.showConfirmPasswordRequiredError() || this.showPasswordMismatchError();
  }

  private showControlError(control: AbstractControl, errorKey: string): boolean {
    return Boolean((control.touched || control.dirty) && control.hasError(errorKey));
  }

  private async hasExistingClienteEmail(email: string): Promise<boolean> {
    try {
      const result = await firstValueFrom(this.clienteVentasAdminService.buscarClientes('correo', email));
      return result.results.some((item) => item.email.trim().toLowerCase() === email);
    } catch {
      return false;
    }
  }

  private submitCreate(): void {
    if (this.form.invalid || this.saving()) {
      return;
    }

    this.saving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.clearDuplicateEmailError();

    const payload = {
      nombre: this.form.controls.fullName.getRawValue().trim(),
      correoElectronico: this.form.controls.email.getRawValue().trim().toLowerCase(),
      telefono: this.form.controls.phone.getRawValue().trim(),
      direccion: this.form.controls.address.getRawValue().trim(),
      roles: this.form.controls.roles.getRawValue(),
      fechaIngreso: this.form.controls.hireDate.getRawValue(),
      password: this.form.controls.password.getRawValue(),
      passwordConfirmacion: this.form.controls.confirmPassword.getRawValue(),
    };

    this.personalAdminService.crearEmpleado(payload).subscribe({
      next: (result) => {
        this.saving.set(false);

        if (result.warning?.toLowerCase().includes('correo de bienvenida')) {
          this.successMessage.set(
            'Empleado creado exitosamente. No se pudo enviar el correo de bienvenida. Verifica la configuración de correo'
          );
          return;
        }

        this.successMessage.set(
          `Empleado ${result.nombre} creado exitosamente. Se ha enviado un correo con las credenciales`
        );
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.handleCreateError(error);
      },
    });
  }

  private handleCreateError(error: HttpErrorResponse): void {
    const backendMessage = this.extractErrorMessage(error);
    const normalizedMessage = backendMessage.toLowerCase();

    if (error.status === 0) {
      this.errorMessage.set('Error de conexión. No se pudo crear el empleado. Verifica tu conexión e intenta nuevamente');
      return;
    }

    if (normalizedMessage.includes('empleado registrado con este correo')) {
      this.setDuplicateEmailError();
      return;
    }

    if (normalizedMessage.includes('fecha de ingreso no puede ser futura')) {
      this.form.controls.hireDate.setErrors({ ...(this.form.controls.hireDate.errors ?? {}), futureDate: true });
      this.form.controls.hireDate.markAsTouched();
      return;
    }

    if (normalizedMessage.includes('las contraseñas no coinciden')) {
      this.form.setErrors({ ...(this.form.errors ?? {}), passwordMismatch: true });
      this.form.controls.confirmPassword.markAsTouched();
      return;
    }

    if (normalizedMessage.includes('teléfono válido')) {
      this.form.controls.phone.setErrors({ ...(this.form.controls.phone.errors ?? {}), pattern: true });
      this.form.controls.phone.markAsTouched();
      return;
    }

    if (normalizedMessage.includes('contraseña debe tener al menos 8 caracteres')) {
      this.form.controls.password.setErrors({ ...(this.form.controls.password.errors ?? {}), minlength: true });
      this.form.controls.password.markAsTouched();
      return;
    }

    if (backendMessage) {
      this.errorMessage.set(backendMessage);
      return;
    }

    this.errorMessage.set('Error al crear el empleado. Por favor intenta nuevamente');
  }

  private extractErrorMessage(error: unknown): string {
    const httpError = error as { error?: { message?: string } | string } | null | undefined;
    if (!httpError) {
      return '';
    }

    const payload = httpError.error;
    if (typeof payload === 'string' && payload.trim()) {
      return payload.trim();
    }

    if (payload && typeof payload === 'object' && typeof payload.message === 'string' && payload.message.trim()) {
      return payload.message.trim();
    }

    return '';
  }

  private setDuplicateEmailError(): void {
    const emailControl = this.form.controls.email;
    const currentErrors = emailControl.errors ?? {};

    emailControl.setErrors({ ...currentErrors, duplicateEmployeeEmail: true });
    emailControl.markAsTouched();
  }

  private clearDuplicateEmailError(): void {
    const emailControl = this.form.controls.email;
    if (!emailControl.hasError('duplicateEmployeeEmail')) {
      return;
    }

    const currentErrors = { ...(emailControl.errors ?? {}) };
    delete currentErrors['duplicateEmployeeEmail'];
    emailControl.setErrors(Object.keys(currentErrors).length ? currentErrors : null);
  }
}
