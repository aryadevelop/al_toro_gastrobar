import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, take, takeUntil } from 'rxjs';
import { MOCK_PRODUCTOS } from '../../../../core/mocks/restaurant.mock';
import { Reserva, ReservaPreorderItem } from '../../../../core/models/domain.models';
import { AuthService } from '../../../../core/services/auth.service';
import { ReservationService } from '../../../../core/services/reservation.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

interface DecorationOption {
  id: string;
  name: string;
  imageUrl: string;
  availableDays: number[];
  fixedZoneId?: string;
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

interface SpecialMenuOption {
  id: string;
  name: string;
  description: string;
  price: number;
}

const ROMANTIC_ZONE_ID = 'zona-romantica';
const ROMANTIC_ADDON_ID = 'addon-romantico';
const ROMANTIC_ADDON_LABEL = 'Agregar pétalos y velas';
const ROMANTIC_ADDON_COST = 20000;
const WHATSAPP_COMPANY_NUMBER = '573001112233';
const SPECIAL_MENU_HINT_MESSAGE = '¡Para más de 10 personas puedes pedir un mismo menú para todo el grupo! Revisa las opciones en la sección de Pre-orden';
const WHATSAPP_NOTE = 'Para confirmar tu reserva especial, debes abonar un valor anticipado, comunicate para definirlo';

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
    availableDays: [1, 2, 3, 4, 5],
    fixedZoneId: ROMANTIC_ZONE_ID
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
    id: ROMANTIC_ZONE_ID,
    name: 'Zona Romántica',
    imageUrl: 'https://picsum.photos/seed/zona-romantica/360/220',
    decorationIds: []
  },
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

