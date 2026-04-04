import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-mobile-header',
  standalone: true,
  template: `
    <header class="mobile-header card">
      <h2>Al Toro</h2>
      <button type="button" class="btn-secondary" (click)="menuToggle.emit()">Men</button>
    </header>
  `,
  styles: [
    `
      .mobile-header {
        display: none;
        justify-content: space-between;
        align-items: center;
        padding: 0.8rem;
        margin-bottom: 1rem;
      }

      h2 {
        margin: 0;
      }

      @media (max-width: 960px) {
        .mobile-header {
          display: flex;
        }
      }
    `
  ]
})
export class MobileHeaderComponent {
  @Output() readonly menuToggle = new EventEmitter<void>();
}