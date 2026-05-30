package com.bpo.gasapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Sistema de logos de marca LOCAL (sin llamadas de red, ideal para el mapa).
 *
 * Los logos se usan ÚNICAMENTE como identificador visual de la estación. GasApp
 * no está afiliada ni respaldada por ninguna compañía. Las marcas y logotipos
 * pertenecen a sus respectivos propietarios (ver disclaimer en "Sobre la app").
 *
 * Cómo añadir logos:
 *  1. Coloca cada PNG/WebP (preferiblemente transparente, ~96 px) en
 *     `app/src/main/res/drawable/` con nombre `logo_<marca>` (minúsculas, sin
 *     espacios), p. ej. `logo_repsol.png`, `logo_cepsa.png`.
 *  2. Registra la marca normalizada -> recurso en [brandLogoRes] de abajo
 *     (descomenta y completa). La clave debe coincidir con [normalizeBrandKey].
 *
 * Mientras un logo no esté registrado, se muestra el avatar de letra de marca
 * (no infractor) como respaldo, por lo que la app funciona sin ningún logo.
 *
 * IMPORTANTE: usa solo logos de los que tengas derecho de uso (propios,
 * licenciados, o vía un proveedor como Brandfetch respetando sus términos).
 */

/** Marcas conocidas (clave normalizada). El orden importa: primera coincidencia. */
private val KNOWN_BRANDS = listOf(
    "REPSOL", "CEPSA", "BP", "GALP", "SHELL", "PETRONOR", "DISA", "AVIA",
    "CARREFOUR", "ALCAMPO", "BALLENOIL", "PLENOIL", "PETROPRIX", "MEROIL",
    "TGAS", "EROSKI", "PETROPRIX", "Q8", "TAMOIL", "ESERGUI"
)

/** Normaliza el rótulo a una clave de marca conocida (o el texto en mayúsculas). */
fun normalizeBrandKey(brand: String): String {
    val upper = brand.uppercase().trim()
    return KNOWN_BRANDS.firstOrNull { upper.contains(it) } ?: upper
}

/**
 * Dominio público de cada marca, para resolver su logo vía Brandfetch.
 * Son datos públicos (nombres de dominio); no contienen ningún logotipo.
 */
private val brandDomains: Map<String, String> = mapOf(
    "REPSOL" to "repsol.com",
    "CEPSA" to "cepsa.com",
    "BP" to "bp.com",
    "GALP" to "galp.com",
    "SHELL" to "shell.com",
    "PETRONOR" to "petronor.com",
    "DISA" to "disagrupo.es",
    "AVIA" to "avia.es",
    "CARREFOUR" to "carrefour.es",
    "ALCAMPO" to "alcampo.es",
    "BALLENOIL" to "ballenoil.com",
    "PLENOIL" to "plenoil.com",
    "PETROPRIX" to "petroprix.com",
    "MEROIL" to "meroil.com",
    "EROSKI" to "eroski.es",
    "Q8" to "q8.com",
    "TAMOIL" to "tamoil.com"
)

/**
 * Referer enviado a Brandfetch. DEBE coincidir con un dominio autorizado en
 * tu panel de Brandfetch (Logo Link → dominios/referrers permitidos), o el CDN
 * devolverá su página de guidelines en vez del logo.
 */
private const val BRANDFETCH_REFERER = "https://gasapp.cloud"

/**
 * URL del icono de marca en Brandfetch (icono cuadrado, ideal para la píldora).
 * Vacía si no hay client id o dominio conocido.
 */
private fun brandfetchUrl(brand: String): String? {
    val clientId = com.bpo.gasapp.BuildConfig.BRANDFETCH_CLIENT_ID
    if (clientId.isBlank()) return null
    val domain = brandDomains[normalizeBrandKey(brand)] ?: return null
    return "https://cdn.brandfetch.io/$domain/icon?c=$clientId"
}

