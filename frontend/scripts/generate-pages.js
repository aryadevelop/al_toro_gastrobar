const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..', 'src', 'app', 'features');

const features = {
  auth: ['login-page', 'register-page', 'profile-page'],
  cliente: [
    'cliente-dashboard',
    'reserva-create-page',
    'reserva-edit-page',
    'reservas-history-page',
    'preorden-page',
    'orden-actual-page',
    'asistencia-page'
  ],
  mesero: [
    'mesero-dashboard',
    'reservas-list-page',
    'llegada-reserva-page',
    'mapa-mesas-page',
    'comanda-editor-page',
    'notificaciones-page'
  ],
  produccion: ['produccion-dashboard', 'comandas-board-page', 'comanda-detalle-page', 'inventario-egreso-page'],
  cajero: ['cajero-dashboard', 'reservas-cajero-page', 'mapa-mesas-cajero-page', 'pago-cierre-page', 'cierre-caja-page'],
  admin: [
    'admin-dashboard-page',
    'ventas-list-page',
    'venta-detalle-page',
    'cliente-historial-page',
    'preparaciones-list-page',
    'preparacion-form-page',
    'productos-list-page',
    'producto-form-page',
    'insumos-list-page',
    'insumo-form-page',
    'decoraciones-page',
    'personal-list-page',
    'personal-form-page',
    'clientes-list-page',
    'cliente-detalle-page'
  ]
};

const toPascal = (value) =>
  value
    .split('-')
    .map((chunk) => (chunk ? `${chunk[0].toUpperCase()}${chunk.slice(1)}` : ''))
    .join('');

for (const [feature, pages] of Object.entries(features)) {
  for (const page of pages) {
    const className = `${toPascal(page)}Component`;
    const selector = `app-${page}`;
    const dir = path.join(root, feature, 'pages', page);

    fs.mkdirSync(dir, { recursive: true });

    const content = [
      "import { Component } from '@angular/core';",
      "import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';",
      '',
      '@Component({',
      `  selector: '${selector}',`,
      '  standalone: true,',
      '  imports: [PageHeaderComponent],',
      '  template: `',
      '    <section class="page-grid">',
      `      <app-page-header title="${toPascal(page)}" subtitle="Vista base en construccion"></app-page-header>`,
      '      <article class="card" style="padding: 1rem;">',
      `        <p>Contenido inicial de ${page}.</p>`,
      '      </article>',
      '    </section>',
      '  `',
      '})',
      `export class ${className} {}`,
      ''
    ].join('\n');

    fs.writeFileSync(path.join(dir, `${page}.component.ts`), content, 'utf8');
  }
}

console.log('Feature pages generated.');
