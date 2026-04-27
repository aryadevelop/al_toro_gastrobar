#!/bin/bash

# Script para actualizar requests de Postman con credenciales hardcoded

DIR="backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing"

# Función para actualizar un archivo
update_file() {
    local file="$1"
    local role="$2"
    local email="$3"
    local password="$4"
    local token_var="$5"

    # Reemplazar pm.environment.get('baseUrl') con '{{baseUrl}}'
    sed -i "s|pm\.environment\.get('baseUrl')|'{{baseUrl}}'|g" "$file"

    # Reemplazar email en el login
    sed -i "s|email: pm\.environment\.get('email$role')|email: '$email'|g" "$file"

    # Reemplazar password en el login
    sed -i "s|password: pm\.environment\.get('passwordValida')|password: '$password'|g" "$file"

    # Reemplazar token variable en headers
    sed -i "s|Bearer {{\([a-zA-Z]*\)Token}}|Bearer {{tmp\1Token}}|g" "$file"

    # Reemplazar pm.environment.set con tmp
    sed -i "s|pm\.environment\.set('\([a-zA-Z]*\)Token'|pm.environment.set('tmp\1Token'|g" "$file"

    # Agregar unset en afterResponse si no existe
    if ! grep -q "unset.*tmp" "$file"; then
        # Buscar si ya tiene afterResponse
        if grep -q "type: afterResponse" "$file"; then
            # Agregar unset al final del código existente
            sed -i "/type: afterResponse/,/language: text\/javascript/{s|language: text/javascript|pm.environment.unset('tmp$token_var');\n    language: text/javascript|}" "$file"
        fi
    fi
}

# Actualizar archivos de CLIENTE
for file in "$DIR"/20-*.yaml "$DIR"/40-*.yaml "$DIR"/50-01*.yaml "$DIR"/10-*.yaml; do
    [ -f "$file" ] && update_file "$file" "Cliente" "cliente1@altoro.com" "password123" "Cliente"
done

# Actualizar archivos de MESERO
for file in "$DIR"/20-01*.yaml "$DIR"/30-*.yaml "$DIR"/70-*.yaml; do
    [ -f "$file" ] && update_file "$file" "Mesero" "mesero1@altoro.com" "password123" "Mesero"
done

# Actualizar archivos de CAJERO
for file in "$DIR"/50-02*.yaml "$DIR"/50-03*.yaml; do
    [ -f "$file" ] && update_file "$file" "Cajero" "cajero1@altoro.com" "password123" "Cajero"
done

echo "✓ Actualización completada"
