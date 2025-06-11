import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.firebase.crashlytics)
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://jitpack.io") }
}

val githubProperties = Properties().apply {
    rootProject.file("github.properties").inputStream().use { stream ->
        this.load(stream)
    }
}

android {
    namespace = "com.geckour.nowplaying4droid"

    compileSdk = 35

    buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "com.geckour.nowplaying4droid"
        targetSdk = 35
        minSdk = 24
        versionCode = 151
        versionName = "3.4.10"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val filesAuthorityValue = applicationId + ".files"
        manifestPlaceholders += mapOf("filesAuthority" to filesAuthorityValue)
        buildConfigField("String", "FILES_AUTHORITY", "\"${filesAuthorityValue}\"")
    }

    applicationVariants.all {
        val keyNameLastFm = "LAST_FM_API_KEY"
        buildConfigField("String", keyNameLastFm, getKey(keyNameLastFm))
        val keyNameTwitterConsumerKey = "TWITTER_CONSUMER_KEY"
        buildConfigField("String", keyNameTwitterConsumerKey, getKey(keyNameTwitterConsumerKey))
        val keyNameTwitterConsumerSecret = "TWITTER_CONSUMER_SECRET"
        buildConfigField("String", keyNameTwitterConsumerSecret, getKey(keyNameTwitterConsumerSecret))
        val keyNameSkuDonate = "SKU_KEY_DONATE"
        buildConfigField("String", keyNameSkuDonate, getKey(keyNameSkuDonate))
        val keyNameMastodonInstancesSecret = "MASTODON_INSTANCES_SECRET"
        buildConfigField("String", keyNameMastodonInstancesSecret, getKey(keyNameMastodonInstancesSecret))
        val keyNameSpotifyClientId = "SPOTIFY_CLIENT_ID"
        buildConfigField("String", keyNameSpotifyClientId, getKey(keyNameSpotifyClientId))
        val keyNameSpotifyClientSecret = "SPOTIFY_CLIENT_SECRET"
        buildConfigField("String", keyNameSpotifyClientSecret, getKey(keyNameSpotifyClientSecret))
        val keyNameGoogleCloudApiKey = "GOOGLE_CLOUD_API_KEY"
        buildConfigField("String", keyNameGoogleCloudApiKey, getKey(keyNameGoogleCloudApiKey))
        val keyNameAppleMusicKitSecretKey = "APPLE_MUSIC_KIT_SECRET_KEY"
        buildConfigField("String", keyNameAppleMusicKitSecretKey, getKey(keyNameAppleMusicKitSecretKey))
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${project.rootDir}/app/signing/debug.keystore")
            storePassword = "android"
            keyAlias = "AndroidDebugKey"
            keyPassword = "android"
        }
        create("release") {
            val releaseSettingGradleFile = File("${project.rootDir}/app/signing/release.gradle")
            if (releaseSettingGradleFile.exists())
                apply(from = releaseSettingGradleFile, to = android)
            else
                throw GradleException("Missing ${releaseSettingGradleFile.absolutePath} . Generate the file by copying and modifying ${project.rootDir}/app/signing/release.gradle.sample .")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    packaging {
        resources.excludes.add("META-INF/*")
    }
    lint {
        checkReleaseBuilds = false
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/geckour/NowPlayingSubjectBuilder")

            credentials {
                username = githubProperties["gpr.usr"] as String?
                password = githubProperties["gpr.key"] as String?
            }
        }
    }
}

fun getKey(keyName: String): String {
    val props = Properties().apply {
        rootProject.file("secret.properties").inputStream().use { stream ->
            this.load(stream)
        }
    }
    return "\"${props[keyName]}\""
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to arrayOf("*.jar", "*.aar"))))
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.material)
    implementation(libs.appcompat)
    implementation(libs.legacy.support.v4)
    implementation(libs.legacy.support.v13)
    implementation(libs.browser)
    implementation(libs.constraintlayout)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.analytics.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.runner)
    testImplementation(libs.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.espresso.core)

    // Logging
    implementation(libs.timber)

    // DI
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    testImplementation(libs.koin.test)

    // Logging
    implementation(libs.timber)

    // KTX
    implementation(libs.core.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)

    // JSON
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)

    // Permission
    implementation(libs.ktx)

    // ViewModel
    implementation(libs.lifecycle.viewmodel.compose)

    // Billing
    implementation(libs.billing.ktx)

    // Preference
    implementation(libs.preference.ktx)

    // Palette API
    implementation(libs.palette.ktx)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Image processing
    implementation(libs.coil)

    // Wear
    implementation(libs.play.services.wearable)

    // Mastodon
    implementation(libs.mastodon4j)

    // Compose
    implementation(libs.ui)
    implementation(libs.androidx.material)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.reorderable)

    // Subject
    implementation(libs.now.playing.subject.builder)

    // JWT
    implementation(libs.java.jwt)
}

apply(plugin = "com.google.gms.google-services")