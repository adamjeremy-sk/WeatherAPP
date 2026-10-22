import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val meteoGateApiKey: String = localProperties.getProperty("METEOGATE_API_KEY")
    ?: (project.findProperty("METEOGATE_API_KEY") as String? ?: "")

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.weatherapp.libre"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.weatherapp.libre"
        minSdk = 33
        targetSdk = 37
        versionCode = 25
        versionName = "0.6.5"

        buildConfigField("String", "METEOGATE_API_KEY", "\"$meteoGateApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
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

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":theme-engine"))
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.app.update.ktx)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    testImplementation(libs.junit)
    testImplementation("xmlpull:xmlpull:1.1.3.1")
    testImplementation("xpp3:xpp3:1.1.4c")
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
