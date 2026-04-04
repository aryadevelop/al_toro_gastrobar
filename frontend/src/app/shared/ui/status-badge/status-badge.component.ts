import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="badge" [class]="'badge ' + tone">{{ label }}</span>`,
  styles: [
    `
      .badge {
        display: inline-block;
        border-radius: 999px;
        padding: 0.2rem 0.65rem;
        font-size: 0.78rem;
        font-weight: 700;
      }

      .neutral {
        background: #f2f2f2;
        color: #0d0d0d;
      }

      .success {
        background: #a4a5a6;
        color: #0d0d0d;
      }

      .danger {
        background: #f23535;
        color: #f2f2f2;
      }
    `
  ]
})
export class StatusBadgeComponent {
  @Input() label = 'Estado';
  @Input() tone: 'neutral' | 'success' | 'danger' = 'neutral';
}