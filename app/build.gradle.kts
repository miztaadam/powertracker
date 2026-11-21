plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.powertracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.powertracker"
        minSdk = 26
        targetSdk = 37
        // Bump together with "version" in web/package.json for each update; a web test catches a mismatch.
        versionCode = 28
        versionName = "1.17"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // BuildConfig.VERSION_NAME: the version line at the bottom of Settings.
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Robolectric needs merged resources/manifest.
            isIncludeAndroidResources = true
            // Robolectric (SDK 37) reaches into JDK internals; JDK 17+ needs these opened.
            all {
                // Mockup PNGs in docs/design/screens are only rewritten with -Pscreenshots.update=true.
                it.systemProperty("screenshots.update", (project.findProperty("screenshots.update") ?: "false").toString())
                // web/src/backup/__fixtures__/android-v2.json is only rewritten with -Pfixtures.update=true (the v1 fixtures are frozen).
                it.systemProperty("fixtures.update", (project.findProperty("fixtures.update") ?: "false").toString())
                it.jvmArgs(
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                )
            }
        }
    }
    sourceSets {
        // Exported Room schemas, read by MigrationTestHelper under Robolectric (debug assets reach unit tests; release is unaffected).
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
}

// Room schemas are exported here and committed (used by migration tests).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.biometric)
    // BiometricPrompt needs a FragmentActivity; biometric alone pulls in a fragment library too old for the current activity library.
    implementation(libs.androidx.fragment)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // Espresso 3.5 (pulled in by compose ui-test) cannot inject input on Robolectric SDK 37.
    testImplementation(libs.androidx.test.espresso.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
