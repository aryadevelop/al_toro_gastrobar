import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-modal-base',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="modal-backdrop" *ngIf="open" (click)="close.emit()">
      <div class="modal card" (click)="$event.stopPropagation()">
        <header>
          <h3>{{ title }}</h3>
          <button type="button" class="btn-secondary" (click)="close.emit()">X</button>
        </header>
        <ng-content></ng-content>
      </div>
    </section>
  `,
  styles: [
    `
      .modal-backdrop {
        position: fixed;
        inset: 0;
        background: rgba(10, 10, 10, 0.4);
        display: grid;
        place-items: center;
      }

      .modal {
        width: min(600px, 92vw);
        padding: 1rem;
      }

      header {
        display: flex;
        justify-content: space-between;
      }
    `
  ]
})
export class ModalBaseComponent {
  @Input() open = false;
  @Input() title = '';
  @Output() readonly close = new EventEmitter<void>();
}