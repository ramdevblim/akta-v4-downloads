buildscript {

    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://raw.githubusercontent.com/NielsenDigitalSDK/nielsenappsdk-android/master/")
        }
        maven {
            url = uri("https://storage.googleapis.com/r8-releases/raw")
        }
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.8.20")
        classpath("com.android.tools:r8:8.2.24")
    }
}