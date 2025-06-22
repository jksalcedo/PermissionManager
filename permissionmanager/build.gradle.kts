plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.jksalcedo.permissionmanager"
    compileSdk = 36

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    publishing {
        singleVariant("release") {
            // withSourcesJar()
            // withJavadocJar()
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.kotlinx.coroutines.android)
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm
    runtimeOnly(libs.kotlinx.coroutines.core.jvm)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.jksalcedo.permissionmanager"
            artifactId = "permissionmanager"
            version = "1.0.0"
            // Publish the release variant
            afterEvaluate {
                from(components.findByName("release") ?: components["android"])
            }
        }
    }

    repositories {
        mavenLocal()
    }
}
