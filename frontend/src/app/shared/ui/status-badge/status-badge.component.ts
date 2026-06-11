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
        background: #FFFFFF;
        color: #333333;
      }

      .success {
        background: #A0A0A0;
        color: #333333;
      }

      .danger {
        background: var(--primary);
        color: #FFFFFF;
      }
    `
  ]
})
export class StatusBadgeComponent {
  @Input() label = 'Estado';
  @Input() tone: 'neutral' | 'success' | 'danger' = 'neutral';
}

