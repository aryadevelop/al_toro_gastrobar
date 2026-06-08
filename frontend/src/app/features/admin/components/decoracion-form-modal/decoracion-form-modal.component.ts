import { Component, EventEmitter, Input, Output, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ModalBaseComponent } from '../../../../shared/ui/modal-base/modal-base.component';
import { DecoracionAdminService } from '../../../../core/services/decoracion-admin.service';
import { MesaMapService } from '../../../../core/services/mesa-map.service';
import { BackendDecoracionAdminResponse, BackendCrearDecoracionRequest, BackendActualizarDecoracionRequest } from '../../../../core/models/api.models';

@Component({
  selector: 'app-decoracion-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ModalBaseComponent],
  template: `
    <app-modal-base 
      [open]="open()" 
      [title]="decoracion() ? 'Editar Decoración' : 'Añadir Decoración'" 
      (close)="close.emit()">
      
      <form [formGroup]="form" (ngSubmit)="guardar()" class="form-container">
        
        <div class="form-group">
          <label for="nombre">Nombre de decoración *</label>
          <input type="text" id="nombre" formControlName="decoracionNombre" class="form-control" placeholder="Ej: Cumpleaños Básico">
          <span class="error" *ngIf="form.controls.decoracionNombre.touched && form.controls.decoracionNombre.invalid">
            El nombre es obligatorio y máximo 100 caracteres.
          </span>
        </div>

        <div class="form-group">
          <label for="costo">Costo Adicional</label>
          <input type="number" id="costo" formControlName="decoracionCostoAdicional" class="form-control" step="0.01">
          <span class="error" *ngIf="form.controls.decoracionCostoAdicional.touched && form.controls.decoracionCostoAdicional.invalid">
            Debe ser un valor válido mayor a 0.
          </span>
        </div>

        <div class="form-group">
          <label for="zonas">Zonas Disponibles</label>
          <select multiple id="zonas" formControlName="zonaIds" class="form-control" style="min-height: 100px;">
            <option *ngFor="let zona of zonasDisponibles()" [value]="zona.zonaId">
              {{ zona.zonaNombre }} (Cap: {{ zona.zonaCapacidadPersonas }})
            </option>
          </select>
          <small class="hint">Mantén presionado Ctrl (o Cmd) para seleccionar varias.</small>
        </div>

        <div class="form-group">
          <label for="imagen">Imagen (Opcional)</label>
          <input type="file" id="imagen" class="form-control" accept=".jpg, .png, .webp" (change)="onFileSelected($event)">
          
          <div class="preview-container" *ngIf="imagePreview() || decoracion()?.decoracionImagenUrl">
            <img [src]="imagePreview() || decoracion()?.decoracionImagenUrl" alt="Vista previa" class="preview-img">
          </div>
          
          <span class="error" *ngIf="fileError()">{{ fileError() }}</span>
          <small class="hint">Formatos permitidos: JPG, PNG, WEBP. Tamaño máximo: 5MB.</small>
        </div>

        <div class="actions">
          <button type="button" class="btn-secondary" (click)="close.emit()">Cancelar</button>
          <button type="button" class="btn-danger" *ngIf="decoracion()?.decoracionImagenUrl" (click)="eliminarImagen()">Eliminar Imagen Actual</button>
          <button type="submit" class="btn-primary" [disabled]="form.invalid || isSubmitting() || !!fileError()">
            {{ isSubmitting() ? 'Guardando...' : 'Guardar cambios' }}
          </button>
        </div>

        <div class="success-message" *ngIf="successMessage()">
          {{ successMessage() }}
        </div>
        <div class="error" *ngIf="serverError()">
          {{ serverError() }}
        </div>
      </form>
    </app-modal-base>
  `,
  styles: [`
    .form-container {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .form-control {
      padding: 0.5rem;
      border: 1px solid var(--border-color);
      border-radius: var(--radius);
      background: var(--surface-color);
      color: var(--text-color);
    }
    .error {
      color: var(--error-color);
      font-size: 0.85rem;
    }
    .hint {
      color: var(--text-muted);
      font-size: 0.8rem;
    }
    .preview-container {
      margin-top: 0.5rem;
      border: 1px dashed var(--border-color);
      padding: 0.5rem;
      border-radius: var(--radius);
      display: flex;
      justify-content: center;
      background: var(--background-color);
    }
    .preview-img {
      max-width: 100%;
      max-height: 200px;
      object-fit: contain;
    }
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
      margin-top: 1rem;
    }
    .success-message {
      color: var(--success-color);
      font-weight: 500;
      text-align: center;
      margin-top: 0.5rem;
    }
  `]
})
export class DecoracionFormModalComponent {
  private readonly fb = inject(FormBuilder);
  private readonly decoracionService = inject(DecoracionAdminService);
  private readonly mesaService = inject(MesaMapService);

