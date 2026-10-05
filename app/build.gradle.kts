plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.hanooot.notes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hanooot.notes"
        minSdk = 26                 // AlarmManager exact alarms + notification channels
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Google Sign-In checks the certificate the APK was signed with against
    // the fingerprint registered in the Cloud console. Gradle's generated
    // debug keystore is different on every machine and on every CI run, which
    // would break sign-in with each new build, so a fixed key is committed
    // and used for both build types.
    // The password can come from the environment, so swapping in a private
    // key later needs a repo secret and no code change.
    val keystoreFile = rootProject.file("keystore/notes.keystore")
    val keystorePassword = System.getenv("SIGNING_PASSWORD") ?: "hanooot"

    signingConfigs {
        getByName("debug") {
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = keystorePassword
                keyAlias = "notes"
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    // Local storage
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Account picker, consent and OAuth tokens. The Drive calls themselves go
    // over plain HTTPS rather than through google-api-client, which would drag
    // in a large and brittle dependency tree for two REST endpoints.
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
