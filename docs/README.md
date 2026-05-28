# Landing de GasApp

Carpeta con la web pública de GasApp. **Hosting destino: IONOS** (también
funciona en cualquier hosting estático: HTML + CSS planos, sin build).

## Archivos

```
docs/
├── index.html        Landing principal
├── privacy.html      Política de privacidad
├── styles.css        Estilos
├── icon.png          Logo (512×512)
└── .htaccess         Gzip, caché y cabeceras (Apache / IONOS)
```

## Subida a IONOS

### Opción A — Panel de IONOS (sin programas)
1. Entra en tu **Panel de IONOS** → *Hosting* → *Webspace Explorer* / *File Manager*.
2. Sitúate en la **raíz pública** del dominio (suele ser `/` o `/public_html/`).
3. **Sube TODO el contenido** de `docs/` (los 5 archivos anteriores). Si el
   explorador oculta archivos que empiezan por punto, márcalos como visibles
   para subir `.htaccess`.
4. Abre tu dominio en el navegador → listo.

### Opción B — FTP/SFTP con FileZilla u otro cliente

- **Servidor / usuario / contraseña:** los de *Datos de acceso FTP* de IONOS.
- **Carpeta destino:** la raíz pública del dominio.
- Arrastra el contenido de `docs/` al panel remoto.

### Subdominio (opcional)

Si quieres usar `gasapp.tudominio.com`, crea el subdominio en IONOS apuntando
a una subcarpeta (p. ej. `/subdomains/gasapp/`) y sube ahí los archivos.

## URL de la política de privacidad

Una vez subida, la URL será:

```
https://TU_DOMINIO/privacy.html
```

Esa es la URL que tienes que pegar en la **ficha de Google Play Console**
(campo *Política de privacidad*).

> Antes de subir, **rellena `[TU NOMBRE]` y `[TU EMAIL]`** en
> `privacy.html` (líneas marcadas con `[...]`).

## Cambios futuros

El `.htaccess` cachea imágenes/CSS 30 días → si actualizas el icono o estilos
y no ves los cambios, fuerza una recarga (Ctrl+F5) o renombra el archivo
(p. ej. `icon-v2.png`).

## ZIP listo para subir

Como atajo, hay un script que empaqueta todo. Genera `landing-ionos.zip`
en la raíz del repo:

```
./scripts/package-landing.sh
```

(O lo descomprimes y subes a IONOS.)
