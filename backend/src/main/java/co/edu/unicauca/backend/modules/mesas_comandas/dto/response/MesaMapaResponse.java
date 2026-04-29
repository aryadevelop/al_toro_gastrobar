package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * DTO para representar una mesa en el mapa de mesas.
 * Incluye estado, mesero asignado, notificaciones activas y flag de borrador.
 */
@Getter
@Builder
public class MesaMapaResponse {

    /** ID de la visita (PK de Mesa) */
    private final Long visitaId;

    /** Identificador de la mesa (ej. "T-04") */
    private final String identificador;

    /** Número de comensales */
    private final Integer numeroPersonas;

    /** Estado de la mesa: "ESPERA", "EN_PREPARACION", "ATENDIDA", "CERRADA" */
    private final String estado;

    /**
     * Nombre completo del mesero asignado.
     * null si la mesa es propia del mesero que hace la petición
     */
    private final String nombreMesero;

    /** Email del mesero asignado */
    private final String emailMesero;

    /** true si la mesa fue creada por el mesero que hace la petición */
    private final Boolean esMesaPropia;

    /** true si la mesa tiene al menos una comanda en estado BORRADOR */
    private final Boolean tieneBorrador;

    /** Lista de notificaciones activas (ATENCION, PLATOS_LISTOS, BEBIDAS_LISTAS, CAMBIO) */
    private final List<NotificacionActivaResponse> notificacionesActivas;
}
