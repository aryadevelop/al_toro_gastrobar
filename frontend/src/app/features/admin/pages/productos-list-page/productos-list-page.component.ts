import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoAdminService } from '../../../../core/services/producto-admin.service';
import { BackendProductoAdminItem } from '../../../../core/models/api.models';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-productos-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent],
  templateUrl: './productos-list-page.component.html',
  styleUrls: ['./productos-list-page.component.scss']
})
export class ProductosListPageComponent implements OnInit {
  allProducts: BackendProductoAdminItem[] = [];
  filteredProducts: BackendProductoAdminItem[] = [];
  
  // Categorías extraídas dinámicamente
  categories: string[] = [];
  selectedCategory: string = 'ALL';
  
  // Búsqueda
  searchQuery: string = '';
  
  // Estados de vista
  isLoading: boolean = true;
  isInitialEmptyState: boolean = false; // Sin productos registrados en DB
  
  // Mensajes de alerta
  alertMessage: string | null = null;
  alertType: 'warning' | 'info' | 'error' | null = null;

  constructor(private readonly productoAdminService: ProductoAdminService) {}

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos(): void {
    this.isLoading = true;
    this.productoAdminService.listarProductosVentaDirecta().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.allProducts = res.data;
          this.isInitialEmptyState = this.allProducts.length === 0;
          this.extractCategories();
          this.applyFilters();
        } else {
          this.allProducts = [];
          this.isInitialEmptyState = true;
        }
        this.isLoading = false;
      },
      error: () => {
        this.allProducts = [];
        this.isInitialEmptyState = true;
        this.isLoading = false;
        this.showAlert('Error al cargar los productos. Intente más tarde.', 'error');
      }
    });
  }

  extractCategories(): void {
    const cats = new Set(this.allProducts.map(p => p.categoria));
    this.categories = Array.from(cats).sort();
  }

  applyFilters(): void {
    this.clearAlert();
    let temp = this.allProducts;

    // Filtro por categoría
    if (this.selectedCategory !== 'ALL') {
      temp = temp.filter(p => p.categoria === this.selectedCategory);
      if (temp.length === 0 && this.searchQuery.trim() === '') {
        this.showAlert(`No hay productos registrados en la categoría ${this.selectedCategory}`, 'info');
      }
    }

    // Filtro por nombre (búsqueda)
    const q = this.searchQuery.trim();
    if (q !== '') {
      // Validar longitud
      if (q.length < 2) {
        this.showAlert('Ingresa al menos 2 caracteres para realizar la búsqueda', 'warning');
        temp = [];
      } else if (/[<>'=";]/.test(q)) {
        // Validar inyección / caracteres especiales
        this.showAlert('Caracteres no permitidos en la búsqueda', 'error');
        temp = [];
      } else {
        const normalizedQuery = this.removeAccents(q).toLowerCase();
        const beforeSearchCount = temp.length;
        
        temp = temp.filter(p => this.removeAccents(p.nombre).toLowerCase().includes(normalizedQuery));
        
        if (temp.length === 0 && beforeSearchCount > 0) {
          this.showAlert(`No se encontraron productos con el nombre '${q}'`, 'info');
        }
      }
    }

    this.filteredProducts = temp;
  }

  onCategoryChange(): void {
    this.applyFilters();
  }

  onSearch(): void {
    this.applyFilters();
  }

  onClearSearch(): void {
    this.searchQuery = '';
    this.applyFilters();
  }

  crearPrimerProducto(): void {
    // Aquí iría la navegación a la vista de creación de producto
    // this.router.navigate(['/admin/productos/crear']);
    window.alert('Navegando a la creación de producto...');
  }

  private removeAccents(str: string): string {
    return str.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
  }

  private showAlert(message: string, type: 'warning' | 'info' | 'error'): void {
    this.alertMessage = message;
    this.alertType = type;
  }

  private clearAlert(): void {
    this.alertMessage = null;
    this.alertType = null;
  }
}
