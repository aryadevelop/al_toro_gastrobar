package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Asignación de mesa física para una {@link Visita}.
 *
 * <p>Modela el espacio físico ocupado durante una visita: la mesa identificada,
 * la zona donde está ubicada, el mesero asignado y el número de comensales.
 * La relación es {@code @OneToOne} con {@code @MapsId}, por lo que comparte
 * la PK ({@code visita_id}) con {@link Visita}.
 *
 * El estado se actualiza al abrir y cerrar la visita, reflejando
 * la disponibilidad en tiempo real para el host de recepción.
 *
 * <p>Estrategia de índices:
 * <ul>
 *   <li>{@code idx_mesa_mesero_id} — carga de mesas activas por mesero.</li>
 *   <li>{@code idx_mesa_zona_id} — filtro de disponibilidad por zona.</li>
 *   <li>{@code idx_mesa_estado} — consulta de mesas disponibles en recepción.</li>
 *   <li>{@code idx_mesa_identificador} — búsqueda por etiqueta de mesa (ej. "T-04").</li>
 *   <li>{@code idx_mesa_activas} — índice compuesto para la vista de mesas activas por mesero.</li>
 * </ul>
 *
 * @see Zona
 * @see co.edu.unicauca.backend.shared.enums.EstadoMesa
 */
@Entity
@Table(name = "mesa", schema = "restaurante",
       indexes = {
           @Index(name = "idx_mesa_mesero_id", columnList = "mesero_id"),
           @Index(name = "idx_mesa_zona_id", columnList = "zona_id"),
           @Index(name = "idx_mesa_estado", columnList = "mesa_estado"),
           @Index(name = "idx_mesa_identificador", columnList = "mesa_identificador"),
           @Index(name = "idx_mesa_activas", columnList = "mesero_id, mesa_estado")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mesa extends AuditableEntity {

    /** PK compartida con {@link Visita} vía {@code @MapsId}. */
    @Id
    @Column(name = "visita_id")
    private Long visitaId;

    /** Visita a la que corresponde esta asignación de mesa; establece la PK mediante {@code @MapsId}. */
    @NotNull(message = "La visita es obligatoria")
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "visita_id", foreignKey = @ForeignKey(name = "fk_mesa_visita"))
    private Visita visita;

    /** Zona del restaurante donde está ubicada la mesa. */
    @NotNull(message = "La zona es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mesa_zona"))
    private Zona zona;

    /** Mesero responsable de atender esta mesa durante la visita. */
    @NotNull(message = "El mesero es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesero_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_mesa_mesero"))
    private Empleado mesero;

    /** Etiqueta física de la mesa en el salón. */
    @NotBlank(message = "El identificador de mesa es obligatorio")
    @Size(max = 20, message = "El identificador no debe exceder 20 caracteres")
    @Column(name = "mesa_identificador", nullable = false, length = 20)
    private String mesaIdentificador;

    /** Cantidad de comensales sentados en la mesa; mínimo 1. */
    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "El número de personas debe ser al menos 1")
    @Column(name = "mesa_numero_personas", nullable = false)
    private Integer mesaNumeroPersonas;

    /** Estado operativo actual de la mesa en el piso del restaurante. */
    @NotNull(message = "El estado de la mesa es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "mesa_estado", nullable = false, length = 20)
    private EstadoMesa mesaEstado;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mesa)) return false;
        Mesa mesa = (Mesa) o;
        return visitaId != null && visitaId.equals(mesa.getVisitaId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
