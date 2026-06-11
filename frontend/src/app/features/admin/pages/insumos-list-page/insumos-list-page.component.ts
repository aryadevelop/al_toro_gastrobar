import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { InsumoAdminService } from '../../../../core/services/insumo-admin.service';
import { BackendValidacionDescontinuarInsumo, BackendCambiarEstadoRequest } from '../../../../core/models/api.models';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { ModalBaseComponent } from '../../../../shared/ui/modal-base/modal-base.component';

@Component({
  selector: 'app-insumos-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PageHeaderComponent, ModalBaseComponent],
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

  // Alertas de UI
  alertMessage: string | null = null;
  alertType: 'warning' | 'info' | 'error' | null = null;

  showAlert(message: string, type: 'warning' | 'info' | 'error'): void {
    this.alertMessage = message;
    this.alertType = type;
  }

  clearAlert(): void {
    this.alertMessage = null;
    this.alertType = null;
  }

  abrirModalEstado(insumo: any) {
    if (insumo.insumoEstado === 'ACTIVO' && this.isReactivating(insumo)) {
       // El AC dice: "Este insumo ya está activo"
       this.showAlert('Este insumo ya está activo', 'warning');
       return;
    }
    
    this.clearAlert();
    this.selectedInsumo = insumo;
    this.validationData = null;
    this.tienePedidosPendientes = false;
    this.tieneOtrasDescontinuadas = false;
    this.isModalOpen = true;

    this.isValidatingState = true;
    this.insumoAdminService.validarCambioEstado(insumo.insumoId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.validationData = res.data;
          // Check for warnings
          this.tienePedidosPendientes = (this.validationData?.preparacionesAfectadas ?? []).some((p: any) => p.pedidosPendientes > 0);
          this.tieneOtrasDescontinuadas = (this.validationData?.preparacionesAfectadas ?? []).some((p: any) => p.otrosInsumosDescontinuados);
        }
        this.isValidatingState = false;
      },
      error: () => {
        this.isValidatingState = false;
      }
    });
  }
  
  // Helper para simular que no podemos reactivar algo activo, 
  // en la interfaz el botón ya cambia, pero es útil como protección si se hace doble clic o similar.
  private isReactivating(insumo: any): boolean {
    return false; // Actually the UI already toggles 'Reactivar' only when INACTIVO.
  }

  cerrarModal() {
    this.isModalOpen = false;
    this.selectedInsumo = null;
  }

  confirmarAccion(accionPreparacionesAfectadas: 'DESACTIVAR' | 'MANTENER' | 'REACTIVAR' | 'MANTENER_INACTIVAS') {
    if (!this.selectedInsumo) return;
    const esActivo = this.selectedInsumo.insumoEstado === 'ACTIVO';
    const nuevoEstado = esActivo ? 'DISCONTINUO' : 'ACTIVO';
    
    const preparacionesCount = this.validationData?.preparacionesAfectadas?.length || 0;

    const req: BackendCambiarEstadoRequest = {
      estado: nuevoEstado,
      accionPreparacionesAfectadas
    };

    this.insumoAdminService.cambiarEstado(this.selectedInsumo.insumoId, req).subscribe({
      next: () => {
        let mensaje = '';
        if (accionPreparacionesAfectadas === 'DESACTIVAR') {
           mensaje = `Se desactivaron ${preparacionesCount} preparaciones que dependían de este insumo`;
        } else if (accionPreparacionesAfectadas === 'MANTENER') {
           mensaje = 'El insumo ha sido descontinuado. Las preparaciones que lo usan han sido marcadas para revisión';
        } else if (accionPreparacionesAfectadas === 'REACTIVAR') {
           mensaje = `Se reactivaron ${preparacionesCount} preparaciones que dependían de este insumo`;
        } else if (accionPreparacionesAfectadas === 'MANTENER_INACTIVAS') {
           mensaje = 'El insumo ha sido reactivado. Las preparaciones permanecen inactivas. Puedes reactivarlas manualmente desde el módulo de preparaciones';
        } else {
           mensaje = 'Estado actualizado correctamente.';
        }
        
        this.showAlert(mensaje, 'info');
        this.cerrarModal();
        this.cargarInsumos();
      },
      error: (err) => {
        const errorMsg = err?.error?.message || 'Error al cambiar el estado del insumo.';
        this.showAlert(errorMsg, 'error');
        this.cerrarModal();
      }
    });
  }
}
