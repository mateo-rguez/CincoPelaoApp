plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)

    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.cincopelao"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.mateorguez.cincopelao"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

}

// Room exporta aquí un JSON con el esquema de cada versión de la BD.
// Se versiona en git para poder escribir y probar migraciones.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    //Compose
    implementation("androidx.compose.foundation:foundation:1.6.7")

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.material.icons.extended)
    implementation(libs.places)
    implementation(libs.androidx.work.runtime.ktx)

    // ROOM
    val room_version = "2.7.2"
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    //HILT
    implementation("com.google.dagger:hilt-android:2.56.2")
    ksp("com.google.dagger:hilt-android-compiler:2.56.2")

    //HILT en Compose
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    //DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    //Calendario de kizitonwose
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("com.kizitonwose.calendar:compose:2.6.0")

    //Timeline de JetLime
    implementation("io.github.pushpalroy:jetlime:3.0.1")

    //BottomNavBar de Exyte
    implementation("com.exyte:animated-navigation-bar:1.0.0")

    //PermissionX
    implementation("com.guolindev.permissionx:permissionx:1.8.1")

    //Serializacion en Kotlin
    implementation(libs.serialization.json)

    //Material2
    implementation(libs.androidx.material)

    //Gestión de SplashScreens
    implementation(libs.androidx.core.splashscreen)


    //-------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}