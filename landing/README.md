# Landing GasApp

Página de presentación de GasApp, estática (HTML + CSS + JS), lista para subir a **IONOS**.

## Contenido
```
landing/
├── index.html        Página principal (hero, funciones, privacidad, FAQ, CTA)
├── privacy.html      Política de privacidad (necesaria para Google Play)
├── styles.css        Estilos y animaciones
├── script.js         Animaciones de scroll, contadores y menú móvil
└── assets/
    ├── favicon.svg
    ├── screen-inicio.png
    ├── screen-mapa.png
    └── screen-favoritas.png
```

## Subir a IONOS

**Opción A — Explorador de archivos / FTP (webspace clásico):**
1. Entra en tu panel de IONOS → *Hosting* → *Webspace* (o usa un cliente FTP como FileZilla con los datos SFTP que te da IONOS).
2. Sube **todo el contenido de la carpeta `landing/`** (no la carpeta en sí) a la raíz pública del dominio (normalmente la carpeta del dominio o `/`).
3. Asegúrate de que `index.html` queda en la raíz para que cargue al abrir el dominio.

**Opción B — Deploy Now (Git):**
- Conecta el repositorio y configura como *static site* apuntando a la carpeta `landing/` como directorio de salida.

## Notas
- Es 100 % estática: no necesita PHP, Node ni base de datos.
- La fuente *Inter* se carga desde Google Fonts (requiere internet del visitante; hay fallback al tipo del sistema).
- Sustituye el enlace `Descargar en Google Play` por la URL real de la ficha cuando esté publicada (buscar `href="#"` / `#download` en `index.html`).
- Antes de publicar en Google Play, usa la URL de `privacy.html` como *Política de privacidad* de la ficha.
- Contacto configurado: `contacto@josedanielchinea.com` (cámbialo si procede en `index.html` y `privacy.html`).

## Vista previa local
```bash
cd landing
python -m http.server 8000
# abre http://localhost:8000
```
