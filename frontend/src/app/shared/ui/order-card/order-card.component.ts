import { Component, Input } from '@angular/core';
import { Comanda } from '../../../core/models/domain.models';

@Component({
  selector: 'app-order-card',
  standalone: true,
  template: `
    <article class="card order-card">
      <h4>Comanda {{ order.id }}</h4>
      <p>Mesa: {{ order.mesaCode }}</p>
      <p>tems: {{ order.items.length }}</p>
      <small>Estado: {{ order.status }}</small>
    </article>
  `,
  styles: [
    `
      .order-card {
        padding: 0.9rem;
      }
    `
  ]
})
export class OrderCardComponent {
  @Input({ required: true }) order!: Comanda;
}