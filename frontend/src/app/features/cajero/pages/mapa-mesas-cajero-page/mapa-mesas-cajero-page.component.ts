import { Component } from '@angular/core';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-mapa-mesas-cajero-page',
  standalone: true,
  imports: [PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="MapaMesasCajeroPage" subtitle="Vista base en construccion"></app-page-header>
      <article class="card" style="padding: 1rem;">
        <p>Contenido inicial de mapa-mesas-cajero-page.</p>
      </article>
    </section>
  `
})
export class MapaMesasCajeroPageComponent {}
