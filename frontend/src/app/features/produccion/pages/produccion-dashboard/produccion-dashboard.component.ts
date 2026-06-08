import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-produccion-dashboard',
  standalone: true,
  imports: [],
  template: `<p>Redirigiendo al tablero...</p>`,
})
export class ProduccionDashboardComponent implements OnInit {
  constructor(private readonly router: Router) {}

  ngOnInit(): void {
    this.router.navigate(['/app/produccion/comandas-board']);
  }
}
