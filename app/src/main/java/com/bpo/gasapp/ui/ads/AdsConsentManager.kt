package com.bpo.gasapp.ui.ads

import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Gestiona el consentimiento de privacidad (Google UMP) requerido para mostrar
 * anuncios en el EEE/Reino Unido y solo inicializa AdMob cuando se puede pedir
 * anuncios de forma conforme al RGPD.
 *
 * Flujo recomendado por Google:
 * 1. Solicita la información de consentimiento actualizada.
 * 2. Muestra el formulario de consentimiento si es necesario.
 * 3. Inicializa MobileAds cuando `canRequestAds()` es verdadero.
 */
object AdsConsentManager {

    private val adsInitialized = AtomicBoolean(false)

    /** Llamar una vez al arrancar, desde una Activity. */
    fun gatherConsentAndInitialize(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Información actualizada: muestra el formulario si hace falta.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (consentInformation.canRequestAds()) initializeMobileAds(activity)
                }
            },
            {
                // Si falla la actualización, intenta con el consentimiento ya disponible.
                if (consentInformation.canRequestAds()) initializeMobileAds(activity)
            }
        )

        // Si ya había consentimiento de una sesión anterior, inicializa de inmediato.
        if (consentInformation.canRequestAds()) initializeMobileAds(activity)
    }

    private fun initializeMobileAds(activity: Activity) {
        if (adsInitialized.getAndSet(true)) return
        MobileAds.initialize(activity) {}
    }
}
