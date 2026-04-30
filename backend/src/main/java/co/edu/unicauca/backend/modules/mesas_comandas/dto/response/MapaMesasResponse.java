package co.edu.unicauca.backend.modules.mesas_comandas.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * DTO de respuesta completa del mapa de mesas.
 * Agrupa todas las zonas con sus mesas activas.
 */
@Getter
@Builder
public class MapaMesasResponse {

    /** Lista de zonas con sus mesas */
    private final List<ZonaMesasResponse> zonas;
}
