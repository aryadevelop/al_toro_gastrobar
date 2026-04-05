import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MenuService } from '../../../core/services/menu.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <div class="brand">
        <img src="assets/images/al-toro-logo-vector.svg" alt="Al Toro Gastrobar" class="brand-logo" />
        <small>Panel operativo</small>
      </div>

      <nav class="menu">
        <a
          *ngFor="let item of menuService.menuByRole()"
          [routerLink]="item.path"
          routerLinkActive="active"
          class="menu-item"
        >
          <span class="icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </a>
      </nav>
    </aside>
  `,
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  constructor(public readonly menuService: MenuService) {}
}