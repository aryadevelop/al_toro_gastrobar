import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MesaItemComanda, MesaMapService } from '../../../../core/services/mesa-map.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-comanda-editor-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid comanda-shell">
      <app-page-header title="Editor de comanda" subtitle="Control de productos y produccion"></app-page-header>

      <article class="card comanda-card">
        <header class="comanda-head">
          <div>
            <p class="comanda-eyebrow">Mesa</p>
            <h3 class="comanda-title">{{ mesaIdentificador() || mesaId() || 'Sin mesa' }}</h3>
          </div>
          <button class="btn-outline" type="button" (click)="goBack()">Volver al mapa</button>
        </header>

        <p class="action-message" *ngIf="actionMessage()">{{ actionMessage() }}</p>
        <p class="comanda-meta" *ngIf="comandaId()">Comanda ID: {{ comandaId() }}</p>

        <section class="comanda-summary">
          <h4>En produccion</h4>
          <div class="summary-state" *ngIf="loadingItems()">Cargando items...</div>
          <div class="summary-state error" *ngIf="!loadingItems() && loadError()">{{ loadError() }}</div>
          <div class="summary-state" *ngIf="!loadingItems() && !loadError() && !itemsProduccion().length">
            Sin productos en produccion.
          </div>
          <ul class="summary-list" *ngIf="!loadingItems() && itemsProduccion().length">
            <li *ngFor="let item of itemsProduccion()">
              <div>
                <p class="item-name">{{ item.nombreProducto }}</p>
                <p class="item-sub" *ngIf="item.descripcion">{{ item.descripcion }}</p>
              </div>
              <span class="item-qty">x{{ item.cantidad }}</span>
            </li>
          </ul>
        </section>

        <form class="form-grid" [formGroup]="comandaForm" (ngSubmit)="onSubmit()">
          <label>
            <span>Mesa</span>
            <input class="input-field" formControlName="mesaCode" readonly />
          </label>
          <label>
            <span>Producto</span>
            <input class="input-field" formControlName="productName" />
          </label>
          <label>
            <span>Cantidad</span>
            <input class="input-field" type="number" min="1" formControlName="quantity" />
          </label>
          <label>
            <span>Observaciones</span>
            <textarea class="input-field" rows="3" formControlName="notes"></textarea>
          </label>
          <button class="btn-primary" type="submit">Guardar cambios</button>
          <p class="save-note" *ngIf="saved()">Cambios guardados localmente.</p>
        </form>
      </article>
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
        color: #333333;
        font-family: 'Manrope', 'Montserrat', 'Segoe UI', sans-serif;
      }

      .comanda-shell {
        gap: 1rem;
      }

      .comanda-card {
        padding: 1rem;
        max-width: 760px;
        background: radial-gradient(circle at top right, rgba(232, 213, 183, 0.35), #ffffff 60%);
        border: 1px solid rgba(44, 24, 16, 0.1);
      }

      .comanda-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 1rem;
        margin-bottom: 1rem;
      }

      .comanda-eyebrow {
        margin: 0;
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        color: #a0a0a0;
      }

      .comanda-title {
        margin: 0.2rem 0 0;
        font-size: 1.2rem;
        color: #2c1810;
      }

      .comanda-summary {
        background: rgba(44, 24, 16, 0.05);
        border-radius: 12px;
        padding: 0.75rem;
        margin-bottom: 1rem;
      }

      .comanda-summary h4 {
        margin: 0 0 0.5rem;
        font-size: 0.9rem;
        color: #2c1810;
      }

      .summary-state {
        font-size: 0.85rem;
        color: #6b7280;
      }

      .summary-state.error {
        color: #c41e3a;
      }

      .summary-list {
        list-style: none;
        padding: 0;
        margin: 0;
        display: grid;
        gap: 0.5rem;
      }

      .summary-list li {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 0.75rem;
        padding: 0.45rem 0.6rem;
        border-radius: 10px;
        background: #ffffff;
        border: 1px solid rgba(44, 24, 16, 0.08);
      }

      .item-name {
        margin: 0;
        font-size: 0.85rem;
        font-weight: 600;
      }

      .item-sub {
        margin: 0.15rem 0 0;
        font-size: 0.75rem;
        color: #a0a0a0;
      }

      .item-qty {
        font-weight: 700;
        color: #c41e3a;
      }

      .btn-outline {
        border: 1px solid rgba(44, 24, 16, 0.25);
        background: #ffffff;
        color: #2c1810;
        border-radius: 999px;
        padding: 0.35rem 0.9rem;
        font-size: 0.78rem;
        cursor: pointer;
      }

      .btn-primary {
        border: none;
        background: #c41e3a;
        color: #ffffff;
        border-radius: 999px;
        padding: 0.4rem 1rem;
        font-size: 0.85rem;
        cursor: pointer;
      }

      .action-message {
        margin: 0 0 0.5rem;
        padding: 0.45rem 0.7rem;
        border-radius: 10px;
        background: rgba(46, 125, 50, 0.12);
        color: #2e7d32;
        font-size: 0.8rem;
        font-weight: 600;
      }

      .comanda-meta {
        margin: 0 0 0.8rem;
        font-size: 0.78rem;
        color: #6b7280;
      }

      .save-note {
        margin: 0;
        color: #2e7d32;
        font-size: 0.85rem;
      }

      @media (max-width: 720px) {
        .comanda-head {
          flex-direction: column;
          align-items: flex-start;
        }
      }
    `,
  ],
})
export class ComandaEditorPageComponent implements OnInit, OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly mesaService = inject(MesaMapService);
  private readonly destroy$ = new Subject<void>();

  readonly saved = signal(false);
  readonly mesaId = signal<string | null>(null);
  readonly mesaIdentificador = signal<string | null>(null);
  readonly comandaId = signal<string | null>(null);
  readonly actionMessage = signal('');
  readonly itemsProduccion = signal<MesaItemComanda[]>([]);
  readonly loadingItems = signal(false);
  readonly loadError = signal<string | null>(null);

  readonly comandaForm = this.formBuilder.nonNullable.group({
    mesaCode: ['', [Validators.required]],
    productName: ['', [Validators.required]],
    quantity: [1, [Validators.required, Validators.min(1)]],
    notes: [''],
  });

  ngOnInit(): void {
    const navMessage = this.router.getCurrentNavigation()?.extras.state?.['actionMessage'];
    if (typeof navMessage === 'string' && navMessage.trim().length > 0) {
      this.actionMessage.set(navMessage.trim());
    }

    const comandaId = this.route.snapshot.queryParamMap.get('comandaId');
    if (comandaId) {
      this.comandaId.set(comandaId);
      if (!this.actionMessage()) {
        this.actionMessage.set('Comanda lista para modificar');
      }
    }

    const mesaId = this.route.snapshot.queryParamMap.get('mesaId');
    if (mesaId) {
      this.mesaId.set(mesaId);
      this.comandaForm.patchValue({ mesaCode: mesaId });
      this.fetchItemsProduccion(mesaId);
      return;
    }

    this.loadError.set('Selecciona una mesa desde el mapa para ver su comanda.');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit(): void {
    if (this.comandaForm.invalid) {
      this.comandaForm.markAllAsTouched();
      return;
    }

    this.saved.set(true);
  }

  goBack(): void {
    this.router.navigateByUrl('/app/mesero/mesas');
  }

  private fetchItemsProduccion(mesaId: string): void {
    this.loadingItems.set(true);
    this.loadError.set(null);

    this.mesaService
      .getItemsProduccion(mesaId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.mesaIdentificador.set(data.identificadorMesa);
          this.itemsProduccion.set(data.items);
          if (data.identificadorMesa) {
            this.comandaForm.patchValue({ mesaCode: data.identificadorMesa });
          }
          this.loadingItems.set(false);
        },
        error: () => {
          this.loadError.set('No pudimos cargar los items en produccion.');
          this.loadingItems.set(false);
        },
      });
  }
}
