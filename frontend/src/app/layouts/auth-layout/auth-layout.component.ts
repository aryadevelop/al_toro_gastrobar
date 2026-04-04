import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="auth-layout">
      <section class="auth-panel">
        <h1>Al Toro Gastrobar</h1>
        <p>Gestin integral del restaurante</p>
      </section>

      <section class="auth-content card">
        <router-outlet></router-outlet>
      </section>
    </div>
  `,
  styleUrls: ['./auth-layout.component.scss']
})
export class AuthLayoutComponent {}