  @Input({ required: true }) set isOpen(val: boolean) {
    this.open.set(val);
    if (val) {
      this.cargarZonas();
    } else {
      this.resetForm();
    }
  }
  
  @Input() set decoracionToEdit(val: BackendDecoracionAdminResponse | null) {
    this.decoracion.set(val);
    if (val) {
      this.form.patchValue({
        decoracionNombre: val.decoracionNombre,
        decoracionCostoAdicional: val.decoracionCostoAdicional,
        zonaIds: val.zonaIds || []
      });
    }
  }

  @Output() readonly close = new EventEmitter<void>();
  @Output() readonly saved = new EventEmitter<void>();

  readonly open = signal(false);
  readonly decoracion = signal<BackendDecoracionAdminResponse | null>(null);
  
  readonly form = this.fb.nonNullable.group({
    decoracionNombre: ['', [Validators.required, Validators.maxLength(100)]],
    decoracionCostoAdicional: [null as number | null, [Validators.min(0)]],
    zonaIds: [[] as number[]]
  });

  readonly zonasDisponibles = signal<any[]>([]);
  readonly selectedFile = signal<File | null>(null);
  readonly imagePreview = signal<string | null>(null);
  readonly fileError = signal<string | null>(null);
  readonly isSubmitting = signal(false);
  readonly successMessage = signal<string | null>(null);
  readonly serverError = signal<string | null>(null);

  private cargarZonas(): void {
    this.mesaService.getZonasDisponibles().subscribe({
      next: (zonas) => {
        this.zonasDisponibles.set(zonas);
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) {
      this.clearFile();
      return;
    }

    const file = input.files[0];
    this.fileError.set(null);

    // Validar extensión
    const validExtensions = ['image/jpeg', 'image/png', 'image/webp'];
    if (!validExtensions.includes(file.type)) {
      this.fileError.set('Formato no soportado. Por favor selecciona JPG, PNG o WEBP');
      this.clearFile();
      return;
    }

    // Validar tamaño (5MB)
    const maxSize = 5 * 1024 * 1024;
    if (file.size > maxSize) {
      this.fileError.set('La imagen supera el tamaño máximo permitido (5MB). Por favor comprime la imagen o selecciona otra');
      this.clearFile();
      return;
    }

    this.selectedFile.set(file);

    // Vista previa
    const reader = new FileReader();
    reader.onload = () => {
      this.imagePreview.set(reader.result as string);
    };
    reader.readAsDataURL(file);
  }

  private clearFile(): void {
    this.selectedFile.set(null);
    this.imagePreview.set(null);
  }

  private resetForm(): void {
    this.form.reset({
      decoracionNombre: '',
      decoracionCostoAdicional: null,
      zonaIds: []
    });
    this.clearFile();
    this.decoracion.set(null);
    this.fileError.set(null);
    this.successMessage.set(null);
    this.serverError.set(null);
    this.isSubmitting.set(false);
  }

  eliminarImagen(): void {
    const dec = this.decoracion();
    if (!dec || !dec.decoracionId) return;

    if (confirm('¿Está seguro de que desea eliminar la imagen de esta decoración?')) {
      this.isSubmitting.set(true);
      this.decoracionService.eliminarImagenDecoracion(dec.decoracionId).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.decoracion.update(d => d ? { ...d, decoracionImagenUrl: null } : null);
          this.saved.emit();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.serverError.set(err.error?.message || 'Error al eliminar la imagen');
        }
      });
    }
  }

  guardar(): void {
    if (this.form.invalid || this.fileError()) return;

    this.isSubmitting.set(true);
    this.serverError.set(null);
    this.successMessage.set(null);

    const values = this.form.getRawValue();
    const dec = this.decoracion();

    const request$ = dec 
      ? this.decoracionService.actualizarDecoracion(dec.decoracionId, values as BackendActualizarDecoracionRequest)
      : this.decoracionService.crearDecoracion(values as BackendCrearDecoracionRequest);

    request$.subscribe({
      next: (res) => {
        const savedDec = res.data;
        if (!savedDec) {
          this.finalizarGuardado();
          return;
        }

        const file = this.selectedFile();
        if (file) {
          // Subir imagen
          this.decoracionService.subirImagenDecoracion(savedDec.decoracionId, file).subscribe({
            next: () => this.finalizarGuardado(),
            error: (err) => {
              this.isSubmitting.set(false);
              this.serverError.set('Decoración guardada, pero falló la subida de imagen: ' + (err.error?.message || 'Error desconocido'));
            }
          });
        } else {
          this.finalizarGuardado();
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.serverError.set(err.error?.message || 'Error al guardar la decoración');
      }
    });
  }

  private finalizarGuardado(): void {
    this.isSubmitting.set(false);
    this.successMessage.set('Decoración guardada correctamente');
    this.saved.emit();
    setTimeout(() => {
      this.close.emit();
    }, 1500);
  }
}
