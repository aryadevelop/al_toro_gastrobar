import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MOCK_DASHBOARD_METRICS } from '../mocks/restaurant.mock';
import { AdminDashboardData, DashboardMetric } from '../models/domain.models';
import { MockApiService } from './mock-api.service';
import { API_PATHS } from '../config/api-paths';
import { BackendDashboardDiarioResponse, ApiEnvelope } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(
    private readonly mockApiService: MockApiService,
    private readonly http: HttpClient
  ) {}

  getMetrics(): Observable<DashboardMetric[]> {
    return this.mockApiService.respond([...MOCK_DASHBOARD_METRICS], 300);
  }

  getAdminDashboard(): Observable<AdminDashboardData> {
    return this.http.get<ApiEnvelope<BackendDashboardDiarioResponse>>(API_PATHS.adminDashboard).pipe(
      map(res => {
        const backendData = res.data;
        
        // Mapear ingresos por tipo de venta (Menu especial vs Carta)
        const menuEspecial = backendData.ingresosPorTipoVenta?.find(t => t.tipoVenta === 'Menu especial')?.totalIngresos || 0;
        const carta = backendData.ingresosPorTipoVenta?.find(t => t.tipoVenta === 'Carta')?.totalIngresos || 0;

        return {
          fecha: backendData.fecha,
          ventasDelDia: {
            totalVentas: backendData.totalIngresos || 0,
            reservasConcretadas: backendData.totalVentasCerradas || 0
          },
          ventasPorMetodo: (backendData.ingresosPorMetodoPago || []).map(m => {
            let metodoMapped: 'CASH' | 'CARD' | 'TRANSFER' = 'CASH';
            if (m.metodoPago.toUpperCase().includes('TARJETA') || m.metodoPago === 'CARD') metodoMapped = 'CARD';
            else if (m.metodoPago.toUpperCase().includes('NEQUI') || m.metodoPago === 'TRANSFER') metodoMapped = 'TRANSFER';
            return {
              metodo: metodoMapped,
              total: m.totalIngresos
            };
          }),
          // El backend no devuelve esto actualmente
          ventasPorZona: [],
          topPlatos: (backendData.productosMasVendidos || []).map(p => ({
            nombre: p.nombreProducto,
            cantidad: p.cantidadVendida,
            total: p.totalGenerado
          })),
          menuEspecialVsCarta: {
            menuEspecial,
            carta
          },
          // El backend no devuelve esto
          variacionVsAyer: 0,
          // El backend no devuelve rendimiento detallado por mesero
          rendimientoMeseros: [],
          // El backend no devuelve pedidos en preparación
          pedidosProduccion: {
            totalActivos: 0,
            promedioMinutos: 0,
            pedidos: []
          },
          pedidosListos: (backendData.pedidosListosDetalle || []).map(p => ({
            id: String(p.comandaId),
            cliente: p.nombreCliente || 'Cliente',
            mesa: p.identificadorMesa,
            items: [] // No devuelto en el detalle
          })),
          personalTurno: {
            resumen: 'Resumen de personal activo hoy',
            grupos: [
              {
                rol: 'Meseros',
                total: backendData.meserosConVisitaActiva || 0,
                personal: []
              },
              {
                rol: 'Cocineros',
                total: backendData.cocinerosRegistradosActivos || 0,
                personal: []
              },
              {
                rol: 'Bartenders',
                total: backendData.bartendersConSesionActiva || 0,
                personal: []
              }
            ]
          },
          ocupacion: {
            ocupadas: backendData.visitasActivas || 0,
            reservasPendientes: backendData.reservasActivasHoy || 0
          }
        };
      })
    );
  }
}