import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of, Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { ComandaDraftData, ComandaDraftItem, ComandaService } from '../../../../core/services/comanda.service';
import { ProductCatalogService, CartaCatalogItem } from '../../../../core/services/product-catalog.service';
import { MesaItemComanda, MesaMapService } from '../../../../core/services/mesa-map.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

type DraftCategory = 'Platos' | 'Bebidas';

interface DraftItem {
  id: string;
  baseProductId: string;
  name: string;
  category: DraftCategory;
  quantity: number;
  unitPrice: number;
  description: string;
  isModification: boolean;
  parentId?: string;
  stockBase: number;
}

@Component({
  selector: 'app-comanda-editor-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, ConfirmDialogComponent],
  template: `
    <section class="page-grid comanda-shell">
      <app-page-header title="Modificar comanda" subtitle="Edición de borrador para cocina y barra"></app-page-header>

      <article class="card comanda-card">
        <header class="comanda-head">
          <div>
            <p class="comanda-eyebrow">Mesa</p>
            <h3 class="comanda-title">{{ mesaIdentificador() || mesaId() || 'Sin mesa' }}</h3>
          </div>
          <button class="btn-outline" type="button" (click)="onCancelForm()">Cancelar</button>
        </header>

        <p class="action-message" *ngIf="actionMessage()" [ngClass]="actionTone()">{{ actionMessage() }}</p>
        <p class="comanda-meta" *ngIf="comandaId()">Comanda ID: {{ comandaId() }}</p>

        <section class="search-shell">
          <label>
            <span>Buscador de productos/preparaciones</span>
            <input
              class="input-field"
              [value]="searchTerm()"
              (input)="onSearchInput($any($event.target).value)"
              placeholder="Ej: Lomo, limonada, pasta..."
            />
          </label>

          <ul class="suggestions" *ngIf="filteredSuggestions().length">
            <li *ngFor="let item of filteredSuggestions()">
              <button type="button" (click)="onSelectSuggestion(item)">
                <span>{{ item.productName }}</span>
                <small>{{ item.unitPrice | currency:'COP':'symbol':'1.0-0' }}</small>
              </button>
            </li>
          </ul>
          <p class="suggestions-empty" *ngIf="searchTerm() && !filteredSuggestions().length">
            No hay resultados para la busqueda.
          </p>
        </section>

        <section class="totals-grid">
          <article class="totals-box">
            <h4>Subtotal borrador</h4>
            <p>{{ subtotalDraft() | currency:'COP':'symbol':'1.0-0' }}</p>
          </article>
          <article class="totals-box">
            <h4>Total acumulado</h4>
            <p>{{ totalAccumulated() | currency:'COP':'symbol':'1.0-0' }}</p>
          </article>
        </section>

        <section class="comanda-summary">
          <h4>Resumen enviado a producción</h4>
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

        <section class="draft-section">
          <h4>Platos</h4>
          <p class="summary-state" *ngIf="!draftPlatos().length">Sin platos en borrador.</p>
          <ng-container *ngFor="let item of draftPlatos()">
            <article class="draft-item" [class.draft-item--mod]="item.isModification">
              <div class="item-main">
                <div>
                  <p class="item-name">{{ item.name }}</p>
                  <p class="item-sub">Unitario {{ item.unitPrice | currency:'COP':'symbol':'1.0-0' }}</p>
                </div>
                <strong>{{ itemSubtotal(item) | currency:'COP':'symbol':'1.0-0' }}</strong>
              </div>

              <div class="item-actions">
                <div class="qty-controls">
                  <button type="button" (click)="changeQuantity(item.id, -1)">-</button>
                  <input
                    type="number"
                    min="1"
                    max="250"
                    [value]="item.quantity"
                    (change)="setQuantity(item.id, $any($event.target).value)"
                  />
                  <button type="button" (click)="changeQuantity(item.id, 1)">+</button>
                </div>

                <button class="btn-danger" type="button" (click)="requestDelete(item.id)">Eliminar</button>
              </div>

              <div class="mod-editor">
                <label>
                  <span>Descripción/modificación</span>
                  <input
                    class="input-field"
                    [value]="item.description"
                    (change)="setDescription(item.id, $any($event.target).value)"
                    placeholder="Sin cebolla, término 3/4, etc."
                  />
                </label>

                <label *ngIf="item.isModification" class="price-field">
                  <span>Valor item mod.</span>
                  <input
                    class="input-field"
                    type="number"
                    min="0"
                    [value]="item.unitPrice"
                    (input)="setItemPrice(item.id, $any($event.target).value)"
                  />
                </label>
              </div>

              <div class="add-mod" *ngIf="!item.isModification">
                <input
                  class="input-field"
                  [value]="modificationDraft(item.id)"
                  (input)="setModificationDraft(item.id, $any($event.target).value)"
                  placeholder="Nueva modificación"
                />
                <button class="btn-outline" type="button" (click)="addModification(item.id)">Añadir</button>
              </div>
            </article>
          </ng-container>
        </section>

        <section class="draft-section">
          <h4>Bebidas</h4>
          <p class="summary-state" *ngIf="!draftBebidas().length">Sin bebidas en borrador.</p>
          <ng-container *ngFor="let item of draftBebidas()">
            <article class="draft-item" [class.draft-item--mod]="item.isModification">
              <div class="item-main">
                <div>
                  <p class="item-name">{{ item.name }}</p>
                  <p class="item-sub">Unitario {{ item.unitPrice | currency:'COP':'symbol':'1.0-0' }}</p>
                </div>
                <strong>{{ itemSubtotal(item) | currency:'COP':'symbol':'1.0-0' }}</strong>
              </div>

              <div class="item-actions">
                <div class="qty-controls">
                  <button type="button" (click)="changeQuantity(item.id, -1)">-</button>
                  <input
                    type="number"
                    min="1"
                    max="250"
                    [value]="item.quantity"
                    (input)="setQuantity(item.id, $any($event.target).value)"
                  />
                  <button type="button" (click)="changeQuantity(item.id, 1)">+</button>
                </div>

                <button class="btn-danger" type="button" (click)="requestDelete(item.id)">Eliminar</button>
              </div>

              <div class="mod-editor">
                <label>
                  <span>Descripción/modificación</span>
                  <input
                    class="input-field"
                    [value]="item.description"
                    (change)="setDescription(item.id, $any($event.target).value)"
                    placeholder="Sin hielo, menos azúcar, etc."
                  />
                </label>

                <label *ngIf="item.isModification" class="price-field">
                  <span>Valor item mod.</span>
                  <input
                    class="input-field"
                    type="number"
                    min="0"
                    [value]="item.unitPrice"
                    (input)="setItemPrice(item.id, $any($event.target).value)"
                  />
                </label>
              </div>

              <div class="add-mod" *ngIf="!item.isModification">
                <input
                  class="input-field"
                  [value]="modificationDraft(item.id)"
                  (input)="setModificationDraft(item.id, $any($event.target).value)"
                  placeholder="Nueva modificación"
                />
                <button class="btn-outline" type="button" (click)="addModification(item.id)">Añadir</button>
              </div>
            </article>
          </ng-container>
        </section>

        <form class="notes-grid" [formGroup]="comandaForm">
          <label>
            <span>Notas para cocina</span>
            <textarea class="input-field" rows="3" formControlName="kitchenNotes"></textarea>
          </label>
          <label>
            <span>Notas para barra</span>
            <textarea class="input-field" rows="3" formControlName="barNotes"></textarea>
          </label>
        </form>

        <footer class="action-row">
          <button class="btn-outline" type="button" (click)="onSaveDraftAndClose()">Cerrar</button>
          <button
            class="btn-outline"
            type="button"
            [disabled]="!canSaveDraft()"
            [attr.title]="!canSaveDraft() ? 'Agrega al menos un producto para guardar.' : null"
            (click)="onSaveDraftOnly()"
          >
            Guardar como espera
          </button>
          <button
            class="btn-primary"
            type="button"
            [disabled]="!canSendToKitchen()"
            [attr.title]="!canSendToKitchen() ? 'Agrega al menos un plato para enviar a cocina.' : null"
            (click)="onSendToKitchen()"
          >
            Enviar a cocina
          </button>
          <button
            class="btn-primary"
            type="button"
            [disabled]="!canSendToBar()"
            [attr.title]="!canSendToBar() ? 'Agrega al menos una bebida para enviar a barra.' : null"
            (click)="onSendToBar()"
          >
            Enviar a barra
          </button>
        </footer>

        <p class="action-hint" *ngIf="!canSaveDraft()">
          Agrega productos a la comanda para habilitar las acciones.
        </p>

        <p class="save-note" *ngIf="saved()">Comanda guardada con éxito.</p>
      </article>

      <app-confirm-dialog
        [open]="showDeleteDialog()"
        title="Eliminar item"
        message="¿Seguro que deseas eliminar este item de la comanda?"
        cancelLabel="Cancelar"
        confirmLabel="Sí, eliminar"
        (cancel)="cancelDelete()"
        (confirm)="confirmDelete()"
      ></app-confirm-dialog>

      <app-confirm-dialog
        [open]="showCancelDialog()"
        title="Cancelar edición"
        message="¿Cancelar la edición? Se perderán las comandas no enviadas a producción"
        cancelLabel="No, seguir editando"
        confirmLabel="Sí, cancelar"
        (cancel)="showCancelDialog.set(false)"
        (confirm)="confirmCancelForm()"
      ></app-confirm-dialog>
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
        max-width: 980px;
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
        margin: 1rem 0;
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

      .btn-primary:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .btn-danger {
        border: 1px solid rgba(196, 30, 58, 0.4);
        background: rgba(196, 30, 58, 0.08);
        color: #a1172f;
        border-radius: 999px;
        padding: 0.34rem 0.76rem;
        font-size: 0.75rem;
      }

      .search-shell {
        display: grid;
        gap: 0.5rem;
      }

      .suggestions {
        list-style: none;
        margin: 0;
        padding: 0;
        border: 1px solid rgba(44, 24, 16, 0.16);
        border-radius: 12px;
        max-height: 220px;
        overflow: auto;
      }

      .suggestions li + li {
        border-top: 1px solid rgba(44, 24, 16, 0.08);
      }

      .suggestions button {
        width: 100%;
        border: none;
        background: transparent;
        padding: 0.55rem 0.75rem;
        display: flex;
        justify-content: space-between;
        align-items: center;
        cursor: pointer;
      }

      .suggestions-empty {
        margin: 0;
        font-size: 0.78rem;
        color: #6b7280;
      }

      .totals-grid {
        margin-top: 0.8rem;
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 0.6rem;
      }

      .totals-box {
        border: 1px solid rgba(44, 24, 16, 0.12);
        border-radius: 10px;
        padding: 0.65rem;
        background: #fff;
      }

      .totals-box h4 {
        margin: 0;
        font-size: 0.82rem;
      }

      .totals-box p {
        margin: 0.25rem 0 0;
        font-weight: 700;
      }

      .draft-section {
        margin-top: 1rem;
        display: grid;
        gap: 0.55rem;
      }

      .draft-section h4 {
        margin: 0;
      }

      .draft-item {
        border: 1px solid rgba(44, 24, 16, 0.15);
        border-radius: 10px;
        padding: 0.62rem;
        display: grid;
        gap: 0.45rem;
        background: #fff;
      }

      .draft-item--mod {
        margin-left: 1.25rem;
        border-style: dashed;
      }

      .item-main {
        display: flex;
        justify-content: space-between;
        gap: 0.75rem;
      }

      .item-actions {
        display: flex;
        justify-content: space-between;
        gap: 0.6rem;
        align-items: center;
      }

      .qty-controls {
        display: inline-flex;
        gap: 0.25rem;
        align-items: center;
      }

      .qty-controls button {
        border: 1px solid rgba(44, 24, 16, 0.2);
        background: #fff;
        border-radius: 8px;
        width: 28px;
        height: 28px;
      }

      .qty-controls input {
        width: 72px;
        border: 1px solid rgba(44, 24, 16, 0.2);
        border-radius: 8px;
        text-align: center;
        padding: 0.28rem;
      }

      .mod-editor {
        display: grid;
        grid-template-columns: minmax(0, 1fr) 130px;
        gap: 0.45rem;
      }

      .mod-editor .price-field {
        align-self: end;
      }

      .add-mod {
        display: grid;
        grid-template-columns: minmax(0, 1fr) auto;
        gap: 0.45rem;
      }

      .notes-grid {
        margin-top: 0.9rem;
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 0.6rem;
      }

      .action-row {
        margin-top: 1rem;
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
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

      .action-message.error {
        background: rgba(196, 30, 58, 0.12);
        color: #a1172f;
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

      .action-hint {
        margin: 0.35rem 0 0;
        font-size: 0.78rem;
        color: #6b7280;
      }

      .integration-note {
        margin: 0;
        max-width: 980px;
        padding: 0.55rem 0.75rem;
        border-radius: 10px;
        border: 1px solid rgba(146, 64, 14, 0.2);
        background: rgba(245, 158, 11, 0.1);
        color: #92400e;
        font-size: 0.78rem;
        font-weight: 600;
      }

      @media (max-width: 720px) {
        .comanda-head {
          flex-direction: column;
          align-items: flex-start;
        }

        .totals-grid,
        .notes-grid,
        .mod-editor {
          grid-template-columns: 1fr;
        }

        .add-mod {
          grid-template-columns: 1fr;
        }

        .action-row {
          flex-direction: column;
        }

        .action-row button {
          width: 100%;
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
  private readonly comandaService = inject(ComandaService);
  private readonly productCatalogService = inject(ProductCatalogService);
  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject = new Subject<string>();
  private readonly defaultStock = 250;

  readonly saved = signal(false);
  readonly mesaId = signal<string | null>(null);
  readonly mesaIdentificador = signal<string | null>(null);
  readonly comandaId = signal<string | null>(null);
  readonly actionMessage = signal('');
  readonly actionTone = signal<'success' | 'error'>('success');
  readonly itemsProduccion = signal<MesaItemComanda[]>([]);
  readonly loadingItems = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly loadingCatalog = signal(false);
  readonly catalog = signal<CartaCatalogItem[]>([]);
  readonly draftItems = signal<DraftItem[]>([]);
  readonly searchTerm = signal('');
  readonly filteredSuggestions = signal<CartaCatalogItem[]>([]);
  readonly showDeleteDialog = signal(false);
  readonly showCancelDialog = signal(false);
  readonly deletingItemId = signal<string | null>(null);
  readonly modificationDraftMap = signal<Record<string, string>>({});
  readonly comandaCocinaId = signal<string | null>(null);
  readonly comandaBarraId = signal<string | null>(null);
  readonly subtotalBorrador = signal(0);
  readonly totalBackend = signal(0);

  readonly comandaForm = this.formBuilder.nonNullable.group({
    kitchenNotes: [''],
    barNotes: [''],
  });

  ngOnInit(): void {
    if (this.isReloadNavigation()) {
      this.router.navigateByUrl('/app/mesero/mesas', { replaceUrl: true });
      return;
    }

    const navMessage = this.router.getCurrentNavigation()?.extras.state?.['actionMessage'];
    if (typeof navMessage === 'string' && navMessage.trim().length > 0) {
      this.actionMessage.set(navMessage.trim());
    }

    this.setupSearch();

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
      this.fetchBorrador(mesaId);
      this.fetchItemsProduccion(mesaId);
      this.fetchMesaDetalle(mesaId);
      return;
    }

    this.loadError.set('Selecciona una mesa desde el mapa para ver su comanda.');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  draftPlatos(): DraftItem[] {
    return this.groupedDraftItems('Platos');
  }

  draftBebidas(): DraftItem[] {
    return this.groupedDraftItems('Bebidas');
  }

  onSearchInput(rawValue: string): void {
    this.searchTerm.set(rawValue);
    const term = rawValue.trim().toLowerCase();
    
    if (!term) {
      this.filteredSuggestions.set([]);
      this.searchSubject.next('');
      return;
    }
    
    this.searchSubject.next(term);
  }

  onSelectSuggestion(item: CartaCatalogItem): void {
    const visitaId = this.mesaId();
    if (!visitaId) {
      return;
    }

    this.comandaService
      .addItem({ visitaId, productoId: item.productId, cantidad: 1 })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (draft) => {
          this.applyDraft(draft);
          this.searchTerm.set('');
          this.filteredSuggestions.set([]);
          this.actionTone.set('success');
          this.actionMessage.set('Producto agregado al borrador.');
        },
        error: (error) => this.handleActionError(this.extractErrorMessage(error) || 'No pudimos agregar el producto al borrador.'),
      });
  }

  changeQuantity(itemId: string, delta: number): void {
    const target = this.draftItems().find((item) => item.id === itemId);
    if (!target) {
      return;
    }
    this.updateQuantityWithValidation(target, target.quantity + delta);
  }

  setQuantity(itemId: string, rawValue: string): void {
    const target = this.draftItems().find((item) => item.id === itemId);
    if (!target) {
      return;
    }
    this.updateQuantityWithValidation(target, Number(rawValue));
  }

  setDescription(itemId: string, value: string): void {
    this.comandaService
      .updateItem(itemId, { descripcion: value.trim() })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (draft) => this.applyDraft(draft),
        error: (error) => this.handleActionError(this.extractErrorMessage(error) || 'No pudimos actualizar la descripción del item.'),
      });
  }

  setItemPrice(itemId: string, rawValue: string): void {
    const _itemId = itemId;
    const _rawValue = rawValue;
    this.actionTone.set('error');
    this.actionMessage.set('El valor del item se define desde backend para este flujo.');
  }

  setModificationDraft(parentId: string, value: string): void {
    this.modificationDraftMap.set({ ...this.modificationDraftMap(), [parentId]: value });
  }

  modificationDraft(parentId: string): string {
    return this.modificationDraftMap()[parentId] ?? '';
  }

  addModification(parentId: string): void {
    const parent = this.draftItems().find((item) => item.id === parentId && !item.isModification);
    if (!parent) {
      return;
    }

    const text = this.modificationDraft(parentId).trim();
    if (!text) {
      this.actionTone.set('error');
      this.actionMessage.set('Escribe una descripción para la modificación.');
      return;
    }

    const visitaId = this.mesaId();
    if (!visitaId) {
      return;
    }

    this.comandaService
      .addItem({
        visitaId,
        productoId: parent.baseProductId,
        cantidad: 1,
        descripcion: text,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (draft) => {
          this.applyDraft(draft);
          this.setModificationDraft(parentId, '');
        },
        error: (error) => this.handleActionError(this.extractErrorMessage(error) || 'No pudimos guardar la modificación.'),
      });
  }

  requestDelete(itemId: string): void {
    this.deletingItemId.set(itemId);
    this.showDeleteDialog.set(true);
  }

  cancelDelete(): void {
    this.showDeleteDialog.set(false);
    this.deletingItemId.set(null);
  }

  confirmDelete(): void {
    const targetId = this.deletingItemId();
    const target = this.draftItems().find((item) => item.id === targetId);
    if (!target) {
      this.cancelDelete();
      return;
    }

    this.comandaService
      .deleteItem(target.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (draft) => {
          this.applyDraft(draft);
          this.cancelDelete();
        },
        error: () => {
          this.cancelDelete();
          this.handleActionError('No pudimos eliminar el item del borrador.');
        },
      });
  }

  itemSubtotal(item: DraftItem): number {
    return item.quantity * item.unitPrice;
  }

  subtotalDraft(): number {
    return this.subtotalBorrador();
  }

  totalAccumulated(): number {
    return this.totalBackend();
  }

  canSendToKitchen(): boolean {
    return Boolean(this.comandaCocinaId() && this.draftPlatos().length);
  }

  canSendToBar(): boolean {
    return Boolean(this.comandaBarraId() && this.draftBebidas().length);
  }

  canSaveDraft(): boolean {
    return this.draftItems().length > 0;
  }

  onSendToKitchen(): void {
    const cocinaId = this.comandaCocinaId();
    if (!cocinaId || !this.draftPlatos().length) {
      return;
    }

    this.comandaService
      .enviarAProduccion(cocinaId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (draft) => {
          this.applyDraft(draft);
          const visitaId = this.mesaId();
          if (visitaId) {
            this.fetchItemsProduccion(visitaId);
          }
          this.actionTone.set('success');
          this.actionMessage.set('Comanda enviada a cocina.');
        },
        error: (error) => this.handleActionError(this.extractErrorMessage(error) || 'No pudimos enviar la comanda a cocina.'),
      });
  }

  onSendToBar(): void {
    const barraId = this.comandaBarraId();
    if (!barraId || !this.draftBebidas().length) {
      return;
    }

    this.comandaService
      .enviarAProduccion(barraId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (draft) => {
          this.applyDraft(draft);
          const visitaId = this.mesaId();
          if (visitaId) {
            this.fetchItemsProduccion(visitaId);
          }
          this.actionTone.set('success');
          this.actionMessage.set('Comanda enviada a barra.');
        },
        error: (error) => this.handleActionError(this.extractErrorMessage(error) || 'No pudimos enviar la comanda a barra.'),
      });
  }

  onSaveDraftOnly(): void {
    if (!this.canSaveDraft()) {
      return;
    }

    this.persistNotes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saved.set(true);
          this.actionTone.set('success');
          this.actionMessage.set('Comanda guardada como espera.');
        },
        error: (error) => this.handleActionError(this.extractErrorMessage(error) || 'No pudimos guardar la comanda.'),
      });
  }

  onSaveDraftAndClose(): void {
    this.persistNotes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.saved.set(true);
          this.router.navigate(['/app/mesero/mesas'], {
            state: { actionMessage: 'Comanda guardada con éxito.' },
          });
        },
        error: () => this.handleActionError('No pudimos guardar las notas de la comanda.'),
      });
  }

  onCancelForm(): void {
    if (!this.draftItems().length) {
      this.goBack();
      return;
    }
    this.showCancelDialog.set(true);
  }

  confirmCancelForm(): void {
    const visitaId = this.mesaId();
    if (!visitaId) {
      this.showCancelDialog.set(false);
      this.goBack();
      return;
    }

    this.comandaService
      .cancelarFormulario(visitaId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showCancelDialog.set(false);
          this.goBack();
        },
        error: () => this.handleActionError('No pudimos cancelar la edición de la comanda.'),
      });
  }

  private goBack(): void {
    this.router.navigateByUrl('/app/mesero/mesas');
  }

  private updateQuantityWithValidation(item: DraftItem, requested: number): void {
    const parsed = Number.isFinite(requested) ? Math.trunc(requested) : item.quantity;
    if (parsed <= 0) {
      this.requestDelete(item.id);
      return;
    }

    const bounded = Math.min(250, Math.max(1, parsed));

    this.comandaService
      .updateItem(item.id, { cantidad: bounded })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (draft) => this.applyDraft(draft),
        error: (error) => this.handleActionError(this.extractErrorMessage(error) || 'No pudimos actualizar la cantidad del item.'),
      });
  }

  private groupedDraftItems(category: DraftCategory): DraftItem[] {
    const all = this.draftItems().filter((item) => item.category === category);
    const roots = all.filter((item) => !item.parentId).sort((a, b) => a.name.localeCompare(b.name, 'es'));
    const result: DraftItem[] = [];

    roots.forEach((root) => {
      result.push(root);
      const children = all.filter((item) => item.parentId === root.id).sort((a, b) => a.name.localeCompare(b.name, 'es'));
      result.push(...children);
    });

    return result;
  }

  private sortDraftItems(items: DraftItem[]): DraftItem[] {
    return [...items].sort((a, b) => a.name.localeCompare(b.name, 'es'));
  }

  private setupSearch(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((term) => {
        if (!term) {
          return of([]);
        }
        return this.productCatalogService.buscarProductos(term).pipe(
          catchError(() => of([]))
        );
      }),
      takeUntil(this.destroy$)
    ).subscribe((items) => {
      this.filteredSuggestions.set(items.slice(0, 12));
    });
  }

  private fetchMesaDetalle(mesaId: string): void {
    this.mesaService
      .getDetalle(mesaId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (detail) => {
          this.mesaIdentificador.set(detail.identificador);
        },
        error: () => undefined,
      });
  }

  private fetchBorrador(visitaId: string): void {
    this.comandaService
      .getBorrador(visitaId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (draft) => this.applyDraft(draft),
        error: () => this.handleActionError('No pudimos cargar el borrador de comanda.'),
      });
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
          this.loadingItems.set(false);
        },
        error: () => {
          this.loadError.set('No pudimos cargar los items en produccion.');
          this.loadingItems.set(false);
        },
      });
  }

  private applyDraft(draft: ComandaDraftData): void {
    this.mesaIdentificador.set(draft.mesaIdentificador || this.mesaIdentificador());
    this.comandaCocinaId.set(draft.comandaCocinaId ?? null);
    this.comandaBarraId.set(draft.comandaBarraId ?? null);
    this.subtotalBorrador.set(draft.subTotal);
    this.totalBackend.set(draft.total);
    this.draftItems.set(this.mapDraftItems(draft));
    this.comandaForm.patchValue(
      {
        kitchenNotes: draft.notasCocina ?? '',
        barNotes: draft.notasBarra ?? '',
      },
      { emitEvent: false }
    );

    if (!this.comandaId()) {
      this.comandaId.set(draft.comandaCocinaId ?? draft.comandaBarraId ?? null);
    }
  }

  private mapDraftItems(draft: ComandaDraftData): DraftItem[] {
    const mapCategory = (items: ComandaDraftItem[], category: DraftCategory): DraftItem[] => {
      const baseItems = items.filter((item) => !(item.descripcion ?? '').trim());
      const mappedBase = baseItems.map((item) => ({
        id: item.comandaItemId,
        baseProductId: item.productoId,
        name: item.productoNombre,
        category,
        quantity: item.cantidad,
        unitPrice: item.precioUnitario,
        description: item.descripcion ?? '',
        isModification: false,
        stockBase: this.defaultStock,
      }));

      const mappedMods = items
        .filter((item) => (item.descripcion ?? '').trim())
        .map((item) => {
          const parent = mappedBase.find((root) => root.baseProductId === item.productoId);
          return {
            id: item.comandaItemId,
            baseProductId: item.productoId,
            name: item.productoNombre,
            category,
            quantity: item.cantidad,
            unitPrice: item.precioUnitario,
            description: item.descripcion ?? '',
            isModification: true,
            parentId: parent?.id,
            stockBase: this.defaultStock,
          } satisfies DraftItem;
        });

      return this.sortDraftItems([...mappedBase, ...mappedMods]);
    };

    return [...mapCategory(draft.platos, 'Platos'), ...mapCategory(draft.bebidas, 'Bebidas')];
  }

  private persistNotes() {
    const requests = [] as Array<ReturnType<ComandaService['updateNotas']>>;
    const kitchenId = this.comandaCocinaId();
    const barId = this.comandaBarraId();

    if (kitchenId) {
      requests.push(this.comandaService.updateNotas(kitchenId, this.comandaForm.controls.kitchenNotes.value));
    }
    if (barId) {
      requests.push(this.comandaService.updateNotas(barId, this.comandaForm.controls.barNotes.value));
    }
    if (!requests.length) {
      return of([]);
    }

    return forkJoin(requests);
  }

  private handleActionError(message: string): void {
    this.actionTone.set('error');
    this.actionMessage.set(message);
  }

  private extractErrorMessage(error: unknown): string {
    const httpError = error as { error?: { message?: string } | string } | null | undefined;
    if (!httpError) {
      return '';
    }

    const backendError = httpError.error;
    if (typeof backendError === 'string' && backendError.trim()) {
      return backendError.trim();
    }

    if (
      backendError &&
      typeof backendError === 'object' &&
      typeof backendError.message === 'string' &&
      backendError.message.trim()
    ) {
      return backendError.message.trim();
    }

    return '';
  }

  private isReloadNavigation(): boolean {
    const navEntry = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined;
    const isBrowserReload = navEntry?.type === 'reload';
    const isInitialNavigation = !this.router.navigated || this.router.getCurrentNavigation()?.id === 1;
    return isBrowserReload && isInitialNavigation;
  }

}
