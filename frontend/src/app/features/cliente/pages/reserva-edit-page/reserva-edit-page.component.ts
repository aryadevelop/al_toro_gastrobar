import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ReservationService } from '../../../../core/services/reservation.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-reserva-edit-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Modificar reserva" subtitle="Actualiza fecha, hora o numero de invitados"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid" [formGroup]="reservaForm" (ngSubmit)="onSubmit()">
          <label>
            <span>Nombre</span>
            <input class="input-field" formControlName="guestName" />
          </label>
          <label>
            <span>Personas</span>
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
            <span>Estado</span>
            <select class="input-field" formControlName="status">
              <option value="PENDING">Pendiente</option>
              <option value="CONFIRMED">Confirmada</option>
              <option value="CANCELLED">Cancelada</option>
            </select>
          </label>
          <button class="btn-primary" type="submit">Actualizar reserva</button>
          <p style="margin:0; color: var(--success);" *ngIf="saved()">Cambios guardados.</p>
        </form>
      </article>
    </section>
  `
})
export class ReservaEditPageComponent implements OnInit {
  readonly saved = signal(false);
  private reservationId = '';

  readonly reservaForm = this.formBuilder.nonNullable.group({
    guestName: ['', [Validators.required]],
    guests: [2, [Validators.required, Validators.min(1)]],
    date: ['', [Validators.required]],
    time: ['', [Validators.required]],
    status: ['PENDING' as 'PENDING' | 'CONFIRMED' | 'CANCELLED', [Validators.required]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly reservationService: ReservationService,
    private readonly activatedRoute: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.reservationId = this.activatedRoute.snapshot.paramMap.get('id') ?? '';
    this.reservationService.list().subscribe((reservas) => {
      const selected = reservas.find((reserva) => reserva.id === this.reservationId) ?? reservas[0];
      if (selected) {
        this.reservationId = selected.id;
        this.reservaForm.patchValue({
          guestName: selected.guestName,
          guests: selected.guests,
          date: selected.date,
          time: selected.time,
          status: selected.status === 'CONFIRMED' ? 'CONFIRMED' : selected.status === 'CANCELLED' ? 'CANCELLED' : 'PENDING'
        });
      }
    });
  }

  onSubmit(): void {
    if (!this.reservationId || this.reservaForm.invalid) {
      this.reservaForm.markAllAsTouched();
      return;
    }

    this.reservationService.update(this.reservationId, this.reservaForm.getRawValue()).subscribe(() => {
      this.saved.set(true);
    });
  }
}