const SPECIAL_MENU_OPTIONS: SpecialMenuOption[] = [
  {
    id: 'menu-especial-parrilla',
    name: 'Menú Especial Parrilla',
    description: 'Entrada + plato fuerte + bebida para todo el grupo',
    price: 180000
  },
  {
    id: 'menu-especial-premium',
    name: 'Menú Especial Premium',
    description: 'Parrilla premium + postre + bebida para todo el grupo',
    price: 240000
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
    <section class="page-grid cliente-compact">
      <app-page-header title="Nueva reserva"></app-page-header>

      <article class="floating-warning card" *ngIf="showFloatingWarning()">
        {{ floatingWarningMessage() }}
      </article>

      <article class="card reservation-card">
        <p class="availability-warning" *ngIf="showNoAvailabilityWarning()">
          Lo sentimos, no hay disponibilidad para la fecha y hora seleccionada. Por favor elija otra fecha u hora
        </p>

        <ng-container *ngIf="!showSummary(); else summaryView">
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
              <p class="zone-lock-message" *ngIf="isZoneSelectionLocked()">
                {{ zoneRestrictionMessage() }}
              </p>
              <div class="card-grid" *ngIf="availableZones().length > 0" [class.disabled-grid]="isZoneSelectionLocked()">
                <label class="option-card" *ngFor="let zone of availableZones()">
                  <input type="radio" name="zone" [value]="zone.id" formControlName="zoneId" [disabled]="isZoneSelectionLocked()" />
                  <img [src]="zone.imageUrl" [alt]="zone.name" />
                  <strong>{{ zone.name }}</strong>
                </label>
              </div>
            </section>

            <section class="form-section" *ngIf="showRomanticAddonOption()">
              <label class="addon-check">
                <input type="checkbox" formControlName="romanticAddon" />
                Agregar pétalos y velas (+$20.000)
              </label>
            </section>

            <section class="form-section">
              <span class="section-label">Pre-ordenar (opcional)</span>

              <p class="special-menu-hint" *ngIf="showSpecialMenuOption()">{{ specialMenuHintMessage }}</p>

              <div class="preorder-tabs" *ngIf="showSpecialMenuOption()">
                <button
                  type="button"
                  class="tab-btn"
                  [class.active]="activePreorderTab() === 'carta'"
                  (click)="setPreorderTab('carta')"
                >
                  Carta
                </button>
                <button
                  type="button"
                  class="tab-btn"
                  [class.active]="activePreorderTab() === 'especial'"
                  (click)="setPreorderTab('especial')"
                >
                  Menú Especial
                </button>
              </div>

              <div class="preorder-grid" *ngIf="activePreorderTab() === 'carta'">
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

              <div class="special-menu-grid" *ngIf="showSpecialMenuOption() && activePreorderTab() === 'especial'">
                <label class="special-menu-card" *ngFor="let menu of specialMenus">
                  <input type="radio" name="special-menu" [value]="menu.id" formControlName="specialMenuId" />
                  <div class="special-menu-copy">
                    <strong>{{ menu.name }}</strong>
                    <small>{{ menu.description }}</small>
                  </div>
                  <span>{{ menu.price | currency:'COP':'symbol':'1.0-0' }}</span>
                </label>
              </div>
            </section>

            <label class="form-label">
              <span>Notas adicionales (opcional)</span>
              <textarea class="input-field" rows="3" formControlName="notes"></textarea>
            </label>

            <div class="action-row">
              <button class="btn-secondary" type="button" (click)="onClose()">Cancelar</button>
              <button class="btn-primary" type="submit">Confirmar reserva</button>
            </div>
          </form>
        </ng-container>

        <ng-template #summaryView>
          <section class="summary-box">
            <h3>Resumen de reserva</h3>

            <p><strong>Cliente:</strong> {{ authService.currentUser()?.fullName ?? 'Cliente' }}</p>
            <p><strong>Fecha:</strong> {{ reservaForm.controls.date.value }}</p>
            <p><strong>Hora:</strong> {{ reservaForm.controls.time.value }}</p>
            <p><strong>Número de personas:</strong> {{ reservaForm.controls.guests.value }}</p>
            <p *ngIf="selectedDecorationName()"><strong>Decoración:</strong> {{ selectedDecorationName() }}</p>
            <p *ngIf="selectedZoneName()"><strong>Zona:</strong> {{ selectedZoneName() }}</p>
            <p><strong>Extras:</strong> {{ summaryExtrasText() }}</p>

            <ul class="summary-costs">
              <li>
                <span>Reserva base</span>
                <strong>Sin costo</strong>
              </li>
              <li *ngIf="reservaForm.controls.romanticAddon.value">
                <span>{{ romanticAddonLabel }}</span>
                <strong>{{ romanticAddonCost | currency:'COP':'symbol':'1.0-0' }}</strong>
              </li>
              <li *ngIf="selectedSpecialMenu() as menu">
                <span>{{ menu.name }}</span>
                <strong>{{ menu.price | currency:'COP':'symbol':'1.0-0' }}</strong>
              </li>
              <li class="summary-total">
                <span>Total extras</span>
                <strong>{{ totalExtraCost() | currency:'COP':'symbol':'1.0-0' }}</strong>
              </li>
            </ul>

            <p class="summary-note" *ngIf="hasExtraServices()">{{ whatsappNote }}</p>

            <div class="action-row">
              <button class="btn-secondary" type="button" (click)="onCancelSummary()">Cancelar</button>
              <button class="btn-primary" type="button" [disabled]="loading()" (click)="onConfirmReservation()">
                {{ loading() ? 'Reservando...' : 'Reservar' }}
              </button>
            </div>
          </section>
        </ng-template>
      </article>
    </section>
  `,
  styles: [
    `
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

      .zone-lock-message {
        margin: 0;
        border: 1px solid rgba(168, 24, 47, 0.35);
        border-radius: 8px;
        padding: 0.45rem 0.55rem;
        background: rgba(168, 24, 47, 0.08);
        color: #6b1111;
        font-size: 0.82rem;
      }

      .disabled-grid {
        opacity: 0.72;
        pointer-events: none;
      }

      .addon-check {
        display: flex;
        align-items: center;
        gap: 0.45rem;
        font-size: 0.84rem;
        font-weight: 600;
      }

      .special-menu-hint {
        margin: 0;
        padding: 0.5rem 0.6rem;
        border-radius: 8px;
        border: 1px solid rgba(168, 24, 47, 0.35);
        background: rgba(168, 24, 47, 0.08);
        color: #6b1111;
        font-size: 0.82rem;
      }

      .preorder-tabs {
        display: flex;
        gap: 0.4rem;
      }

      .tab-btn {
        border: 1px solid rgba(168, 24, 47, 0.4);
        background: #ffffff;
        color: #6b1111;
        border-radius: 8px;
        padding: 0.35rem 0.62rem;
        font-size: 0.8rem;
        cursor: pointer;
      }

      .tab-btn.active {
        background: #a8182f;
        color: #ffffff;
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

      .special-menu-grid {
        display: grid;
        gap: 0.5rem;
      }

      .special-menu-card {
        display: grid;
        grid-template-columns: auto 1fr auto;
        gap: 0.5rem;
        align-items: start;
        border: 1px solid rgba(10, 10, 10, 0.2);
        border-radius: 10px;
        padding: 0.5rem;
        background: #ffffff;
      }

      .special-menu-copy {
        display: grid;
        gap: 0.2rem;
      }

      .special-menu-copy strong {
        font-size: 0.84rem;
      }

      .special-menu-copy small {
        font-size: 0.76rem;
        color: var(--muted);
      }

      .special-menu-card span {
        font-size: 0.8rem;
        font-weight: 700;
        color: #6b1111;
      }

      .summary-box {
        display: grid;
        gap: 0.5rem;
      }

      .summary-box h3,
      .summary-box p {
        margin: 0;
      }

      .summary-costs {
        margin: 0;
        padding: 0;
        list-style: none;
        display: grid;
        gap: 0.35rem;
      }

      .summary-costs li {
        display: flex;
        justify-content: space-between;
        gap: 0.5rem;
        font-size: 0.84rem;
      }

      .summary-total {
        border-top: 1px dashed rgba(10, 10, 10, 0.2);
        padding-top: 0.35rem;
        font-weight: 700;
      }

      .summary-note {
        margin: 0;
        border: 1px solid rgba(168, 24, 47, 0.36);
        border-radius: 8px;
        padding: 0.45rem 0.55rem;
        background: rgba(168, 24, 47, 0.08);
        color: #6b1111;
        font-size: 0.8rem;
      }

      .action-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.45rem;
      }

      @media (max-width: 640px) {
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
        .form-section,
        .special-menu-grid,
        .summary-box {
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
        .section-label,
        .addon-check,
        .special-menu-copy strong,
        .summary-costs li {
          font-size: 0.76rem;
        }

        .special-menu-copy small,
        .special-menu-card span,
        .summary-note,
        .special-menu-hint,
        .zone-lock-message {
          font-size: 0.72rem;
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
  readonly showSummary = signal(false);
  readonly activePreorderTab = signal<'carta' | 'especial'>('carta');

  readonly romanticAddonCost = ROMANTIC_ADDON_COST;
  readonly romanticAddonLabel = ROMANTIC_ADDON_LABEL;
  readonly specialMenuHintMessage = SPECIAL_MENU_HINT_MESSAGE;
  readonly whatsappNote = WHATSAPP_NOTE;
  readonly specialMenus = SPECIAL_MENU_OPTIONS;

  readonly reservaForm = this.formBuilder.nonNullable.group({
    date: ['', [Validators.required]],
    time: ['', [Validators.required]],
    guests: [2, [Validators.required, Validators.min(1), Validators.max(20)]],
    decorationId: [''],
    zoneId: [''],
    romanticAddon: [false],
    specialMenuId: [''],
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
  private previousGuests = this.reservaForm.controls.guests.value;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly reservationService: ReservationService,
    public readonly authService: AuthService,
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
      this.syncRomanticAddonState();
    });

    this.reservaForm.controls.zoneId.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.syncRomanticAddonState();
    });

    this.reservaForm.controls.guests.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((value) => {
      if (value > 10 && this.previousGuests <= 10) {
        this.showFloating(this.specialMenuHintMessage);
      }

      if (value <= 10) {
        this.reservaForm.controls.specialMenuId.setValue('');
        if (this.activePreorderTab() === 'especial') {
          this.activePreorderTab.set('carta');
        }
      }

      this.previousGuests = value;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  showRomanticAddonOption(): boolean {
    return !this.reservaForm.controls.decorationId.value && this.reservaForm.controls.zoneId.value === ROMANTIC_ZONE_ID;
  }

  showSpecialMenuOption(): boolean {
    return this.reservaForm.controls.guests.value > 10;
  }

  setPreorderTab(tab: 'carta' | 'especial'): void {
    this.activePreorderTab.set(tab);
  }

  selectedSpecialMenu(): SpecialMenuOption | null {
    const menuId = this.reservaForm.controls.specialMenuId.value;
    return this.specialMenus.find((item) => item.id === menuId) ?? null;
  }

  selectedDecorationName(): string {
    const selected = this.selectedDecoration();
    return selected?.name ?? '';
  }

  selectedZoneName(): string {
    const selected = this.getZoneById(this.getEffectiveZoneId());
    return selected?.name ?? '';
  }

  isZoneSelectionLocked(): boolean {
    return Boolean(this.selectedDecoration()?.fixedZoneId);
  }

  zoneRestrictionMessage(): string {
    const fixedZone = this.getFixedZoneForDecoration();
    if (!fixedZone) {
      return '';
    }

    return `Esta decoración no permite seleccionar zona. La zona es [${fixedZone.name}]`;
  }

  summaryExtrasText(): string {
    const extras: string[] = [];

    if (this.reservaForm.controls.romanticAddon.value) {
      extras.push(this.romanticAddonLabel);
    }

    const menu = this.selectedSpecialMenu();
    if (menu) {
      extras.push(menu.name);
    }

    return extras.length > 0 ? extras.join(', ') : 'Sin costo extra';
  }

  hasExtraServices(): boolean {
    return this.reservaForm.controls.romanticAddon.value || !!this.selectedSpecialMenu();
  }

  totalExtraCost(): number {
    const menuCost = this.selectedSpecialMenu()?.price ?? 0;
    const romanticCost = this.reservaForm.controls.romanticAddon.value ? this.romanticAddonCost : 0;
    return menuCost + romanticCost;
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

    this.showSummary.set(true);
  }

  onCancelSummary(): void {
    this.showSummary.set(false);
  }

  onConfirmReservation(): void {
    if (this.loading()) {
      return;
    }

    const { date, time, decorationId } = this.reservaForm.getRawValue();
    const effectiveZoneId = this.getEffectiveZoneId();

    if (!date || !time) {
      this.showSummary.set(false);
      this.showFloating('La fecha y hora son obligatorias');
      return;
    }

    this.loading.set(true);

    this.reservationService.list().pipe(take(1)).subscribe({
      next: (items) => {
        this.existingReservations = items;

        if (!this.isSelectionStillAvailable(date, time, decorationId, effectiveZoneId)) {
          this.loading.set(false);
          this.showSummary.set(false);
          this.showFloating('Lo sentimos, la disponibilidad cambió. Por favor revise nuevamente.');
          return;
        }

        const payload = this.buildReservationPayload();

        this.reservationService.create(payload).subscribe({
          next: () => {
            this.loading.set(false);
            this.showSummary.set(false);

            if (payload.status === 'PENDING') {
              this.redirectToWhatsapp();
              return;
            }

            void this.router.navigateByUrl('/app/cliente', {
              state: { flashMessage: `Su reserva para el día ${payload.date} fue agendada correctamente` }
            });
          },
          error: () => {
            this.loading.set(false);
            this.showFloating('No fue posible registrar la reserva. Intenta nuevamente.');
          }
        });
      },
      error: () => {
        this.loading.set(false);
        this.showFloating('No fue posible verificar disponibilidad. Intenta nuevamente.');
      }
    });
  }

  private buildReservationPayload(): Omit<Reserva, 'id' | 'status'> & { status?: Reserva['status'] } {
    const formValue = this.reservaForm.getRawValue();
    const currentUser = this.authService.currentUser();
    const effectiveZoneId = this.getEffectiveZoneId();

    const selectedDecoration = DECORATION_OPTIONS.find((item) => item.id === formValue.decorationId);
    const selectedZone = this.getZoneById(effectiveZoneId);

    const preorderItems = this.getSelectedPreorders();

    if (formValue.romanticAddon) {
      preorderItems.push({
        productId: ROMANTIC_ADDON_ID,
        productName: ROMANTIC_ADDON_LABEL,
        quantity: 1
      });
    }

    const specialMenu = this.selectedSpecialMenu();
    if (specialMenu) {
      preorderItems.push({
        productId: specialMenu.id,
        productName: specialMenu.name,
        quantity: 1
      });
    }

    return {
      clienteId: currentUser?.id ?? 'guest-client',
      guestName: currentUser?.fullName ?? 'Cliente',
      guests: formValue.guests,
      date: formValue.date,
      time: formValue.time,
      decorationId: selectedDecoration?.id,
      decorationName: selectedDecoration?.name,
      zoneId: selectedZone?.id,
      zoneName: selectedZone?.name,
      notes: this.buildNotes(formValue.notes.trim()),
      preorderItems,
      status: this.hasExtraServices() ? 'PENDING' : 'CONFIRMED'
    };
  }

  private buildNotes(userNotes: string): string {
    const extras: string[] = [];

    if (this.reservaForm.controls.romanticAddon.value) {
      extras.push(`${this.romanticAddonLabel} (+$20.000)`);
    }

    const specialMenu = this.selectedSpecialMenu();
    if (specialMenu) {
      extras.push(`${specialMenu.name} (${this.formatCurrency(specialMenu.price)})`);
    }

    const extrasText = extras.length > 0 ? `Extras: ${extras.join(', ')}` : '';

    return [userNotes, extrasText].filter(Boolean).join(' | ');
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
      this.syncRomanticAddonState();
      return;
    }

    if (this.isDateTimeInPast(date, time) || !this.isWithinReservationHours(time)) {
      this.availableDecorations.set([]);
      this.availableZones.set([]);
      this.reservaForm.controls.decorationId.setValue('');
      this.reservaForm.controls.zoneId.setValue('');
      this.syncRomanticAddonState();
      return;
    }

    const unavailable = this.isSlotUnavailable(date, time, this.existingReservations);
    if (unavailable) {
      this.availableDecorations.set([]);
      this.availableZones.set([]);
      this.reservaForm.controls.decorationId.setValue('');
      this.reservaForm.controls.zoneId.setValue('');
      this.showNoAvailabilityWarning.set(true);
      this.syncRomanticAddonState();
      return;
    }

    const decorations = this.getDecorationsForDate(date);
    this.availableDecorations.set(decorations);

    if (!decorations.some((item) => item.id === this.reservaForm.controls.decorationId.value)) {
      this.reservaForm.controls.decorationId.setValue('');
      this.reservaForm.controls.zoneId.setValue('');
    }

    this.updateAvailableZones();
    this.syncRomanticAddonState();
  }

  private updateAvailableZones(): void {
    if (this.availableDecorations().length === 0) {
      this.availableZones.set([]);
      this.reservaForm.controls.zoneId.setValue('');
      return;
    }

    const selectedDecorationId = this.reservaForm.controls.decorationId.value;
    const zones = this.getZonesForSelection(selectedDecorationId);

    this.availableZones.set(zones);

    if (this.isZoneSelectionLocked()) {
      this.reservaForm.controls.zoneId.setValue('');
      return;
    }

    if (!zones.some((zone) => zone.id === this.reservaForm.controls.zoneId.value)) {
      this.reservaForm.controls.zoneId.setValue('');
    }
  }

  private syncRomanticAddonState(): void {
    if (!this.showRomanticAddonOption() && this.reservaForm.controls.romanticAddon.value) {
      this.reservaForm.controls.romanticAddon.setValue(false);
    }
  }

  private getDecorationsForDate(date: string): DecorationOption[] {
    const day = new Date(`${date}T00:00:00`).getDay();
    return DECORATION_OPTIONS.filter((item) => item.availableDays.includes(day));
  }

  private getZonesForSelection(selectedDecorationId: string): ZoneOption[] {
    if (!selectedDecorationId) {
      return [...ZONE_OPTIONS];
    }

    const selectedDecoration = DECORATION_OPTIONS.find((item) => item.id === selectedDecorationId);
    if (!selectedDecoration) {
      return [];
    }

    if (selectedDecoration.fixedZoneId) {
      const fixedZone = this.getZoneById(selectedDecoration.fixedZoneId);
      return fixedZone ? [fixedZone] : [];
    }

    return ZONE_OPTIONS.filter((zone) => {
      return zone.decorationIds.includes(selectedDecorationId);
    });
  }

  private isSelectionStillAvailable(date: string, time: string, decorationId: string, zoneId: string): boolean {
    if (this.isSlotUnavailable(date, time, this.existingReservations)) {
      return false;
    }

    const decorations = this.getDecorationsForDate(date);
    if (decorationId && !decorations.some((item) => item.id === decorationId)) {
      return false;
    }

    const zones = this.getZonesForSelection(decorationId);

    if (zoneId && !zones.some((item) => item.id === zoneId)) {
      return false;
    }

    return true;
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

  private isSlotUnavailable(date: string, time: string, reservations: Reserva[]): boolean {
    if (FULLY_BOOKED_SLOTS.some((slot) => slot.date === date && slot.time === time)) {
      return true;
    }

    const sameSlotReservations = reservations.filter(
      (item) => item.date === date && item.time === time && item.status !== 'CANCELLED'
    );

    return sameSlotReservations.length >= 3;
  }

  private redirectToWhatsapp(): void {
    const formValue = this.reservaForm.getRawValue();
    const extras = this.summaryExtrasText();

    const message = [
      'Hola, quiero confirmar una reserva especial en Al Toro Gastrobar.',
      `Fecha: ${formValue.date}`,
      `Hora: ${formValue.time}`,
      `Número de personas: ${formValue.guests}`,
      `Extras: ${extras}`,
      WHATSAPP_NOTE
    ].join('\n');

    const url = `https://wa.me/${WHATSAPP_COMPANY_NUMBER}?text=${encodeURIComponent(message)}`;
    window.location.href = url;
  }

  private selectedDecoration(): DecorationOption | undefined {
    return DECORATION_OPTIONS.find((item) => item.id === this.reservaForm.controls.decorationId.value);
  }

  private getFixedZoneForDecoration(): ZoneOption | undefined {
    const fixedZoneId = this.selectedDecoration()?.fixedZoneId;
    if (!fixedZoneId) {
      return undefined;
    }

    return this.getZoneById(fixedZoneId);
  }

  private getZoneById(zoneId: string): ZoneOption | undefined {
    return ZONE_OPTIONS.find((item) => item.id === zoneId);
  }

  private getEffectiveZoneId(): string {
    const fixedZoneId = this.selectedDecoration()?.fixedZoneId;
    if (fixedZoneId) {
      return fixedZoneId;
    }

    return this.reservaForm.controls.zoneId.value;
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0
    }).format(value);
  }

  private showFloating(message: string): void {
    this.floatingWarningMessage.set(message);
    this.showFloatingWarning.set(true);
    setTimeout(() => this.showFloatingWarning.set(false), 3500);
  }
}
