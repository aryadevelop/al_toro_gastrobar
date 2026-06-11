import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-mesero-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <section class="page-grid">
      <nav class="card" style="padding:0.75rem 1rem; display:flex; gap:0.75rem; flex-wrap:wrap; align-items:center;">
        <a routerLink="reservas" routerLinkActive="active-tab" [routerLinkActiveOptions]="{ exact: true }" class="tab-link">Reservas</a>
        <a routerLink="mesas" routerLinkActive="active-tab" [routerLinkActiveOptions]="{ exact: true }" class="tab-link">Mesas</a>
        <a routerLink="servicio" routerLinkActive="active-tab" [routerLinkActiveOptions]="{ exact: true }" class="tab-link">Servicio</a>
      </nav>

      <router-outlet></router-outlet>
    </section>
  `,
  styles: [
    `
      .tab-link {
        text-decoration: none;
        color: #ffffff;
        padding: 0.65rem 1rem;
        border-radius: 999px;
        background: transparent;
        border: 1px solid rgba(255, 255, 255, 0.4);
        transition: all 0.2s ease;
      }

      .tab-link:hover {
        background: rgba(255, 255, 255, 0.1);
      }

      .tab-link.active-tab {
        background: var(--bg);
        border-color: var(--bg);
        color: var(--primary);
      }
    `,
  ],
})
export class MeseroShellComponent {}