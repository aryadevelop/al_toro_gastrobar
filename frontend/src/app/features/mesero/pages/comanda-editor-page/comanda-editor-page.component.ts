import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
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

interface DraftStoragePayload {
  items: DraftItem[];
  kitchenNote: string;
  barNote: string;
  sentAccumulated: number;
}

@Component({
  selector: 'app-comanda-editor-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, ConfirmDialogComponent],
  template: `
    <section class="page-grid comanda-shell">
      <app-page-header title="Modificar comanda" subtitle="Edición de borrador para cocina y barra"></app-page-header>

      <p class="integration-note">
        Integración parcial activa: la carga de mesa/producción es en tiempo real; guardar y enviar operan en modo local
        mientras se habilitan endpoints de comanda en backend.
      </p>

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
                    (input)="setDescription(item.id, $any($event.target).value)"
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
                    (input)="setDescription(item.id, $any($event.target).value)"
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
          <button class="btn-primary" type="button" [disabled]="!canSendToProduction()" (click)="onSendToProduction()">
            Enviar a producción
          </button>
        </footer>

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
  private readonly productCatalogService = inject(ProductCatalogService);
  private readonly destroy$ = new Subject<void>();
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
  readonly sentAccumulated = signal(0);

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

    this.loadCatalog();

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
      this.restoreDraft(mesaId);
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
      return;
    }

    this.filteredSuggestions.set(
      this.catalog()
        .filter((item) => item.productName.toLowerCase().includes(term))
        .slice(0, 12)
    );
  }

  onSelectSuggestion(item: CartaCatalogItem): void {
    const existing = this.draftItems().find((entry) => !entry.isModification && entry.baseProductId === item.productId);
    if (existing) {
      this.changeQuantity(existing.id, 1);
    } else {
      this.draftItems.set(
        this.sortDraftItems([
          ...this.draftItems(),
          {
            id: this.buildId(),
            baseProductId: item.productId,
            name: item.productName,
            category: item.category,
            quantity: 1,
            unitPrice: item.unitPrice,
            description: '',
            isModification: false,
            stockBase: this.defaultStock,
          },
        ])
      );
      this.persistDraft();
    }

    this.searchTerm.set('');
    this.filteredSuggestions.set([]);
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
    this.patchItem(itemId, (item) => ({ ...item, description: value.trim() }));
  }

  setItemPrice(itemId: string, rawValue: string): void {
    const price = Math.max(0, Number(rawValue) || 0);
    this.patchItem(itemId, (item) => ({ ...item, unitPrice: price }));
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

    this.draftItems.set(
      this.sortDraftItems([
        ...this.draftItems(),
        {
          id: this.buildId(),
          baseProductId: parent.baseProductId,
          name: `${parent.name} (modificación)`,
          category: parent.category,
          quantity: 1,
          unitPrice: parent.unitPrice,
          description: text,
          isModification: true,
          parentId,
          stockBase: parent.stockBase,
        },
      ])
    );

    this.setModificationDraft(parentId, '');
    this.persistDraft();
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

    const updated = target.isModification
      ? this.draftItems().filter((item) => item.id !== target.id)
      : this.draftItems().filter((item) => item.id !== target.id && item.parentId !== target.id);

    this.draftItems.set(this.sortDraftItems(updated));
    this.persistDraft();
    this.cancelDelete();
  }

  itemSubtotal(item: DraftItem): number {
    return item.quantity * item.unitPrice;
  }

  subtotalDraft(): number {
    return this.draftItems().reduce((acc, item) => acc + this.itemSubtotal(item), 0);
  }

  totalAccumulated(): number {
    return this.subtotalDraft() + this.sentAccumulated();
  }

  canSendToProduction(): boolean {
    return this.draftItems().length > 0;
  }

  onSendToProduction(): void {
    if (!this.canSendToProduction()) {
      return;
    }

    const sent = this.draftItems().map((item) => ({
      nombreProducto: item.name,
      descripcion: item.description || undefined,
      categoriaProducto: item.category,
      cantidad: item.quantity,
      estadoComanda: 'PENDIENTE',
    }));

    this.itemsProduccion.set([...this.itemsProduccion(), ...sent]);
    this.sentAccumulated.set(this.sentAccumulated() + this.subtotalDraft());
    this.draftItems.set([]);
    this.modificationDraftMap.set({});
    this.saved.set(false);
    this.actionTone.set('success');
    this.actionMessage.set('Comanda enviada en modo local. Pendiente integración backend para envío real a producción.');
    this.persistDraft();
  }

  onSaveDraftAndClose(): void {
    this.saved.set(true);
    this.persistDraft();
    this.router.navigate(['/app/mesero/mesas'], {
      state: { actionMessage: 'Comanda guardada en modo local. Pendiente persistencia backend.' },
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
    const key = this.draftStorageKey();
    if (key) {
      localStorage.removeItem(key);
    }
    this.showCancelDialog.set(false);
    this.goBack();
  }

  private goBack(): void {
    this.router.navigateByUrl('/app/mesero/mesas');
  }

  private updateQuantityWithValidation(item: DraftItem, requested: number): void {
    const parsed = Number.isFinite(requested) ? Math.trunc(requested) : item.quantity;
    const bounded = Math.min(250, Math.max(1, parsed));
    const maxStock = this.availableStockForItem(item);

    if (bounded > maxStock) {
      this.actionTone.set('error');
      this.actionMessage.set(`Solo hay ${maxStock} unidades disponibles de este producto`);
      return;
    }

    this.patchItem(item.id, (current) => ({ ...current, quantity: bounded }));
  }

  private availableStockForItem(item: DraftItem): number {
    const reservedByOthers = this.draftItems()
      .filter((entry) => entry.baseProductId === item.baseProductId && entry.id !== item.id)
      .reduce((acc, entry) => acc + entry.quantity, 0);
    return Math.max(1, item.stockBase - reservedByOthers);
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

  private patchItem(itemId: string, updater: (item: DraftItem) => DraftItem): void {
    const updated = this.draftItems().map((item) => (item.id === itemId ? updater(item) : item));
    this.draftItems.set(updated);
    this.persistDraft();
  }

  private loadCatalog(): void {
    this.loadingCatalog.set(true);
    this.productCatalogService
      .listCartaItems()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (items) => {
          this.catalog.set(items.sort((a, b) => a.productName.localeCompare(b.productName, 'es')));
          this.loadingCatalog.set(false);
        },
        error: () => {
          this.catalog.set([]);
          this.loadingCatalog.set(false);
        },
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

  private restoreDraft(mesaId: string): void {
    const raw = localStorage.getItem(this.draftStorageKey(mesaId));
    if (!raw) {
      return;
    }

    try {
      const parsed = JSON.parse(raw) as DraftStoragePayload;
      this.draftItems.set(Array.isArray(parsed.items) ? parsed.items : []);
      this.sentAccumulated.set(Number(parsed.sentAccumulated) || 0);
      this.comandaForm.patchValue({
        kitchenNotes: parsed.kitchenNote ?? '',
        barNotes: parsed.barNote ?? '',
      });
    } catch {
      localStorage.removeItem(this.draftStorageKey(mesaId));
    }
  }

  private persistDraft(): void {
    const key = this.draftStorageKey();
    if (!key) {
      return;
    }

    const payload: DraftStoragePayload = {
      items: this.draftItems(),
      kitchenNote: this.comandaForm.controls.kitchenNotes.value,
      barNote: this.comandaForm.controls.barNotes.value,
      sentAccumulated: this.sentAccumulated(),
    };
    localStorage.setItem(key, JSON.stringify(payload));
  }

  private draftStorageKey(mesaIdParam?: string): string {
    const mesa = mesaIdParam ?? this.mesaId() ?? 'unknown';
    return `mesero-comanda-draft-${mesa}`;
  }

  private isReloadNavigation(): boolean {
    const navEntry = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined;
    return navEntry?.type === 'reload';
  }

  private buildId(): string {
    return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  }
}
