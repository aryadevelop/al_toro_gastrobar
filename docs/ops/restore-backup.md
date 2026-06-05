# Restaurar un backup de PostgreSQL

## Listar backups disponibles

```bash
ls -lh backups/daily/
```

## Restaurar (detiene la app primero)

```bash
docker compose stop api
gunzip -c backups/daily/<archivo>.sql.gz | \
  docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
docker compose start api
```

## Verificar

```bash
docker compose exec postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c \
  "SELECT count(*) FROM restaurante.usuario; SELECT count(*) FROM restaurante.reserva; SELECT count(*) FROM restaurante.venta;"
```

## Notas

- Los backups diarios se guardan en `backups/daily/`, semanales en `backups/weekly/`, mensuales en `backups/monthly/`.
- Retención: 7 días / 4 semanas / 6 meses.
- La carpeta `backups/` está en `.gitignore` — no se sube al repositorio.
- Para forzar un backup manual sin esperar el schedule: `docker compose exec db-backup /backup.sh`
