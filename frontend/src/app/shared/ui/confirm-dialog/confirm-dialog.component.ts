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
          <button type="button" [ngClass]="cancelBtnClass" (click)="cancel.emit()">{{ cancelLabel }}</button>
          <button type="button" [ngClass]="confirmBtnClass" (click)="confirm.emit()">{{ confirmLabel }}</button>
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
        background: rgba(24, 29, 27, 0.7);
        z-index: 9999;
      }

      .dialog {
        max-width: 420px;
        padding: 1.5rem;
        background: var(--bg) !important;
        color: #1a1a1a !important;
        border: 2px solid var(--surface) !important;
        border-radius: 14px;
      }

      .dialog h3 {
        margin-top: 0;
        color: var(--surface);
      }

      .dialog .actions button {
        background: var(--surface) !important;
        color: #ffffff !important;
        border: none !important;
      }

      .dialog .actions button:hover {
        background: #8a0d0d !important;
        box-shadow: 0 4px 12px rgba(181, 18, 18, 0.4);
      }

      .actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.7rem;
        margin-top: 1rem;
      }
    `
  ]
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input() title = 'Confirmación';
  @Input() message = '¿Deseas continuar?';
  @Input() cancelLabel = 'Cancelar';
  @Input() confirmLabel = 'Confirmar';
  @Input() cancelBtnClass = 'btn-secondary';
  @Input() confirmBtnClass = 'btn-danger';
  @Output() readonly confirm = new EventEmitter<void>();
  @Output() readonly cancel = new EventEmitter<void>();
}
