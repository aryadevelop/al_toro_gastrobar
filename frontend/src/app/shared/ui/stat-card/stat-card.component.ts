import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="card stat-card" [class.stat-card--compact]="compact">
      <p>{{ label }}</p>
      <h3>{{ value }}</h3>
      <small *ngIf="trend !== null">Tendencia: {{ trend }}%</small>
    </article>
  `,
  styles: [
    `
      .stat-card {
        padding: 1rem;
      }

      .stat-card--compact {
        padding: 0.62rem 0.72rem;
      }

      p,
      h3,
      small {
        margin: 0;
      }

      .stat-card--compact p {
        font-size: 0.84rem;
      }

      .stat-card--compact h3 {
        font-size: 1.28rem;
        line-height: 1.05;
      }

      .stat-card--compact small {
        font-size: 0.74rem;
      }

      p,
      small {
        color: var(--muted);
      }
    `
  ]
})
export class StatCardComponent {
  @Input() label = '';
  @Input() value: number | string = 0;
  @Input() trend: number | null = null;
  @Input() compact = false;
}