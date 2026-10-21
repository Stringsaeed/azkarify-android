import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val uploadPropertiesFile = rootProject.file("keystore.properties")
val uploadProperties = Properties().apply {
    if (uploadPropertiesFile.isFile) uploadPropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "sh.saeed.azkarify"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }
    defaultConfig {
        applicationId = "sh.saeed.azkarify"
        minSdk = 26
        targetSdk = 37
        versionCode = 20300
        versionName = "2.3.0"
        buildConfigField("long", "CATALOG_CLOUD_PROJECT_NUMBER", "97729473680L")
        buildConfigField("String", "REVENUECAT_API_KEY", "\"goog_gXukEcpOrbSvmesgwvvQkHSUCQW\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val uploadSigning =
        if (uploadPropertiesFile.isFile) {
            signingConfigs.create("upload") {
                storeFile = rootProject.file(uploadProperties.getProperty("storeFile"))
                storePassword = uploadProperties.getProperty("storePassword")
                keyAlias = uploadProperties.getProperty("keyAlias")
                keyPassword = uploadProperties.getProperty("keyPassword")
            }
        } else null
    buildFeatures { compose = true; buildConfig = true }
    bundle { language { enableSplit = false } }
    buildTypes {
        debug {
            val token = providers.environmentVariable("AZKARIFY_CATALOG_DEV_TOKEN").orElse("").get()
            val escapedToken = token.replace("\\", "\\\\").replace("\"", "\\\"")
            buildConfigField("String", "CATALOG_DEV_TOKEN", "\"$escapedToken\"")
            buildConfigField("String", "CATALOG_BASE_URL", "\"http://127.0.0.1:8787/v1\"")
        }
        release {
            if (uploadSigning != null) signingConfig = uploadSigning
            buildConfigField("String", "CATALOG_DEV_TOKEN", "\"\"")
            buildConfigField("String", "CATALOG_BASE_URL", "\"https://staging.api.saeed.sh/v1\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric reaches into FileDescriptor internals on JDK 21.
        unitTests.all { it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED", "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.compose.navigation.suite)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.preview)
    debugImplementation(libs.androidx.compose.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization)
    implementation(libs.adhan)
    implementation(libs.revenuecat.ui)
    implementation(libs.play.integrity)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.test)
    debugImplementation(libs.androidx.compose.test.manifest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
}
