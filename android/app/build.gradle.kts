import java.util.Properties
plugins { id("com.google.gms.google-services"); id("com.android.application"); id("org.jetbrains.kotlin.android") }
val signingProps = Properties().apply { val source = rootProject.file("signing.properties"); if (source.exists()) source.inputStream().use { load(it) } }
android {
    namespace = "com.ad.towerflow"
    compileSdk = 35
    defaultConfig { applicationId = "com.ad.towerflow"; minSdk = 26; targetSdk = 35; versionCode = 5; versionName = "0.4.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs { if (signingProps.isNotEmpty()) create("production") { storeFile = file(signingProps.getProperty("storeFile")); storePassword = signingProps.getProperty("storePassword"); keyAlias = signingProps.getProperty("keyAlias"); keyPassword = signingProps.getProperty("keyPassword") } }
    buildTypes { release { isMinifyEnabled = false; if (signingProps.isNotEmpty()) signingConfig = signingConfigs.getByName("production") } }
}

dependencies { implementation(platform("com.google.firebase:firebase-bom:35.1.0")); implementation("com.google.firebase:firebase-messaging") }
