plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.dualprofile.probe"; compileSdk = 36
 defaultConfig { applicationId = "com.dualprofile.probe"; minSdk = 28; targetSdk = 36; versionCode = 1; versionName = "1.0" }
 if (providers.gradleProperty("probeNative").orNull == "true") sourceSets.getByName("main").jniLibs.srcDir("fixtures/jniLibs")
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }

}
kotlin { jvmToolchain(17) }
