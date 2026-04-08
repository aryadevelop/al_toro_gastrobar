import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SearchFilter } from '../../../core/models/domain.models';

@Component({
  selector: 'app-search-filters',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="card filters">
      <ng-container *ngFor="let filter of filters">
        <label>
          <span>{{ filter.label }}</span>
          <input
            *ngIf="filter.type === 'text'"
            [(ngModel)]="model[filter.key]"
            class="input-field"
            (ngModelChange)="apply()"
          />
          <input
            *ngIf="filter.type === 'date'"
            type="date"
            [(ngModel)]="model[filter.key]"
            class="input-field"
            (ngModelChange)="apply()"
          />
          <select
            *ngIf="filter.type === 'select'"
            [(ngModel)]="model[filter.key]"
            class="input-field"
            (ngModelChange)="apply()"
          >
            <option value="">Todos</option>
            <option *ngFor="let option of filter.options ?? []" [value]="option.value">{{ option.label }}</option>
          </select>
        </label>
      </ng-container>
    </section>
  `,
  styles: [
    `
      .filters {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
        gap: 0.75rem;
        padding: 1rem;
      }

      label {
        display: grid;
        gap: 0.3rem;
        font-size: 0.9rem;
      }
    `
  ]
})
export class SearchFiltersComponent {
  @Input() filters: SearchFilter[] = [];
  @Output() readonly filtersChange = new EventEmitter<Record<string, string>>();

  model: Record<string, string> = {};

  apply(): void {
    this.filtersChange.emit(this.model);
  }
}