#!/usr/bin/env python3
"""
Script para actualizar requests de Postman con credenciales hardcoded
"""
import os
import re

MANUAL_DIR = "backend/postman/postman/collections/manual-testing/Al Toro - Manual Testing"

# Patrones de reemplazo
REPLACEMENTS = {
    # Cliente files
    'CLIENTE': {
        'email': 'cliente1@altoro.com',
        'password': 'password123',
        'token': 'tmpClienteToken',
        'files': ['20-07', '20-08', '10-02', '10-03', '10-04', '40-01', '40-02', '40-03', '50-01']
    },
    # Mesero files
    'MESERO': {
        'email': 'mesero1@altoro.com',
        'password': 'password123',
        'token': 'tmpMeseroToken',
        'files': ['20-01', '30-02', '70-01']
    },
    # Cajero files
    'CAJERO': {
        'email': 'cajero1@altoro.com',
        'password': 'password123',
        'token': 'tmpCajeroToken',
        'files': ['50-02', '50-03']
    }
}

def update_file(filepath, email, password, token):
    """Actualiza un archivo con credenciales hardcoded"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Reemplazar baseUrl
    content = re.sub(
        r"pm\.environment\.get\('baseUrl'\)",
        "'{{baseUrl}}'",
        content
    )

    # Reemplazar email
    content = re.sub(
        r"email: pm\.environment\.get\('email[A-Z][a-z]+'\)",
        f"email: '{email}'",
        content
    )

    # Reemplazar password
    content = re.sub(
        r"password: pm\.environment\.get\('passwordValida'\)",
        f"password: '{password}'",
        content
    )

    # Reemplazar token en headers (Bearer {{clienteToken}} → Bearer {{tmpClienteToken}})
    content = re.sub(
        r"Bearer \{\{[a-z]+Token\}\}",
        f"Bearer {{{{{token}}}}}",
        content
    )

    # Reemplazar pm.environment.set token
    content = re.sub(
        r"pm\.environment\.set\('[a-z]+Token'",
        f"pm.environment.set('{token}'",
        content
    )

    # Agregar unset en afterResponse si no existe
    if 'unset' not in content and 'beforeRequest' in content:
        # Buscar si ya tiene afterResponse
        if 'type: afterResponse' not in content:
            # Agregar nuevo bloque afterResponse antes de 'order:'
            cleanup = f"""  - type: afterResponse
    code: |-
      pm.environment.unset('{token}');
    language: text/javascript
"""
            content = re.sub(r'(\norder:)', f'\n{cleanup}\\1', content)
        else:
            # Agregar unset al afterResponse existente
            content = re.sub(
                r"(type: afterResponse\s+code: \|-\s+)",
                f"\\1pm.environment.unset('{token}');\n      ",
                content,
                count=1
            )

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"✓ Actualizado: {os.path.basename(filepath)}")

def main():
    """Función principal"""
    for role, config in REPLACEMENTS.items():
        email = config['email']
        password = config['password']
        token = config['token']

        for file_prefix in config['files']:
            # Buscar archivos que coincidan con el prefijo
            for filename in os.listdir(MANUAL_DIR):
                if filename.startswith(file_prefix) and filename.endswith('.yaml'):
                    filepath = os.path.join(MANUAL_DIR, filename)
                    update_file(filepath, email, password, token)

    print("\n✅ Actualización completada")

if __name__ == '__main__':
    main()
