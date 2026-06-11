import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-mobile-header',
  standalone: true,
  template: `
    <header class="mobile-header card">
      <img src="assets/images/al-toro-logo-vector.svg" alt="Al Toro Gastrobar" class="mobile-logo" />
      <button type="button" class="btn-secondary" (click)="menuToggle.emit()">Menu</button>
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
        border-bottom: 2px solid rgba(211, 47, 47, 0.26);
        background-image: linear-gradient(90deg, rgba(211, 47, 47, 0.07) 0 3px, transparent 3px);
      }

      .mobile-logo {
        display: block;
        width: min(146px, 56vw);
        height: auto;
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

