import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="not-found card">
      <p class="code">404</p>
      <h1>Pgina no encontrada</h1>
      <p>La ruta solicitada no existe o fue movida.</p>
      <a routerLink="/app/dashboard" class="btn-primary">Volver al inicio</a>
    </section>
  `,
  styles: [
    `
      .not-found {
        margin: 3rem auto;
        max-width: 540px;
        padding: 2rem;
        text-align: center;
      }

      .code {
        margin: 0;
        font-size: 3rem;
        color: var(--accent);
        font-weight: 800;
      }
    `
  ]
})
export class NotFoundPage {}