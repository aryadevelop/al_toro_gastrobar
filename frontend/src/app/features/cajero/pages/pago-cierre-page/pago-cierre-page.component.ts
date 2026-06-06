import { CommonModule, CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../../../../core/services/auth.service';
import {
  AjusteItem,
  ClienteBusqueda,
  CuentaItem,
  CuentaMesaService,
  CuentaPreliminar,
  MetodoPago,
} from '../../../../core/services/cuenta-mesa.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

/** Estado editable de un ítem en modo ajuste */
interface ItemAjusteLocal {
  comandaItemId: number;
  nombreProducto: string;
  categoriaProducto: string;
  descripcion?: string;
  esModificado: boolean;
  esMenuEspecial: boolean;
  menuGrupo?: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  /** null = activo; true = marcado para eliminar (pendiente confirmación) */
  eliminado: boolean;
  /** true = diálogo de confirmación de eliminación abierto */
  confirmandoEliminar: boolean;
}

@Component({
  selector: 'app-pago-cierre-page',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, CurrencyPipe],
  template: `
    <section class="page-grid cierre-shell">
      <app-page-header
        title="Cuenta de mesa"
        subtitle="Revisa los detalles antes de registrar el pago"
      ></app-page-header>

      <!-- ── Cargando ── -->
      <div class="estado-centro" *ngIf="loading()">
        <div class="spinner"></div>
        <p>Cargando cuenta…</p>
      </div>

      <!-- ── Error carga ── -->
      <div class="alerta alerta-error" *ngIf="!loading() && cargaError()">
        <p>{{ cargaError() }}</p>
        <button class="btn-outline" type="button" (click)="recargarCuenta()">Reintentar</button>
      </div>

      <!-- ── Contenido principal ── -->
      <ng-container *ngIf="!loading() && !cargaError() && cuenta() as c">

        <!-- Banner de mensaje temporal -->
        <div class="alerta" [ngClass]="mensajeTono()" *ngIf="mensajeAccion()">
          {{ mensajeAccion() }}
        </div>

        <!-- ════ MODAL: Confirmar cancelar proceso (CA-14) ════ -->
        <div class="modal-overlay" *ngIf="mostrandoCancelar()" (click)="cerrarModalCancelar()">
          <div class="modal-box" (click)="$event.stopPropagation()">
            <p class="modal-pregunta">¿Cancelar el proceso?</p>
            <p class="modal-desc">Se perderán las modificaciones no guardadas.</p>
            <div class="modal-acciones">
              <button class="btn-primary btn-danger" type="button" (click)="confirmarCancelar()">
                Sí, cancelar
              </button>
              <button class="btn-outline" type="button" (click)="cerrarModalCancelar()">
                Continuar aquí
              </button>
            </div>
          </div>
        </div>

        <!-- ════ GRID PRINCIPAL ════ -->
        <div class="cuenta-grid">

          <!-- ─── COLUMNA IZQUIERDA ─── -->
          <div class="cuenta-izquierda">

            <!-- SECCIÓN: Identificación del cliente -->
            <article class="card seccion">
              <h2 class="seccion-titulo">Identificación del cliente</h2>

              <!-- Cliente ya asignado -->
              <ng-container *ngIf="clienteAsignado(); else sinCliente">
                <div class="info-fila">
                  <span class="info-label">Cliente</span>
                  <span class="info-valor">{{ c.clienteNombre ?? '—' }}</span>
                </div>
                <div class="info-fila" *ngIf="c.clienteEmail">
                  <span class="info-label">Correo</span>
                  <span class="info-valor correo">{{ c.clienteEmail }}</span>
                </div>
                <div class="info-fila puntos-fila" *ngIf="c.puntosCanjeables !== undefined">
                  <div>
                    <span class="info-label">Puntos canjeables</span>
                    <span class="puntos-badge">⭐ {{ puntosActuales() }}</span>
                  </div>
                  <button
                    class="btn-secondary btn-sm"
                    type="button"
                    *ngIf="puntosActuales() > 0 && !puntosCanjeados()"
                    [disabled]="canjeandoPuntos()"
                    (click)="canjearPuntos()"
                  >{{ canjeandoPuntos() ? 'Canjeando…' : 'Canjear puntos' }}</button>
                  <span class="tag-canjeado" *ngIf="puntosCanjeados()">✓ Puntos canjeados</span>
                </div>
                <div class="info-fila" *ngIf="c.puntosAcumulados !== undefined">
                  <span class="info-label">Puntos acumulados (vida)</span>
                  <span class="info-valor">{{ c.puntosAcumulados }}</span>
                </div>
              </ng-container>

              <!-- Sin cliente: búsqueda o invitado -->
              <ng-template #sinCliente>
                <ng-container *ngIf="!modoInvitado()">
                  <div class="busqueda-bloque">
                    <label class="campo-label" for="buscarCorreo">Identificador cliente (correo)</label>
                    <input
                      id="buscarCorreo"
                      class="input-field"
                      type="email"
                      placeholder="ej. cliente@correo.com"
                      [ngModel]="busquedaCorreo()"
                      (ngModelChange)="onBusquedaChange($event)"
                      autocomplete="off"
                    />
                    <div class="busqueda-resultados" *ngIf="resultadosBusqueda().length > 0">
                      <button
                        class="resultado-item"
                        type="button"
                        *ngFor="let cliente of resultadosBusqueda()"
                        (click)="seleccionarCliente(cliente)"
                      >
                        <span class="resultado-nombre">{{ cliente.nombre }}</span>
                        <span class="resultado-email">{{ cliente.email }}</span>
                      </button>
                    </div>
                    <p class="no-encontrado" *ngIf="sinResultados()">Cliente no encontrado</p>
                    <p class="buscando" *ngIf="buscandoCliente()">Buscando…</p>
                  </div>
                  <div class="separador-o"><hr /><span>o</span><hr /></div>
                  <button class="btn-outline btn-bloque" type="button" (click)="continuarComoInvitado()">
                    Continuar como invitado
                  </button>
                </ng-container>

                <ng-container *ngIf="modoInvitado()">
                  <div class="info-fila">
                    <span class="info-label">Cliente</span>
                    <span class="info-valor muted">Invitado (sin cuenta asignada)</span>
                  </div>
                  <button class="btn-link-small" type="button" (click)="deshacerInvitado()">
                    Asignar cliente
                  </button>
                </ng-container>
              </ng-template>
            </article>

            <!-- SECCIÓN: Detalles de la visita -->
            <article class="card seccion">
              <h2 class="seccion-titulo">Detalles de la visita</h2>
              <div class="info-grid">
                <div class="info-fila">
                  <span class="info-label">Mesa</span>
                  <span class="info-valor">{{ c.mesaIdentificador ?? '—' }}</span>
                </div>
                <div class="info-fila">
                  <span class="info-label">Hora de llegada</span>
                  <span class="info-valor">{{ formatearFecha(c.fechaHoraLlegada) }}</span>
                </div>
                <div class="info-fila">
                  <span class="info-label">Mesero</span>
                  <span class="info-valor">{{ c.meseroNombre ?? '—' }}</span>
                </div>
              </div>
            </article>

            <!-- SECCIÓN: Productos -->
            <article class="card seccion">
              <div class="seccion-head">
                <h2 class="seccion-titulo">Productos</h2>
                <button
                  class="btn-ajuste"
                  type="button"
                  *ngIf="!modoAjuste()"
                  (click)="activarAjuste()"
                >✏️ Ajustar cantidades</button>
              </div>

              <!-- Vista normal: lista agrupada -->
              <ng-container *ngIf="!modoAjuste()">
                <ng-container *ngFor="let grupo of gruposItems()">
                  <p class="grupo-titulo">{{ grupo.categoria }}</p>
                  <ul class="items-lista">
                    <li class="item-fila" *ngFor="let item of grupo.items">
                      <div class="item-info">
                        <p class="item-nombre">{{ item.nombreProducto }}</p>
                        <p class="item-desc" *ngIf="item.descripcion">{{ item.descripcion }}</p>
                        <p class="item-desc muted" *ngIf="item.esMenuEspecial && !item.descripcion">Menú especial</p>
                      </div>
                      <span class="item-cantidad">x{{ item.cantidad }}</span>
                      <span class="item-precio">{{ item.precioUnitario | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
                      <span class="item-subtotal">{{ item.subtotal | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
                    </li>
                  </ul>
                </ng-container>
                <div class="lista-vacia" *ngIf="(c.items?.length ?? 0) === 0">Sin productos registrados</div>
              </ng-container>

              <!-- Modo ajuste (CA-05 / CA-07 / CA-08 / CA-09 / CA-10) -->
              <ng-container *ngIf="modoAjuste()">
                <div class="ajuste-aviso">
                  💡 Modifica cantidades o precios, y elimina ítems si es necesario.
                  <span *ngIf="hayModificaciones()" class="hay-cambios">● Hay cambios sin guardar</span>
                </div>

                <ng-container *ngFor="let grupo of gruposItemsAjuste()">
                  <p class="grupo-titulo">{{ grupo.categoria }}</p>
                  <div class="ajuste-lista">
                    <div
                      class="ajuste-item"
                      *ngFor="let item of grupo.items"
                      [class.ajuste-eliminado]="item.eliminado"
                    >
                      <div class="ajuste-info">
                        <p class="item-nombre">{{ item.nombreProducto }}</p>
                        <p class="item-desc" *ngIf="item.descripcion">{{ item.descripcion }}</p>
                      </div>

                      <!-- Ítem activo -->
                      <ng-container *ngIf="!item.eliminado && !item.confirmandoEliminar">
                        <!-- CA-07: Controles de cantidad, máx 255, mín 1 -->
                        <div class="qty-ctrl">
                          <button
                            class="qty-btn"
                            type="button"
                            [disabled]="item.cantidad <= 1"
                            (click)="decrementar(item)"
                          >−</button>
                          <input
                            class="qty-input"
                            type="number"
                            min="1"
                            max="255"
                            [ngModel]="item.cantidad"
                            (ngModelChange)="setCantidad(item, $event)"
                          />
                          <button
                            class="qty-btn"
                            type="button"
                            [disabled]="item.cantidad >= 255"
                            (click)="incrementar(item)"
                          >+</button>
                        </div>

                        <!-- CA-06: Precio editable solo en ítems modificados -->
                        <div class="precio-ctrl" *ngIf="item.esModificado">
                          <label class="precio-label">Precio unit.</label>
                          <input
                            class="precio-input input-field"
                            type="number"
                            min="0"
                            step="100"
                            [ngModel]="item.precioUnitario"
                            (ngModelChange)="setPrecio(item, $event)"
                          />
                        </div>

                        <span class="ajuste-subtotal">
                          {{ item.subtotal | currency:'COP':'symbol-narrow':'1.0-0' }}
                        </span>

                        <!-- CA-08: Botón eliminar abre confirmación -->
                        <button
                          class="btn-eliminar"
                          type="button"
                          title="Eliminar ítem"
                          (click)="solicitarConfirmacionEliminar(item)"
                        >🗑️</button>
                      </ng-container>

                      <!-- CA-08: Diálogo de confirmación de eliminación -->
                      <ng-container *ngIf="!item.eliminado && item.confirmandoEliminar">
                        <div class="confirmar-eliminar">
                          <span class="confirmar-pregunta">¿Eliminar este ítem?</span>
                          <div class="confirmar-btns">
                            <button class="btn-primary btn-danger btn-xs" type="button" (click)="confirmarEliminar(item)">
                              Sí, eliminar
                            </button>
                            <button class="btn-outline btn-xs" type="button" (click)="cancelarConfirmacionEliminar(item)">
                              Cancelar
                            </button>
                          </div>
                        </div>
                      </ng-container>

                      <!-- Ítem marcado como eliminado -->
                      <ng-container *ngIf="item.eliminado">
                        <span class="tag-eliminado">Eliminado</span>
                        <button class="btn-link-small" type="button" (click)="deshacerEliminado(item)">
                          Deshacer
                        </button>
                      </ng-container>
                    </div>
                  </div>
                </ng-container>

                <!-- CA-09: Guardar ajustes (si hay cambios) / Salir (si no hay) -->
                <!-- CA-10: Cancelar ajuste sin guardar -->
                <div class="ajuste-acciones">
                  <button
                    class="btn-primary"
                    type="button"
                    [disabled]="guardandoAjuste()"
                    (click)="guardarAjuste()"
                  >
                    {{ guardandoAjuste() ? 'Guardando…' : (hayModificaciones() ? 'Guardar ajustes' : 'Salir del modo ajuste') }}
                  </button>
                  <button
                    class="btn-outline"
                    type="button"
                    *ngIf="hayModificaciones()"
                    (click)="cancelarAjuste()"
                  >Cancelar ajustes</button>
                </div>
              </ng-container>
            </article>

            <!-- SECCIÓN: Decoración -->
            <article class="card seccion" *ngIf="c.decoracionNombre">
              <h2 class="seccion-titulo">Decoración adicional</h2>
              <div class="info-fila">
                <span class="info-valor">{{ c.decoracionNombre }}</span>
                <span class="info-valor">{{ c.valorDecoracion | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
              </div>
            </article>

            <!-- SECCIÓN: Abonos -->
            <article class="card seccion" *ngIf="(c.anticipos?.length ?? 0) > 0">
              <h2 class="seccion-titulo">Abonos registrados</h2>
              <ul class="abonos-lista">
                <li class="abono-fila" *ngFor="let abono of c.anticipos">
                  <span class="tag-tipo" [ngClass]="'tipo-' + abono.tipo.toLowerCase()">{{ abono.tipo }}</span>
                  <span class="muted">{{ abono.metodo }}</span>
                  <span class="muted">{{ formatearFecha(abono.fechaHora) }}</span>
                  <span class="abono-monto">{{ abono.monto | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
                </li>
              </ul>
              <div class="abono-resumen">
                <div class="resumen-fila">
                  <span>Monto abonado</span>
                  <span class="valor-verde">− {{ (c.montoAbonado ?? 0) | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
                </div>
                <div class="resumen-fila resumen-saldo">
                  <span>Saldo pendiente</span>
                  <span>{{ (c.saldoPendiente ?? 0) | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
                </div>
              </div>
            </article>
          </div>

          <!-- ─── COLUMNA DERECHA ─── -->
          <div class="cuenta-derecha">

            <!-- SECCIÓN: Resumen de totales -->
            <article class="card seccion resumen-card">
              <h2 class="seccion-titulo">Resumen</h2>
              <div class="resumen-fila">
                <span>Total preorden</span>
                <span>{{ totalPreorden() | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
              </div>
              <div class="resumen-fila" *ngIf="c.valorDecoracion">
                <span>Decoración</span>
                <span>{{ c.valorDecoracion | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
              </div>
              <div class="resumen-fila" *ngIf="descuentoAplicado() > 0">
                <span>Descuento</span>
                <span class="valor-verde">− {{ descuentoAplicado() | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
              </div>
              <div class="resumen-fila resumen-total">
                <span>Total a pagar</span>
                <span>{{ totalAPagar() | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
              </div>
              <div class="resumen-fila" *ngIf="(c.montoAbonado ?? 0) > 0">
                <span>Ya abonado</span>
                <span class="valor-verde">− {{ (c.montoAbonado ?? 0) | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
              </div>
              <div class="resumen-fila resumen-saldo" *ngIf="(c.saldoPendiente ?? 0) > 0">
                <span>Saldo pendiente</span>
                <span>{{ saldoConDescuento() | currency:'COP':'symbol-narrow':'1.0-0' }}</span>
              </div>
            </article>

            <!-- SECCIÓN: Descuento (CA-11) -->
            <article class="card seccion" *ngIf="!cuentaCerrada()">
              <h2 class="seccion-titulo">Descuento</h2>
              <div class="descuento-row">
                <input
                  id="inputDescuento"
                  class="input-field"
                  [class.input-error]="!!errorDescuento()"
                  type="number"
                  min="0"
                  step="100"
                  placeholder="0"
                  [ngModel]="descuentoInput()"
                  (ngModelChange)="descuentoInput.set(+$event)"
                />
                <button class="btn-secondary btn-sm" type="button" (click)="aplicarDescuento()">
                  Aplicar
                </button>
              </div>
              <!-- CA-11: Error si descuento > total -->
              <p class="error-field" *ngIf="errorDescuento()">{{ errorDescuento() }}</p>
            </article>

            <!-- SECCIÓN: Método de pago y registro (CA-12 / CA-13) -->
            <article class="card seccion pago-card" *ngIf="!cuentaCerrada()">
              <h2 class="seccion-titulo">Método de pago</h2>

              <div class="metodos-row">
                <button
                  class="metodo-btn"
                  type="button"
                  *ngFor="let m of metodos"
                  [class.metodo-activo]="metodoPago() === m.value"
                  (click)="seleccionarMetodo(m.value)"
                >{{ m.icono }} {{ m.label }}</button>
              </div>

              <!-- CA-13: Error cuando no se selecciona método -->
              <p class="error-field metodo-error" *ngIf="errorMetodoPago()">
                {{ errorMetodoPago() }}
              </p>

              <div class="pago-acciones">
                <button
                  class="btn-primary btn-bloque"
                  type="button"
                  id="btnRegistrarPago"
                  [disabled]="registrandoPago() || !clienteListoParaPago()"
                  (click)="registrarPago()"
                >{{ registrandoPago() ? 'Registrando…' : 'Registrar pago' }}</button>

                <!-- CA-14: Cancelar proceso -->
                <button class="btn-outline btn-bloque" type="button" (click)="solicitarCancelar()">
                  Cancelar
                </button>
              </div>
            </article>

            <!-- ─── Cuenta cerrada (CA-12) ─── -->
            <article class="card seccion exito-card" *ngIf="cuentaCerrada()">
              <div class="exito-icono">✅</div>
              <h2>Pago registrado correctamente</h2>
              <p>La venta ha sido registrada. El estado de la mesa ha cambiado a "Cerrada".</p>
              <button class="btn-primary btn-bloque" type="button" (click)="irAMapa()">
                Volver al mapa de mesas
              </button>
            </article>
          </div>
        </div>
      </ng-container>
    </section>
  `,
  styles: [
    `
      /* ─── Shell ─── */
      .cierre-shell { gap: 1rem; }

      /* ─── Estado cargando ─── */
      .estado-centro {
        display: grid;
        place-items: center;
        gap: 0.5rem;
        min-height: 160px;
        color: var(--muted);
      }
      .spinner {
        width: 32px; height: 32px;
        border: 3px solid rgba(111,78,55,.2);
        border-top-color: #6f4e37;
        border-radius: 50%;
        animation: spin .8s linear infinite;
      }
      @keyframes spin { to { transform: rotate(360deg); } }

      /* ─── Alertas ─── */
      .alerta {
        border-radius: 10px;
        padding: .65rem .85rem;
        font-size: .84rem;
        font-weight: 600;
      }
      .alerta-error {
        color: #7f1d1d;
        background: rgba(239,68,68,.1);
        border: 1px solid rgba(239,68,68,.3);
        display: flex; flex-direction: column; gap: .4rem;
      }
      .alerta.success {
        color: #14532d;
        background: rgba(34,197,94,.12);
        border: 1px solid rgba(34,197,94,.3);
      }
      .alerta.error {
        color: #7f1d1d;
        background: rgba(239,68,68,.1);
        border: 1px solid rgba(239,68,68,.3);
      }

      /* ─── Modal Cancelar (CA-14) ─── */
      .modal-overlay {
        position: fixed; inset: 0;
        background: rgba(0,0,0,.4);
        display: grid; place-items: center;
        z-index: 9999;
        animation: fadeIn .15s ease;
      }
      @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
      .modal-box {
        background: #fff;
        border-radius: 16px;
        padding: 1.5rem 1.75rem;
        max-width: 360px; width: 90%;
        display: grid; gap: .75rem;
        box-shadow: 0 20px 60px rgba(0,0,0,.25);
      }
      .modal-pregunta {
        margin: 0; font-size: 1rem; font-weight: 700;
      }
      .modal-desc {
        margin: 0; font-size: .84rem; color: var(--muted);
      }
      .modal-acciones { display: flex; gap: .6rem; }

      /* ─── Grid principal ─── */
      .cuenta-grid {
        display: grid;
        grid-template-columns: 1fr 340px;
        gap: 1rem;
        align-items: start;
      }
      @media (max-width: 860px) {
        .cuenta-grid { grid-template-columns: 1fr; }
      }

      /* ─── Secciones / cards ─── */
      .seccion { padding: 1rem; display: grid; gap: .75rem; }
      .seccion + .seccion { margin-top: .75rem; }
      .seccion-titulo {
        margin: 0; font-size: .9rem; font-weight: 700;
        text-transform: uppercase; letter-spacing: .04em;
        color: #6f4e37;
        border-left: 3px solid #6f4e37; padding-left: .45rem;
      }
      .seccion-head { display: flex; justify-content: space-between; align-items: center; }

      /* ─── Info rows ─── */
      .info-grid { display: grid; gap: .4rem; }
      .info-fila {
        display: flex; justify-content: space-between;
        align-items: center; gap: .5rem; min-height: 28px;
      }
      .info-label { font-size: .75rem; color: var(--muted); text-transform: uppercase; letter-spacing: .03em; flex-shrink: 0; }
      .info-valor { font-size: .88rem; font-weight: 600; text-align: right; }
      .info-valor.correo { font-size: .82rem; font-weight: 400; color: #6f4e37; }
      .info-valor.muted { font-weight: 400; color: var(--muted); }

      /* ─── Puntos ─── */
      .puntos-fila { flex-wrap: wrap; gap: .4rem; }
      .puntos-badge {
        display: inline-block;
        background: rgba(111,78,55,.1); color: #6f4e37;
        border-radius: 999px; padding: .1rem .5rem;
        font-size: .8rem; font-weight: 700; margin-left: .35rem;
      }
      .tag-canjeado {
        font-size: .78rem; color: #166534; font-weight: 600;
        background: rgba(34,197,94,.12);
        padding: .15rem .5rem; border-radius: 999px;
      }

      /* ─── Búsqueda cliente ─── */
      .busqueda-bloque { display: grid; gap: .5rem; }
      .campo-label { font-size: .78rem; color: var(--muted); font-weight: 600; }
      .busqueda-resultados {
        border: 1px solid rgba(10,10,10,.12);
        border-radius: 10px; overflow: hidden; display: grid;
      }
      .resultado-item {
        border: none; background: #fffaf5;
        padding: .55rem .75rem; cursor: pointer;
        display: flex; justify-content: space-between; gap: .5rem;
        text-align: left; transition: background .15s;
      }
      .resultado-item:hover { background: rgba(111,78,55,.08); }
      .resultado-item + .resultado-item { border-top: 1px solid rgba(10,10,10,.07); }
      .resultado-nombre { font-weight: 600; font-size: .84rem; }
      .resultado-email { font-size: .76rem; color: var(--muted); }
      .no-encontrado { margin: 0; font-size: .8rem; color: #b45309; padding: .35rem .1rem; }
      .buscando { margin: 0; font-size: .8rem; color: var(--muted); padding: .35rem .1rem; }
      .separador-o {
        display: flex; align-items: center; gap: .5rem;
        color: var(--muted); font-size: .78rem;
      }
      .separador-o hr { flex: 1; border: none; border-top: 1px solid rgba(10,10,10,.12); margin: 0; }

      /* ─── Lista de items ─── */
      .grupo-titulo {
        margin: .5rem 0 .25rem; font-size: .72rem; font-weight: 700;
        text-transform: uppercase; letter-spacing: .05em; color: var(--muted);
      }
      .items-lista { list-style: none; margin: 0; padding: 0; display: grid; gap: .35rem; }
      .item-fila {
        display: grid; grid-template-columns: 1fr auto auto auto;
        gap: .45rem; align-items: center;
        padding: .45rem .55rem;
        border: 1px solid rgba(10,10,10,.08);
        border-radius: 9px; background: #fffaf5;
      }
      .item-nombre { margin: 0; font-size: .84rem; font-weight: 600; }
      .item-desc { margin: .1rem 0 0; font-size: .74rem; color: var(--muted); }
      .item-desc.muted { font-style: italic; }
      .item-cantidad, .item-precio { font-size: .78rem; color: var(--muted); white-space: nowrap; }
      .item-subtotal { font-size: .84rem; font-weight: 700; white-space: nowrap; }
      .lista-vacia { color: var(--muted); font-size: .84rem; text-align: center; padding: .75rem; }

      /* ─── Modo Ajuste ─── */
      .ajuste-aviso {
        background: rgba(111,78,55,.08);
        border: 1px solid rgba(111,78,55,.2);
        border-radius: 10px;
        padding: .55rem .75rem; font-size: .8rem; color: #6f4e37;
        display: flex; justify-content: space-between; align-items: center;
        flex-wrap: wrap; gap: .3rem;
      }
      .hay-cambios {
        font-size: .74rem; font-weight: 700;
        color: #b45309; letter-spacing: .02em;
      }
      .ajuste-lista { display: grid; gap: .4rem; margin-bottom: .35rem; }
      .ajuste-item {
        display: flex; align-items: center; gap: .5rem;
        flex-wrap: wrap; padding: .5rem .6rem;
        border: 1px solid rgba(10,10,10,.1);
        border-radius: 9px; background: #fffaf5;
        transition: background .15s;
      }
      .ajuste-item.ajuste-eliminado {
        opacity: .5; background: rgba(239,68,68,.05);
        border-color: rgba(239,68,68,.25);
      }
      .ajuste-info { flex: 1; min-width: 120px; }

      /* CA-07: Controles de cantidad */
      .qty-ctrl { display: flex; align-items: center; gap: .25rem; }
      .qty-btn {
        width: 28px; height: 28px;
        border: 1px solid rgba(10,10,10,.2);
        background: #fff; border-radius: 6px; cursor: pointer;
        font-size: 1rem; display: grid; place-items: center;
        font-weight: 700; color: #6f4e37;
        transition: background .15s;
      }
      .qty-btn:disabled { opacity: .35; cursor: not-allowed; }
      .qty-btn:not(:disabled):hover { background: rgba(111,78,55,.08); }
      .qty-input {
        width: 50px; text-align: center;
        border: 1px solid rgba(10,10,10,.2);
        border-radius: 6px; padding: .2rem .3rem; font-size: .84rem; background: #fffaf5;
      }
      .qty-input:focus { outline: none; border-color: #6f4e37; }
      .precio-ctrl { display: flex; flex-direction: column; gap: .15rem; }
      .precio-label { font-size: .68rem; color: var(--muted); text-transform: uppercase; }
      .precio-input { width: 90px !important; padding: .2rem .35rem !important; font-size: .82rem !important; }
      .ajuste-subtotal { font-weight: 700; font-size: .84rem; white-space: nowrap; }

      /* CA-08: Confirmación de eliminación */
      .btn-eliminar {
        border: none; background: none; cursor: pointer;
        font-size: 1.05rem; padding: .1rem; opacity: .7;
        transition: opacity .15s;
      }
      .btn-eliminar:hover { opacity: 1; }
      .confirmar-eliminar {
        display: flex; flex-direction: column; gap: .4rem;
        padding: .35rem .5rem;
        background: rgba(239,68,68,.07);
        border: 1px solid rgba(239,68,68,.25);
        border-radius: 8px; flex: 1;
      }
      .confirmar-pregunta { font-size: .8rem; font-weight: 700; color: #b91c1c; margin: 0; }
      .confirmar-btns { display: flex; gap: .35rem; }
      .btn-xs { padding: .25rem .55rem !important; font-size: .75rem !important; }
      .tag-eliminado { font-size: .74rem; color: #b91c1c; font-weight: 600; }

      /* CA-09 y CA-10: Botones ajuste */
      .ajuste-acciones { display: flex; gap: .5rem; flex-wrap: wrap; margin-top: .35rem; }

      /* ─── Abonos ─── */
      .abonos-lista { list-style: none; margin: 0; padding: 0; display: grid; gap: .35rem; }
      .abono-fila { display: flex; gap: .5rem; align-items: center; font-size: .82rem; flex-wrap: wrap; }
      .tag-tipo { border-radius: 999px; padding: .1rem .45rem; font-size: .7rem; font-weight: 700; text-transform: uppercase; }
      .tipo-anticipo { background: rgba(59,130,246,.12); color: #1d4ed8; }
      .tipo-devolucion { background: rgba(239,68,68,.1); color: #b91c1c; }
      .abono-monto { font-weight: 700; }
      .abono-resumen { border-top: 1px solid rgba(10,10,10,.1); padding-top: .55rem; display: grid; gap: .3rem; }

      /* ─── Resumen ─── */
      .resumen-card { position: sticky; top: 1rem; }
      .resumen-fila { display: flex; justify-content: space-between; font-size: .85rem; padding: .2rem 0; }
      .resumen-total { font-weight: 700; font-size: 1rem; border-top: 1px solid rgba(10,10,10,.12); padding-top: .45rem; margin-top: .2rem; }
      .resumen-saldo { font-weight: 700; color: #6f4e37; }
      .valor-verde { color: #166534; font-weight: 600; }

      /* ─── Descuento (CA-11) ─── */
      .descuento-row { display: flex; gap: .45rem; align-items: center; }
      .descuento-row .input-field { flex: 1; }
      .input-error { border-color: #ef4444 !important; }
      .error-field { margin: 0; font-size: .78rem; color: #b91c1c; font-weight: 600; }
      .metodo-error { margin-top: .25rem; }

      /* ─── Métodos de pago (CA-12 / CA-13) ─── */
      .metodos-row { display: flex; gap: .4rem; flex-wrap: wrap; }
      .metodo-btn {
        flex: 1; min-width: 90px;
        border: 1px solid rgba(10,10,10,.18);
        background: #fff; border-radius: 10px;
        padding: .55rem .5rem; cursor: pointer;
        font-size: .8rem; font-weight: 600;
        transition: all .15s;
      }
      .metodo-btn.metodo-activo { background: #6f4e37; color: #fff; border-color: #6f4e37; }
      .pago-acciones { display: grid; gap: .4rem; margin-top: .35rem; }

      /* ─── Éxito (CA-12) ─── */
      .exito-card { text-align: center; padding: 1.5rem 1rem; }
      .exito-icono { font-size: 2.5rem; margin-bottom: .5rem; }
      .exito-card h2 { margin: 0 0 .4rem; font-size: 1rem; color: #166534; }
      .exito-card p { margin: 0 0 .8rem; font-size: .84rem; color: var(--muted); }

      /* ─── Botones utilitarios ─── */
      .btn-ajuste {
        border: 1px solid rgba(111,78,55,.35);
        background: rgba(111,78,55,.07); color: #6f4e37;
        border-radius: 999px; padding: .3rem .7rem;
        font-size: .76rem; font-weight: 600; cursor: pointer;
        transition: background .15s;
      }
      .btn-ajuste:hover { background: rgba(111,78,55,.15); }
      .btn-sm { padding: .3rem .65rem !important; font-size: .78rem !important; }
      .btn-bloque { width: 100%; }
      .btn-link-small {
        border: none; background: none; color: #6f4e37;
        font-size: .78rem; font-weight: 600; cursor: pointer;
        padding: 0; text-decoration: underline;
      }
      .btn-outline {
        border: 1px solid rgba(10,10,10,.2);
        background: #fff; border-radius: 10px;
        padding: .6rem 1rem; cursor: pointer;
        font-weight: 600; font-size: .84rem;
      }
      .btn-danger { background: #ef4444 !important; border-color: #ef4444 !important; }
      .muted { color: var(--muted); }

      @media (max-width: 550px) {
        .item-fila { display: flex; flex-wrap: wrap; justify-content: space-between; }
        .item-info { width: 100%; margin-bottom: 0.2rem; }
        .info-fila { flex-direction: column; align-items: flex-start; gap: 0.1rem; }
        .info-valor { text-align: left; }
      }
    `,
  ],
})
export class PagoCierrePageComponent implements OnInit, OnDestroy {
  private readonly cuentaService = inject(CuentaMesaService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy$ = new Subject<void>();
  private readonly busqueda$ = new Subject<string>();

  /** ID de la visita obtenido de los query params */
  private visitaId = 0;
  /** Snapshot de los ítems al momento de activar el modo ajuste (para CA-10) */
  private snapshotAjuste: ItemAjusteLocal[] = [];

  /* ── Estado principal ── */
  readonly loading = signal(true);
  readonly cargaError = signal<string | null>(null);
  readonly cuenta = signal<CuentaPreliminar | null>(null);

  /* ── Mensajes temporales ── */
  readonly mensajeAccion = signal('');
  readonly mensajeTono = signal<'success' | 'error' | ''>('');

  /* ── Cliente ── */
  readonly busquedaCorreo = signal('');
  readonly resultadosBusqueda = signal<ClienteBusqueda[]>([]);
  readonly buscandoCliente = signal(false);
  readonly sinResultados = signal(false);
  readonly clienteAsignado = signal(false);
  readonly modoInvitado = signal(false);

  /* ── Puntos ── */
  readonly puntosActuales = signal(0);
  readonly canjeandoPuntos = signal(false);
  readonly puntosCanjeados = signal(false);

  /* ── Ajuste de cantidades ── */
  readonly modoAjuste = signal(false);
  readonly itemsAjuste = signal<ItemAjusteLocal[]>([]);
  readonly guardandoAjuste = signal(false);

  /* ── Pago ── */
  readonly descuentoInput = signal<number>(0);
  readonly descuentoAplicado = signal(0);
  readonly errorDescuento = signal('');
  readonly metodoPago = signal<MetodoPago | null>(null);
  readonly errorMetodoPago = signal('');
  readonly registrandoPago = signal(false);
  readonly cuentaCerrada = signal(false);

  /* ── Modal cancelar (CA-14) ── */
  readonly mostrandoCancelar = signal(false);

  readonly metodos: { value: MetodoPago; label: string; icono: string }[] = [
    { value: 'EFECTIVO', label: 'Efectivo', icono: '💵' },
    { value: 'TARJETA', label: 'Tarjeta', icono: '💳' },
    { value: 'TRANSFERENCIA', label: 'Transferencia', icono: '🏦' },
  ];

  /* ── Computed ── */
  readonly totalPreorden = computed(() => this.cuenta()?.totalPreorden ?? 0);
  readonly totalAPagar = computed(() =>
    Math.max(0, (this.cuenta()?.totalAPagar ?? 0) - this.descuentoAplicado())
  );
  readonly saldoConDescuento = computed(() =>
    Math.max(0, (this.cuenta()?.saldoPendiente ?? this.totalAPagar()) - this.descuentoAplicado())
  );
  readonly gruposItems = computed(() => this.agruparItems(this.cuenta()?.items ?? []));
  readonly gruposItemsAjuste = computed(() => this.agruparItemsAjuste(this.itemsAjuste()));

  /**
   * CA-09: detecta si hay alguna modificación respecto al snapshot original.
   * Se usa para mostrar/ocultar "Cancelar ajustes" y cambiar el label de "Guardar".
   */
  readonly hayModificaciones = computed(() => {
    const items = this.itemsAjuste();
    for (let i = 0; i < items.length; i++) {
      const snap = this.snapshotAjuste[i];
      if (!snap) return true;
      if (
        items[i].cantidad !== snap.cantidad ||
        items[i].precioUnitario !== snap.precioUnitario ||
        items[i].eliminado !== snap.eliminado
      ) return true;
    }
    return false;
  });

  /** El cajero puede pagar si el cliente está asignado, en modo invitado,
   *  o si la cuenta ya trae clienteId desde el servidor. */
  readonly clienteListoParaPago = computed(
    () => this.clienteAsignado() || this.modoInvitado() || !!(this.cuenta()?.clienteId)
  );

  ngOnInit(): void {
    // CA-15: al cargar (o recargar) se obtiene la cuenta limpia del servidor.
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const rawId = params['visitaId'];
      if (rawId) {
        this.visitaId = Number(rawId);
        this.cargarCuenta();
      } else {
        this.cargaError.set('No se especificó una visita válida.');
        this.loading.set(false);
      }
    });

