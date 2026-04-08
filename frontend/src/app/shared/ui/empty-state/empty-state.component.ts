import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <section class="card empty-state">
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
    </section>
  `,
  styles: [
    `
      .empty-state {
        text-align: center;
        padding: 1.2rem;
      }

      h3,
      p {
        margin: 0;
      }

      p {
        color: var(--muted);
      }
    `
  ]
})
export class EmptyStateComponent {
  @Input() title = 'Sin datos';
  @Input() description = 'No hay informacin para mostrar.';
}