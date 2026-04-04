import { Component, Input } from '@angular/core';
import { Mesa } from '../../../core/models/domain.models';

@Component({
  selector: 'app-table-item',
  standalone: true,
  template: `
    <article class="table-item" [class]="mesa.status.toLowerCase()">
      <h5>{{ mesa.code }}</h5>
      <small>{{ mesa.seats }} puestos</small>
    </article>
  `,
  styles: [
    `
      .table-item {
        border-radius: 12px;
        padding: 0.8rem;
        border: 1px solid rgba(15, 76, 58, 0.18);
        background: #fff;
      }

      .available {
        border-color: #96d3b1;
      }

      .occupied {
        border-color: #ef9e9e;
      }

      .reserved {
        border-color: #f0c98b;
      }
    `
  ]
})
export class TableItemComponent {
  @Input({ required: true }) mesa!: Mesa;
}