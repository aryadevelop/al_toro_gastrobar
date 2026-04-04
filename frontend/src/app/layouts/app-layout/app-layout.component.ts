import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BreadcrumbsComponent } from '../components/breadcrumbs/breadcrumbs.component';
import { MobileHeaderComponent } from '../components/mobile-header/mobile-header.component';
import { SidebarComponent } from '../components/sidebar/sidebar.component';
import { TopbarComponent } from '../components/topbar/topbar.component';

@Component({
  selector: 'app-app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopbarComponent, MobileHeaderComponent, BreadcrumbsComponent],
  template: `
    <div class="app-layout">
      <aside class="desktop-sidebar">
        <app-sidebar></app-sidebar>
      </aside>

      <div class="content-wrapper">
        <app-mobile-header (menuToggle)="showMobileMenu.set(!showMobileMenu())"></app-mobile-header>

        <div class="mobile-menu" *ngIf="showMobileMenu()">
          <app-sidebar></app-sidebar>
        </div>

        <app-topbar></app-topbar>
        <app-breadcrumbs></app-breadcrumbs>

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
}