import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY") ?: ""
val webClientId: String = localProperties.getProperty("WEB_CLIENT_ID") ?: ""

// Por defecto se usan los IDs DE PRUEBA de AdMob (Google los proporciona).
// Sustituye los reales en local.properties cuando publiques.
val admobAppId: String = localProperties.getProperty("ADMOB_APP_ID")
    ?: "ca-app-pub-3940256099942544~3347511713"
val admobBannerUnit: String = localProperties.getProperty("ADMOB_BANNER_UNIT")
    ?: "ca-app-pub-3940256099942544/6300978111"
val billingProductId: String = localProperties.getProperty("BILLING_REMOVE_ADS_PRODUCT")
    ?: "remove_ads"
val coffeeUrl: String = localProperties.getProperty("COFFEE_URL") ?: ""
// Client ID de Brandfetch (https://brandfetch.com). Vacío = sin logos de red
// (se usa el avatar de letra de marca como respaldo).
val brandfetchClientId: String = localProperties.getProperty("BRANDFETCH_CLIENT_ID") ?: ""

val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystoreProperties.containsKey("storeFile")

android {
    namespace = "com.bpo.gasapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bpo.gasapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 12
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
        buildConfigField("String", "ADMOB_BANNER_UNIT", "\"$admobBannerUnit\"")
        buildConfigField("String", "BILLING_REMOVE_ADS_PRODUCT", "\"$billingProductId\"")
        buildConfigField("String", "COFFEE_URL", "\"$coffeeUrl\"")
        val buildDate = SimpleDateFormat("dd/MM/yyyy").format(Date())
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
        buildConfigField("String", "BRANDFETCH_CLIENT_ID", "\"$brandfetchClientId\"")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else null
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)

    implementation(libs.work.runtime.ktx)
    implementation(libs.coil.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.billing.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
