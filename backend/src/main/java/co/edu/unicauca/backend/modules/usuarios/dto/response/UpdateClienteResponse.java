package co.edu.unicauca.backend.modules.usuarios.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Respuesta devuelta por el endpoint {@code PUT /api/clientes/me}.
 *
 * <p>Comunica el éxito de la actualización del perfil e incluye los datos
 * actualizados del cliente para que el frontend refleje los cambios en tiempo real.
 *
 * @see co.edu.unicauca.backend.modules.usuarios.controller.ClienteController#actualizarMiPerfil(UpdateClienteRequest)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClienteResponse {

    /** Indica si la actualización fue exitosa; siempre {@code true} en esta respuesta. */
    private Boolean success;

    /** Mensaje amistoso confirmando la actualización exitosa. */
    private String message;

    /** Datos actualizados del cliente. */
    private ClienteData cliente;

    /**
     * Subclase con los datos actuales del cliente tras la actualización.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteData {

        /** Identificador único del cliente (ID de la tabla cliente). */
        private Long id;

        /** Nombre completo del cliente. */
        private String nombre;

        /** Correo electrónico del usuario asociado. */
        private String email;

        /** Teléfono de contacto del cliente. */
        private String telefono;

        /** Dirección de domicilio del cliente; {@code null} si no fue proporcionada. */
        private String direccion;

        /** Saldo actual de puntos de fidelización. */
        private Integer puntosActuales;

        /** Total acumulado de puntos (nunca disminuye). */
        private Integer puntosAcumulados;

        /** Flag de aceptación de términos y condiciones. */
        private Boolean aceptaTerminos;
    }
}
