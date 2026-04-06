import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { Reserva, ReservaPreorderItem } from '../../../../core/models/domain.models';
import { MOCK_PRODUCTOS } from '../../../../core/mocks/restaurant.mock';
import { AuthService } from '../../../../core/services/auth.service';
import { ReservationService } from '../../../../core/services/reservation.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

interface DecorationOption {
  id: string;
  name: string;
  imageUrl: string;
  availableDays: number[];
}

interface ZoneOption {
  id: string;
  name: string;
  imageUrl: string;
  decorationIds: string[];
}

interface PreorderSelection {
  productId: string;
  productName: string;
  unitPrice: number;
  selected: boolean;
  quantity: number;
}

const DECORATION_OPTIONS: DecorationOption[] = [
  {
    id: 'decor-clasica',
    name: 'Decoración Clásica',
    imageUrl: 'https://picsum.photos/seed/decor-clasica/360/220',
    availableDays: [0, 1, 2, 3, 4, 5, 6]
  },
  {
    id: 'decor-romantica',
    name: 'Decoración Romántica',
    imageUrl: 'https://picsum.photos/seed/decor-romantica/360/220',
    availableDays: [1, 2, 3, 4, 5]
  },
  {
    id: 'decor-celebracion',
    name: 'Decoración Celebración',
    imageUrl: 'https://picsum.photos/seed/decor-celebracion/360/220',
    availableDays: [5, 6, 0]
  }
];

const ZONE_OPTIONS: ZoneOption[] = [
  {
    id: 'zona-terraza',
    name: 'Zona Terraza',
    imageUrl: 'https://picsum.photos/seed/zona-terraza/360/220',
    decorationIds: ['decor-clasica', 'decor-celebracion']
  },
  {
    id: 'zona-salon',
    name: 'Zona Salón Principal',
    imageUrl: 'https://picsum.photos/seed/zona-salon/360/220',
    decorationIds: ['decor-clasica', 'decor-romantica', 'decor-celebracion']
  },
  {
    id: 'zona-vip',
    name: 'Zona VIP',
    imageUrl: 'https://picsum.photos/seed/zona-vip/360/220',
    decorationIds: ['decor-romantica', 'decor-celebracion']
  }
];

const FULLY_BOOKED_SLOTS = [
  { date: '2026-04-05', time: '20:00' },
  { date: '2026-04-15', time: '19:30' }
];

