import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  template: `
    <div class="skeleton" [style.height.px]="height"></div>
  `,
  styles: [
    `
      .skeleton {
        width: 100%;
        border-radius: 8px;
        background: linear-gradient(90deg, #ece7de 25%, #f4f2ec 50%, #ece7de 75%);
        background-size: 200% 100%;
        animation: shimmer 1.2s infinite;
      }

      @keyframes shimmer {
        0% {
          background-position: 200% 0;
        }
        100% {
          background-position: -200% 0;
        }
      }
    `
  ]
})
export class SkeletonLoaderComponent {
  @Input() height = 16;
}