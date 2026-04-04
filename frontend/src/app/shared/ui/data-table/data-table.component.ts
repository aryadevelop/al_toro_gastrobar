import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

export interface DataColumn {
  key: string;
  label: string;
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="table-wrapper card">
      <table>
        <thead>
          <tr>
            <th *ngFor="let column of columns">{{ column.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let row of rows">
            <td *ngFor="let column of columns">{{ row[column.key] }}</td>
          </tr>
          <tr *ngIf="rows.length === 0">
            <td [attr.colspan]="columns.length">Sin datos para mostrar.</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [
    `
      .table-wrapper {
        overflow-x: auto;
      }

      table {
        width: 100%;
        border-collapse: collapse;
      }

      th,
      td {
        text-align: left;
        padding: 0.75rem;
        border-bottom: 1px solid rgba(15, 76, 58, 0.12);
      }
    `
  ]
})
export class DataTableComponent {
  @Input({ required: true }) columns: DataColumn[] = [];
  @Input({ required: true }) rows: Array<Record<string, string | number | null | undefined>> = [];
}