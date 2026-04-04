import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="card stat-card">
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

      p,
      h3,
      small {
        margin: 0;
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
}