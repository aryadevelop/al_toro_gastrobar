import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs';

@Component({
  selector: 'app-breadcrumbs',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <nav class="breadcrumbs" aria-label="breadcrumbs">
      <a routerLink="/app/dashboard">Inicio</a>
      <ng-container *ngFor="let crumb of breadcrumbs$ | async">
        <span>/</span>
        <a [routerLink]="crumb.path">{{ crumb.label }}</a>
      </ng-container>
    </nav>
  `,
  styles: [
    `
      .breadcrumbs {
        display: flex;
        gap: 0.4rem;
        font-size: 0.85rem;
        margin-bottom: 0.8rem;
        color: var(--muted);
      }
    `
  ]
})
export class BreadcrumbsComponent {
  readonly breadcrumbs$ = this.router.events.pipe(
    filter((event) => event instanceof NavigationEnd),
    startWith(null),
    map(() => this.buildCrumbs(this.router.url))
  );

  constructor(private readonly router: Router) {}

  private buildCrumbs(url: string): Array<{ label: string; path: string }> {
    const cleanPath = url.split('?')[0] ?? '';
    const segments = cleanPath.split('/').filter(Boolean).slice(1);
    return segments.map((segment, index) => ({
      label: segment.replace(/-/g, ' '),
      path: `/app/${segments.slice(0, index + 1).join('/')}`
    }));
  }
}