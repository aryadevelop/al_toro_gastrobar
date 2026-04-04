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
        background: #ece8df;
      }

      .success {
        background: #d7f0e1;
        color: #0b6033;
      }

      .danger {
        background: #f5d8d8;
        color: #8d1c1c;
      }
    `
  ]
})
export class StatusBadgeComponent {
  @Input() label = 'Estado';
  @Input() tone: 'neutral' | 'success' | 'danger' = 'neutral';
}