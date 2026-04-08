import { Component, Input } from '@angular/core';
import { Reserva } from '../../../core/models/domain.models';

@Component({
  selector: 'app-reservation-card',
  standalone: true,
  template: `
    <article class="card reservation-card" [class.reservation-card--compact]="compact">
      <h4>{{ reservation.guestName }}</h4>
      <p>{{ reservation.date }} {{ reservation.time }} | {{ reservation.guests }} personas</p>
      <small>Estado: {{ reservation.status }}</small>
    </article>
  `,
  styles: [
    `
      .reservation-card {
        padding: 0.9rem;
      }

      .reservation-card h4,
      .reservation-card p,
      .reservation-card small {
        margin: 0;
      }

      .reservation-card h4 {
        margin-bottom: 0.42rem;
      }

      .reservation-card p {
        margin-bottom: 0.32rem;
      }

      .reservation-card--compact {
        padding: 0.64rem 0.72rem;
      }

      .reservation-card--compact h4 {
        font-size: 1.06rem;
        margin-bottom: 0.34rem;
      }

      .reservation-card--compact p {
        font-size: 0.84rem;
        margin-bottom: 0.26rem;
      }

      .reservation-card--compact small {
        font-size: 0.78rem;
      }
    `
  ]
})
export class ReservationCardComponent {
  @Input({ required: true }) reservation!: Reserva;
  @Input() compact = false;
}