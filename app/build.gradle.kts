plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.example.smartnotifier"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.teyanday.smartnotifier"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            // デバッグ版のパッケージ名を com.teyanday.smartnotifier.debug に変更
            applicationIdSuffix = ".debug"

            // アプリ名の語尾に (Debug) を付けるための設定
            manifestPlaceholders["appName"] = "SmartNotifier(D)"

            // デバッグ時はビルド速度優先（必要に応じて）
            isMinifyEnabled = false
        }

        release {
            // リリース版（内部テスト版）の設定
            isMinifyEnabled = false
            manifestPlaceholders["appName"] = "SmartNotifier"

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

ksp {
    // @Database exportSchema = trueによる出力先の指定
    arg("room.schemaLocation", "${projectDir}/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.lifecycle)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.rxjava2)
    implementation(libs.androidx.work.gcm)
    implementation(libs.androidx.work.testing)
    implementation(libs.androidx.work.multiprocess)
}
