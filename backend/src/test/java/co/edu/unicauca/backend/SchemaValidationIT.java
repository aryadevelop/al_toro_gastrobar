package co.edu.unicauca.backend;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que las entidades JPA coincidan  con el esquema real de PostgreSQL.
 *
 * <p>Niveles de verificación:
 * <ol>
 *   <li>Hibernate validate pasa al cargar el contexto Spring.</li>
 *   <li>Cada entidad JPA tiene su tabla en el schema {@code restaurante}.</li>
 *   <li>Cada columna mapeada en una entidad existe en la DB.</li>
 *   <li>La DB no tiene columnas huérfanas sin mapeo en entidades.</li>
 *   <li>La nulabilidad {@code @Column(nullable)} coincide con {@code NOT NULL} en la DB.</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Validación exacta: Entidades JPA vs Esquema de Base de Datos")
class SchemaValidationIT {

    private static final String SCHEMA = "restaurante";

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private DataSource dataSource;

    // =========================================================================
    // Test 1 — Hibernate validate
    // =========================================================================

    /**
     * Si este test pasa, {@code ddl-auto=validate} comparó todas las entidades contra la
     * DB sin encontrar diferencias de tablas ni columnas (validación nativa de Hibernate).
     */
    @Test
    @DisplayName("1. Hibernate validate: contexto carga sin errores de esquema")
    void hibernateValidatePasses() {
        assertNotNull(emf,
                "El EntityManagerFactory debe inicializar correctamente. ");
    }

    // =========================================================================
    // Test 2 — Existencia de tablas
    // =========================================================================

    @Test
    @DisplayName("2. Tablas: cada entidad JPA tiene su tabla en el schema 'restaurante'")
    void allEntityTablesExistInDatabase() throws Exception {
        Set<String> dbTables = getDbTables();
        Set<String> missing = new TreeSet<>();

        for (AbstractEntityPersister p : getAllPersisters()) {
            String table = stripSchema(p.getTableName()).toLowerCase();
            if (!table.isBlank() && !dbTables.contains(table)) {
                missing.add(table);
            }
        }

        assertTrue(missing.isEmpty(),
                "\n[FALLO] Tablas definidas en entidades JPA pero AUSENTES en la DB:\n  - "
                        + String.join("\n  - ", missing));
    }

    // =========================================================================
    // Test 3 — Columnas de entidad presentes en la DB
    // =========================================================================

    @Test
    @DisplayName("3. Columnas: cada columna mapeada en JPA existe en la DB")
    void allEntityColumnsExistInDatabase() throws Exception {
        List<String> missing = new ArrayList<>();

        for (AbstractEntityPersister p : getAllPersisters()) {
            String table = stripSchema(p.getTableName()).toLowerCase();
            Set<String> dbCols = getDbColumns(table);
            if (dbCols.isEmpty()) continue; // tabla ausente ya detectada en Test 2

            // Columnas del identificador (PK)
            for (String col : p.getIdentifierColumnNames()) {
                if (isBlankCol(col) || dbCols.contains(col.toLowerCase())) continue;
                missing.add(String.format("  [%s] PK '%s' no existe en DB", table, col));
            }

            // Columnas de propiedades
            String[] props = p.getPropertyNames();
            for (String prop : props) {
                String[] cols = p.getPropertyColumnNames(prop);
                if (cols == null) continue;
                for (String col : cols) {
                    if (isBlankCol(col) || dbCols.contains(col.toLowerCase())) continue;
                    missing.add(String.format("  [%s.%s] columna '%s' no existe en DB", table, prop, col));
                }
            }
        }

        assertTrue(missing.isEmpty(),
                "\n[FALLO] Columnas en entidades JPA AUSENTES en la DB:\n" + String.join("\n", missing));
    }

    // =========================================================================
    // Test 4 — Columnas de DB sin mapeo en entidades
    // =========================================================================

    @Test
    @DisplayName("4. Columnas: la DB no tiene columnas huérfanas sin mapear en JPA")
    void noOrphanColumnsInDatabase() throws Exception {
        Map<String, Set<String>> entityColsByTable = buildEntityColumnsMap();
        List<String> orphans = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : entityColsByTable.entrySet()) {
            String table = entry.getKey();
            Set<String> entityCols = entry.getValue();
            Set<String> dbCols = getDbColumns(table);

            Set<String> extra = new TreeSet<>(dbCols);
            extra.removeAll(entityCols);

            for (String col : extra) {
                orphans.add(String.format("  [%s] columna '%s' existe en DB pero NO está mapeada en ninguna entidad",
                        table, col));
            }
        }

