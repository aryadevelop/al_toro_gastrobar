import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { ReservationService } from '../../../../core/services/reservation.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-reserva-create-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Crear reserva" subtitle="Reserva una mesa en pocos pasos"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid" [formGroup]="reservaForm" (ngSubmit)="onSubmit()">
          <label>
            <span>Nombre de contacto</span>
            <input class="input-field" formControlName="guestName" />
          </label>

          <label>
            <span>Cantidad de personas</span>
            <input class="input-field" type="number" min="1" formControlName="guests" />
          </label>

          <label>
            <span>Fecha</span>
            <input class="input-field" type="date" formControlName="date" />
          </label>

          <label>
            <span>Hora</span>
            <input class="input-field" type="time" formControlName="time" />
          </label>

          <label>
            <span>Notas</span>
            <textarea class="input-field" rows="3" formControlName="notes"></textarea>
          </label>

          <p class="error-text" *ngIf="reservaForm.invalid && reservaForm.touched">Completa los campos requeridos.</p>
          <button class="btn-primary" type="submit">Guardar reserva</button>
          <p style="margin: 0; color: var(--success);" *ngIf="success()">Reserva registrada con exito.</p>
        </form>
      </article>
    </section>
  `
})
export class ReservaCreatePageComponent {
  readonly success = signal(false);

  readonly reservaForm = this.formBuilder.nonNullable.group({
    guestName: ['', [Validators.required]],
    guests: [2, [Validators.required, Validators.min(1)]],
    date: ['', [Validators.required]],
    time: ['', [Validators.required]],
    notes: ['']
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly reservationService: ReservationService,
    private readonly authService: AuthService
  ) {}

  onSubmit(): void {
    if (this.reservaForm.invalid) {
      this.reservaForm.markAllAsTouched();
      return;
    }

    const currentUser = this.authService.currentUser();
    this.reservationService
      .create({
        ...this.reservaForm.getRawValue(),
        clienteId: currentUser?.id ?? 'guest-client'
      })
      .subscribe(() => {
        this.success.set(true);
        this.reservaForm.reset({ guestName: '', guests: 2, date: '', time: '', notes: '' });
      });
  }
}
