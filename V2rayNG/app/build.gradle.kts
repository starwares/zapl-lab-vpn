plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.jaredsburrows.license")
}

// Commit the APK is built from. About shows it so a published binary can be matched to its source
// revision; builds from a source archive without Git history report "unknown".
val sourceCommit: String = runCatching {
    providers.exec {
        commandLine("git", "rev-parse", "--short=12", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrDefault("").ifEmpty { "unknown" }

/**
 * Turns the repository's legal texts into WebView pages under assets/legal/, so every APK carries
 * the GPL-3.0 text and the third-party notices of the exact source revision it was built from.
 */
abstract class GenerateLegalAssetsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val licenseFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val noticeFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val licenseTexts: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val legalDir = outputDirectory.get().dir("legal").asFile
        legalDir.deleteRecursively()
        legalDir.mkdirs()
        legalDir.resolve("license.html").writeText(page(listOf(licenseFile.get().asFile)))
        legalDir.resolve("notices.html").writeText(
            page(noticeFiles.files.toList() + licenseTexts.files.sortedBy { it.name })
        )
    }

    private fun page(files: List<File>): String = buildString {
        append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        append("<style>body{margin:0}pre{white-space:pre-wrap;word-wrap:break-word;font-size:12px}")
        append("h2{font-family:sans-serif;font-size:14px}</style></head><body>")
        files.forEach { file ->
            append("<h2>").append(escape(file.name)).append("</h2><pre>")
            append(escape(file.readText())).append("</pre>")
        }
        append("</body></html>")
    }

    private fun escape(text: String) =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

val generateLegalAssets = tasks.register<GenerateLegalAssetsTask>("generateLegalAssets") {
    val repositoryRoot = rootProject.layout.projectDirectory.dir("..")
    licenseFile.set(repositoryRoot.file("LICENSE"))
    noticeFiles.from(repositoryRoot.file("NOTICE"), repositoryRoot.file("THIRD_PARTY_NOTICES.md"))
    licenseTexts.from(repositoryRoot.dir("third_party/licenses").asFileTree.matching { include("*.txt") })
}

android {
    namespace = "com.v2ray.ang"
    compileSdk = 37

    defaultConfig {
        // Application ID of this distribution, distinct from upstream v2rayNG. The Kotlin namespace
        // stays com.v2ray.ang, so JNI symbol names and upstream merges are unaffected.
        applicationId = "store.zapl.lab.vpn"
        minSdk = 24
        targetSdk = 37
        versionCode = 748
        versionName = "2.3.8"

        // GitHub owner/name of this distribution: source code offer, issues, privacy policy and the
        // release channel read by the in-app update check.
        buildConfigField("String", "SOURCE_REPOSITORY", "\"starwares/zapl-lab-vpn\"")
        buildConfigField("String", "SOURCE_COMMIT", "\"$sourceCommit\"")
        // In-app update check against the releases of SOURCE_REPOSITORY. Off until that release
        // channel is published; store builds must keep it off (store update policies).
        buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "false")

        val abiFilterList = (properties["ABI_FILTERS"] as? String)?.split(';')
        splits {
            abi {
                isEnable = true
                reset()
                if (!abiFilterList.isNullOrEmpty()) {
                    include(*abiFilterList.toTypedArray())
                } else {
                    include(
                        "arm64-v8a",
                        "armeabi-v7a",
                        "x86_64",
                        "x86"
                    )
                }
                isUniversalApk = abiFilterList.isNullOrEmpty()
            }
        }

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

    flavorDimensions.add("distribution")
    productFlavors {
        create("fdroid") {
            dimension = "distribution"
            applicationIdSuffix = ".fdroid"
            buildConfigField("String", "DISTRIBUTION", "\"F-Droid\"")
        }
        create("playstore") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"Play Store\"")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    applicationVariants.all {
        val variant = this
        val isFdroid = variant.productFlavors.any { it.name == "fdroid" }
        if (isFdroid) {
            val versionCodes =
                mapOf(
                    "armeabi-v7a" to 2, "arm64-v8a" to 1, "x86" to 4, "x86_64" to 3, "universal" to 0
                )

            variant.outputs
                .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
                .forEach { output ->
                    val abi = output.getFilter("ABI") ?: "universal"
                    output.outputFileName = "zapl-lab-vpn_${variant.versionName}-fdroid_${abi}.apk"
                    if (versionCodes.containsKey(abi)) {
                        output.versionCodeOverride =
                            (100 * variant.versionCode + versionCodes[abi]!!).plus(5000000)
                    } else {
                        return@forEach
                    }
                }
        } else {
            val versionCodes =
                mapOf("armeabi-v7a" to 4, "arm64-v8a" to 4, "x86" to 4, "x86_64" to 4, "universal" to 4)

            variant.outputs
                .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
                .forEach { output ->
                    val abi = if (output.getFilter("ABI") != null)
                        output.getFilter("ABI")
                    else
                        "universal"

                    output.outputFileName = "zapl-lab-vpn_${variant.versionName}_${abi}.apk"
                    if (versionCodes.containsKey(abi)) {
                        output.versionCodeOverride =
                            (1000000 * versionCodes[abi]!!).plus(variant.versionCode)
                    } else {
                        return@forEach
                    }
                }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf(
            "en",
            "zh-rCN",
            "zh-rTW",
            "vi",
            "ru",
            "fa",
            "ar",
            "bn",
            "bqi-rIR"
        )
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            generateLegalAssets,
            GenerateLegalAssetsTask::outputDirectory
        )
    }
}

dependencies {
    // Core Libraries
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    // AndroidX Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Compose Libraries
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // Data and Storage Libraries
    implementation(libs.mmkv.static)
    implementation(libs.gson)
    implementation(libs.okhttp)

    // Reactive and Utility Libraries
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // QR Code: CameraX + ZXing
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.compose)
    implementation(libs.core) // zxing core

    // AndroidX Lifecycle and Architecture Components
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // Background Task Libraries
    implementation(libs.work.runtime.ktx)
    implementation(libs.work.multiprocess)

    // Reorderable list
    implementation(libs.reorderable)

    // Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.org.mockito.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