        assertTrue(orphans.isEmpty(),
                "\n[FALLO] Columnas en la DB SIN MAPEO en entidades JPA:\n" + String.join("\n", orphans));
    }

    // =========================================================================
    // Test 5 — Nulabilidad
    // =========================================================================

    @Test
    @DisplayName("5. Nulabilidad: @Column(nullable) coincide con NOT NULL en la DB")
    void nullabilityMatchesBetweenEntityAndDatabase() throws Exception {
        List<String> mismatches = new ArrayList<>();

        for (AbstractEntityPersister p : getAllPersisters()) {
            String table = stripSchema(p.getTableName()).toLowerCase();
            Map<String, Boolean> dbNullability = getDbColumnNullability(table);
            if (dbNullability.isEmpty()) continue;

            boolean[] entityNullability = p.getPropertyNullability();
            String[] props = p.getPropertyNames();

            for (int i = 0; i < props.length; i++) {
                String[] cols = p.getPropertyColumnNames(props[i]);
                if (cols == null) continue;
                for (String col : cols) {
                    if (isBlankCol(col)) continue;
                    Boolean dbNullable = dbNullability.get(col.toLowerCase());
                    if (dbNullable == null) continue; // columna faltante ya cubierta en Test 3

                    boolean entityNullable = entityNullability[i];
                    if (entityNullable != dbNullable) {
                        mismatches.add(String.format(
                                "  [%s.%s] entidad=nullable:%s  vs  DB=nullable:%s",
                                table, col, entityNullable, dbNullable));
                    }
                }
            }
        }

        assertTrue(mismatches.isEmpty(),
                "\n[FALLO] Discrepancias de nulabilidad entre entidades JPA y la DB:\n"
                        + String.join("\n", mismatches));
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /** Obtiene todos los {@link AbstractEntityPersister} registrados en Hibernate. */
    private List<AbstractEntityPersister> getAllPersisters() {
        SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);
        List<AbstractEntityPersister> result = new ArrayList<>();
        sf.getMappingMetamodel().forEachEntityDescriptor(descriptor -> {
            if (descriptor instanceof AbstractEntityPersister aep) {
                result.add(aep);
            }
        });
        return result;
    }

    /** Construye un mapa {@code tabla → columnas} a partir de los metadatos de las entidades. */
    private Map<String, Set<String>> buildEntityColumnsMap() {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        for (AbstractEntityPersister p : getAllPersisters()) {
            String table = stripSchema(p.getTableName()).toLowerCase();
            Set<String> cols = map.computeIfAbsent(table, k -> new TreeSet<>());

            for (String col : p.getIdentifierColumnNames()) {
                if (!isBlankCol(col)) cols.add(col.toLowerCase());
            }
            for (String prop : p.getPropertyNames()) {
                String[] propCols = p.getPropertyColumnNames(prop);
                if (propCols != null) {
                    for (String col : propCols) {
                        if (!isBlankCol(col)) cols.add(col.toLowerCase());
                    }
                }
            }
        }
        return map;
    }

    private Set<String> getDbTables() throws SQLException {
        Set<String> tables = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, SCHEMA, null, new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }
        return tables;
    }

    private Set<String> getDbColumns(String tableName) throws SQLException {
        Set<String> cols = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, SCHEMA, tableName, null)) {
            while (rs.next()) {
                cols.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }
        return cols;
    }

    private Map<String, Boolean> getDbColumnNullability(String tableName) throws SQLException {
        Map<String, Boolean> map = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, SCHEMA, tableName, null)) {
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME").toLowerCase();
                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                map.put(col, nullable);
            }
        }
        return map;
    }

    private String stripSchema(String tableName) {
        if (tableName == null) return "";
        int dot = tableName.lastIndexOf('.');
        return dot >= 0 ? tableName.substring(dot + 1) : tableName;
    }

    private boolean isBlankCol(String col) {
        return col == null || col.isBlank();
    }
}
