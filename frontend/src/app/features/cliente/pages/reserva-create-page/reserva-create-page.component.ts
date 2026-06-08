import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { Reserva, ReservaPreorderItem } from '../../../../core/models/domain.models';
import { AuthService } from '../../../../core/services/auth.service';
import { ProductCatalogService } from '../../../../core/services/product-catalog.service';
import { ReservationDetailData, ReservationService } from '../../../../core/services/reservation.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

interface DecorationOption {
  id: string;
  name: string;
  imageUrl: string;
  compatibleZoneIds: string[];
  fixedZoneId?: string;
}

interface ZoneOption {
  id: string;
  name: string;
  imageUrl: string;
}

interface CartaModification {
  id: string;
  text: string;
  quantity: number;
}

interface CartaItemState {
  productId: string;
  productName: string;
  category: 'Platos' | 'Bebidas';
  description: string;
  unitPrice: number;
  quantity: number;
  modificationDraft: string;
  modifications: CartaModification[];
}

interface SpecialMenuOption {
  id: string;
  name: string;
  description: string;
  pricePerPerson: number;
  customizationOptions: Array<{ optionId: string; optionName: string }>;
}

const ROMANTIC_ZONE_ID = 'zona-romantica';
const ROMANTIC_ADDON_ID = 'addon-romantico';
const ROMANTIC_ADDON_LABEL = 'Agregar pétalos y velas';
const ROMANTIC_ADDON_COST = 20000;
const WHATSAPP_COMPANY_NUMBER = '573001112233';
const SPECIAL_MENU_HINT_MESSAGE = '¡Para más de 10 personas puedes pedir un mismo menú para todo el grupo! Revisa las opciones en la sección de Pre-orden';
const MAX_QTY_PER_ITEM = 250;
const MAX_QTY_MESSAGE = 'La cantidad máxima por producto/bebida es de 250';
const WHATSAPP_NOTE = 'Para confirmar tu reserva especial, debes abonar un valor anticipado, comunicate para definirlo';
const DEFAULT_OPTION_IMAGE = 'https://picsum.photos/seed/altoro-option/360/220';
const RESERVATION_HOURS = [
  { value: '17:00', label: '5:00 p.m.' },
  { value: '18:00', label: '6:00 p.m.' },
  { value: '19:00', label: '7:00 p.m.' },
  { value: '20:00', label: '8:00 p.m.' },
  { value: '21:00', label: '9:00 p.m.' },
  { value: '22:00', label: '10:00 p.m.' },
] as const;

const DECORATION_OPTIONS: DecorationOption[] = [];
const ZONE_OPTIONS: ZoneOption[] = [];
const SPECIAL_MENU_OPTIONS: SpecialMenuOption[] = [];

