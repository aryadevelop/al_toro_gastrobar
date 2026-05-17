package co.edu.unicauca.backend.modules.mesas_comandas.service;

import java.util.Locale;

/**
 * Normaliza descripciones libres de ítems de comanda para comparación entre
 * registros independientemente de mayúsculas, espacios redundantes o valores
 * ausentes.
 *
 * <p>La forma normalizada se usa para detectar coincidencias al añadir un
 * ítem, al modificar uno existente y al fusionar comandas en operaciones de
 * "atender cambio". Un valor nulo o compuesto solo por espacios se trata como
 * cadena vacía, de modo que dos ítems "sin descripción" se reconocen como el
 * mismo.
 */
public final class DescripcionNormalizer {

    private DescripcionNormalizer() {
    }

    /**
     * Devuelve la forma comparable de la descripción aplicando recorte de
     * extremos, colapso de espacios internos consecutivos y conversión a
     * minúscula. Una entrada nula o vacía tras el recorte se mapea a la cadena
     * vacía.
     *
     * @param descripcion texto crudo proveniente del cliente; puede ser
     *                    {@code null}
     * @return cadena normalizada lista para igualdad por valor
     */
    public static String normalizar(String descripcion) {
        if (descripcion == null) {
            return "";
        }
        String recortado = descripcion.trim();
        if (recortado.isEmpty()) {
            return "";
        }
        return recortado.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
