import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-pagination-base',
  standalone: true,
  template: `
    <nav class="pagination">
      <button type="button" class="btn-secondary" [disabled]="page <= 1" (click)="changePage(page - 1)">Anterior</button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button type="button" class="btn-secondary" [disabled]="page >= totalPages" (click)="changePage(page + 1)">Siguiente</button>
    </nav>
  `,
  styles: [
    `
      .pagination {
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 0.8rem;
      }
    `
  ]
})
export class PaginationBaseComponent {
  @Input() page = 1;
  @Input() totalPages = 1;
  @Output() readonly pageChange = new EventEmitter<number>();

  changePage(nextPage: number): void {
    this.pageChange.emit(nextPage);
  }
}