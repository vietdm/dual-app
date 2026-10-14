plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "com.dualprofile.app"
    compileSdk = 36
    defaultConfig { applicationId = "com.dualprofile.app"; minSdk = 28; targetSdk = 36; versionCode = 1; versionName = "0.1.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    val signingVars = listOf("DUALPROFILE_KEYSTORE", "DUALPROFILE_KEYSTORE_PASSWORD", "DUALPROFILE_KEY_ALIAS", "DUALPROFILE_KEY_PASSWORD")
    if (signingVars.all { !System.getenv(it).isNullOrBlank() }) {
        signingConfigs.create("production") { storeFile = file(System.getenv(signingVars[0])); storePassword = System.getenv(signingVars[1]); keyAlias = System.getenv(signingVars[2]); keyPassword = System.getenv(signingVars[3]) }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("production")
    }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
    lint { abortOnError = true; checkReleaseBuilds = true }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core:1.7.0")
}
