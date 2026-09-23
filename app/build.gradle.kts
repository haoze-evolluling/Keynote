import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile: File? = listOf(
    rootProject.file("Keynote-keystore/keystore.properties"),
    rootProject.file("keystore/keystore.properties"),
    rootProject.file("keystore.properties")
).firstOrNull { it.exists() }

val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile != null && keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val releaseStoreFilePath: String? = keystoreProperties.getProperty("storeFile")
    ?: System.getenv("KEYNOTE_KEYSTORE_FILE")
val releaseStorePassword: String? = keystoreProperties.getProperty("storePassword")
    ?: System.getenv("KEYNOTE_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = keystoreProperties.getProperty("keyAlias")
    ?: System.getenv("KEYNOTE_KEY_ALIAS")
val releaseKeyPassword: String? = keystoreProperties.getProperty("keyPassword")
    ?: System.getenv("KEYNOTE_KEY_PASSWORD")

val releaseKeystoreFile: File? = releaseStoreFilePath?.let { path ->
    val fromPropDir = keystorePropertiesFile?.parentFile?.let { File(it, path) }
    val fromRoot = rootProject.file(path)
    val fromApp = file(path)
    when {
        fromPropDir != null && fromPropDir.exists() -> fromPropDir
        fromRoot.exists() -> fromRoot
        fromApp.exists() -> fromApp
        else -> null
    }
}

val signDebugWithRelease = project.findProperty("signDebugWithRelease") in listOf("true", "1", "")

android {
    namespace = "com.haoze.keynote"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.haoze.keynote"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"
    }

    signingConfigs {
        if (releaseKeystoreFile != null && releaseKeystoreFile.exists() &&
            !releaseStorePassword.isNullOrBlank() && !releaseKeyAlias.isNullOrBlank()
        ) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword ?: releaseStorePassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            if (signDebugWithRelease) {
                signingConfigs.findByName("release")?.let { releaseSigningConfig ->
                    signingConfig = releaseSigningConfig
                }
            }
        }
        release {
            isMinifyEnabled = true
            signingConfigs.findByName("release")?.let { releaseSigningConfig ->
                signingConfig = releaseSigningConfig
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    androidResources {
        localeFilters += listOf("zh", "en")
    }
}

val apkVersionName = android.defaultConfig.versionName ?: "unknown"

listOf("debug", "release").forEach { buildType ->
    val capitalizedBuildType = buildType.replaceFirstChar { it.uppercase() }
    val apkOutputDirectory = layout.buildDirectory.dir("outputs/apk/$buildType")
    val versionedApkOutputDirectory = layout.buildDirectory.dir("outputs/apk/versioned/$buildType")

    val copyApkTask = tasks.register<Copy>("copy${capitalizedBuildType}ApkWithVersion") {
        dependsOn("assemble$capitalizedBuildType")
        from(apkOutputDirectory)
        include("app-$buildType.apk")
        rename("app-$buildType.apk", "Keynote-$buildType-v$apkVersionName.apk")
        into(versionedApkOutputDirectory)
    }

    tasks.configureEach {
        if (name == "assemble$capitalizedBuildType") {
            finalizedBy(copyApkTask)
        }
    }
}

dependencies {
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.gson)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation("com.github.jeziellago:compose-markdown:0.5.2")
}
