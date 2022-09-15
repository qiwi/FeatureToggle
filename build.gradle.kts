buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(Dependencies.androidPlugin)
        classpath(Dependencies.kotlinPlugin)
        classpath(Dependencies.dokkaPlugin)
        classpath(Dependencies.kspPlugin)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}