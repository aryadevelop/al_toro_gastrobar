import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="overlay" *ngIf="open">
      <div class="card dialog">
        <h3>{{ title }}</h3>
        <p>{{ message }}</p>
        <div class="actions">
          <button type="button" class="btn-secondary" (click)="cancel.emit()">Cancelar</button>
          <button type="button" class="btn-danger" (click)="confirm.emit()">Confirmar</button>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      .overlay {
        position: fixed;
        inset: 0;
        display: grid;
        place-items: center;
        background: rgba(24, 29, 27, 0.35);
      }

      .dialog {
        max-width: 420px;
        padding: 1rem;
      }

      .actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.7rem;
      }
    `
  ]
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input() title = 'Confirmacin';
  @Input() message = 'Deseas continuar?';
  @Output() readonly confirm = new EventEmitter<void>();
  @Output() readonly cancel = new EventEmitter<void>();
}