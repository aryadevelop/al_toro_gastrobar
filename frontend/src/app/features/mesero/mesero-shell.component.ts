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
        color: inherit;
        padding: 0.65rem 1rem;
        border-radius: 999px;
        background: rgba(0,0,0,0.05);
      }

      .tab-link.active-tab {
        background: #8b5e3c;
        color: #fff;
      }
    `,
  ],
})
export class MeseroShellComponent {}