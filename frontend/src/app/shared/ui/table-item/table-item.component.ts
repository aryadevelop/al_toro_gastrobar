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
        border: 1px solid rgba(10, 10, 10, 0.18);
        background: #FFFFFF;
      }

      .available {
        border-color: #A0A0A0;
      }

      .occupied {
        border-color: #6F4E37;
      }

      .reserved {
        border-color: #0A0A0A;
      }
    `
  ]
})
export class TableItemComponent {
  @Input({ required: true }) mesa!: Mesa;
}

