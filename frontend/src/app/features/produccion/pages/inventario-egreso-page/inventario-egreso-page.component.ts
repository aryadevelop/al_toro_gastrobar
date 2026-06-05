import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { InventoryService } from '../../../../core/services/inventory.service';
import { BackendInventarioItemBusqueda, BackendAjusteInventarioRequest } from '../../../../core/models/api.models';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-inventario-egreso-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  templateUrl: './inventario-egreso-page.component.html',
  styleUrls: ['./inventario-egreso-page.component.scss']
})
export class InventarioEgresoPageComponent implements OnInit {
  ajusteForm: FormGroup;
  
  // Búsqueda
  searchQuery$ = new Subject<string>();
  searchResults: BackendInventarioItemBusqueda[] = [];
  isSearching = false;
  showDropdown = false;
  
  // Producto seleccionado
  selectedProduct: BackendInventarioItemBusqueda | null = null;
  
  // Estado de carga al guardar
  isSaving = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryService,
    private readonly router: Router
  ) {
    this.ajusteForm = this.fb.group({
      productoBusqueda: [''], // Campo para escribir
      productoId: [null, [Validators.required]],
      cantidad: [null, [Validators.required, Validators.min(0.01)]],
      tipoMovimiento: [null, [Validators.required]],
      proveedor: [''],
      numeroFactura: [''],
      observaciones: ['']
    });
  }

  ngOnInit(): void {
    // Configurar autocompletado
    this.searchQuery$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((query) => {
        if (!query || query.length < 2) {
          this.searchResults = [];
          this.showDropdown = false;
          this.isSearching = false;
          return [];
        }
        this.isSearching = true;
        return this.inventoryService.buscarItemsInventario(query);
      })
    ).subscribe({
      next: (res) => {
        if (res && res.data) {
          this.searchResults = res.data;
          this.showDropdown = true;
        } else {
          this.searchResults = [];
          this.showDropdown = false;
        }
        this.isSearching = false;
      },
      error: () => {
        this.searchResults = [];
        this.showDropdown = false;
        this.isSearching = false;
      }
    });

    // Detectar si cambian el texto de búsqueda manualmente después de haber seleccionado algo
    this.ajusteForm.get('productoBusqueda')?.valueChanges.subscribe((val) => {
      if (this.selectedProduct && val !== this.selectedProduct.nombre) {
        this.clearSelection();
      }
      if (typeof val === 'string') {
        this.searchQuery$.next(val);
      }
    });
  }

  // Prevenir que el usuario pierda datos no guardados al recargar
  @HostListener('window:beforeunload', ['$event'])
  unloadNotification($event: any): void {
    if (this.ajusteForm.dirty && !this.isSaving) {
      $event.returnValue = true;
    }
  }

  onSearchFocus(): void {
    if (this.searchResults.length > 0) {
      this.showDropdown = true;
    }
  }

  // Cerrar dropdown al hacer click fuera (se maneja también desde HTML o blur)
  onSearchBlur(): void {
    setTimeout(() => {
      this.showDropdown = false;
    }, 200);
  }

  selectProduct(product: BackendInventarioItemBusqueda): void {
    this.selectedProduct = product;
    this.ajusteForm.patchValue({
      productoBusqueda: product.nombre,
      productoId: product.productoId
    });
    this.searchResults = [];
    this.showDropdown = false;
  }

  clearSelection(): void {
    this.selectedProduct = null;
    this.ajusteForm.patchValue({
      productoId: null
    });
  }

  cancelar(): void {
    if (this.ajusteForm.dirty) {
      if (window.confirm('¿Está seguro de cancelar? Los datos ingresados se perderán.')) {
        this.ajusteForm.reset();
        this.router.navigate(['/produccion']);
      }
    } else {
      this.router.navigate(['/produccion']);
    }
  }

  guardar(): void {
    // Validar tipo de movimiento
    const tipo = this.ajusteForm.get('tipoMovimiento')?.value;
    if (!tipo) {
      window.alert('Atención: El tipo de movimiento es obligatorio');
      return;
    }

    // Validar producto
    if (!this.ajusteForm.get('productoId')?.value || !this.selectedProduct) {
      window.alert('Atención: El producto o insumo es obligatorio');
      return;
    }

    // Validar cantidad
    const cantidad = this.ajusteForm.get('cantidad')?.value;
    if (!cantidad || cantidad <= 0) {
      window.alert('Atención: La cantidad es obligatoria y debe ser mayor a 0');
      return;
    }

    // Validar stock si es EGRESO
    if (tipo === 'EGRESO') {
      if (this.selectedProduct.stockActual < cantidad) {
        window.alert(`Stock insuficiente. Stock actual: ${this.selectedProduct.stockActual} ${this.selectedProduct.unidadMedida}`);
        return;
      }
    }

    if (this.ajusteForm.invalid) {
      this.ajusteForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;

    const request: BackendAjusteInventarioRequest = {
      productoId: this.selectedProduct.productoId,
      tipoMovimiento: tipo,
      cantidad: cantidad,
      proveedor: this.ajusteForm.get('proveedor')?.value || undefined,
      numeroFactura: this.ajusteForm.get('numeroFactura')?.value || undefined,
      observaciones: this.ajusteForm.get('observaciones')?.value || undefined
    };

    this.inventoryService.registrarAjuste(request).subscribe({
      next: () => {
        this.isSaving = false;
        this.ajusteForm.reset();
        window.alert('Ajuste de inventario registrado correctamente.');
        this.router.navigate(['/produccion']);
      },
      error: () => {
        this.isSaving = false;
        window.alert('Error: No se pudo registrar el ajuste de inventario.');
      }
    });
  }
}
