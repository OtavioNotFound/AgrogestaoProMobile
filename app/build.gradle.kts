import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

val privateSupabaseProperties = rootProject.file("supabase.md")
    .takeIf { it.exists() }
    ?.readLines()
    ?.mapNotNull { line ->
        val separator = line.indexOf(':')
        if (separator <= 0) return@mapNotNull null
        val name = line.substring(0, separator).trim().lowercase()
        val value = line.substring(separator + 1).trim().trim('`')
        name.takeIf(String::isNotBlank)?.let { it to value }
    }
    ?.toMap()
    .orEmpty()

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun firstConfiguredValue(vararg values: String?): String =
    values.firstOrNull { !it.isNullOrBlank() }.orEmpty()

val supabaseUrl = firstConfiguredValue(
    System.getenv("SUPABASE_URL"),
    localProperties.getProperty("SUPABASE_URL"),
    privateSupabaseProperties["api url"]
).substringBefore("/rest/v1").trimEnd('/')
val supabaseAnonKey = firstConfiguredValue(
    System.getenv("SUPABASE_ANON_KEY"),
    localProperties.getProperty("SUPABASE_ANON_KEY"),
    privateSupabaseProperties["publishable key"]
)

android {
    namespace = "com.agrogestao.pro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.agrogestao.pro"
        minSdk = 24
        targetSdk = 35
        versionCode = 9
        versionName = "1.1.0-beta8"

        testInstrumentationRunner = "com.agrogestao.pro.TestRunner"
        buildConfigField("String", "SUPABASE_URL", supabaseUrl.asBuildConfigString())
        buildConfigField("String", "SUPABASE_ANON_KEY", supabaseAnonKey.asBuildConfigString())
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)

    debugImplementation(libs.androidx.ui.tooling)
}
