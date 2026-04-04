import { Component, Input } from '@angular/core';
import { Reserva } from '../../../core/models/domain.models';

@Component({
  selector: 'app-reservation-card',
  standalone: true,
  template: `
    <article class="card reservation-card">
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
    `
  ]
})
export class ReservationCardComponent {
  @Input({ required: true }) reservation!: Reserva;
}