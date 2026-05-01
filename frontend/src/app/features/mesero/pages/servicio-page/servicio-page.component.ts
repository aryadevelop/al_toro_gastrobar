import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-servicio-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Servicio" subtitle="Accesos rápidos para atención en sala"></app-page-header>

      <section style="display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:1rem;">
        <article class="card" style="padding:1rem;">
          <h3>Marcar llegada</h3>
          <p>Accede al detalle de una reserva y marca la llegada del cliente.</p>
          <a routerLink="/app/mesero/reservas" class="button-link">Ir a reservas</a>
        </article>

        <article class="card" style="padding:1rem;">
          <h3>Comandas</h3>
          <p>Gestiona pre-orden, comanda y preparación desde el flujo de servicio.</p>
          <a routerLink="/app/mesero/comanda-editor" class="button-link">Abrir editor</a>
        </article>

        <article class="card" style="padding:1rem;">
          <h3>Notificaciones</h3>
          <p>Consulta y atiende solicitudes operativas del turno.</p>
          <a routerLink="/app/mesero/notificaciones" class="button-link">Ver notificaciones</a>
        </article>
      </section>
    </section>
  `,
})
export class ServicioMeseroPageComponent {}