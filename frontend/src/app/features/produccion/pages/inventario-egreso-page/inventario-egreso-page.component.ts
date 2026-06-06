import { Component, OnDestroy, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { InventoryMovementService } from '../../../../core/services/inventory-movement.service';
import { WebSocketService } from '../../../../core/services/websocket.service';
import { BackendInventarioItemBusqueda, BackendInventarioMovimientoRequest } from '../../../../core/models/api.models';

@Component({
  selector: 'app-inventario-egreso-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  templateUrl: './inventario-egreso-page.component.html',
  styleUrls: ['./inventario-egreso-page.component.scss']
})
export class InventarioEgresoPageComponent implements OnInit, OnDestroy {
  readonly form = this.fb.nonNullable.group({
    search: [''],
    selectedItemName: [''],
    tipoElemento: ['' as 'PRODUCTO' | 'INSUMO'],
    elementoId: [null as number | null],
    stockActual: [{ value: 0, disabled: true }],
    unidad: [{ value: '', disabled: true }],
    tipoMovimiento: ['EGRESO' as 'INGRESO' | 'EGRESO', [Validators.required]],
    cantidad: [null as number | null, [Validators.required, Validators.min(1)]],
    proveedor: [''],
    numeroFactura: [''],
    observaciones: ['']
  });

  // Autocomplete state
  searchResults: BackendInventarioItemBusqueda[] = [];
  isSearching = false;
  showDropdown = false;
  private readonly searchSubject = new Subject<string>();
  private searchSub?: Subscription;

  // Validation
  stockErrorMsg: string | null = null;
  submitErrorMsg: string | null = null;
  isSubmitting = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly movementService: InventoryMovementService,
    private readonly wsService: WebSocketService,
    private readonly router: Router
  ) {}

  @HostListener('window:beforeunload', ['$event'])
  unloadNotification($event: any) {
    if (this.form.dirty) {
      $event.returnValue = true;
    }
  }

  ngOnInit(): void {
    this.searchSub = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        this.isSearching = true;
        if (!query) {
          this.isSearching = false;
          return [];
        }
        return this.movementService.buscarItems(query);
      })
    ).subscribe({
      next: (res: any) => {
        this.searchResults = res.success ? res.data : [];
        this.showDropdown = this.searchResults.length > 0;
        this.isSearching = false;
      },
      error: () => {
        this.searchResults = [];
        this.showDropdown = false;
        this.isSearching = false;
      }
    });

    // Validar stock cada vez que cambia la cantidad o el tipo de mov
    this.form.valueChanges.subscribe(() => {
      this.validarStock();
    });
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  onSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchSubject.next(target.value);
  }

  seleccionarItem(item: BackendInventarioItemBusqueda): void {
    this.form.patchValue({
      search: item.nombre,
      selectedItemName: item.nombre,
      tipoElemento: item.tipo,
      elementoId: item.id,
      stockActual: item.stockActual,
      unidad: item.unidad
    });
    this.showDropdown = false;
    this.validarStock();
  }

  validarStock(): void {
    this.stockErrorMsg = null;
    const { tipoMovimiento, cantidad, elementoId } = this.form.getRawValue();
    const stockActual = this.form.getRawValue().stockActual;
    
    if (elementoId && tipoMovimiento === 'EGRESO' && cantidad !== null && cantidad > stockActual) {
      this.stockErrorMsg = `Stock insuficiente. Stock actual: ${stockActual} ${this.form.getRawValue().unidad}`;
    }
  }

  cancelar(): void {
    if (this.form.dirty) {
      const confirm = window.confirm('¿Está seguro de cancelar? Los datos ingresados se perderán.');
      if (!confirm) return;
    }
    this.form.reset();
    this.stockErrorMsg = null;
    this.submitErrorMsg = null;
  }

  guardar(): void {
    this.submitErrorMsg = null;

    // BDD Required Validations
    if (!this.form.getRawValue().elementoId) {
      this.submitErrorMsg = 'El producto o insumo es obligatorio';
      return;
    }
    if (!this.form.getRawValue().tipoMovimiento) {
      this.submitErrorMsg = 'El tipop de movidmiento es obligatorio';
      return;
    }
    if (!this.form.getRawValue().cantidad) {
      this.submitErrorMsg = 'La cantidad es obligatoria';
      return;
    }
    
    this.validarStock();
    if (this.stockErrorMsg) {
      // Evitar guardar si hay error de stock
      return;
    }

    this.isSubmitting = true;
    const req: BackendInventarioMovimientoRequest = {
      tipoElemento: this.form.getRawValue().tipoElemento,
      elementoId: this.form.getRawValue().elementoId!,
      tipoMovimiento: this.form.getRawValue().tipoMovimiento,
      cantidad: this.form.getRawValue().cantidad!,
      observaciones: this.form.getRawValue().observaciones || undefined
    };

    // Añadir datos opcionales a observaciones
    const proveedor = this.form.getRawValue().proveedor;
    const factura = this.form.getRawValue().numeroFactura;
    let notasAdicionales = req.observaciones || '';
    if (proveedor) notasAdicionales += ` | Proveedor: ${proveedor}`;
    if (factura) notasAdicionales += ` | Factura: ${factura}`;
    req.observaciones = notasAdicionales.trim() !== '' ? notasAdicionales : undefined;

    this.movementService.registrarMovimiento(req).subscribe({
      next: () => {
        this.isSubmitting = false;
        
        // Notificar cambio a las comandas (real-time update)
        if (req.tipoMovimiento === 'EGRESO') {
          this.wsService.sendMessage('/app/produccion/comandas', { accion: 'STOCK_EGRESO' });
        } else {
          this.wsService.sendMessage('/app/produccion/comandas', { accion: 'STOCK_INGRESO' });
        }

        window.alert('Ajuste de inventario registrado correctamente.');
        this.form.reset({ tipoMovimiento: 'EGRESO' });
        this.form.markAsPristine();
      },
      error: () => {
        this.isSubmitting = false;
        window.alert('Error al registrar el ajuste.');
      }
    });
  }
}
