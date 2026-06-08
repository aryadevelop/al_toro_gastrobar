import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PreparacionAdminService } from '../../../../core/services/preparacion-admin.service';
import { BackendValidacionCambioEstado, BackendCambiarEstadoRequest } from '../../../../core/models/api.models';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { ModalBaseComponent } from '../../../../shared/ui/modal-base/modal-base.component';

@Component({
  selector: 'app-preparaciones-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, ModalBaseComponent],
  templateUrl: './preparaciones-list-page.component.html',
  styleUrls: ['./preparaciones-list-page.component.scss']
})
export class PreparacionesListPageComponent implements OnInit {
  preparaciones: any[] = [];
  isLoading = true;

  // Estado Modal Cambio de Estado
  isModalOpen = false;
  selectedPrep: any | null = null;
  stateChangeReason = '';
  isValidatingState = false;
  validationData: BackendValidacionCambioEstado | null = null;

  constructor(private readonly preparacionAdminService: PreparacionAdminService) {}

  ngOnInit(): void {
    this.cargarPreparaciones();
  }

  cargarPreparaciones(): void {
    this.isLoading = true;
    this.preparacionAdminService.listarPreparaciones().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.preparaciones = res.data;
        }
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  abrirModalEstado(prep: any) {
    this.selectedPrep = prep;
    this.stateChangeReason = '';
    this.validationData = null;
    this.isModalOpen = true;

    // Solo validamos reservas si vamos a Desactivar (pasar a INACTIVE)
    if (prep.estado === 'ACTIVE') {
      this.isValidatingState = true;
      this.preparacionAdminService.validarCambioEstado(prep.id).subscribe({
        next: (res) => {
          if (res.success) {
            this.validationData = res.data;
          }
          this.isValidatingState = false;
        },
        error: () => {
          this.isValidatingState = false;
        }
      });
    }
  }

  cerrarModal() {
    this.isModalOpen = false;
    this.selectedPrep = null;
  }

  confirmarCambioEstado(notificarClientes?: boolean) {
    if (!this.selectedPrep) return;
    const nuevoEstado = this.selectedPrep.estado === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    
    const req: BackendCambiarEstadoRequest = {
      nuevoEstado,
      motivo: this.stateChangeReason || undefined,
      notificarClientes
    };

    this.preparacionAdminService.cambiarEstado(this.selectedPrep.id, req).subscribe({
      next: () => {
        if (nuevoEstado === 'INACTIVE') {
          window.alert('Preparación desactivada correctamente.');
        } else {
          window.alert('Preparación activada correctamente.');
        }
        this.cerrarModal();
        this.cargarPreparaciones();
      },
      error: () => {
        window.alert('Error al cambiar el estado de la preparación.');
      }
    });
  }
}
