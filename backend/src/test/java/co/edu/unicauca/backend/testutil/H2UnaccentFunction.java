package co.edu.unicauca.backend.testutil;

import java.text.Normalizer;

/**
 * Función Java registrada como alias H2 para emular {@code unaccent()} de PostgreSQL
 * en tests {@code @DataJpaTest}. Elimina diacríticos de una cadena para que las
 * queries nativas con {@code unaccent()} funcionen en el perfil H2.
 *
 * <p>Registrar en el {@code @BeforeEach} del test mediante:
 * <pre>
 *   testEntityManager.getEntityManager()
 *       .createNativeQuery("CREATE ALIAS IF NOT EXISTS unaccent FOR " +
 *           "\"co.edu.unicauca.backend.testutil.H2UnaccentFunction.unaccent\"")
 *       .executeUpdate();
 * </pre>
 */
public final class H2UnaccentFunction {

    private H2UnaccentFunction() {}

    /**
     * Elimina los diacríticos de {@code s} mediante descomposición NFD. Retorna
     * {@code null} si la entrada es {@code null}, preservando la semántica de SQL.
     *
     * @param s cadena de entrada; puede ser {@code null}
     * @return cadena sin diacríticos, o {@code null}
     */
    public static String unaccent(String s) {
        if (s == null) {
            return null;
        }
        String nfd = Normalizer.normalize(s, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }
}
