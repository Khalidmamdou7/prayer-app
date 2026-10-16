import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Release signing comes from keystore.properties (local, git-ignored) or, on CI, from the
 * ANDROID_KEYSTORE_* environment variables. Without either, release builds are unsigned.
 */
val signing: Map<String, String>? = run {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        Properties().apply { file.inputStream().use(::load) }.let { p ->
            mapOf(
                "storeFile" to rootProject.file(p.getProperty("storeFile")).path,
                "storePassword" to p.getProperty("storePassword"),
                "keyAlias" to p.getProperty("keyAlias"),
                "keyPassword" to p.getProperty("keyPassword"),
            )
        }
    } else {
        System.getenv("ANDROID_KEYSTORE_PATH")?.let { path ->
            mapOf(
                "storeFile" to path,
                "storePassword" to System.getenv("ANDROID_KEYSTORE_PASSWORD").orEmpty(),
                "keyAlias" to System.getenv("ANDROID_KEY_ALIAS").orEmpty(),
                "keyPassword" to System.getenv("ANDROID_KEY_PASSWORD").orEmpty(),
            )
        }
    }
}

/** Single source of truth: version.properties. A tag v1.2.3 must match versionName=1.2.3. */
val appVersionName: String = Properties().apply {
    rootProject.file("version.properties").inputStream().use(::load)
}.getProperty("versionName")

/** 1.2.3 → 10203. Always increases with the version, which Android and Play require for updates. */
val appVersionCode: Int = appVersionName.split(".").map(String::toInt).let { parts ->
    require(parts.size == 3 && parts.all { it in 0..99 }) { "versionName must look like 1.2.3 (each part 0–99)" }
    parts[0] * 10000 + parts[1] * 100 + parts[2]
}

android {
    namespace = "com.azan.prayer"
    compileSdk = 36

    defaultConfig {
        // The Play Store identity: it can never change once the app is published.
        applicationId = "io.github.khalidmamdou7.azan"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        // A fixed, committed debug key (the standard public "android" credentials), so every
        // debug build, including the APKs from GitHub Actions, installs over the previous one.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (signing != null) {
            create("release") {
                storeFile = file(signing.getValue("storeFile"))
                storePassword = signing["storePassword"]
                keyAlias = signing["keyAlias"]
                keyPassword = signing["keyPassword"]
            }
        }
    }

    buildTypes {
        debug {
            // Lets a test build sit next to the Play Store version on the same phone.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    }
    bundle {
        // Keep every language in every install: the app lets people switch language in-app,
        // and Play would otherwise only deliver the phone's language.
        language {
            enableSplit = false
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Screenshot tests only run with -Pscreenshots (they need Robolectric's large Android jars).
                it.systemProperty("screenshots", project.findProperty("screenshots") ?: "false")
                it.systemProperty("roborazzi.test.record", "true")
                project.findProperty("robolectricRepo")?.let { repo ->
                    it.systemProperty("robolectric.dependency.repo.url", repo)
                }
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    testImplementation("junit:junit:4.13.2")
    // Android's org.json is only a stub in local unit tests.
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.39.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.39.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
