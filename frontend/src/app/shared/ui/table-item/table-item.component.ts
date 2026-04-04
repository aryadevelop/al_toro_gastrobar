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
        border: 1px solid rgba(13, 13, 13, 0.18);
        background: #f2f2f2;
      }

      .available {
        border-color: #a4a5a6;
      }

      .occupied {
        border-color: #f23535;
      }

      .reserved {
        border-color: #0d0d0d;
      }
    `
  ]
})
export class TableItemComponent {
  @Input({ required: true }) mesa!: Mesa;
}