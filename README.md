# GasApp — Gasolina barata España

Aplicación Android para encontrar las gasolineras más baratas de España en
tiempo real, comparar precios de combustible, navegar hasta ellas y llevar un
control privado de tus repostajes.

Los precios provienen de la **API oficial y gratuita del Gobierno**
(Ministerio para la Transición Ecológica), que publica todas las estaciones de
servicio de España con sus precios actualizados varias veces al día.

## Funcionalidades

### Buscar y ver precios
- **Inicio tipo dashboard** con la gasolinera más barata cerca de ti.
- **Lista** ordenada por precio/distancia/valor, con **buscador** por marca o ciudad.
- **Heatmap de precio** (verde/ámbar/rojo según la media de la zona) y media mostrada.
- **Mapa** con clústeres y **carteles de precio**; al tocar un marcador, panel
  inferior con info rápida, compartir, "Ir" y favorito.
- **Selector de combustible**: Gasolina 95, Gasolina 98, Diésel, Diésel Premium.
- **Filtros**: distancia (1/5/10/25 km), marca y "abiertas ahora".
- **Ubicación y distancia** a cada gasolinera; **pull-to-refresh**.
- **Detalle** con foto (Street View), todos los precios, historial de precios,
  compartir y "Ir allí" (Google Maps con fallback web).

### Cuenta y personalización
- **Login** con email/contraseña y **Google**; perfil con avatar y edición de nombre.
- **Favoritas** y **combustible por defecto** sincronizados en Firestore.
- **Onboarding** inicial y **Ajustes**: tema claro/oscuro, paleta de marca o
  Material You, combustible por defecto, alertas de precio.

### Inteligencia y herramientas
- **Notificaciones** de bajada de precio en favoritas y **alertas por umbral**.
- **Refresco automático** en segundo plano (WorkManager).
- **Comparador de depósito lleno**, **planificador de ruta** (corredor),
  **modo ahorro** (ahorro neto por desvío) y **modo coche**.
- **Estadísticas privadas**: gasto mensual con gráfico, consumo real
  (L/100 km), coste/km, **OCR del ticket** (ML Kit) y **exportar a CSV**.

### Sistema
- **Widget** con las favoritas más baratas, **atajo de app** y avatar de marca
  por gasolinera.

## Arquitectura

MVVM + Jetpack Compose, con separación en capas:

```
domain/   modelos, repositorios (interfaces), utilidades de negocio
data/     API (Retrofit), cache (Room), localización, ajustes (DataStore),
          remoto (Firestore), mapeadores, repositorios
ui/       pantallas Compose + ViewModels (StateFlow)
di/       módulos Hilt (network, database, location, settings, firebase, repository)
work/     WorkManager · widget/ Glance · notifications/
```

**Stack principal:** Kotlin · Jetpack Compose (Material 3 / Material You) · Hilt ·
Retrofit + kotlinx.serialization · Room · DataStore · Coroutines/Flow ·
Navigation Compose · Google Maps Compose · FusedLocation · WorkManager · Glance ·
ML Kit Text Recognition · Credential Manager · Firebase (Auth + Firestore).

## Configuración para compilar

El proyecto necesita archivos locales que **no** se versionan:

1. **`local.properties`**:

   ```
   MAPS_API_KEY=tu_clave_de_google_maps
   WEB_CLIENT_ID=xxxxx.apps.googleusercontent.com   # opcional, solo para login con Google
   ```

   - `MAPS_API_KEY`: Google Cloud Console → habilita *Maps SDK for Android*
     (y *Street View Static API* para la foto del detalle).
   - `WEB_CLIENT_ID`: ID de cliente web OAuth de Firebase (tras habilitar Google
     en Authentication). Sin él, el login con Google queda desactivado.

2. **`app/google-services.json`** — descárgalo de tu proyecto en
   [Firebase Console](https://console.firebase.google.com) (app Android con
   package `com.bpo.gasapp`). Habilita **Authentication** (Email + Google) y
   **Firestore**.

Para login con Google en dispositivo, añade tu **huella SHA-1** a la app Android
en Firebase y vuelve a descargar `google-services.json`.

Después, abre el proyecto en Android Studio y ejecuta, o desde la terminal:

```
./gradlew :app:assembleDebug
```

### Reglas de Firestore sugeridas

```
match /users/{uid} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
  match /favorites/{doc} {
    allow read, write: if request.auth != null && request.auth.uid == uid;
  }
}
```

## Datos del proyecto

- **Package:** `com.bpo.gasapp`
- **minSdk:** 24 · **targetSdk:** 35
