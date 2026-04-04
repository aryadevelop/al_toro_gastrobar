import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  standalone: true,
  template: `
    <header class="page-header">
      <h1>{{ title }}</h1>
      <p>{{ subtitle }}</p>
      <ng-content></ng-content>
    </header>
  `,
  styles: [
    `
      .page-header {
        display: grid;
        gap: 0.3rem;
      }

      h1 {
        margin: 0;
      }

      p {
        margin: 0;
        color: var(--muted);
      }
    `
  ]
})
export class PageHeaderComponent {
  @Input({ required: true }) title = '';
  @Input() subtitle = '';
}