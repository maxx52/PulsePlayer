import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val signingPropertiesFile = rootProject.file("keystore.properties")
val signingProperties = Properties().apply {
    if (signingPropertiesFile.exists()) {
        signingPropertiesFile.inputStream().use { input -> load(input) }
    }
}
val releaseBannerId = providers.gradleProperty("PULSE_YANDEX_BANNER_ID").orElse("").get()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "ru.maxx52.pulseplayer"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.maxx52.pulseplayer"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "0.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("store") {
            dimension = "distribution"
            buildConfigField("boolean", "ADS_ENABLED", "true")
        }
        create("personal") {
            dimension = "distribution"
            applicationIdSuffix = ".personal"
            versionNameSuffix = "-personal"
            resValue("string", "app_name", "Pulse Personal")
            buildConfigField("boolean", "ADS_ENABLED", "false")
        }
    }

    signingConfigs {
        if (signingPropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "YANDEX_BANNER_ID", "\"demo-banner-yandex\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
            buildConfigField("String", "YANDEX_BANNER_ID", "\"$releaseBannerId\"")
            if (signingPropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        resValues = true
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-session:1.11.0")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    add("storeImplementation", "com.yandex.android:mobileads:8.5.0")
}

val verifyReleaseConfiguration by tasks.registering {
    group = "verification"
    description = "Проверяет рекламу и подпись перед release-сборкой."
    doLast {
        check(releaseBannerId.isNotBlank()) {
            "Добавьте PULSE_YANDEX_BANNER_ID в пользовательский gradle.properties."
        }
        check(signingPropertiesFile.exists()) {
            "Создайте keystore.properties по примеру keystore.properties.example."
        }
    }
}

tasks.matching { it.name == "preStoreReleaseBuild" }.configureEach {
    dependsOn(verifyReleaseConfiguration)
}
