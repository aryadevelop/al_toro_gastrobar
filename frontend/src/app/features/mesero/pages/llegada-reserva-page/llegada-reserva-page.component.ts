import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MesaAsignacionPayload, MesaMapService, MesaZonaDisponible } from '../../../../core/services/mesa-map.service';
import { ReservationService } from '../../../../core/services/reservation.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

type OrigenAsignacion = 'mapa' | 'reservas' | null;

interface AsignacionNavState {
  openAsignacion?: boolean;
  origen?: OrigenAsignacion;
  reservaId?: string | number;
  numeroPersonas?: number;
  zonaId?: string | number;
}

@Component({
  selector: 'app-llegada-reserva-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid llegada-shell">
      <app-page-header title="Asignar mesa" subtitle="Identificador, zona y numero de personas"></app-page-header>

      <div class="toast" *ngIf="successMessage()">{{ successMessage() }}</div>

      <article class="card empty-card" *ngIf="!modalOpen()">
        <p>Selecciona una reserva o agrega una mesa desde el mapa para continuar.</p>
        <button class="btn-outline" type="button" (click)="goBack()">Volver</button>
      </article>

      <div class="modal-backdrop" *ngIf="modalOpen()" (click)="cancelarAsignacion()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <header class="modal-head">
            <div>
              <p class="modal-eyebrow">Asignacion de mesa</p>
              <h3>Completa los datos</h3>
              <p class="modal-sub" *ngIf="reservaId()">Reserva #{{ reservaId() }}</p>
            </div>
            <button class="btn-link" type="button" (click)="cancelarAsignacion()">Cerrar</button>
          </header>

          <form class="form-grid" [formGroup]="asignarForm" (ngSubmit)="onSubmit()">
            <label class="field">
              <span>Identificador de mesa</span>
              <input
                class="input-field"
                formControlName="mesaIdentificador"
                maxlength="20"
                (input)="clearFieldError('mesaIdentificador')"
              />
              <small class="field-error" *ngIf="mesaIdentificadorError()">{{ mesaIdentificadorError() }}</small>
            </label>

            <div class="field">
              <span>Zonas disponibles</span>
              <div class="zone-state" *ngIf="loadingZonas()">Cargando zonas...</div>
              <div class="zone-state error" *ngIf="!loadingZonas() && zonasError()">{{ zonasError() }}</div>

              <div class="zone-list" *ngIf="!loadingZonas() && zonas().length">
                <label class="zone-option" *ngFor="let zona of zonas()">
                  <input
                    type="radio"
                    formControlName="zonaId"
                    [value]="zona.id"
                    (change)="clearFieldError('zonaId')"
                  />
                  <span class="zone-name">{{ zona.name }}</span>
                  <span class="zone-meta">Disponibles: {{ zona.disponibilidad }}</span>
                </label>
              </div>
              <small class="field-error" *ngIf="zonaError()">{{ zonaError() }}</small>
            </div>

            <label class="field">
              <span>Numero de personas</span>
              <input
                class="input-field"
                type="number"
                min="1"
                formControlName="numeroPersonas"
                (input)="clearFieldError('numeroPersonas')"
              />
              <small class="field-error" *ngIf="numeroPersonasError()">{{ numeroPersonasError() }}</small>
            </label>

            <label class="field">
              <span>Notas (opcional)</span>
              <textarea class="input-field" rows="2" formControlName="mesaNotas"></textarea>
            </label>

            <div class="form-actions">
              <button class="btn-outline" type="button" (click)="cancelarAsignacion()">Cancelar</button>
              <button class="btn-primary" type="submit" [disabled]="saving()">
                {{ saving() ? 'Guardando...' : 'Guardar' }}
              </button>
            </div>
            <p class="form-error" *ngIf="generalError()">{{ generalError() }}</p>
          </form>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
        color: #333333;
        font-family: 'Manrope', 'Montserrat', 'Segoe UI', sans-serif;
      }

      .llegada-shell {
        gap: 1rem;
      }

      .toast {
        background: rgba(46, 125, 50, 0.12);
        color: #2e7d32;
        border: 1px solid rgba(46, 125, 50, 0.2);
        padding: 0.6rem 0.9rem;
        border-radius: 10px;
        font-size: 0.85rem;
      }

      .empty-card {
        padding: 1rem;
        display: grid;
        gap: 0.75rem;
        justify-items: start;
      }

      .modal-backdrop {
        position: fixed;
        inset: 0;
        display: grid;
        place-items: center;
        padding: 1rem;
        background: rgba(20, 12, 8, 0.45);
        z-index: 30;
      }

      .modal-card {
        width: min(720px, 94vw);
        background: #ffffff;
        border-radius: 16px;
        padding: 1rem;
        display: grid;
        gap: 0.8rem;
      }

      .modal-head {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 1rem;
      }

      .modal-eyebrow {
        margin: 0;
        font-size: 0.72rem;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        color: #a0a0a0;
      }

      .modal-head h3 {
        margin: 0.2rem 0 0;
        font-size: 1.1rem;
        color: #2c1810;
      }

      .modal-sub {
        margin: 0.2rem 0 0;
        font-size: 0.8rem;
        color: #6b4a3a;
      }

      .form-grid {
        display: grid;
        gap: 0.75rem;
      }

      .field {
        display: grid;
        gap: 0.3rem;
        font-size: 0.8rem;
      }

      .input-field {
        border: 1px solid rgba(44, 24, 16, 0.2);
        border-radius: 10px;
        padding: 0.45rem 0.6rem;
        font-size: 0.85rem;
      }

      .zone-state {
        font-size: 0.8rem;
        color: #6b4a3a;
      }

      .zone-state.error {
        color: #c41e3a;
      }

      .zone-list {
        display: grid;
        gap: 0.45rem;
        margin-top: 0.2rem;
      }

      .zone-option {
        display: flex;
        align-items: center;
        gap: 0.6rem;
        padding: 0.45rem 0.6rem;
        border-radius: 10px;
        border: 1px solid rgba(44, 24, 16, 0.12);
        font-size: 0.82rem;
        cursor: pointer;
      }

      .zone-name {
        font-weight: 600;
        color: #2c1810;
      }

      .zone-meta {
        margin-left: auto;
        color: #6b4a3a;
        font-size: 0.75rem;
      }

      .field-error {
        color: #c41e3a;
        font-size: 0.75rem;
      }

      .form-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
      }

      .form-error {
        margin: 0;
        color: #c41e3a;
        font-size: 0.8rem;
      }

      .btn-primary {
        border: none;
        background: #c41e3a;
        color: #ffffff;
        border-radius: 999px;
        padding: 0.45rem 1.1rem;
        font-size: 0.85rem;
        cursor: pointer;
      }

      .btn-outline {
        border: 1px solid rgba(44, 24, 16, 0.25);
        background: #ffffff;
        color: #2c1810;
        border-radius: 999px;
        padding: 0.45rem 1.1rem;
        font-size: 0.85rem;
        cursor: pointer;
      }

      .btn-link {
        border: none;
        background: none;
        color: #c41e3a;
        cursor: pointer;
      }

      @media (max-width: 720px) {
        .form-actions {
          flex-direction: column;
        }

        .form-actions button {
          width: 100%;
        }
      }
    `,
  ],
})
export class LlegadaReservaPageComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly mesaService = inject(MesaMapService);
  private readonly reservationService = inject(ReservationService);
  private readonly destroy$ = new Subject<void>();
  private readonly navState = this.router.getCurrentNavigation()?.extras.state as AsignacionNavState | undefined;

  readonly modalOpen = signal(false);
  readonly origen = signal<OrigenAsignacion>(null);
  readonly reservaId = signal<string | null>(null);
  readonly zonas = signal<MesaZonaDisponible[]>([]);
  readonly loadingZonas = signal(false);
  readonly zonasError = signal<string | null>(null);
  readonly saving = signal(false);
  readonly generalError = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly fieldErrors = signal<{ mesaIdentificador?: string; zonaId?: string; numeroPersonas?: string }>({});

  readonly asignarForm = this.formBuilder.nonNullable.group({
    mesaIdentificador: ['', [Validators.required, Validators.maxLength(20)]],
    zonaId: ['', [Validators.required]],
    numeroPersonas: [1, [Validators.required, Validators.min(1)]],
    mesaNotas: [''],
  });

  ngOnInit(): void {
    if (this.navState?.openAsignacion) {
      this.modalOpen.set(true);
      this.origen.set(this.navState?.origen ?? null);
      const reservaId = this.navState?.reservaId ? String(this.navState.reservaId) : null;
      if (reservaId) {
        this.reservaId.set(reservaId);
      }

      if (this.navState?.numeroPersonas) {
        this.asignarForm.patchValue({ numeroPersonas: this.navState.numeroPersonas });
      }

      if (this.navState?.zonaId) {
        this.asignarForm.patchValue({ zonaId: String(this.navState.zonaId) });
      }
    }

    this.cargarZonas();
    this.cargarReservaSiAplica();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit(): void {
    this.generalError.set(null);
    this.fieldErrors.set({});

    if (this.asignarForm.invalid) {
      this.asignarForm.markAllAsTouched();
      return;
    }

    const raw = this.asignarForm.getRawValue();
    const mesaIdentificador = raw.mesaIdentificador.trim();
    if (!mesaIdentificador) {
      this.fieldErrors.set({ mesaIdentificador: 'El identificador de mesa es obligatorio' });
      return;
    }

    const payload: MesaAsignacionPayload = {
      mesaIdentificador,
      zonaId: raw.zonaId,
      numeroPersonas: Number(raw.numeroPersonas),
      reservaId: this.reservaId() ?? undefined,
      mesaNotas: raw.mesaNotas?.trim() || undefined,
    };

    this.saving.set(true);
    this.mesaService
      .asignarMesa(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.modalOpen.set(false);
          this.successMessage.set('Mesa creada correctamente');
          timer(900)
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => this.goBack());
        },
        error: (error) => {
          this.saving.set(false);
          this.applyBackendError(error);
        },
      });
  }

  cancelarAsignacion(): void {
    this.modalOpen.set(false);
    this.goBack();
  }

  goBack(): void {
    const origen = this.origen();
    if (origen === 'reservas') {
      this.router.navigateByUrl('/app/mesero/reservas');
      return;
    }

    this.router.navigateByUrl('/app/mesero/mesas');
  }

  clearFieldError(field: 'mesaIdentificador' | 'zonaId' | 'numeroPersonas'): void {
    const current = this.fieldErrors();
    if (!current[field]) {
      return;
    }
    this.fieldErrors.set({ ...current, [field]: undefined });
  }

  mesaIdentificadorError(): string | null {
    const server = this.fieldErrors().mesaIdentificador;
    if (server) {
      return server;
    }

    const control = this.asignarForm.controls.mesaIdentificador;
    if (control.touched && control.hasError('required')) {
      return 'El identificador de mesa es obligatorio';
    }
    if (control.touched && control.hasError('maxlength')) {
      return 'El identificador ingresado no puede superar los 20 caracteres. Por favor elige otro.';
    }
    return null;
  }

  zonaError(): string | null {
    const server = this.fieldErrors().zonaId;
    if (server) {
      return server;
    }

    const control = this.asignarForm.controls.zonaId;
    if (control.touched && control.hasError('required')) {
      return 'La zona es obligatoria';
    }
    return null;
  }

  numeroPersonasError(): string | null {
    const server = this.fieldErrors().numeroPersonas;
    if (server) {
      return server;
    }

    const control = this.asignarForm.controls.numeroPersonas;
    if (control.touched && control.hasError('required')) {
      return 'El numero de personas es obligatorio';
    }
    if (control.touched && control.hasError('min')) {
      return 'El numero de personas debe ser al menos 1';
    }
    return null;
  }

  private cargarZonas(): void {
    this.loadingZonas.set(true);
    this.zonasError.set(null);

    this.mesaService
      .getZonasDisponibles()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (zonas) => {
          this.zonas.set(zonas);
          this.loadingZonas.set(false);
        },
        error: () => {
          this.zonasError.set('No pudimos cargar las zonas disponibles.');
          this.loadingZonas.set(false);
        },
      });
  }

  private cargarReservaSiAplica(): void {
    const reservaId = this.reservaId();
    if (!reservaId) {
      return;
    }

    this.reservationService
      .getDetail(reservaId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (detail) => {
          this.asignarForm.patchValue({
            numeroPersonas: detail.reservation.guests,
            zonaId: detail.reservation.zoneId ?? this.asignarForm.controls.zonaId.value,
          });
        },
        error: () => {
          // ignore
        },
      });
  }

  private applyBackendError(error: unknown): void {
    const message = this.extractErrorMessage(error);
    if (!message) {
      this.generalError.set('No pudimos crear la mesa.');
      return;
    }

    const normalized = message.toLowerCase();
    if (normalized.includes('identificador')) {
      this.fieldErrors.set({ ...this.fieldErrors(), mesaIdentificador: message });
      return;
    }

    if (normalized.includes('zona')) {
      this.fieldErrors.set({ ...this.fieldErrors(), zonaId: message });
      return;
    }

    if (normalized.includes('persona')) {
      this.fieldErrors.set({ ...this.fieldErrors(), numeroPersonas: message });
      return;
    }

    this.generalError.set(message);
  }

  private extractErrorMessage(error: unknown): string | null {
    if (error instanceof HttpErrorResponse) {
      const payload = error.error as { message?: string } | string | null;
      if (payload && typeof payload === 'object' && 'message' in payload && payload.message) {
        return payload.message;
      }
      if (typeof payload === 'string') {
        return payload;
      }
    }
    return null;
  }
}
