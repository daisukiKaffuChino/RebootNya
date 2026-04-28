plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.aboutLibraries)
}

android {
    namespace = "github.daisukikaffuchino.rebootnya"
    compileSdk = 37

    defaultConfig {
        applicationId = "github.daisukikaffuchino.rebootnya"
        minSdk = 28
        targetSdk = 37
        versionCode = 260428
        versionName = "1.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        aidl = true
    }

    @Suppress("UnstableApiUsage")
    androidResources {
        generateLocaleConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    //noinspection WrongGradleMethod
    aboutLibraries {
        export {
            outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        }
    }
}

configurations.configureEach {
    exclude(group = "androidx.appcompat", module = "appcompat")
}

dependencies {
    implementation(libs.dev.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.preference)

    implementation(libs.core)
    implementation(libs.core.ktx)
    implementation(libs.provider)
    implementation(libs.api)
    implementation(libs.material.preference)
    implementation(libs.borderview)
    implementation(libs.recyclerview.ktx)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.view)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