@Component({
  selector: 'app-reserva-create-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Nueva reserva"></app-page-header>

      <article class="floating-warning card" *ngIf="showFloatingWarning()">
        {{ floatingWarningMessage() }}
      </article>

      <article class="card reservation-card">
        <p class="availability-warning" *ngIf="showNoAvailabilityWarning()">
          Lo sentimos, no hay disponibilidad para la fecha y hora seleccionada. Por favor elija otra fecha u hora
        </p>

        <form class="form-grid form-compact" [formGroup]="reservaForm" (ngSubmit)="onSubmit()" novalidate>
          <section class="schedule-grid">
            <label class="form-label">
              <span>Fecha *</span>
              <input class="input-field" type="date" formControlName="date" />
            </label>

            <label class="form-label">
              <span>Hora *</span>
              <input class="input-field" type="time" min="17:00" max="22:00" step="1800" formControlName="time" />
            </label>
          </section>

          <p class="error-text" *ngIf="showPastDateError()">
            La fecha y hora de la reserva no pueden ser en el pasado
          </p>
          <p class="error-text" *ngIf="showOutOfHoursError()">
            Nuestro horario de reserva es de 5:00 p.m. a 10:00 p.m. Por favor seleccione una hora válida
          </p>

          <section class="form-section">
            <span class="section-label">Número de personas *</span>
            <div class="guest-stepper">
              <button type="button" class="btn-secondary stepper-btn" (click)="changeGuests(-1)">-</button>
              <input class="input-field guest-input" type="number" min="1" max="20" formControlName="guests" />
              <button type="button" class="btn-secondary stepper-btn" (click)="changeGuests(1)">+</button>
            </div>
          </section>

          <section class="form-section">
            <span class="section-label">Decoraciones disponibles (opcional)</span>
            <div class="card-grid" *ngIf="availableDecorations().length > 0">
              <label class="option-card" *ngFor="let decoration of availableDecorations()">
                <input
                  type="radio"
                  name="decoration"
                  [value]="decoration.id"
                  formControlName="decorationId"
                />
                <img [src]="decoration.imageUrl" [alt]="decoration.name" />
                <strong>{{ decoration.name }}</strong>
              </label>
            </div>
          </section>

          <section class="form-section">
            <span class="section-label">Zonas disponibles (dependen de la decoración)</span>
            <div class="card-grid" *ngIf="availableZones().length > 0">
              <label class="option-card" *ngFor="let zone of availableZones()">
                <input type="radio" name="zone" [value]="zone.id" formControlName="zoneId" />
                <img [src]="zone.imageUrl" [alt]="zone.name" />
                <strong>{{ zone.name }}</strong>
              </label>
            </div>
          </section>

          <section class="form-section">
            <span class="section-label">Pre-ordenar a la carta (opcional)</span>
            <div class="preorder-grid">
              <article class="preorder-item" *ngFor="let item of preorderSelections; let i = index">
                <label>
                  <input
                    type="checkbox"
                    [checked]="item.selected"
                    (change)="togglePreorder(i, $any($event.target).checked)"
                  />
                  {{ item.productName }} - {{ item.unitPrice | currency:'COP':'symbol':'1.0-0' }}
                </label>
                <input
                  class="input-field preorder-qty"
                  type="number"
                  min="1"
                  [disabled]="!item.selected"
                  [value]="item.quantity"
                  (input)="setPreorderQuantity(i, $any($event.target).value)"
                />
              </article>
            </div>
          </section>

          <label class="form-label">
            <span>Notas adicionales (opcional)</span>
            <textarea class="input-field" rows="3" formControlName="notes"></textarea>
          </label>

          <div class="action-row">
            <button class="btn-secondary" type="button" (click)="onClose()">Cerrar</button>
            <button class="btn-primary" type="submit" [disabled]="loading()">
              {{ loading() ? 'Confirmando...' : 'Confirmar reserva' }}
            </button>
          </div>
        </form>
      </article>
    </section>
  `,
  styles: [
    `
      .info-banner,
      .reservation-card,
      .floating-warning {
        padding: 1rem;
        max-width: 860px;
      }

      .floating-warning {
        border: 1px solid #a8182f;
        background: rgba(168, 24, 47, 0.12);
        color: #6b1111;
        font-weight: 700;
      }

      .availability-warning {
        margin: 0 0 0.85rem;
        border: 1px solid #a8182f;
        border-radius: 8px;
        padding: 0.6rem 0.75rem;
        background: rgba(168, 24, 47, 0.1);
        color: #6b1111;
        font-size: 0.86rem;
      }

      .schedule-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 0.6rem;
      }

      .form-section {
        display: grid;
        gap: 0.45rem;
      }

      .section-label {
        font-size: 0.82rem;
        font-weight: 700;
      }

      .guest-stepper {
        display: grid;
        grid-template-columns: auto 1fr auto;
        gap: 0.45rem;
        align-items: center;
        max-width: 240px;
      }

      .guest-input {
        text-align: center;
      }

      .stepper-btn {
        min-width: 34px;
        min-height: 34px;
        padding: 0;
      }

      .card-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 0.6rem;
      }

      .option-card {
        display: grid;
        gap: 0.35rem;
        border: 1px solid rgba(10, 10, 10, 0.18);
        border-radius: 10px;
        padding: 0.45rem;
        background: #ffffff;
      }

      .option-card img {
        width: 100%;
        height: 110px;
        border-radius: 8px;
        object-fit: cover;
      }

      .option-card strong {
        font-size: 0.84rem;
      }

      .preorder-grid {
        display: grid;
        gap: 0.55rem;
      }

      .preorder-item {
        display: grid;
        grid-template-columns: 1fr 76px;
        gap: 0.5rem;
        align-items: center;
      }

      .preorder-item label {
        font-size: 0.84rem;
      }

      .preorder-qty {
        text-align: center;
      }

      .action-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.45rem;
      }

      @media (max-width: 640px) {
        .info-banner,
        .reservation-card,
        .floating-warning {
          padding: 0.72rem;
        }

        .availability-warning {
          margin-bottom: 0.6rem;
          padding: 0.45rem 0.55rem;
          font-size: 0.78rem;
        }

        .schedule-grid,
        .card-grid,
        .preorder-grid,
        .form-section {
          gap: 0.4rem;
        }

        .card-grid {
          grid-template-columns: repeat(auto-fit, minmax(128px, 1fr));
        }

        .option-card {
          padding: 0.3rem;
          gap: 0.28rem;
          border-radius: 8px;
        }

        .option-card img {
          height: 78px;
          border-radius: 7px;
        }

        .option-card strong,
        .preorder-item label,
        .section-label {
          font-size: 0.76rem;
        }

        .preorder-item {
          grid-template-columns: 1fr 62px;
          gap: 0.35rem;
        }

        .guest-stepper {
          max-width: 205px;
          gap: 0.35rem;
        }

        .stepper-btn {
          min-width: 30px;
          min-height: 30px;
        }

        .action-row {
          grid-template-columns: 1fr;
        }
      }
    `
  ]
})
export class ReservaCreatePageComponent implements OnInit, OnDestroy {
  readonly loading = signal(false);
  readonly showNoAvailabilityWarning = signal(false);
  readonly floatingWarningMessage = signal('');
  readonly showFloatingWarning = signal(false);
  readonly availableDecorations = signal<DecorationOption[]>([]);
  readonly availableZones = signal<ZoneOption[]>([]);

  readonly reservaForm = this.formBuilder.nonNullable.group({
    date: ['', [Validators.required]],
    time: ['', [Validators.required]],
    guests: [2, [Validators.required, Validators.min(1), Validators.max(20)]],
    decorationId: [''],
    zoneId: [''],
    notes: ['']
  });

  preorderSelections: PreorderSelection[] = MOCK_PRODUCTOS.filter((item) => item.status === 'ACTIVE').map((item) => ({
    productId: item.id,
    productName: item.name,
    unitPrice: item.price,
    selected: false,
    quantity: 1
  }));

  private readonly destroy$ = new Subject<void>();
  private existingReservations: Reserva[] = [];

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly reservationService: ReservationService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.reservationService.list().pipe(takeUntil(this.destroy$)).subscribe((items) => {
      this.existingReservations = items;
      this.updateAvailability();
    });

    this.reservaForm.controls.date.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.updateAvailability();
    });

    this.reservaForm.controls.time.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.updateAvailability();
    });

    this.reservaForm.controls.decorationId.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.updateAvailableZones();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  changeGuests(delta: number): void {
    const control = this.reservaForm.controls.guests;
    const next = Math.min(20, Math.max(1, control.value + delta));
    control.setValue(next);
  }

  togglePreorder(index: number, checked: boolean): void {
    const item = this.preorderSelections[index];
    if (!item) {
      return;
    }

    item.selected = checked;
    if (!checked) {
      item.quantity = 1;
    }
  }

  setPreorderQuantity(index: number, rawValue: string): void {
    const item = this.preorderSelections[index];
    if (!item) {
      return;
    }

    const parsed = Number(rawValue);
    if (!Number.isFinite(parsed) || parsed < 1) {
      item.quantity = 1;
      return;
    }

    item.quantity = Math.floor(parsed);
  }

  showPastDateError(): boolean {
    const date = this.reservaForm.controls.date.value;
    const time = this.reservaForm.controls.time.value;

    if (!date || !time) {
      return false;
    }

    return this.isDateTimeInPast(date, time);
  }

  showOutOfHoursError(): boolean {
    const time = this.reservaForm.controls.time.value;

    if (!time) {
      return false;
    }

    return !this.isWithinReservationHours(time);
  }

  onClose(): void {
    void this.router.navigateByUrl('/app/cliente');
  }

  onSubmit(): void {
    const { date, time } = this.reservaForm.getRawValue();

    if (!date || !time) {
      this.showFloating('La fecha y hora son obligatorias');
      this.reservaForm.controls.date.markAsTouched();
      this.reservaForm.controls.time.markAsTouched();
      return;
    }

    if (this.isDateTimeInPast(date, time)) {
      this.reservaForm.controls.date.markAsTouched();
      this.reservaForm.controls.time.markAsTouched();
      return;
    }

    if (!this.isWithinReservationHours(time)) {
      this.reservaForm.controls.time.markAsTouched();
      return;
    }

    if (this.showNoAvailabilityWarning()) {
      this.showFloating('Lo sentimos, no hay disponibilidad para la fecha y hora seleccionada. Por favor elija otra fecha u hora');
      return;
    }

    if (this.reservaForm.invalid) {
      this.reservaForm.markAllAsTouched();
      return;
    }

    const formValue = this.reservaForm.getRawValue();
    const currentUser = this.authService.currentUser();

    const selectedDecoration = DECORATION_OPTIONS.find((item) => item.id === formValue.decorationId);
    const selectedZone = ZONE_OPTIONS.find((item) => item.id === formValue.zoneId);
    const preorderItems = this.getSelectedPreorders();

    this.loading.set(true);

    this.reservationService
      .create({
        clienteId: currentUser?.id ?? 'guest-client',
        guestName: currentUser?.fullName ?? 'Cliente',
        guests: formValue.guests,
        date: formValue.date,
        time: formValue.time,
        decorationId: selectedDecoration?.id,
        decorationName: selectedDecoration?.name,
        zoneId: selectedZone?.id,
        zoneName: selectedZone?.name,
        notes: formValue.notes.trim(),
        preorderItems
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          void this.router.navigateByUrl('/app/cliente', {
            state: { flashMessage: 'Reserva registrada con éxito.' }
          });
        },
        error: () => {
          this.loading.set(false);
          this.showFloating('No fue posible registrar la reserva. Intenta nuevamente.');
        }
      });
  }

  private getSelectedPreorders(): ReservaPreorderItem[] {
    return this.preorderSelections
      .filter((item) => item.selected)
      .map((item) => ({
        productId: item.productId,
        productName: item.productName,
        quantity: item.quantity
      }));
  }

  private updateAvailability(): void {
    this.showNoAvailabilityWarning.set(false);

    const date = this.reservaForm.controls.date.value;
    const time = this.reservaForm.controls.time.value;

    if (!date || !time) {
      this.availableDecorations.set([]);
      this.availableZones.set([]);
      this.reservaForm.controls.decorationId.setValue('');
      this.reservaForm.controls.zoneId.setValue('');
      return;
    }

    if (this.isDateTimeInPast(date, time) || !this.isWithinReservationHours(time)) {
      this.availableDecorations.set([]);
      this.availableZones.set([]);
      this.reservaForm.controls.decorationId.setValue('');
      this.reservaForm.controls.zoneId.setValue('');
      return;
    }

    const unavailable = this.isSlotUnavailable(date, time);
    if (unavailable) {
      this.availableDecorations.set([]);
      this.availableZones.set([]);
      this.reservaForm.controls.decorationId.setValue('');
      this.reservaForm.controls.zoneId.setValue('');
      this.showNoAvailabilityWarning.set(true);
      return;
    }

    const day = new Date(`${date}T00:00:00`).getDay();
    const decorations = DECORATION_OPTIONS.filter((item) => item.availableDays.includes(day));
    this.availableDecorations.set(decorations);

    if (!decorations.some((item) => item.id === this.reservaForm.controls.decorationId.value)) {
      this.reservaForm.controls.decorationId.setValue('');
      this.reservaForm.controls.zoneId.setValue('');
    }

    this.updateAvailableZones();
  }

  private updateAvailableZones(): void {
    const activeDecorationIds = this.availableDecorations().map((item) => item.id);
    if (activeDecorationIds.length === 0) {
      this.availableZones.set([]);
      this.reservaForm.controls.zoneId.setValue('');
      return;
    }

    const selectedDecorationId = this.reservaForm.controls.decorationId.value;

    const zones = ZONE_OPTIONS.filter((zone) => {
      if (selectedDecorationId) {
        return zone.decorationIds.includes(selectedDecorationId);
      }

      return zone.decorationIds.some((id) => activeDecorationIds.includes(id));
    });

    this.availableZones.set(zones);

    if (!zones.some((zone) => zone.id === this.reservaForm.controls.zoneId.value)) {
      this.reservaForm.controls.zoneId.setValue('');
    }
  }

  private isWithinReservationHours(time: string): boolean {
    const [hours, minutes] = time.split(':').map(Number);
    const totalMinutes = hours * 60 + minutes;
    const start = 17 * 60;
    const end = 22 * 60;
    return totalMinutes >= start && totalMinutes <= end;
  }

  private isDateTimeInPast(date: string, time: string): boolean {
    const selected = new Date(`${date}T${time}:00`);
    return selected.getTime() < Date.now();
  }

  private isSlotUnavailable(date: string, time: string): boolean {
    if (FULLY_BOOKED_SLOTS.some((slot) => slot.date === date && slot.time === time)) {
      return true;
    }

    const sameSlotReservations = this.existingReservations.filter(
      (item) => item.date === date && item.time === time && item.status !== 'CANCELLED'
    );

    return sameSlotReservations.length >= 3;
  }

  private showFloating(message: string): void {
    this.floatingWarningMessage.set(message);
    this.showFloatingWarning.set(true);
    setTimeout(() => this.showFloatingWarning.set(false), 3500);
  }
}
