import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ComandaProduccionService } from '../../../../core/services/comanda-produccion.service';
import { BackendComandaProduccionDetalle } from '../../../../core/models/api.models';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-comanda-detalle-page',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  templateUrl: './comanda-detalle-page.component.html',
  styleUrls: ['./comanda-detalle-page.component.scss'],
})
export class ComandaDetallePageComponent implements OnInit {
  detalle: BackendComandaProduccionDetalle | null = null;
  loading = true;
  error = '';
  actionLoading = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly produccionService: ComandaProduccionService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/app/produccion/comandas-board']);
      return;
    }
    this.produccionService.obtenerDetalle(id).subscribe({
      next: (data) => {
        this.detalle = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudo cargar el detalle de la comanda.';
        this.loading = false;
      },
    });
  }

  volver(): void {
    this.router.navigate(['/app/produccion/comandas-board']);
  }

  iniciarPreparacion(): void {
    if (!this.detalle || this.actionLoading) return;
    this.actionLoading = true;
    this.produccionService.iniciarPreparacion(this.detalle.comandaId).subscribe({
      next: () => {
        this.actionLoading = false;
        this.volver();
      },
      error: () => { this.actionLoading = false; },
    });
  }

  marcarListo(): void {
    if (!this.detalle || this.actionLoading) return;
    this.actionLoading = true;
    this.produccionService.marcarListo(this.detalle.comandaId).subscribe({
      next: () => {
        this.actionLoading = false;
        this.volver();
      },
      error: () => { this.actionLoading = false; },
    });
  }
}
