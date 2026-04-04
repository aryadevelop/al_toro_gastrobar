import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-timeline-status',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ol class="timeline">
      <li *ngFor="let item of items; let i = index" [class.active]="i <= currentIndex">{{ item }}</li>
    </ol>
  `,
  styles: [
    `
      .timeline {
        list-style: none;
        display: grid;
        gap: 0.4rem;
        padding: 0;
      }

      li {
        border-left: 3px solid #ddd;
        padding-left: 0.6rem;
      }

      li.active {
        border-left-color: var(--primary);
        font-weight: 700;
      }
    `
  ]
})
export class TimelineStatusComponent {
  @Input() items: string[] = [];
  @Input() currentIndex = 0;
}