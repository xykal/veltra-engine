plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.veltra.data"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime); ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.datastore.preferences)
}