    /* Búsqueda de cliente con debounce (CA-02) */
    this.busqueda$
      .pipe(
        debounceTime(350),
        distinctUntilChanged(),
        switchMap((correo) => {
          if (correo.length < 2) {
            this.resultadosBusqueda.set([]);
            this.sinResultados.set(false);
            this.buscandoCliente.set(false);
            return of([]);
          }
          this.buscandoCliente.set(true);
          this.sinResultados.set(false);
          return this.cuentaService.buscarClientes(correo);
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (resultados) => {
          this.buscandoCliente.set(false);
          this.resultadosBusqueda.set(resultados);
          this.sinResultados.set(resultados.length === 0 && this.busquedaCorreo().length >= 2);
        },
        error: () => {
          this.buscandoCliente.set(false);
          this.resultadosBusqueda.set([]);
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /* ════════ Carga de cuenta (CA-15) ════════ */

  cargarCuenta(): void {
    this.loading.set(true);
    this.cargaError.set(null);
    // CA-15: al recargar se limpian todos los estados locales
    this.modoAjuste.set(false);
    this.itemsAjuste.set([]);
    this.snapshotAjuste = [];
    this.descuentoInput.set(0);
    this.descuentoAplicado.set(0);
    this.errorDescuento.set('');
    this.metodoPago.set(null);
    this.errorMetodoPago.set('');
    this.puntosCanjeados.set(false);

    this.cuentaService.getCuenta(this.visitaId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.cuenta.set(data);
        this.puntosActuales.set(data.puntosCanjeables ?? 0);
        if (data.clienteId) this.clienteAsignado.set(true);
        this.loading.set(false);
      },
      error: (err) => {
        this.cargaError.set(this.resolverError(err, 'No se pudo cargar la cuenta de la mesa.'));
        this.loading.set(false);
      },
    });
  }

  recargarCuenta(): void { this.cargarCuenta(); }

  /* ════════ Búsqueda de cliente (CA-02) ════════ */

  onBusquedaChange(valor: string): void {
    this.busquedaCorreo.set(valor);
    this.busqueda$.next(valor);
  }

  seleccionarCliente(cliente: ClienteBusqueda): void {
    this.resultadosBusqueda.set([]);
    this.sinResultados.set(false);
    this.buscandoCliente.set(false);

    this.cuentaService.asignarCliente(this.visitaId, cliente.clienteId).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.clienteAsignado.set(true);
        this.modoInvitado.set(false);
        this.recargarCuenta();
      },
      error: (err) => {
        this.mostrarMensaje(this.resolverError(err, 'No se pudo asignar el cliente.'), 'error');
      },
    });
  }

  /* ════════ Invitado (CA-04) ════════ */

  continuarComoInvitado(): void {
    this.cuentaService.asignarCliente(this.visitaId, undefined).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.modoInvitado.set(true);
        this.clienteAsignado.set(false);
        this.resultadosBusqueda.set([]);
      },
      error: (err) => {
        this.mostrarMensaje(this.resolverError(err, 'No se pudo continuar como invitado.'), 'error');
      },
    });
  }

  deshacerInvitado(): void {
    this.modoInvitado.set(false);
    this.busquedaCorreo.set('');
    this.resultadosBusqueda.set([]);
  }

  /* ════════ Canje de puntos (CA-03) ════════ */

  canjearPuntos(): void {
    const clienteId = this.cuenta()?.clienteId;
    const emailCajero = this.authService.currentUser()?.email;
    if (!clienteId || !emailCajero) return;

    this.canjeandoPuntos.set(true);
    this.cuentaService.canjearPuntos(clienteId, emailCajero).pipe(takeUntil(this.destroy$)).subscribe({
      next: (resp) => {
        this.puntosActuales.set(resp.puntosActuales);
        this.puntosCanjeados.set(true);
        this.canjeandoPuntos.set(false);
        const actual = this.cuenta();
        if (actual) this.cuenta.set({ ...actual, puntosCanjeables: 0 });
        this.mostrarMensaje('Puntos canjeados exitosamente.', 'success');
      },
      error: (err) => {
        this.canjeandoPuntos.set(false);
        this.mostrarMensaje(this.resolverError(err, 'No se pudieron canjear los puntos.'), 'error');
      },
    });
  }

  /* ════════ Modo ajuste de cantidades (CA-05 / CA-07 / CA-08 / CA-09 / CA-10) ════════ */

  activarAjuste(): void {
    const items = this.cuenta()?.items ?? [];
    const copia: ItemAjusteLocal[] = items.map((item) => ({
      comandaItemId: item.comandaItemId,
      nombreProducto: item.nombreProducto,
      categoriaProducto: item.categoriaProducto,
      descripcion: item.descripcion,
      esModificado: item.esModificado,
      esMenuEspecial: item.esMenuEspecial,
      menuGrupo: item.menuGrupo,
      cantidad: item.cantidad,
      precioUnitario: item.precioUnitario,
      subtotal: item.subtotal,
      eliminado: false,
      confirmandoEliminar: false,
    }));
    // CA-10: guardamos snapshot para poder revertir
    this.snapshotAjuste = copia.map((i) => ({ ...i }));
    this.itemsAjuste.set(copia);
    this.modoAjuste.set(true);
  }

  /** CA-10: Descarta cambios y restaura los valores del snapshot */
  cancelarAjuste(): void {
    this.itemsAjuste.set(this.snapshotAjuste.map((i) => ({ ...i })));
    this.modoAjuste.set(false);
    this.itemsAjuste.set([]);
    this.snapshotAjuste = [];
  }

  /** CA-07: Incrementar con límite de 255 */
  incrementar(item: ItemAjusteLocal): void {
    if (item.cantidad < 255) this.setCantidad(item, item.cantidad + 1);
  }

  /** CA-07: Decrementar con mínimo de 1 */
  decrementar(item: ItemAjusteLocal): void {
    if (item.cantidad > 1) this.setCantidad(item, item.cantidad - 1);
  }

  /** CA-07: Cambio de cantidad — valida rango y recalcula subtotal */
  setCantidad(item: ItemAjusteLocal, valor: number): void {
    const nueva = Math.max(1, Math.min(255, Math.round(Number(valor) || 1)));
    item.cantidad = nueva;
    item.subtotal = parseFloat((item.precioUnitario * nueva).toFixed(2));
    this.itemsAjuste.set([...this.itemsAjuste()]);
  }

  /** CA-06: Cambio de precio en ítems con descripción */
  setPrecio(item: ItemAjusteLocal, valor: number): void {
    const nuevo = Math.max(0, Number(valor) || 0);
    item.precioUnitario = nuevo;
    item.subtotal = parseFloat((nuevo * item.cantidad).toFixed(2));
    this.itemsAjuste.set([...this.itemsAjuste()]);
  }

  /** CA-08: Abre diálogo de confirmación en el ítem */
  solicitarConfirmacionEliminar(item: ItemAjusteLocal): void {
    item.confirmandoEliminar = true;
    this.itemsAjuste.set([...this.itemsAjuste()]);
  }

  /** CA-08: Usuario confirma la eliminación */
  confirmarEliminar(item: ItemAjusteLocal): void {
    item.confirmandoEliminar = false;
    item.eliminado = true;
    this.itemsAjuste.set([...this.itemsAjuste()]);
  }

  /** CA-08: Usuario cancela la confirmación sin eliminar */
  cancelarConfirmacionEliminar(item: ItemAjusteLocal): void {
    item.confirmandoEliminar = false;
    this.itemsAjuste.set([...this.itemsAjuste()]);
  }

  /** Deshacer un ítem marcado como eliminado */
  deshacerEliminado(item: ItemAjusteLocal): void {
    item.eliminado = false;
    this.itemsAjuste.set([...this.itemsAjuste()]);
  }

  /**
   * CA-09: Guardar ajustes.
   * - Si no hay cambios → simplemente sale del modo ajuste.
   * - Si hay cambios → llama al backend y actualiza la cuenta.
   */
  guardarAjuste(): void {
    if (!this.hayModificaciones()) {
      // CA-09: Sin cambios → sale del modo sin llamada al backend
      this.modoAjuste.set(false);
      this.itemsAjuste.set([]);
      this.snapshotAjuste = [];
      return;
    }

    this.guardandoAjuste.set(true);

    const itemsParaEnviar: AjusteItem[] = this.itemsAjuste()
      .filter((i) => !i.eliminado)
      .map((i) => ({
        comandaItemId: i.comandaItemId,
        cantidad: i.cantidad,
        precio: i.esModificado ? i.precioUnitario : undefined,
      }));

    const eliminados = this.itemsAjuste()
      .filter((i) => i.eliminado)
      .map((i) => i.comandaItemId);

    this.cuentaService.ajustarItems(this.visitaId, itemsParaEnviar, eliminados)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (cuentaActualizada) => {
          this.cuenta.set(cuentaActualizada);
          this.modoAjuste.set(false);
          this.itemsAjuste.set([]);
          this.snapshotAjuste = [];
          this.guardandoAjuste.set(false);
          this.mostrarMensaje('Ajustes guardados correctamente.', 'success');
        },
        error: (err) => {
          this.guardandoAjuste.set(false);
          this.mostrarMensaje(this.resolverError(err, 'No se pudieron guardar los ajustes.'), 'error');
        },
      });
  }

  /* ════════ Descuento (CA-11) ════════ */

  aplicarDescuento(): void {
    const val = Number(this.descuentoInput()) || 0;
    const totalBase = this.cuenta()?.totalAPagar ?? 0;

    this.errorDescuento.set('');

    if (val < 0) {
      this.errorDescuento.set('El descuento no puede ser negativo.');
      return;
    }

    // CA-11: Validar que el descuento no supere el total
    if (val > totalBase) {
      this.errorDescuento.set('El descuento no puede ser mayor al total.');
      return;
    }

    this.descuentoAplicado.set(val);
    this.mostrarMensaje(
      val > 0
        ? `Descuento de $${val.toLocaleString('es-CO')} aplicado.`
        : 'Descuento eliminado.',
      'success'
    );
  }

  /* ════════ Método de pago ════════ */

  seleccionarMetodo(metodo: MetodoPago): void {
    this.metodoPago.set(metodo);
    this.errorMetodoPago.set(''); // Limpiar error al seleccionar
  }

  /* ════════ Registrar pago (CA-12 / CA-13) ════════ */

  registrarPago(): void {
    // CA-13: Validar que haya método de pago seleccionado
    if (!this.metodoPago()) {
      this.errorMetodoPago.set('Se debe seleccionar el método de pago.');
      return;
    }
    this.errorMetodoPago.set('');

    const emailCajero = this.authService.currentUser()?.email;
    if (!emailCajero) {
      this.mostrarMensaje('No se pudo obtener el correo del cajero. Vuelva a iniciar sesión.', 'error');
      return;
    }

    this.registrandoPago.set(true);

    this.cuentaService
      .cerrarCuenta(
        this.visitaId,
        emailCajero,
        this.metodoPago()!,
        this.descuentoAplicado() > 0 ? this.descuentoAplicado() : undefined
      )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          // CA-12: Mostrar mensaje de éxito y la tarjeta de cuenta cerrada
          this.registrandoPago.set(false);
          this.cuentaCerrada.set(true);
          // CA-12: El mapa se actualiza en tiempo real vía WebSocket existente en el backend
        },
        error: (err) => {
          this.registrandoPago.set(false);
          this.mostrarMensaje(this.resolverError(err, 'No se pudo registrar el pago.'), 'error');
        },
      });
  }

  /* ════════ Cancelar proceso (CA-14) ════════ */

  solicitarCancelar(): void {
    this.mostrandoCancelar.set(true);
  }

  cerrarModalCancelar(): void {
    this.mostrandoCancelar.set(false);
  }

  confirmarCancelar(): void {
    this.mostrandoCancelar.set(false);
    this.router.navigate(['/app/cajero/mapa-mesas']);
  }

  /* ════════ Navegación ════════ */

  irAMapa(): void {
    this.router.navigate(['/app/cajero/mapa-mesas']);
  }

  /* ════════ Helpers ════════ */

  formatearFecha(value?: string): string {
    if (!value) return '—';
    const parsed = new Date(value);
    if (isNaN(parsed.getTime())) return value;
    return parsed.toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
  }

  private agruparItems(items: CuentaItem[]): { categoria: string; items: CuentaItem[] }[] {
    return this.agruparPor(items, (i) => i.categoriaProducto);
  }

  private agruparItemsAjuste(items: ItemAjusteLocal[]): { categoria: string; items: ItemAjusteLocal[] }[] {
    return this.agruparPor(items, (i) => i.categoriaProducto);
  }

  private agruparPor<T extends { categoriaProducto: string }>(
    items: T[],
    getCategoria: (i: T) => string
  ): { categoria: string; items: T[] }[] {
    const orden = ['PLATO', 'BEBIDA', 'OTRO'];
    const mapa = new Map<string, T[]>();
    for (const item of items) {
      const cat = getCategoria(item) ?? 'OTRO';
      if (!mapa.has(cat)) mapa.set(cat, []);
      mapa.get(cat)!.push(item);
    }
    const resultado = orden
      .filter((c) => mapa.has(c))
      .map((c) => ({ categoria: this.labelCategoria(c), items: mapa.get(c)! }));
    for (const [k, v] of mapa) {
      if (!orden.includes(k)) resultado.push({ categoria: this.labelCategoria(k), items: v });
    }
    return resultado;
  }

  private labelCategoria(cat: string): string {
    switch (cat) {
      case 'PLATO': return '🍽️ Platos';
      case 'BEBIDA': return '🥤 Bebidas';
      default: return '📦 Otros';
    }
  }

  private mostrarMensaje(texto: string, tono: 'success' | 'error'): void {
    this.mensajeAccion.set(texto);
    this.mensajeTono.set(tono);
    setTimeout(() => {
      this.mensajeAccion.set('');
      this.mensajeTono.set('');
    }, 4500);
  }

  private resolverError(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse) {
      if (typeof err.error?.message === 'string' && err.error.message.trim()) return err.error.message;
      if (typeof err.error === 'string' && err.error.trim()) return err.error;
    }
    return fallback;
  }
}
