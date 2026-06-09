import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { MobileHeaderComponent } from '../components/mobile-header/mobile-header.component';
import { SidebarComponent } from '../components/sidebar/sidebar.component';
import { TopbarComponent } from '../components/topbar/topbar.component';

@Component({
  selector: 'app-app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopbarComponent, MobileHeaderComponent],
  template: `
    <div class="app-layout">
      <aside class="desktop-sidebar">
        <app-sidebar></app-sidebar>
      </aside>

      <div class="content-wrapper">
        <app-mobile-header (menuToggle)="showMobileMenu.set(!showMobileMenu())"></app-mobile-header>

        <div class="mobile-menu" *ngIf="showMobileMenu()">
          <app-sidebar (itemClicked)="showMobileMenu.set(false)"></app-sidebar>
        </div>

        <app-topbar></app-topbar>

        <main class="page-container">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styleUrls: ['./app-layout.component.scss']
})
export class AppLayoutComponent {
  readonly showMobileMenu = signal(false);

  constructor(private router: Router) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntilDestroyed()
    ).subscribe(() => {
      this.showMobileMenu.set(false);
    });
  }
}