import { Component, EventEmitter, Input, Output, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ModalBaseComponent } from '../../../../shared/ui/modal-base/modal-base.component';
import { DecoracionAdminService } from '../../../../core/services/decoracion-admin.service';
import { MesaMapService } from '../../../../core/services/mesa-map.service';
import { BackendDecoracionAdminResponse, BackendCrearDecoracionRequest, BackendActualizarDecoracionRequest } from '../../../../core/models/api.models';
import { environment } from '../../../../../environments/environment';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-decoracion-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ModalBaseComponent, ConfirmDialogComponent],
  template: `
    <app-modal-base 
      [open]="open()" 
      [title]="decoracion() ? 'Editar Decoración' : 'Añadir Decoración'" 
      (close)="close.emit()">
      
      <form [formGroup]="form" (ngSubmit)="guardar()" class="form-container">
        
        <div class="form-group">
          <label for="nombre" class="form-label">Nombre de decoración <span class="text-danger">*</span></label>
          <input type="text" id="nombre" formControlName="decoracionNombre" class="form-control minimal-input" placeholder="Ej: Cumpleaños Básico">
          <span class="error" *ngIf="form.controls.decoracionNombre.touched && form.controls.decoracionNombre.invalid">
            Obligatorio (máx 100 caracteres).
          </span>
        </div>

        <div class="form-group">
          <label for="costo" class="form-label">Costo Adicional</label>
          <div class="input-group">
            <span class="input-prefix">$</span>
            <input type="number" id="costo" formControlName="decoracionCostoAdicional" class="form-control minimal-input with-prefix" placeholder="0.00" step="1000">
          </div>
          <span class="error" *ngIf="form.controls.decoracionCostoAdicional.touched && form.controls.decoracionCostoAdicional.invalid">
            Valor inválido.
          </span>
        </div>

        <div class="form-group">
          <label class="form-label">Zonas Disponibles</label>
          <div class="zonas-grid">
            <label class="zona-checkbox" *ngFor="let zona of zonasDisponibles()">
              <input type="checkbox" 
                     [checked]="hasZona(zona.id)"
                     (change)="toggleZona(zona.id, $event)">
              <span class="zona-name">{{ zona.name }}</span>
              <span class="zona-cap">(Cap: {{ zona.capacidadTotal }})</span>
            </label>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Imagen (Opcional)</label>
          
          <div class="file-upload-wrapper" [class.has-image]="imagePreview() || decoracion()?.decoracionImagenUrl">
            <input type="file" id="imagen" class="file-input" accept=".jpg, .png, .webp" (change)="onFileSelected($event)">
            <label for="imagen" class="file-label">
              <span *ngIf="!(imagePreview() || decoracion()?.decoracionImagenUrl)">Haz clic para subir una foto</span>
              <span *ngIf="imagePreview() || decoracion()?.decoracionImagenUrl">Cambiar imagen</span>
            </label>
          </div>
          
          <div class="preview-container" *ngIf="imagePreview() || decoracion()?.decoracionImagenUrl">
            <img [src]="imagePreview() || getImageUrl(decoracion()?.decoracionImagenUrl)" alt="Vista previa" class="preview-img">
            <button type="button" class="btn-remove-img" (click)="eliminarImagenOpcional()">Eliminar</button>
          </div>
          
          <span class="error" *ngIf="fileError()">{{ fileError() }}</span>
        </div>

        <div class="actions">
          <button type="button" class="btn btn-outline-secondary minimal-btn" (click)="close.emit()">Cancelar</button>
          <button type="submit" class="btn btn-primary minimal-btn" [disabled]="form.invalid || isSubmitting() || !!fileError()">
            {{ isSubmitting() ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>

        <div class="success-message" *ngIf="successMessage()">{{ successMessage() }}</div>
        <div class="error-message" *ngIf="serverError()">{{ serverError() }}</div>
      </form>
    </app-modal-base>

    <app-confirm-dialog
      [open]="dialogAbierto()"
      [title]="dialogTitulo()"
      [message]="dialogMensaje()"
      (cancel)="dialogAbierto.set(false)"
      (confirm)="ejecutarAccionConfirmada()">
    </app-confirm-dialog>
  `,
  styles: [`
    .form-container {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      padding: 0;
    }
    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }
    .form-label {
      font-weight: 500;
      font-size: 0.85rem;
      color: var(--text-color, #333);
      margin-bottom: 0.1rem;
    }
    .text-danger { color: #dc3545; }
    
    .minimal-input {
      border: 1px solid rgba(0,0,0,0.15);
      border-radius: 6px;
      padding: 0.3rem 0.5rem;
      background: #ffffff;
      font-size: 0.9rem;
      transition: all 0.2s;
      box-shadow: inset 0 1px 2px rgba(0,0,0,0.03);
    }
    .minimal-input:focus {
      outline: none;
      border-color: #9e7f66;
      box-shadow: 0 0 0 3px rgba(158, 127, 102, 0.2);
    }

    .input-group {
      position: relative;
      display: flex;
      align-items: center;
    }
    .input-prefix {
      position: absolute;
      left: 1rem;
      color: #666;
      font-weight: 500;
      pointer-events: none;
    }
    .with-prefix {
      padding-left: 2rem;
      width: 100%;
    }

    .zonas-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
      gap: 0.3rem;
      margin-top: 0;
    }
    .zona-checkbox {
      display: flex;
      align-items: center;
      gap: 0.3rem;
      background: #ffffff;
      border: 1px solid rgba(0,0,0,0.1);
      padding: 0.3rem 0.4rem;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .zona-checkbox:hover {
      background: #fdfbf9;
      border-color: #d4c4b7;
    }
    .zona-checkbox input[type="checkbox"] {
      width: 1.1rem;
      height: 1.1rem;
      accent-color: #8c6a51;
    }
    .zona-name {
      font-size: 0.85rem;
      font-weight: 500;
    }
    .zona-cap {
      font-size: 0.7rem;
      color: #777;
    }

    .file-upload-wrapper {
      position: relative;
      overflow: hidden;
      display: inline-block;
      width: 100%;
    }
    .file-input {
      position: absolute;
      left: 0;
      top: 0;
      opacity: 0;
      width: 100%;
      height: 100%;
      cursor: pointer;
    }
    .file-label {
      display: block;
      padding: 0.4rem;
      border: 1px dashed rgba(0,0,0,0.2);
      border-radius: 6px;
      text-align: center;
      background: #faf8f5;
      color: #666;
      font-size: 0.8rem;
      transition: all 0.2s;
      margin-top: 0;
    }
    .file-upload-wrapper:hover .file-label {
      background: #f3eee8;
      border-color: #9e7f66;
    }
    .has-image .file-label {
      display: none;
    }

    .preview-container {
      position: relative;
      margin-top: 0.2rem;
      border-radius: 6px;
      overflow: hidden;
      background: #000;
      display: flex;
      justify-content: center;
      align-items: center;
      height: 90px;
    }
    .preview-img {
      max-width: 100%;
      max-height: 100%;
      object-fit: cover;
      opacity: 0.9;
    }
    .btn-remove-img {
      position: absolute;
      top: 0.5rem;
      right: 0.5rem;
      background: rgba(220, 53, 69, 0.9);
      color: white;
      border: none;
      padding: 0.3rem 0.6rem;
      border-radius: 4px;
      font-size: 0.8rem;
      cursor: pointer;
    }

    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      margin-top: 0;
      padding-top: 0.3rem;
      border-top: 1px solid rgba(0,0,0,0.05);
    }
    .minimal-btn {
      padding: 0.4rem 1.2rem;
      border-radius: 6px;
      font-weight: 500;
      transition: all 0.2s;
      border: none;
    }
    .btn-outline-secondary {
      background: transparent;
      border: 1px solid rgba(0,0,0,0.2);
      color: #555;
    }
    .btn-outline-secondary:hover {
      background: #f5f5f5;
    }
    .btn-primary {
      background: #8c6a51;
      color: white;
    }
    .btn-primary:hover {
      background: #73553f;
    }
    .btn-primary:disabled {
      background: #ccc;
      cursor: not-allowed;
    }

    .error, .error-message {
      color: #dc3545;
      font-size: 0.8rem;
      margin-top: 0.2rem;
    }
    .success-message {
      color: #28a745;
      font-weight: 500;
      text-align: center;
      margin-top: 0.5rem;
      font-size: 0.9rem;
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

  readonly dialogAbierto = signal(false);
  readonly dialogTitulo = signal('');
  readonly dialogMensaje = signal('');
  private accionConfirmacion: (() => void) | null = null;

  private cargarZonas(): void {
    this.mesaService.getZonasDisponibles().subscribe({
      next: (zonas) => {
        this.zonasDisponibles.set(zonas);
      }
    });
  }

  getImageUrl(path: string | undefined | null): string {
    if (!path) return '';
    if (path.startsWith('http')) return path;
    const base = environment.apiBaseUrl.replace(/\/api\/?$/, '');
    const cleanPath = path.startsWith('/') ? path : '/' + path;
    return `${base}${cleanPath}`;
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

    this.dialogTitulo.set('Eliminar Imagen');
    this.dialogMensaje.set('¿Está seguro de que desea eliminar la imagen de esta decoración?');
    this.accionConfirmacion = () => {
      this.isSubmitting.set(true);
      this.decoracionService.eliminarImagenDecoracion(dec.decoracionId).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.decoracion.update(d => d ? { ...d, decoracionImagenUrl: null } : null);
          this.saved.emit();
          this.dialogAbierto.set(false);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.serverError.set(err.error?.message || 'Error al eliminar la imagen');
          this.dialogAbierto.set(false);
        }
      });
    };
    this.dialogAbierto.set(true);
  }

  ejecutarAccionConfirmada(): void {
    if (this.accionConfirmacion) {
      this.accionConfirmacion();
    }
  }

  eliminarImagenOpcional(): void {
    if (this.selectedFile()) {
      this.clearFile();
    } else if (this.decoracion()?.decoracionImagenUrl) {
      this.eliminarImagen();
    }
  }

  hasZona(id: string): boolean {
    const ids = this.form.controls.zonaIds.value || [];
    return ids.includes(Number(id));
  }

  toggleZona(idStr: string, event: Event): void {
    const id = Number(idStr);
    const checked = (event.target as HTMLInputElement).checked;
    const current = this.form.controls.zonaIds.value || [];
    
    if (checked) {
      if (!current.includes(id)) {
        this.form.controls.zonaIds.setValue([...current, id]);
      }
    } else {
      this.form.controls.zonaIds.setValue(current.filter(x => x !== id));
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
