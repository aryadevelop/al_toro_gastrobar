import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';
import { PreparacionAdminService } from '../../../../core/services/preparacion-admin.service';
import { BackendEstadoHistorial } from '../../../../core/models/api.models';

@Component({
  selector: 'app-preparacion-form-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Formulario de preparacion" subtitle="Crear o editar recetas base"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid form-compact" [formGroup]="form" (ngSubmit)="onSubmit()">
          <label><span>Nombre</span><input class="input-field" formControlName="name" /></label>
          <label><span>Minutos estimados</span><input class="input-field" type="number" min="1" formControlName="estimatedMinutes" /></label>
          <label><span>Ingredientes (coma separada)</span><textarea class="input-field" rows="3" formControlName="ingredients"></textarea></label>
          <button class="btn-primary" type="submit">Guardar preparacion</button>
          <p *ngIf="saved()" style="margin:0; color: var(--success);">Preparacion guardada (mock).</p>
        </form>
      </article>

      <!-- Historial de Estados (solo si es edición) -->
      <article class="card mt-4" style="padding: 1rem; max-width: 760px;" *ngIf="isEditMode">
        <h4 class="mb-3">Historial de Cambios de Estado</h4>
        <div *ngIf="isLoadingHistory" class="text-secondary">Cargando historial...</div>
        
        <div class="table-responsive" *ngIf="!isLoadingHistory && stateHistory.length > 0">
          <table class="table table-sm">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Usuario</th>
                <th>Estado Anterior</th>
                <th>Estado Nuevo</th>
                <th>Motivo</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let h of stateHistory">
                <td>{{ h.fechaHora | date:'short' }}</td>
                <td>{{ h.usuario }}</td>
                <td>{{ h.estadoAnterior }}</td>
                <td>{{ h.estadoNuevo }}</td>
                <td>{{ h.motivo || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <div *ngIf="!isLoadingHistory && stateHistory.length === 0" class="text-muted">
          No hay cambios de estado registrados.
        </div>
      </article>
    </section>
  `
})
export class PreparacionFormPageComponent implements OnInit {
  readonly saved = signal(false);
  isEditMode = false;
  preparacionId: string | null = null;
  stateHistory: BackendEstadoHistorial[] = [];
  isLoadingHistory = false;

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required]],
    estimatedMinutes: [15, [Validators.required, Validators.min(1)]],
    ingredients: ['', [Validators.required]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly preparacionAdminService: PreparacionAdminService
  ) {}

  ngOnInit(): void {
    this.preparacionId = this.route.snapshot.paramMap.get('id');
    if (this.preparacionId && this.preparacionId !== 'new') {
      this.isEditMode = true;
      this.cargarHistorial();
    }
  }

  cargarHistorial(): void {
    if (!this.preparacionId) return;
    this.isLoadingHistory = true;
    this.preparacionAdminService.obtenerHistorialEstados(this.preparacionId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.stateHistory = res.data;
        }
        this.isLoadingHistory = false;
      },
      error: () => {
        this.isLoadingHistory = false;
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saved.set(true);
  }
}