@Component({
  selector: 'app-reserva-create-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid cliente-compact">
      <app-page-header [title]="editMode() ? 'Modificar reserva' : 'Nueva reserva'"></app-page-header>

      <article class="floating-warning card" *ngIf="showFloatingWarning()">
        {{ floatingWarningMessage() }}
      </article>

      <article class="card reservation-card">
        <p class="context-info">
          Horario de reservas: 5:00 p.m. a 10:00 p.m. Las decoraciones, zonas y extras dependen de la disponibilidad y compatibilidad de la fecha/hora elegida.
        </p>

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
                <select class="input-field" formControlName="time">
                  <option value="" disabled>Selecciona una hora</option>
                  <option *ngFor="let slot of reservationHours" [value]="slot.value">{{ slot.label }}</option>
                </select>
              </label>
            </section>

            <p class="error-text" *ngIf="showMissingDateMessage()">
              No hay fecha seleccionada
            </p>
            <p class="error-text" *ngIf="showMissingTimeMessage()">
              No hay hora seleccionada
            </p>

            <p class="error-text" *ngIf="showPastDateError()">
              La fecha y hora de la reserva no pueden ser en el pasado
            </p>
            <p class="error-text" *ngIf="showSameDayCutoffError()">
              Ya no es posible realizar o modificar reservas para hoy (después de las 4:00 p.m.)
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
                  <input type="radio" name="decorationId" [value]="decoration.id" formControlName="decorationId" />
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
              <div class="card-grid" *ngIf="filteredZones().length > 0" [class.disabled-grid]="isZoneSelectionLocked()">
                <label class="option-card" *ngFor="let zone of filteredZones()">
                  <input
                    type="radio"
                    name="zoneId"
                    [value]="zone.id"
                    formControlName="zoneId"
                  />
                  <img [src]="zone.imageUrl" [alt]="zone.name" />
                  <strong>{{ zone.name }}</strong>
                </label>
              </div>
            </section>

            <section class="form-section" *ngIf="romanticAddonAvailable()">
              <p class="romantic-note">Zona romantica: puedes agregar petalos y velas si lo deseas.</p>
              <label class="addon-check">
                <input type="checkbox" formControlName="romanticAddon" />
                Agregar pétalos y velas (+$20.000)
              </label>
            </section>

            <section class="form-section">
              <div class="preorder-head">
                <span class="section-label">Pre-ordenar</span>
                <strong class="preorder-total">Total aproximado: {{ grandTotal() | currency:'COP':'symbol':'1.0-0' }}</strong>
              </div>

              <div class="preorder-tabs">
                <button
                  type="button"
                  class="tab-btn"
                  [class.active]="activePreorderTab() === 'carta'"
                  (click)="setPreorderTab('carta')"
                >
                  A la carta
                </button>
                <button
                  type="button"
                  class="tab-btn"
                  [class.active]="activePreorderTab() === 'especial'"
                  [disabled]="!showSpecialMenuOption()"
                  (click)="setPreorderTab('especial')"
                >
                  Menú especial
                </button>
              </div>

              <p class="qty-limit-warning" *ngIf="qtyLimitWarning()">
                {{ qtyLimitWarning() }}
              </p>

              <p class="special-menu-hint" *ngIf="!showSpecialMenuOption()">
                Menú especial se habilita con más de 10 personas.
              </p>

              <ng-container *ngIf="activePreorderTab() === 'carta'; else specialMenuTab">
                <section class="menu-category">
                  <div class="carta-category-tabs">
                    <button
                      type="button"
                      class="tab-btn"
                      [class.active]="activeCartaCategory() === 'Platos'"
                      (click)="setCartaCategory('Platos')"
                    >
                      Platos ({{ selectedCartaItemsCount('Platos') }})
                    </button>
                    <button
                      type="button"
                      class="tab-btn"
                      [class.active]="activeCartaCategory() === 'Bebidas'"
                      (click)="setCartaCategory('Bebidas')"
                    >
                      Bebidas ({{ selectedCartaItemsCount('Bebidas') }})
                    </button>
                  </div>

                  <button
                    type="button"
                    class="btn-secondary catalog-toggle"
                    (click)="toggleCartaList()"
                  >
                    {{ isCartaListExpanded() ? 'Ocultar productos' : 'Desplegar productos' }}
                  </button>

                  <ng-container *ngIf="isCartaListExpanded()">
                    <article class="preorder-item-card" *ngFor="let item of visibleCartaItems()">
                      <div class="item-head">
                        <div class="item-title-block">
                          <strong>{{ item.productName }}</strong>
                          <span>{{ item.unitPrice | currency:'COP':'symbol':'1.0-0' }}</span>
                        </div>
                        <button
                          type="button"
                          class="btn-secondary compact-toggle"
                          (click)="toggleCartaItemExpand(item.productId)"
                        >
                          {{ isCartaItemExpanded(item.productId) ? 'Ocultar' : 'Detalles' }}
                        </button>
                      </div>

                      <div class="qty-controls">
                        <button type="button" class="qty-btn" (click)="changeCartaItemQuantity(item.productId, -1)">-</button>
                        <input
                          class="input-field qty-input"
                          type="number"
                          min="0"
                          max="250"
                          [value]="item.quantity"
                          (input)="setCartaItemQuantity(item.productId, $any($event.target).value)"
                        />
                        <button type="button" class="qty-btn" (click)="changeCartaItemQuantity(item.productId, 1)">+</button>
                      </div>

                      <small class="subtotal">Subtotal: {{ getCartaItemSubtotal(item) | currency:'COP':'symbol':'1.0-0' }}</small>

                      <div class="item-extra" *ngIf="isCartaItemExpanded(item.productId)">
                        <p>{{ item.description }}</p>

                        <ng-container *ngIf="item.category === 'Platos'">
                          <div class="modification-editor">
                            <input
                              class="input-field"
                              type="text"
                              [value]="item.modificationDraft"
                              placeholder="modificaciones (opcional)"
                              (input)="setCartaModificationDraft(item.productId, $any($event.target).value)"
                            />
                            <button type="button" class="btn-secondary" (click)="addCartaModification(item.productId)">Añadir</button>
                          </div>

                          <div class="modification-list" *ngIf="item.modifications.length > 0">
                            <article class="modification-item" *ngFor="let mod of item.modifications">
                              <div>
                                <strong>{{ mod.text }}</strong>
                                <small>Costo por definir por el cajero al cerrar la cuenta</small>
                              </div>
                              <div class="qty-controls">
                                <button type="button" class="qty-btn" (click)="changeCartaModificationQuantity(item.productId, mod.id, -1)">-</button>
                                <input
                                  class="input-field qty-input"
                                  type="number"
                                  min="0"
                                  max="250"
                                  [value]="mod.quantity"
                                  (input)="setCartaModificationQuantity(item.productId, mod.id, $any($event.target).value)"
                                />
                                <button type="button" class="qty-btn" (click)="changeCartaModificationQuantity(item.productId, mod.id, 1)">+</button>
                              </div>
                            </article>
                          </div>
                        </ng-container>
                      </div>
                    </article>

                    <p class="special-menu-hint" *ngIf="visibleCartaItems().length === 0">
                      No hay productos disponibles en esta categoría.
                    </p>
                  </ng-container>
                </section>
              </ng-container>

              <ng-template #specialMenuTab>
                <p class="special-menu-hint" *ngIf="showSpecialMenuOption()">
                  Solo puedes seleccionar un menú especial por reserva
                </p>

                <div class="special-menu-grid" *ngIf="showSpecialMenuOption()">
                  <article class="special-menu-card" *ngFor="let menu of specialMenus">
                    <label class="special-menu-select">
                      <input
                        type="radio"
                        name="special-menu"
                        [value]="menu.id"
                        [checked]="reservaForm.controls.specialMenuId.value === menu.id"
                        (change)="onSpecialMenuSelected(menu.id)"
                      />
                      <div class="special-menu-copy">
                        <strong>{{ menu.name }}</strong>
                        <small>{{ menu.description }}</small>
                      </div>
                    </label>

                    <span class="special-menu-price">{{ menu.pricePerPerson | currency:'COP':'symbol':'1.0-0' }} por persona</span>

                    <div class="special-menu-actions">
                      <button type="button" class="btn-secondary" (click)="toggleSpecialMenuCustomization(menu.id)">Modificar</button>
                    </div>

                    <div class="special-menu-qty" *ngIf="reservaForm.controls.specialMenuId.value === menu.id">
                      <span>Cantidad de platos</span>
                      <div class="qty-controls">
                        <button type="button" class="qty-btn" (click)="changeSpecialMenuQuantity(-1)">-</button>
                        <input
                          class="input-field qty-input"
                          type="number"
                          min="1"
                          max="250"
                          [value]="reservaForm.controls.specialMenuQty.value"
                          (input)="setSpecialMenuQuantity($any($event.target).value)"
                        />
                        <button type="button" class="qty-btn" (click)="changeSpecialMenuQuantity(1)">+</button>
                      </div>
                      <small class="subtotal">Subtotal: {{ specialMenuSubtotal() | currency:'COP':'symbol':'1.0-0' }}</small>
                    </div>

                    <div class="special-menu-mods" *ngIf="expandedSpecialMenuId() === menu.id && reservaForm.controls.specialMenuId.value === menu.id">
                      <label *ngFor="let option of menu.customizationOptions">
                        <input
                          type="checkbox"
                          [checked]="isSpecialMenuCustomizationSelected(option.optionId)"
                          (change)="toggleSpecialMenuCustomizationOption(option.optionId, $any($event.target).checked)"
                        />
                        {{ option.optionName }}
                      </label>
                    </div>
                  </article>
                </div>
              </ng-template>
            </section>

            <label class="form-label">
              <span>Notas adicionales (opcional)</span>
              <textarea class="input-field" rows="3" formControlName="notes"></textarea>
            </label>

            <div class="action-row">
              <button class="btn-secondary" type="button" (click)="onClose()">Cancelar</button>
              <button class="btn-primary" type="submit">{{ editMode() ? 'Confirmar cambios' : 'Confirmar reserva' }}</button>
            </div>
          </form>
        </ng-container>

        <ng-template #summaryView>
          <section class="summary-box">
            <h3>{{ editMode() ? 'Resumen de cambios' : 'Resumen de reserva' }}</h3>

            <p><strong>Cliente:</strong> {{ authService.currentUser()?.fullName ?? 'Cliente' }}</p>
            <p><strong>Fecha:</strong> {{ reservaForm.controls.date.value }}</p>
            <p><strong>Hora:</strong> {{ reservaForm.controls.time.value }}</p>
            <p><strong>Número de personas:</strong> {{ reservaForm.controls.guests.value }}</p>
            <p *ngIf="selectedDecorationName()"><strong>Decoración:</strong> {{ selectedDecorationName() }}</p>
            <p *ngIf="selectedZoneName()"><strong>Zona:</strong> {{ selectedZoneName() }}</p>
            <p><strong>Pre-orden:</strong> {{ preorderSummaryText() }}</p>
            <p><strong>Extras:</strong> {{ summaryExtrasText() }}</p>

            <ul class="summary-costs">
              <li>
                <span>Reserva base</span>
                <strong>Sin costo</strong>
              </li>
              <li *ngIf="romanticAddonChecked()">
                <span>{{ romanticAddonLabel }}</span>
                <strong>{{ romanticAddonCost | currency:'COP':'symbol':'1.0-0' }}</strong>
              </li>
              <li *ngIf="preorderTotal() > 0">
                <span>Pre-orden ({{ activePreorderTab() === 'especial' ? 'Menú especial' : 'A la carta' }})</span>
                <strong>{{ preorderTotal() | currency:'COP':'symbol':'1.0-0' }}</strong>
              </li>
              <li class="summary-total">
                <span>Total de la reserva</span>
                <strong>{{ grandTotal() | currency:'COP':'symbol':'1.0-0' }}</strong>
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
        border: 1px solid #6F4E37;
        background: rgba(111, 78, 55, 0.12);
        color: #4d3323;
        font-weight: 700;
      }

      .availability-warning {
        margin: 0 0 0.85rem;
        border: 1px solid #6F4E37;
        border-radius: 8px;
        padding: 0.6rem 0.75rem;
        background: rgba(111, 78, 55, 0.1);
        color: #4d3323;
        font-size: 0.86rem;
      }

      .context-info {
        margin: 0 0 0.85rem;
        border: 1px solid rgba(111, 78, 55, 0.35);
        border-radius: 8px;
        padding: 0.6rem 0.75rem;
        background: rgba(111, 78, 55, 0.08);
        color: #4d3323;
        font-size: 0.84rem;
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
        border: 1px solid rgba(111, 78, 55, 0.35);
        border-radius: 8px;
        padding: 0.45rem 0.55rem;
        background: rgba(111, 78, 55, 0.08);
        color: #4d3323;
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

      .romantic-note {
        margin: 0;
        border: 1px solid rgba(111, 78, 55, 0.35);
        border-radius: 8px;
        padding: 0.45rem 0.55rem;
        background: rgba(111, 78, 55, 0.08);
        color: #4d3323;
        font-size: 0.82rem;
      }

      .preorder-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 0.5rem;
        flex-wrap: wrap;
      }

      .preorder-total {
        font-size: 0.84rem;
        color: #4d3323;
      }

      .preorder-tabs {
        display: flex;
        gap: 0.4rem;
      }

      .tab-btn {
        border: 1px solid rgba(111, 78, 55, 0.6);
        background: #ffffff;
        color: #5b3f2c;
        border-radius: 8px;
        padding: 0.35rem 0.62rem;
        font-size: 0.8rem;
        cursor: pointer;
      }

      .tab-btn.active {
        background: #6F4E37;
        color: #ffffff;
      }

      .tab-btn:disabled {
        opacity: 0.55;
        cursor: not-allowed;
      }

      .special-menu-hint {
        margin: 0;
        padding: 0.5rem 0.6rem;
        border-radius: 8px;
        border: 1px solid rgba(111, 78, 55, 0.35);
        background: rgba(111, 78, 55, 0.08);
        color: #4d3323;
        font-size: 0.82rem;
      }

      .qty-limit-warning {
        margin: 0;
        padding: 0.45rem 0.55rem;
        border-radius: 8px;
        border: 1px solid rgba(196, 30, 58, 0.35);
        background: rgba(196, 30, 58, 0.08);
        color: #7a1122;
        font-size: 0.82rem;
        font-weight: 600;
      }

      .carta-category-tabs {
        display: flex;
        gap: 0.4rem;
        flex-wrap: wrap;
      }

      .menu-category {
        display: grid;
        gap: 0.4rem;
      }

      .menu-category h4 {
        margin: 0;
        font-size: 0.9rem;
      }

      .preorder-item-card {
        display: grid;
        gap: 0.35rem;
        border: 1px solid rgba(10, 10, 10, 0.18);
        border-radius: 10px;
        padding: 0.55rem;
        background: #ffffff;
      }

      .item-head {
        display: flex;
        justify-content: space-between;
        gap: 0.45rem;
        align-items: flex-start;
      }

      .item-title-block {
        display: grid;
        gap: 0.15rem;
      }

      .item-head strong {
        font-size: 0.84rem;
      }

      .item-head span {
        font-size: 0.8rem;
        font-weight: 700;
        color: #4d3323;
      }

      .compact-toggle {
        padding: 0.3rem 0.48rem;
        font-size: 0.74rem;
        border-radius: 7px;
      }

      .item-extra {
        display: grid;
        gap: 0.35rem;
      }

      .preorder-item-card p {
        margin: 0;
        color: var(--muted);
        font-size: 0.78rem;
      }

      .qty-controls {
        display: grid;
        grid-template-columns: 30px 1fr 30px;
        gap: 0.35rem;
        align-items: center;
        max-width: 210px;
      }

      .qty-btn {
        border: 1px solid rgba(111, 78, 55, 0.68);
        border-radius: 8px;
        background: #6F4E37;
        color: #ffffff;
        min-height: 30px;
        cursor: pointer;
      }

      .qty-input {
        text-align: center;
        padding: 0.35rem 0.45rem;
      }

      .subtotal {
        color: var(--muted);
        font-size: 0.76rem;
      }

      .modification-editor {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 0.35rem;
      }

      .modification-list {
        display: grid;
        gap: 0.35rem;
      }

      .modification-item {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 0.45rem;
        border: 1px dashed rgba(10, 10, 10, 0.2);
        border-radius: 8px;
        padding: 0.42rem;
      }

      .modification-item strong,
      .modification-item small {
        display: block;
      }

      .modification-item strong {
        font-size: 0.78rem;
      }

      .modification-item small {
        font-size: 0.7rem;
        color: var(--muted);
      }

      .special-menu-grid {
        display: grid;
        gap: 0.5rem;
      }

      .special-menu-card {
        display: grid;
        gap: 0.45rem;
        border: 1px solid rgba(10, 10, 10, 0.2);
        border-radius: 10px;
        padding: 0.5rem;
        background: #ffffff;
      }

      .special-menu-select {
        display: grid;
        grid-template-columns: auto 1fr;
        gap: 0.45rem;
        align-items: start;
      }

      .special-menu-copy {
        display: grid;
        gap: 0.15rem;
      }

      .special-menu-copy strong {
        font-size: 0.84rem;
      }

      .special-menu-copy small {
        font-size: 0.76rem;
        color: var(--muted);
      }

      .special-menu-price {
        font-size: 0.8rem;
        font-weight: 700;
        color: #4d3323;
      }

      .special-menu-actions {
        display: flex;
      }

      .special-menu-qty {
        display: grid;
        gap: 0.3rem;
      }

      .special-menu-qty span {
        font-size: 0.8rem;
      }

      .special-menu-mods {
        display: grid;
        gap: 0.28rem;
      }

      .special-menu-mods label {
        display: flex;
        gap: 0.35rem;
        align-items: center;
        font-size: 0.78rem;
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
        border: 1px solid rgba(111, 78, 55, 0.36);
        border-radius: 8px;
        padding: 0.45rem 0.55rem;
        background: rgba(111, 78, 55, 0.08);
        color: #4d3323;
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
        .form-section,
        .summary-box,
        .special-menu-grid,
        .menu-category {
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
        .section-label,
        .addon-check,
        .item-head strong,
        .special-menu-copy strong,
        .summary-costs li {
          font-size: 0.76rem;
        }

        .zone-lock-message,
        .special-menu-hint,
        .special-menu-copy small,
        .special-menu-price,
        .summary-note,
        .preorder-total,
        .preorder-item-card p,
        .special-menu-qty span,
        .modification-item strong,
        .modification-item small,
        .special-menu-mods label {
          font-size: 0.72rem;
        }

        .qty-controls {
          max-width: 176px;
          grid-template-columns: 28px 1fr 28px;
        }

        .qty-btn {
          min-height: 28px;
        }

        .guest-stepper {
          max-width: 205px;
          gap: 0.35rem;
        }

        .stepper-btn {
          min-width: 30px;
          min-height: 30px;
        }

        .modification-editor {
          grid-template-columns: 1fr;
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
  readonly qtyLimitWarning = signal('');
  readonly editMode = signal(false);
  readonly submitAttempted = signal(false);
  readonly availableDecorations = signal<DecorationOption[]>([]);
  readonly availableZones = signal<ZoneOption[]>([]);
  readonly showSummary = signal(false);
  readonly activePreorderTab = signal<'carta' | 'especial'>('carta');
  readonly activeCartaCategory = signal<'Platos' | 'Bebidas'>('Platos');
  readonly isCartaListExpanded = signal(false);
  readonly expandedSpecialMenuId = signal<string | null>(null);
  readonly expandedCartaItems = signal<Record<string, boolean>>({});
  readonly romanticAddonAvailable = signal(false);
  readonly romanticAddonChecked = signal(false);

  readonly romanticAddonCost = ROMANTIC_ADDON_COST;
  readonly romanticAddonLabel = ROMANTIC_ADDON_LABEL;
  readonly specialMenuHintMessage = SPECIAL_MENU_HINT_MESSAGE;
  readonly whatsappNote = WHATSAPP_NOTE;
  readonly reservationHours = RESERVATION_HOURS;
  specialMenus: SpecialMenuOption[] = [...SPECIAL_MENU_OPTIONS];

  readonly reservaForm = this.formBuilder.nonNullable.group({
    date: ['', [Validators.required]],
    time: ['', [Validators.required]],
    guests: [2, [Validators.required, Validators.min(1), Validators.max(20)]],
    decorationId: [''],
    zoneId: [''],
    romanticAddon: [false],
    specialMenuId: [''],
    specialMenuQty: [1, [Validators.required, Validators.min(1), Validators.max(MAX_QTY_PER_ITEM)]],
    notes: ['']
  });

  cartaItems: CartaItemState[] = this.buildCartaItems();
  specialMenuCustomizationSelection: string[] = [];

  private readonly destroy$ = new Subject<void>();
  private editingReservationId = '';
  private previousGuests = this.reservaForm.controls.guests.value;
  private editReservationData: ReservationDetailData | null = null;
  private editReservationLoaded = false;
  private cartaCatalogLoaded = false;
  private specialMenusLoaded = false;
  private editFormHydrated = false;
  private qtyLimitTimeout: ReturnType<typeof setTimeout> | undefined;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly reservationService: ReservationService,
    private readonly productCatalogService: ProductCatalogService,
    public readonly authService: AuthService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router
  ) { }

  ngOnInit(): void {
    this.editingReservationId = this.activatedRoute.snapshot.paramMap.get('id') ?? '';
    this.editMode.set(Boolean(this.editingReservationId));

    this.loadCatalog();

    this.reservaForm.controls.date.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.clearSubmitAttemptIfReady();
      this.updateAvailability();
    });

    this.reservaForm.controls.time.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.clearSubmitAttemptIfReady();
      this.updateAvailability();
    });

    this.reservaForm.controls.decorationId.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.updateAvailableZones();
      this.syncZoneControlState();
      this.evaluateRomanticAddonAvailability();
    });

    this.reservaForm.controls.zoneId.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((val) => {
      this.evaluateRomanticAddonAvailability(val);
    });

    this.reservaForm.controls.romanticAddon.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => this.romanticAddonChecked.set(value));

    this.reservaForm.controls.guests.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((value) => {
      if (value > 10 && this.previousGuests <= 10) {
        this.showFloating(this.specialMenuHintMessage);
      }

      if (value <= 10) {
        this.clearSpecialMenuSelection();
        if (this.activePreorderTab() === 'especial') {
          this.activePreorderTab.set('carta');
        }
      }

      this.previousGuests = value;
    });

    if (this.editMode()) {
      this.loadEditReservation();
      return;
    }

    this.syncZoneControlState();
    this.updateAvailability();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.qtyLimitTimeout) {
      clearTimeout(this.qtyLimitTimeout);
    }
  }

  evaluateRomanticAddonAvailability(newZoneId?: string): void {
    const fixedZoneId = this.selectedDecoration()?.fixedZoneId;
    const currentZoneId = typeof newZoneId === 'string' ? newZoneId : this.reservaForm.controls.zoneId.value;
    const effectiveZoneId = fixedZoneId || currentZoneId;

    let isRomantic = false;

    // 1. Validar por la zona seleccionada
    if (effectiveZoneId) {
      const selectedZone = this.availableZones().find((z) => String(z.id) === String(effectiveZoneId));
      if (selectedZone) {
        const nameLower = (selectedZone.name || '').toLowerCase();
        const normName = this.normalizeText(selectedZone.name || '');
        if (
          nameLower.includes('rom') || 
          normName.includes('rom') || 
          nameLower.includes('vela') ||
          String(selectedZone.id) === ROMANTIC_ZONE_ID
        ) {
          isRomantic = true;
        }
      }
    }

    // 2. Validar por la decoración seleccionada (por si el usuario elige la decoración de velas)
    const currentDecoId = this.reservaForm.controls.decorationId.value;
    if (currentDecoId && !isRomantic) {
      const selectedDeco = this.availableDecorations().find((d) => String(d.id) === String(currentDecoId));
      if (selectedDeco) {
        const nameLower = (selectedDeco.name || '').toLowerCase();
        const normName = this.normalizeText(selectedDeco.name || '');
        if (nameLower.includes('rom') || normName.includes('rom') || nameLower.includes('vela')) {
          isRomantic = true;
        }
      }
    }

    this.romanticAddonAvailable.set(isRomantic);
    this.syncRomanticAddonState();
  }

  showSpecialMenuOption(): boolean {
    return this.reservaForm.controls.guests.value > 10;
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

  setPreorderTab(tab: 'carta' | 'especial'): void {
    if (tab === 'especial' && !this.showSpecialMenuOption()) {
      this.showFloating('Menú especial solo está habilitado para más de 10 personas');
      return;
    }

    if (tab === this.activePreorderTab()) {
      return;
    }

    if (tab === 'carta') {
      this.clearSpecialMenuSelection();
      this.isCartaListExpanded.set(false);
    } else {
      this.clearCartaSelection();
    }

    this.activePreorderTab.set(tab);
  }

  onSpecialMenuSelected(menuId: string): void {
    const current = this.reservaForm.controls.specialMenuId.value;

    if (current !== menuId) {
      this.specialMenuCustomizationSelection = [];
      this.expandedSpecialMenuId.set(null);
      this.reservaForm.controls.specialMenuQty.setValue(1);
    }

    this.reservaForm.controls.specialMenuId.setValue(menuId);
  }

  toggleSpecialMenuCustomization(menuId: string): void {
    if (this.reservaForm.controls.specialMenuId.value !== menuId) {
      this.onSpecialMenuSelected(menuId);
    }

    const current = this.expandedSpecialMenuId();
    this.expandedSpecialMenuId.set(current === menuId ? null : menuId);
  }

  isSpecialMenuCustomizationSelected(option: string): boolean {
    return this.specialMenuCustomizationSelection.includes(option);
  }

  toggleSpecialMenuCustomizationOption(option: string, checked: boolean): void {
    if (checked) {
      if (!this.specialMenuCustomizationSelection.includes(option)) {
        this.specialMenuCustomizationSelection = [...this.specialMenuCustomizationSelection, option];
      }
      return;
    }

    this.specialMenuCustomizationSelection = this.specialMenuCustomizationSelection.filter((item) => item !== option);
  }

  changeSpecialMenuQuantity(delta: number): void {
    if (!this.selectedSpecialMenu()) {
      return;
    }

    const current = this.reservaForm.controls.specialMenuQty.value;
    const next = this.normalizeQuantity(current + delta, false);
    this.reservaForm.controls.specialMenuQty.setValue(Math.max(1, next));
  }

  setSpecialMenuQuantity(rawValue: string): void {
    if (!this.selectedSpecialMenu()) {
      return;
    }

    const next = this.normalizeQuantity(rawValue, false);
    this.reservaForm.controls.specialMenuQty.setValue(Math.max(1, next));
  }

  cartaItemsByCategory(category: 'Platos' | 'Bebidas'): CartaItemState[] {
    return this.cartaItems.filter((item) => item.category === category);
  }

  setCartaCategory(category: 'Platos' | 'Bebidas'): void {
    if (category === this.activeCartaCategory()) {
      return;
    }

    this.activeCartaCategory.set(category);
    this.isCartaListExpanded.set(false);
  }

  toggleCartaList(): void {
    this.isCartaListExpanded.set(!this.isCartaListExpanded());
  }

  visibleCartaItems(): CartaItemState[] {
    return this.cartaItemsByCategory(this.activeCartaCategory());
  }

  selectedCartaItemsCount(category: 'Platos' | 'Bebidas'): number {
    return this.cartaItemsByCategory(category).filter((item) => item.quantity > 0).length;
  }

  toggleCartaItemExpand(productId: string): void {
    const expanded = this.expandedCartaItems();
    this.expandedCartaItems.set({
      ...expanded,
      [productId]: !expanded[productId],
    });
  }

  isCartaItemExpanded(productId: string): boolean {
    return Boolean(this.expandedCartaItems()[productId]);
  }

  changeCartaItemQuantity(productId: string, delta: number): void {
    const item = this.cartaItems.find((entry) => entry.productId === productId);
    if (!item) {
      return;
    }

    const next = this.normalizeQuantity(item.quantity + delta, true);
    item.quantity = next;
  }

  setCartaItemQuantity(productId: string, rawValue: string): void {
    const item = this.cartaItems.find((entry) => entry.productId === productId);
    if (!item) {
      return;
    }

    item.quantity = this.normalizeQuantity(rawValue, true);
  }

  setCartaModificationDraft(productId: string, value: string): void {
    const item = this.cartaItems.find((entry) => entry.productId === productId);
    if (!item) {
      return;
    }

    item.modificationDraft = value;
  }

  addCartaModification(productId: string): void {
    const item = this.cartaItems.find((entry) => entry.productId === productId);
    if (!item) {
      return;
    }

    const text = item.modificationDraft.trim();
    if (!text) {
      return;
    }

    item.modifications.push({
      id: `${productId}-mod-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      text,
      quantity: 1
    });

    item.modificationDraft = '';
  }

  changeCartaModificationQuantity(productId: string, modId: string, delta: number): void {
    const modification = this.findCartaModification(productId, modId);
    if (!modification) {
      return;
    }

    const next = this.normalizeQuantity(modification.quantity + delta, true);
    if (next <= 0) {
      this.removeCartaModification(productId, modId);
      return;
    }

    modification.quantity = next;
  }

  setCartaModificationQuantity(productId: string, modId: string, rawValue: string): void {
    const modification = this.findCartaModification(productId, modId);
    if (!modification) {
      return;
    }

    const next = this.normalizeQuantity(rawValue, true);
    if (next <= 0) {
      this.removeCartaModification(productId, modId);
      return;
    }

    modification.quantity = next;
  }

  getCartaItemSubtotal(item: CartaItemState): number {
    return item.unitPrice * item.quantity;
  }

  cartaTotal(): number {
    return this.cartaItems.reduce((total, item) => total + this.getCartaItemSubtotal(item), 0);
  }

  selectedSpecialMenu(): SpecialMenuOption | null {
    const menuId = this.reservaForm.controls.specialMenuId.value;
    return this.specialMenus.find((item) => item.id === menuId) ?? null;
  }

  specialMenuSubtotal(): number {
    const selectedMenu = this.selectedSpecialMenu();
    if (!selectedMenu || this.activePreorderTab() !== 'especial') {
      return 0;
    }

    return selectedMenu.pricePerPerson * this.reservaForm.controls.specialMenuQty.value;
  }

  preorderTotal(): number {
    if (this.activePreorderTab() === 'especial') {
      return this.specialMenuSubtotal();
    }

    return this.cartaTotal();
  }

  selectedDecorationName(): string {
    return this.selectedDecoration()?.name ?? '';
  }

  selectedZoneName(): string {
    return this.getZoneById(this.getEffectiveZoneId())?.name ?? '';
  }

  preorderSummaryText(): string {
    if (this.activePreorderTab() === 'especial') {
      const special = this.selectedSpecialMenu();
      if (!special) {
        return 'Sin pre-orden';
      }

      const selectedOptionNames = special.customizationOptions
        .filter((option) => this.specialMenuCustomizationSelection.includes(option.optionId))
        .map((option) => option.optionName);

      const customizations = selectedOptionNames.length > 0 ? ` (${selectedOptionNames.join(', ')})` : '';

      return `${special.name} x ${this.reservaForm.controls.specialMenuQty.value}${customizations}`;
    }

    const itemCount = this.cartaItems.reduce((total, item) => total + item.quantity, 0);
    if (itemCount === 0) {
      return 'Sin pre-orden';
    }

    return `A la carta: ${itemCount} ítems`;
  }

  summaryExtrasText(): string {
    const extras: string[] = [];

    if (this.reservaForm.controls.romanticAddon.value) {
      extras.push(this.romanticAddonLabel);
    }

    const menu = this.selectedSpecialMenu();
    if (menu && this.activePreorderTab() === 'especial') {
      extras.push(menu.name);
    }

    return extras.length > 0 ? extras.join(', ') : 'Sin costo extra';
  }

  hasExtraServices(): boolean {
    return this.romanticAddonChecked() ||
      (this.activePreorderTab() === 'especial' && !!this.selectedSpecialMenu());
  }


  grandTotal(): number {
    const preorder = this.preorderTotal();
    const romantic = this.romanticAddonChecked() ? this.romanticAddonCost : 0;
    return preorder + romantic;
  }

  changeGuests(delta: number): void {
    const control = this.reservaForm.controls.guests;
    const next = Math.min(20, Math.max(1, control.value + delta));
    control.setValue(next);
  }

  showPastDateError(): boolean {
    const date = this.reservaForm.controls.date.value;
    const time = this.reservaForm.controls.time.value;

    if (!date || !time) {
      return false;
    }

    return this.isDateTimeInPast(date, time);
  }

  showSameDayCutoffError(): boolean {
    // TESTING MODE: restricción de cutoff del mismo día deshabilitada.
    // Para restaurar en producción, descomentar el bloque de abajo:
    //
    // const dateStr = this.reservaForm.controls.date.value;
    // if (!dateStr) { return false; }
    // const selectedDate = new Date(`${dateStr}T00:00:00`);
    // const today = new Date();
    // today.setHours(0, 0, 0, 0);
    // if (selectedDate.getTime() === today.getTime()) {
    //   const now = new Date();
    //   if (now.getHours() >= 16) { return true; }
    // }
    return false;
  }

  showMissingDateMessage(): boolean {
    const date = this.reservaForm.controls.date.value;
    if (date) {
      return false;
    }

    return this.submitAttempted() || this.reservaForm.controls.date.touched || this.reservaForm.controls.time.touched;
  }

  showMissingTimeMessage(): boolean {
    const time = this.reservaForm.controls.time.value;
    if (time) {
      return false;
    }

    return this.submitAttempted() || this.reservaForm.controls.date.touched || this.reservaForm.controls.time.touched;
  }

  showOutOfHoursError(): boolean {
    const time = this.reservaForm.controls.time.value;

    if (!time) {
      return false;
    }

    return !this.isWithinReservationHours(time);
  }

  onClose(): void {
    if (this.editMode()) {
      void this.router.navigateByUrl('/app/cliente/reservas/history');
      return;
    }

    void this.router.navigateByUrl('/app/cliente');
  }

  onSubmit(): void {
    this.submitAttempted.set(true);
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

    if (this.showSameDayCutoffError()) {
      this.reservaForm.controls.date.markAsTouched();
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

    if (this.activePreorderTab() === 'especial' && !this.selectedSpecialMenu()) {
      this.showFloating('Selecciona un menú especial o cambia a A la carta');
      return;
    }

    if (this.reservaForm.invalid) {
      this.reservaForm.markAllAsTouched();
      return;
    }

    this.showSummary.set(true);
  }

  private clearSubmitAttemptIfReady(): void {
    const date = this.reservaForm.controls.date.value;
    const time = this.reservaForm.controls.time.value;
    if (date && time && this.submitAttempted()) {
      this.submitAttempted.set(false);
    }
  }

  onCancelSummary(): void {
    if (this.editMode()) {
      this.onClose();
      return;
    }

    this.showSummary.set(false);
  }

  onConfirmReservation(): void {
    if (this.loading()) {
      return;
    }

    const { date, time } = this.reservaForm.getRawValue();

    if (!date || !time) {
      this.showSummary.set(false);
      this.showFloating('La fecha y hora son obligatorias');
      return;
    }

    this.loading.set(true);

    const payload = this.buildReservationPayload();

    if (this.editMode() && this.editingReservationId) {
      this.reservationService.update(this.editingReservationId, payload).subscribe({
        next: (result) => {
          this.loading.set(false);
          this.showSummary.set(false);

          if (result.requiresWhatsApp) {
            this.redirectToWhatsapp(result.whatsappMessage);
            return;
          }

          void this.router.navigateByUrl('/app/cliente', {
            state: { flashMessage: 'La reserva fue modificada correctamente' }
          });
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          const backendMessage =
            (typeof err.error?.message === 'string' && err.error.message.trim().length > 0
              ? err.error.message
              : '') ||
            (typeof err.error === 'string' && err.error.trim().length > 0 ? err.error : '');

          this.showFloating(backendMessage || 'No fue posible modificar la reserva. Intenta nuevamente.');
        }
      });

      return;
    }

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
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const backendMessage =
          (typeof err.error?.message === 'string' && err.error.message.trim().length > 0
            ? err.error.message
            : '') ||
          (typeof err.error === 'string' && err.error.trim().length > 0 ? err.error : '');

        this.showFloating(backendMessage || 'No fue posible registrar la reserva. Intenta nuevamente.');
      }
    });
  }

  private buildReservationPayload(): Omit<Reserva, 'id' | 'status'> & { status?: Reserva['status'] } {
    const formValue = this.reservaForm.getRawValue();
    const currentUser = this.authService.currentUser();
    const effectiveZoneId = this.getEffectiveZoneId();

    const selectedDecoration = this.availableDecorations().find((item) => item.id === formValue.decorationId);
    const selectedZone = this.getZoneById(effectiveZoneId);

    const preorderItems = this.buildPreorderItems();

    if (this.romanticAddonChecked()) {
      preorderItems.push({
        productId: ROMANTIC_ADDON_ID,
        productName: ROMANTIC_ADDON_LABEL,
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

  private buildPreorderItems(): ReservaPreorderItem[] {
    if (this.activePreorderTab() === 'especial') {
      const selectedMenu = this.selectedSpecialMenu();
      if (!selectedMenu) {
        return [];
      }

      const selectedOptions = selectedMenu.customizationOptions.filter((option) =>
        this.specialMenuCustomizationSelection.includes(option.optionId)
      );
      const optionNames = selectedOptions.map((option) => option.optionName);

      const menuName = optionNames.length > 0 ? `${selectedMenu.name} (${optionNames.join(', ')})` : selectedMenu.name;

      return [
        {
          productId: selectedMenu.id,
          productName: menuName,
          quantity: this.reservaForm.controls.specialMenuQty.value,
          description: optionNames.length > 0 ? `Opciones: ${optionNames.join(', ')}` : undefined,
          isSpecialMenu: true,
          modificationOptionIds: selectedOptions.map((option) => option.optionId),
        }
      ];
    }

    const items: ReservaPreorderItem[] = [];

    this.cartaItems.forEach((item) => {
      if (item.quantity > 0) {
        const modificationsText = item.modifications.length > 0
          ? `Modificaciones: ${item.modifications.map((modification) => `${modification.text} x${modification.quantity}`).join(', ')}`
          : undefined;

        items.push({
          productId: item.productId,
          productName: item.productName,
          quantity: item.quantity,
          description: modificationsText,
        });
      }
    });

    return items;
  }

  private buildNotes(userNotes: string): string {
    const notes: string[] = [];

    if (userNotes) {
      notes.push(userNotes);
    }

    if (this.activePreorderTab() === 'especial' && this.specialMenuCustomizationSelection.length > 0) {
      const selectedMenu = this.selectedSpecialMenu();
      const selectedOptionNames = selectedMenu
        ? selectedMenu.customizationOptions
          .filter((option) => this.specialMenuCustomizationSelection.includes(option.optionId))
          .map((option) => option.optionName)
        : [];

      if (selectedOptionNames.length > 0) {
        notes.push(`Modificaciones menú: ${selectedOptionNames.join(', ')}`);
      }
    }

    if (this.romanticAddonChecked()) {
      notes.push(`${this.romanticAddonLabel} (+$20.000)`);
    }

    return notes.join(' | ');
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
      this.evaluateRomanticAddonAvailability(); // ← resetea el signal Y limpia el valor
      return;
    }

    if (this.isDateTimeInPast(date, time) || !this.isWithinReservationHours(time)) {
      this.availableDecorations.set([]);
      this.availableZones.set([]);
      this.reservaForm.controls.decorationId.setValue('');
      this.reservaForm.controls.zoneId.setValue('');
      this.evaluateRomanticAddonAvailability();
      return;
    }

    this.reservationService.getAvailability(date, time).subscribe({
      next: (availability) => {
        this.showNoAvailabilityWarning.set(!availability.available);

        this.availableDecorations.set(
          availability.decorations.map((item) => ({
            id: item.id,
            name: item.name,
            imageUrl: this.toOptionImage(`decor-${item.id}`),
            compatibleZoneIds: item.compatibleZoneIds ?? [],
            fixedZoneId:
              item.allowZoneSelection === false && (item.compatibleZoneIds?.length ?? 0) === 1
                ? item.compatibleZoneIds?.[0]
                : undefined,
          }))
        );

        this.availableZones.set(
          availability.zones.map((item) => ({
            id: item.id,
            name: item.name,
            imageUrl: this.toOptionImage(`zona-${item.id}`),
          }))
        );

        if (!this.availableDecorations().some((item) => item.id === this.reservaForm.controls.decorationId.value)) {
          this.reservaForm.controls.decorationId.setValue('');
        }

        if (!this.availableZones().some((item) => item.id === this.reservaForm.controls.zoneId.value)) {
          this.reservaForm.controls.zoneId.setValue('');
        }

        this.syncZoneControlState();
        this.evaluateRomanticAddonAvailability();
      },
      error: () => {
        this.availableDecorations.set([]);
        this.availableZones.set([]);
        this.showNoAvailabilityWarning.set(true);
      },
    });
  }

  private updateAvailableZones(): void {
    const currentZoneId = this.reservaForm.controls.zoneId.value;
    const zones = this.getZonesForSelection(this.reservaForm.controls.decorationId.value);
    if (currentZoneId && !zones.some((zone) => zone.id === currentZoneId)) {
      this.reservaForm.controls.zoneId.setValue('');
      this.showFloating('La decoración no es compatible con la zona escogida');
    }
  }

  private syncRomanticAddonState(): void {
    if (!this.romanticAddonAvailable() && this.reservaForm.controls.romanticAddon.value) {
      this.reservaForm.controls.romanticAddon.setValue(false);
    }
  }

  private syncZoneControlState(): void {
    const zoneControl = this.reservaForm.controls.zoneId;

    if (this.isZoneSelectionLocked()) {
      if (zoneControl.enabled) {
        zoneControl.disable({ emitEvent: false });
      }
      return;
    }

    if (zoneControl.disabled) {
      zoneControl.enable({ emitEvent: false });
    }
  }

  private clearSpecialMenuSelection(): void {
    this.reservaForm.controls.specialMenuId.setValue('');
    this.reservaForm.controls.specialMenuQty.setValue(1);
    this.specialMenuCustomizationSelection = [];
    this.expandedSpecialMenuId.set(null);
  }

  private clearCartaSelection(): void {
    this.cartaItems.forEach((item) => {
      item.quantity = 0;
      item.modificationDraft = '';
      item.modifications = [];
    });
    this.isCartaListExpanded.set(false);
    this.expandedCartaItems.set({});
  }

  private getDecorationsForDate(date: string): DecorationOption[] {
    void date;
    return this.availableDecorations();
  }

  private getZonesForSelection(selectedDecorationId: string): ZoneOption[] {
    if (!selectedDecorationId) {
      return [...this.availableZones()];
    }

    const selectedDecoration = this.availableDecorations().find((item) => item.id === selectedDecorationId);
    if (!selectedDecoration) {
      return [];
    }

    if (selectedDecoration.fixedZoneId) {
      return this.availableZones().filter((zone) => zone.id === selectedDecoration.fixedZoneId);
    }

    if (selectedDecoration.compatibleZoneIds.length === 0) {
      return [...this.availableZones()];
    }

    return this.availableZones().filter((zone) => selectedDecoration.compatibleZoneIds.includes(zone.id));
  }

  filteredZones(): ZoneOption[] {
    return this.getZonesForSelection(this.reservaForm.controls.decorationId.value);
  }

  private isSelectionStillAvailable(date: string, time: string, decorationId: string, zoneId: string): boolean {
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

  private isWithinReservationHours(_time: string): boolean {
    // TESTING MODE: sin restricción de horario.
    // Para restaurar en producción, reemplazar con:
    // const [hours, minutes] = _time.split(':').map(Number);
    // const totalMinutes = hours * 60 + minutes;
    // const start = 17 * 60;
    // const end = 22 * 60;
    // return totalMinutes >= start && totalMinutes <= end;
    return true;
  }

  private isDateTimeInPast(date: string, time: string): boolean {
    const selected = new Date(`${date}T${time}:00`);
    return selected.getTime() < Date.now();
  }

  private loadCatalog(): void {
    this.productCatalogService.listCartaItems().subscribe({
      next: (items) => {
        this.cartaItems = items.map((item) => ({
          productId: item.productId,
          productName: item.productName,
          category: item.category,
          description: item.description,
          unitPrice: item.unitPrice,
          quantity: 0,
          modificationDraft: '',
          modifications: [],
        }));
        this.cartaCatalogLoaded = true;
        this.tryHydrateEditForm();
      },
      error: () => {
        this.cartaItems = this.buildCartaItems();
        this.cartaCatalogLoaded = true;
        this.tryHydrateEditForm();
      },
    });

    this.productCatalogService.listSpecialMenus().subscribe({
      next: (menus) => {
        this.specialMenus = menus;
        this.specialMenusLoaded = true;
        this.tryHydrateEditForm();
      },
      error: () => {
        this.specialMenus = [];
        this.specialMenusLoaded = true;
        this.tryHydrateEditForm();
      },
    });
  }

  private loadEditReservation(): void {
    this.reservationService.getDetail(this.editingReservationId).subscribe({
      next: (detail) => {
        this.editReservationData = detail;
        this.editReservationLoaded = true;
        this.tryHydrateEditForm();
      },
      error: (err: HttpErrorResponse) => {
        const backendMessage =
          (typeof err.error?.message === 'string' && err.error.message.trim().length > 0
            ? err.error.message
            : '') ||
          (typeof err.error === 'string' && err.error.trim().length > 0 ? err.error : '');

        this.showFloating(backendMessage || 'No fue posible cargar la reserva a modificar.');
        void this.router.navigateByUrl('/app/cliente/reservas/history');
      }
    });
  }

  private tryHydrateEditForm(): void {
    if (!this.editMode() || this.editFormHydrated) {
      return;
    }

    if (!this.editReservationLoaded || !this.cartaCatalogLoaded || !this.specialMenusLoaded || !this.editReservationData) {
      return;
    }

    this.hydrateEditForm(this.editReservationData.reservation);
    this.editFormHydrated = true;
  }

  private hydrateEditForm(reservation: Reserva): void {
    this.clearSpecialMenuSelection();
    this.clearCartaSelection();

    this.reservaForm.patchValue(
      {
        date: reservation.date,
        time: this.normalizeToAllowedHour(reservation.time),
        guests: reservation.guests,
        decorationId: reservation.decorationId ?? '',
        zoneId: reservation.zoneId ?? '',
        romanticAddon: false,
        specialMenuId: '',
        specialMenuQty: 1,
        notes: reservation.notes ?? '',
      },
      { emitEvent: false }
    );

    this.previousGuests = reservation.guests;
    this.hydratePreorder(reservation.preorderItems ?? []);
    this.updateAvailability();
  }

  private hydratePreorder(items: ReservaPreorderItem[]): void {
    if (items.length === 0) {
      this.activePreorderTab.set('carta');
      return;
    }

    const specialMenuItem = items.find((item) => this.specialMenus.some((menu) => menu.id === item.productId));
    if (specialMenuItem) {
      this.activePreorderTab.set('especial');
      this.reservaForm.controls.specialMenuId.setValue(specialMenuItem.productId, { emitEvent: false });
      this.reservaForm.controls.specialMenuQty.setValue(Math.max(1, specialMenuItem.quantity), { emitEvent: false });
      this.specialMenuCustomizationSelection = specialMenuItem.modificationOptionIds ?? [];
      return;
    }

    this.activePreorderTab.set('carta');
    this.activeCartaCategory.set('Platos');
    this.isCartaListExpanded.set(false);
    items.forEach((item) => {
      const cartaItem = this.cartaItems.find((entry) => entry.productId === item.productId);
      if (!cartaItem) {
        return;
      }

      this.activeCartaCategory.set(cartaItem.category);
      this.expandedCartaItems.set({
        ...this.expandedCartaItems(),
        [cartaItem.productId]: true,
      });

      cartaItem.quantity = Math.max(0, item.quantity);

      if (item.description?.trim()) {
        cartaItem.modifications = [
          {
            id: `${cartaItem.productId}-mod-hydrated`,
            text: item.description.trim(),
            quantity: 1,
          },
        ];
      }
    });
  }

  private redirectToWhatsapp(customMessage?: string): void {
    const formValue = this.reservaForm.getRawValue();

    const message = customMessage?.trim()
      ? customMessage
      : [
        'Hola, quiero confirmar una reserva especial en Al Toro Gastrobar.',
        `Fecha: ${formValue.date}`,
        `Hora: ${formValue.time}`,
        `Número de personas: ${formValue.guests}`,
        `Extras: ${this.summaryExtrasText()}`,
        `Total aproximado: ${this.formatCurrency(this.grandTotal())}`,
        WHATSAPP_NOTE,
      ].join('\n');

    const url = `https://wa.me/${WHATSAPP_COMPANY_NUMBER}?text=${encodeURIComponent(message)}`;
    window.location.href = url;
  }

  private selectedDecoration(): DecorationOption | undefined {
    return this.availableDecorations().find((item) => item.id === this.reservaForm.controls.decorationId.value);
  }

  private getFixedZoneForDecoration(): ZoneOption | undefined {
    const fixedZoneId = this.selectedDecoration()?.fixedZoneId;
    if (!fixedZoneId) {
      return undefined;
    }

    return this.getZoneById(fixedZoneId);
  }

  private getZoneById(zoneId: string | number | null | undefined): ZoneOption | undefined {
    if (zoneId == null || zoneId === '') {
      return undefined;
    }
    return this.availableZones().find((item) => String(item.id) === String(zoneId));
  }

  private isRomanticZone(zone?: ZoneOption): boolean {
    if (!zone) {
      return false;
    }

    if (String(zone.id) === ROMANTIC_ZONE_ID) {
      return true;
    }

    const nameLower = (zone.name || '').toLowerCase();
    const normalizedName = this.normalizeText(zone.name);

    return nameLower.includes('rom') ||
      normalizedName.includes('rom') ||
      normalizedName.includes('zona romantica');
  }

  private normalizeText(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
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

  private findCartaModification(productId: string, modId: string): CartaModification | undefined {
    const item = this.cartaItems.find((entry) => entry.productId === productId);
    return item?.modifications.find((entry) => entry.id === modId);
  }

  private removeCartaModification(productId: string, modId: string): void {
    const item = this.cartaItems.find((entry) => entry.productId === productId);
    if (!item) {
      return;
    }

    item.modifications = item.modifications.filter((entry) => entry.id !== modId);
  }

  private normalizeQuantity(rawValue: string | number, allowZero: boolean): number {
    const parsed = typeof rawValue === 'number' ? rawValue : Number(String(rawValue).trim());

    if (!Number.isFinite(parsed)) {
      return allowZero ? 0 : 1;
    }

    const floored = Math.floor(parsed);
    if (floored > MAX_QTY_PER_ITEM) {
      this.triggerQtyLimitWarning();
      return MAX_QTY_PER_ITEM;
    }

    if (floored < 0) {
      return allowZero ? 0 : 1;
    }

    return floored;
  }

  private buildCartaItems(): CartaItemState[] {
    return [];
  }

  private normalizeToAllowedHour(rawTime: string): string {
    if (this.reservationHours.some((slot) => slot.value === rawTime)) {
      return rawTime;
    }

    const [rawHour, rawMinute] = rawTime.split(':').map((value) => Number(value));
    if (!Number.isFinite(rawHour) || !Number.isFinite(rawMinute)) {
      return this.reservationHours[0].value;
    }

    const hourWithRounding = rawMinute >= 30 ? rawHour + 1 : rawHour;
    const clampedHour = Math.max(17, Math.min(22, hourWithRounding));
    const normalized = `${String(clampedHour).padStart(2, '0')}:00`;

    return this.reservationHours.some((slot) => slot.value === normalized)
      ? normalized
      : this.reservationHours[0].value;
  }

  private toOptionImage(seed: string): string {
    return `https://picsum.photos/seed/${seed}/360/220`;
  }

  private showFloating(message: string): void {
    this.floatingWarningMessage.set(message);
    this.showFloatingWarning.set(true);
    setTimeout(() => this.showFloatingWarning.set(false), 3500);
  }

  private triggerQtyLimitWarning(): void {
    this.qtyLimitWarning.set(MAX_QTY_MESSAGE);
    if (this.qtyLimitTimeout) {
      clearTimeout(this.qtyLimitTimeout);
    }

    this.qtyLimitTimeout = setTimeout(() => this.qtyLimitWarning.set(''), 3500);
  }
}


