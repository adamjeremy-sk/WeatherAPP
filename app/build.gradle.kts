import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val meteoGateApiKey: String = localProperties.getProperty("METEOGATE_API_KEY")
    ?: (project.findProperty("METEOGATE_API_KEY") as String? ?: "")

val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE") ?: System.getenv("RELEASE_STORE_FILE")
val releaseStorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: System.getenv("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS") ?: System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: System.getenv("RELEASE_KEY_PASSWORD")

/**
 * RELEASE SIGNING FALLBACK (Fixlist M-2: Resolved by Design)
 *
 * If the release keystore properties are not provided (e.g., in CI environments
 * or by collaborators building from source), the release build intentionally
 * falls back to the debug signing configuration.
 *
 * Rationale: Hard-failing the build when the keystore is missing breaks automated
 * CI pipelines (which need to run `assembleRelease` to verify R8/ProGuard rules)
 * and prevents open-source contributors from generating release-variant APKs.
 *
 * WARNING: A release build signed with the debug key CANNOT be uploaded to the
 * Google Play Console. To build a true production release, ensure the keystore
 * properties are populated in `local.properties` or passed via environment variables.
 */
val hasReleaseKeystore = releaseStoreFile != null && releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null

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
        versionCode = 26
        versionName = "0.7.0"

        buildConfigField("String", "METEOGATE_API_KEY", "\"$meteoGateApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
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
            isIncludeAndroidResources = true
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
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.app.update.ktx)
    debugImplementation(libs.leakcanary)

    testImplementation(libs.junit)
    testImplementation(libs.xmlpull)
    testImplementation(libs.xpp3)
    testImplementation(libs.kxml2)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

abstract class VerifyEdgeToEdgeTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val srcDir: DirectoryProperty

    @TaskAction
    fun verify() {
        val bannedKotlin = listOf(
            "statusBarColor",
            "navigationBarColor",
            "setStatusBarColor",
            "setNavigationBarColor",
            "systemUiVisibility",
            "SYSTEM_UI_FLAG",
            "FLAG_TRANSLUCENT_STATUS",
            "FLAG_TRANSLUCENT_NAVIGATION",
            "FLAG_FULLSCREEN"
        )
        val bannedXml = listOf(
            "android:statusBarColor",
            "android:navigationBarColor",
            "android:windowLightStatusBar",
            "android:windowLightNavigationBar",
            "android:windowTranslucentStatus",
            "android:windowTranslucentNavigation"
        )

        var violations = 0
        val dir = srcDir.get().asFile

        if (dir.exists()) {
            dir.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }.forEach { file ->
                val bannedList = if (file.extension == "kt") bannedKotlin else bannedXml
                file.useLines { lines ->
                    lines.forEachIndexed { index, line ->
                        bannedList.forEach { banned ->
                            if (line.contains(banned)) {
                                logger.error("Edge-to-Edge Violation [${file.path}:${index + 1}]: '$line'")
                                violations++
                            }
                        }
                    }
                }
            }
        }

        if (violations > 0) {
            throw GradleException("Edge-to-Edge compliance check failed with $violations violation(s).")
        } else {
            logger.lifecycle("Edge-to-Edge compliance check passed cleanly. 0 violations found.")
        }
    }
}

tasks.register<VerifyEdgeToEdgeTask>("verifyEdgeToEdge") {
    group = "verification"
    description = "Enforces Edge-to-Edge compliance by scanning source code for forbidden deprecated APIs."
    srcDir.set(layout.projectDirectory.dir("src/main"))
}

tasks.matching { it.name == "assembleDebug" || it.name == "assembleRelease" }.configureEach {
    dependsOn("verifyEdgeToEdge")
}
