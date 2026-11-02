import java.util.Properties
plugins { id("com.google.gms.google-services"); id("com.android.application"); id("org.jetbrains.kotlin.android") }
val signingProps = Properties().apply { val source = rootProject.file("signing.properties"); if (source.exists()) source.inputStream().use { load(it) } }
android {
    namespace = "com.ad.towerflow"
    compileSdk = 35
    defaultConfig { applicationId = "com.ad.towerflow"; minSdk = 26; targetSdk = 35; versionCode = 11; versionName = "0.5.5" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
        signingConfigs {
        create("production") {
            val ks = if (signingProps.isNotEmpty() && signingProps.getProperty("storeFile") != null) file(signingProps.getProperty("storeFile")) else rootProject.file("towerflow-release.jks")
            if (ks.exists()) {
                storeFile = ks
                storePassword = signingProps.getProperty("storePassword", "towerflow2026")
                keyAlias = signingProps.getProperty("keyAlias", "towerflow")
                keyPassword = signingProps.getProperty("keyPassword", "towerflow2026")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("production")
        }
    }
}

dependencies { implementation(platform("com.google.firebase:firebase-bom:35.1.0")); implementation("com.google.firebase:firebase-messaging") }
