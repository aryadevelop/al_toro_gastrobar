import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InsumoAdminService } from '../../../../core/services/insumo-admin.service';
import { BackendValidacionDescontinuarInsumo, BackendCambiarEstadoRequest } from '../../../../core/models/api.models';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { ModalBaseComponent } from '../../../../shared/ui/modal-base/modal-base.component';

@Component({
  selector: 'app-insumos-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, ModalBaseComponent],
  templateUrl: './insumos-list-page.component.html',
  styleUrls: ['./insumos-list-page.component.scss']
})
export class InsumosListPageComponent implements OnInit {
  insumos: any[] = [];
  isLoading = true;

  // Modal State
  isModalOpen = false;
  selectedInsumo: any | null = null;
  isValidatingState = false;
  validationData: BackendValidacionDescontinuarInsumo | null = null;
  
  // Helpers
  tienePedidosPendientes = false;
  tieneOtrasDescontinuadas = false;

  constructor(private readonly insumoAdminService: InsumoAdminService) {}

  ngOnInit(): void {
    this.cargarInsumos();
  }

  cargarInsumos(): void {
    this.isLoading = true;
    this.insumoAdminService.listarInsumos().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.insumos = res.data;
        }
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  abrirModalEstado(insumo: any) {
    if (insumo.estado === 'ACTIVE' && this.selectedInsumo?.estado === 'ACTIVE' && insumo.id === this.selectedInsumo.id) {
      // BDD: Validar reactivar - insumo ya activo
      window.alert('Este insumo ya está activo');
      return;
    }

    this.selectedInsumo = insumo;
    this.validationData = null;
    this.tienePedidosPendientes = false;
    this.tieneOtrasDescontinuadas = false;
    this.isModalOpen = true;

    this.isValidatingState = true;
    this.insumoAdminService.validarCambioEstado(insumo.id).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.validationData = res.data;
          // Check for warnings
          this.tienePedidosPendientes = this.validationData.preparacionesAfectadas.some(p => p.pedidosPendientes > 0);
          this.tieneOtrasDescontinuadas = this.validationData.preparacionesAfectadas.some(p => p.otrosInsumosDescontinuados);
        }
        this.isValidatingState = false;
      },
      error: () => {
        this.isValidatingState = false;
      }
    });
  }

  cerrarModal() {
    this.isModalOpen = false;
    this.selectedInsumo = null;
  }

  confirmarAccion(accionPreparacionesAfectadas: 'DESACTIVAR' | 'MANTENER' | 'REACTIVAR' | 'MANTENER_INACTIVAS') {
    if (!this.selectedInsumo) return;
    const nuevoEstado = this.selectedInsumo.estado === 'ACTIVE' ? 'DISCONTINUED' : 'ACTIVE';
    
    const req: BackendCambiarEstadoRequest = {
      nuevoEstado,
      accionPreparacionesAfectadas
    };

    this.insumoAdminService.cambiarEstado(this.selectedInsumo.id, req).subscribe({
      next: () => {
        if (nuevoEstado === 'DISCONTINUED') {
          if (accionPreparacionesAfectadas === 'DESACTIVAR') {
            window.alert(`Se desactivaron las preparaciones que dependían de este insumo.`);
          } else {
            window.alert('El insumo ha sido descontinuado. Las preparaciones que lo usan han sido marcadas para revisión.');
          }
        } else {
          if (accionPreparacionesAfectadas === 'REACTIVAR') {
            window.alert('Se reactivaron las preparaciones que dependían de este insumo.');
          } else {
            window.alert('El insumo ha sido reactivado. Las preparaciones permanecen inactivas. Puedes reactivarlas manualmente desde el módulo de preparaciones.');
          }
        }
        this.cerrarModal();
        this.cargarInsumos();
      },
      error: () => {
        window.alert('Error al cambiar el estado del insumo.');
      }
    });
  }
}