/**
 * Registro marca -> recurso drawable. Vacío por defecto (se usa el avatar de
 * letra). Descomenta y añade tus recursos cuando incluyas los PNG:
 *
 * internal val brandLogoRes: Map<String, Int> = mapOf(
 *     "REPSOL" to R.drawable.logo_repsol,
 *     "CEPSA"  to R.drawable.logo_cepsa,
 *     "BP"     to R.drawable.logo_bp,
 *     "SHELL"  to R.drawable.logo_shell,
 *     "GALP"   to R.drawable.logo_galp,
 *     "DISA"   to R.drawable.logo_disa,
 * )
 */
internal val brandLogoRes: Map<String, Int> = emptyMap()

/**
 * Logo de la estación: muestra el logo local si existe; si no, el avatar de
 * letra de marca. El logo va en un contenedor blanco discreto para que la
 * identidad visual de GasApp siga siendo la dominante.
 */
@Composable
fun BrandLogo(brand: String, size: Int = 44, modifier: Modifier = Modifier) {
    val localRes = brandLogoRes[normalizeBrandKey(brand)]
    val remoteUrl = remember(brand) { brandfetchUrl(brand) }

    when {
        // 1) Logo local (máxima prioridad, sin red).
        localRes != null -> LogoFrame(size, modifier) {
            Image(
                painter = painterResource(localRes),
                contentDescription = brand,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        // 2) Logo de Brandfetch (cacheado por Coil); avatar mientras carga o si falla.
        //    El Logo Link de Brandfetch exige un Referer de un dominio autorizado
        //    en tu cuenta (protección anti-hotlinking); por eso se envía aquí.
        remoteUrl != null -> coil.compose.SubcomposeAsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(remoteUrl)
                .setHeader("Referer", BRANDFETCH_REFERER)
                .crossfade(true)
                .build(),
            contentDescription = brand,
            contentScale = ContentScale.Fit,
            loading = { BrandAvatar(brand = brand, size = size) },
            error = { BrandAvatar(brand = brand, size = size) },
            success = { state ->
                LogoFrame(size, Modifier) {
                    androidx.compose.foundation.Image(
                        painter = state.painter,
                        contentDescription = brand,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            modifier = modifier.size(size.dp)
        )
        // 3) Respaldo: avatar de letra de marca (no infractor).
        else -> BrandAvatar(brand = brand, size = size, modifier = modifier)
    }
}

/**
 * Descarga (con caché de Coil) el icono de marca como bitmap, para poder
 * dibujarlo de forma SÍNCRONA en los marcadores del mapa (la carga asíncrona
 * con subcomposición crashea el renderizador de marcadores).
 */
suspend fun loadBrandBitmap(
    context: android.content.Context,
    brand: String
): androidx.compose.ui.graphics.ImageBitmap? {
    val url = brandfetchUrl(brand) ?: return null
    val result = coil.Coil.imageLoader(context).execute(
        coil.request.ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(96)
            .build()
    )
    val drawable = (result as? coil.request.SuccessResult)?.drawable ?: return null
    return drawable.toBitmap().asImageBitmap()
}

/**
 * Logo de marca SÍNCRONO (sin red ni subcomposición): dibuja el [bitmap] ya
 * cargado o, si es null, el avatar de letra. Seguro para los marcadores.
 */
@Composable
fun BrandLogoStatic(
    brand: String,
    bitmap: androidx.compose.ui.graphics.ImageBitmap?,
    size: Int = 34,
    modifier: Modifier = Modifier
) {
    if (bitmap != null) {
        LogoFrame(size, modifier) {
            Image(
                bitmap = bitmap,
                contentDescription = brand,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        BrandAvatar(brand = brand, size = size, modifier = modifier)
    }
}

/** Marco cuadrado blanco; el logo ocupa todo el espacio (como las referencias). */
@Composable
private fun LogoFrame(size: Int, modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) { content() }
}
