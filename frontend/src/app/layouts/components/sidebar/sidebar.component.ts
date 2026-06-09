import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MenuService } from '../../../core/services/menu.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar" (click)="itemClicked.emit()">
      <div class="brand">
        <img src="assets/images/al-toro-logo-vector.svg" alt="Al Toro Gastrobar" class="brand-logo" />
      </div>

      <nav class="menu">
        <a
          *ngFor="let item of menuService.menuByRole()"
          [routerLink]="item.path"
          routerLinkActive="active"
          class="menu-item"
          (click)="itemClicked.emit()"
        >
          <span class="icon" aria-hidden="true">{{ getIconSymbol(item.icon) }}</span>
          <span class="menu-label">{{ item.label }}</span>
        </a>
      </nav>
    </aside>
  `,
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  @Output() itemClicked = new EventEmitter<void>();

  private readonly iconSymbolByName: Record<string, string> = {
    home: '⌂',
    person: '◉',
    history: '◷',
    restaurant: '◌',
    kitchen: '◍',
    payments: '$',
    shield: '⛨',
    badge: '★'
  };

  constructor(public readonly menuService: MenuService) {}

  getIconSymbol(iconName: string): string {
    return this.iconSymbolByName[iconName] ?? '•';
  }
}