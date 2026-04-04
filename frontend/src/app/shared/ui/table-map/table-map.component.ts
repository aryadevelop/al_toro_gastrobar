import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { Mesa } from '../../../core/models/domain.models';
import { TableItemComponent } from '../table-item/table-item.component';

@Component({
  selector: 'app-table-map',
  standalone: true,
  imports: [CommonModule, TableItemComponent],
  template: `
    <section class="card table-map">
      <app-table-item *ngFor="let mesa of mesas" [mesa]="mesa"></app-table-item>
    </section>
  `,
  styles: [
    `
      .table-map {
        padding: 1rem;
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
        gap: 0.75rem;
      }
    `
  ]
})
export class TableMapComponent {
  @Input() mesas: Mesa[] = [];
}