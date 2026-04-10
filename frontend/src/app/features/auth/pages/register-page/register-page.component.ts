import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';

const noWhitespaceValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '');
  return value.trim().length === 0 ? { whitespace: true } : null;
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

  const hasUppercase = /[A-Z]/.test(value);
  const hasLowercase = /[a-z]/.test(value);
  const hasNumber = /\d/.test(value);
  const hasSpecialChar = /[^A-Za-z0-9]/.test(value);

  return hasUppercase && hasLowercase && hasNumber && hasSpecialChar
    ? null
    : { invalidPasswordComplexity: true };
};

const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (!password || !confirmPassword) {
    return null;
  }

  return password === confirmPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="register-shell">
      <article class="card register-card">
        <h1>Crear Cuenta</h1>
        <p class="subtitle">Registra tu cuenta en Al Toro</p>

        <p class="form-error" *ngIf="formMessage()">{{ formMessage() }}</p>

        <form class="form-grid form-compact" [formGroup]="registerForm" (ngSubmit)="onSubmit()" autocomplete="off" novalidate>
          <label class="form-label">
            <span>Nombre Completo *</span>
            <input
              class="input-field"
              [class.input-invalid]="showNameAnyError()"
              formControlName="fullName"
              maxlength="50"
              autocomplete="off"
            />
            <p class="error-text" *ngIf="showNameRequiredError()">Este campo no puede estar vacío</p>
            <p class="error-text" *ngIf="showNameFormatError()">El nombre solo puede contener letras y espacios</p>
            <p class="error-text" *ngIf="showNameMaxLengthError()">El nombre no puede exceder los 50 caracteres</p>
          </label>

          <label class="form-label">
            <span>Correo electrónico *</span>
            <input
              class="input-field"
              type="email"
              [class.input-invalid]="showEmailAnyError()"
              formControlName="email"
              autocomplete="off"
            />
            <p class="error-text" *ngIf="showEmailRequiredError()">Este campo no puede estar vacío</p>
            <p class="error-text" *ngIf="showEmailFormatError()">
              Ingresa un correo electrónico válido (ej. nombre&#64;dominio.com)
            </p>
            <p class="error-text" *ngIf="showEmailDuplicateError()">
              Este correo electrónico ya está registrado. Por favor inicia sesión o utiliza otro correo
            </p>
          </label>

          <label class="form-label">
            <span>Número de teléfono *</span>
            <input
              class="input-field"
              [class.input-invalid]="showPhoneAnyError()"
              formControlName="phone"
              maxlength="10"
              autocomplete="off"
              (input)="onPhoneInput()"
            />
            <p class="error-text" *ngIf="showPhoneRequiredError()">Este campo no puede estar vacío</p>
            <p class="error-text" *ngIf="showPhoneFormatError()">Ingresa un número de teléfono válido de 10 dígitos</p>
          </label>

          <label class="form-label">
            <span>Contraseña *</span>
            <div class="password-row">
              <input
                class="input-field"
                [class.input-invalid]="showPasswordAnyError()"
                [type]="showPassword() ? 'text' : 'password'"
                formControlName="password"
                autocomplete="new-password"
              />
              <button type="button" class="btn-eye" (click)="showPassword.set(!showPassword())" aria-label="Ver contraseña">
                Ver
              </button>
            </div>
            <p class="error-text" *ngIf="showPasswordRequiredError()">Este campo no puede estar vacío</p>
            <p class="error-text" *ngIf="showPasswordMinLengthError()">La contraseña debe tener al menos 8 caracteres</p>
            <p class="error-text" *ngIf="showPasswordComplexityError()">
              La contraseña debe incluir mayúscula, minúscula, número y carácter especial
            </p>
          </label>

          <label class="form-label">
            <span>Confirmar contraseña *</span>
            <div class="password-row">
              <input
                class="input-field"
                [class.input-invalid]="showConfirmPasswordAnyError()"
                [type]="showConfirmPassword() ? 'text' : 'password'"
                formControlName="confirmPassword"
                autocomplete="new-password"
              />
              <button
                type="button"
                class="btn-eye"
                (click)="showConfirmPassword.set(!showConfirmPassword())"
                aria-label="Ver confirmación de contraseña"
              >
                Ver
              </button>
            </div>
            <p class="error-text" *ngIf="showConfirmPasswordRequiredError()">Este campo no puede estar vacío</p>
            <p class="error-text" *ngIf="showPasswordMismatchError()">Las contraseñas no coinciden</p>
          </label>

          <label class="terms-row">
            <input type="checkbox" formControlName="acceptTerms" />
            <span>Acepto los términos y condiciones *</span>
          </label>
          <p class="error-text" *ngIf="showTermsError()">
            Debes aceptar los términos y condiciones para registrarte
          </p>

          <div class="action-row">
            <button class="btn-secondary" type="button" (click)="onCancel()">Cancelar</button>
            <button class="btn-primary register-btn" type="submit" [disabled]="loading()">
              {{ loading() ? 'Registrando...' : 'Registrarse' }}
            </button>
          </div>
        </form>

        <a routerLink="/auth/login" class="login-link">Volver a inicio de sesión</a>
      </article>
    </section>
  `,
  styleUrls: ['./register-page.component.scss']
})
export class RegisterPageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  readonly loading = signal(false);
  readonly submitted = signal(false);
  readonly formMessage = signal('');
  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);

  readonly registerForm = this.formBuilder.nonNullable.group(
    {
      fullName: [
        '',
        [
          Validators.required,
          noWhitespaceValidator,
          Validators.maxLength(50),
          fullNameFormatValidator
        ]
      ],
      email: ['', [Validators.required, noWhitespaceValidator, Validators.email]],
      phone: ['', [Validators.required, noWhitespaceValidator, Validators.pattern(/^\d{10}$/)]],
      password: [
        '',
        [
          Validators.required,
          noWhitespaceValidator,
          Validators.minLength(8),
          passwordComplexityValidator
        ]
      ],
      confirmPassword: ['', [Validators.required, noWhitespaceValidator]],
      acceptTerms: [false, [Validators.requiredTrue]]
    },
    { validators: [passwordMatchValidator] }
  );

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.resetFormToInitialState();

    this.registerForm.controls.email.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.clearEmailDuplicateError();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.resetFormToInitialState();
  }

  onPhoneInput(): void {
    const control = this.registerForm.controls.phone;
    const digitsOnly = control.value.replace(/\D/g, '').slice(0, 10);

    if (digitsOnly !== control.value) {
      control.setValue(digitsOnly);
    }
  }

  onSubmit(): void {
    this.submitted.set(true);
    this.formMessage.set('');
    this.normalizeInputValues();

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.formMessage.set('Por favor completa los campos obligatorios y corrige los datos inválidos.');
      return;
    }

    this.loading.set(true);

    const formValue = this.registerForm.getRawValue();

    this.authService
      .register({
        fullName: formValue.fullName.trim().replace(/\s+/g, ' '),
        email: formValue.email.trim(),
        phone: formValue.phone.trim(),
        password: formValue.password
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.resetFormToInitialState();
          void this.router.navigate(['/app/cliente'], {
            state: {
              flashMessage: 'Cuenta creada exitosamente. Bienvenido a Al Toro Gastrobar'
            }
          });
        },
        error: (error: HttpErrorResponse) => {
          this.loading.set(false);

          if (error.status === 409) {
            this.setDuplicateEmailError();
            return;
          }

          this.formMessage.set('No fue posible completar el registro. Intenta nuevamente.');
        }
      });
  }

  onCancel(): void {
    this.resetFormToInitialState();
    void this.router.navigateByUrl('/auth/login');
  }

  showNameRequiredError(): boolean {
    const control = this.registerForm.controls.fullName;
    return this.shouldShowRequired(control);
  }

  showNameFormatError(): boolean {
    const control = this.registerForm.controls.fullName;

    if (!this.wasInteracted(control)) {
      return false;
    }

    return control.hasError('invalidNameFormat');
  }

  showNameMaxLengthError(): boolean {
    const control = this.registerForm.controls.fullName;

    if (!this.wasInteracted(control)) {
      return false;
    }

    return control.hasError('maxlength');
  }

  showNameAnyError(): boolean {
    return this.showNameRequiredError() || this.showNameFormatError() || this.showNameMaxLengthError();
  }

  showEmailRequiredError(): boolean {
    const control = this.registerForm.controls.email;
    return this.shouldShowRequired(control);
  }

  showEmailFormatError(): boolean {
    const control = this.registerForm.controls.email;

    if (!this.wasInteracted(control)) {
      return false;
    }

    if (control.hasError('required') || control.hasError('whitespace')) {
      return false;
    }

    return control.hasError('email');
  }

  showEmailDuplicateError(): boolean {
    const control = this.registerForm.controls.email;
    return this.wasInteracted(control) && control.hasError('duplicateEmail');
  }

  showEmailAnyError(): boolean {
    return this.showEmailRequiredError() || this.showEmailFormatError() || this.showEmailDuplicateError();
  }

  showPhoneRequiredError(): boolean {
    const control = this.registerForm.controls.phone;
    return this.shouldShowRequired(control);
  }

  showPhoneFormatError(): boolean {
    const control = this.registerForm.controls.phone;

    if (!this.wasInteracted(control)) {
      return false;
    }

    if (control.hasError('required') || control.hasError('whitespace')) {
      return false;
    }

    return control.hasError('pattern');
  }

  showPhoneAnyError(): boolean {
    return this.showPhoneRequiredError() || this.showPhoneFormatError();
  }

  showPasswordRequiredError(): boolean {
    const control = this.registerForm.controls.password;
    return this.shouldShowRequired(control);
  }

  showPasswordMinLengthError(): boolean {
    const control = this.registerForm.controls.password;

    if (!this.wasInteracted(control)) {
      return false;
    }

    if (control.hasError('required') || control.hasError('whitespace')) {
      return false;
    }

    return control.hasError('minlength');
  }

  showPasswordComplexityError(): boolean {
    const control = this.registerForm.controls.password;

    if (!this.wasInteracted(control)) {
      return false;
    }

    if (control.hasError('required') || control.hasError('whitespace') || control.hasError('minlength')) {
      return false;
    }

    return control.hasError('invalidPasswordComplexity');
  }

  showPasswordAnyError(): boolean {
    return this.showPasswordRequiredError() || this.showPasswordMinLengthError() || this.showPasswordComplexityError();
  }

  showConfirmPasswordRequiredError(): boolean {
    const control = this.registerForm.controls.confirmPassword;
    return this.shouldShowRequired(control);
  }

  showPasswordMismatchError(): boolean {
    const control = this.registerForm.controls.confirmPassword;
    return this.wasInteracted(control) && this.registerForm.hasError('passwordMismatch');
  }

  showConfirmPasswordAnyError(): boolean {
    return this.showConfirmPasswordRequiredError() || this.showPasswordMismatchError();
  }

  showTermsError(): boolean {
    const control = this.registerForm.controls.acceptTerms;
    return this.wasInteracted(control) && control.hasError('required');
  }

  private shouldShowRequired(control: AbstractControl): boolean {
    if (!this.wasInteracted(control)) {
      return false;
    }

    return control.hasError('required') || control.hasError('whitespace');
  }

  private wasInteracted(control: AbstractControl): boolean {
    return control.touched || this.submitted();
  }

  private setDuplicateEmailError(): void {
    const emailControl = this.registerForm.controls.email;
    const currentErrors = emailControl.errors ?? {};

    emailControl.setErrors({ ...currentErrors, duplicateEmail: true });
    emailControl.markAsTouched();
  }

  private clearEmailDuplicateError(): void {
    const emailControl = this.registerForm.controls.email;

    if (!emailControl.hasError('duplicateEmail')) {
      return;
    }

    const currentErrors = { ...(emailControl.errors ?? {}) };
    delete currentErrors['duplicateEmail'];

    const hasOtherErrors = Object.keys(currentErrors).length > 0;
    emailControl.setErrors(hasOtherErrors ? currentErrors : null);
  }

  private normalizeInputValues(): void {
    const controls = this.registerForm.controls;

    controls.fullName.setValue(controls.fullName.value.trim().replace(/\s+/g, ' '));
    controls.email.setValue(controls.email.value.trim());
    controls.phone.setValue(controls.phone.value.trim());
    controls.confirmPassword.setValue(controls.confirmPassword.value.trim());
  }

  private resetFormToInitialState(): void {
    this.registerForm.reset({
      fullName: '',
      email: '',
      phone: '',
      password: '',
      confirmPassword: '',
      acceptTerms: false
    });

    this.submitted.set(false);
    this.formMessage.set('');
    this.showPassword.set(false);
    this.showConfirmPassword.set(false);
  }
}

