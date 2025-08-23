// Súbor: settings.gradle.kts
// Umiestnenie: C:\Users\dusan\AndroidStudioProjects\QR\settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }

    // TÁTO ČASŤ JE KĽÚČOVÁ PRE KATALÓG VERZIÍ:
   /* versionCatalogs {
        create("libs") { // "libs" je názov, ktorý používate (napr. libs.plugins...)
            from(files("gradle/libs.versions.toml")) // Cesta k vášmu .toml súboru z koreňa projektu
        }
    }*/
}

rootProject.name = "QR" // Názov vášho projektu
include(":app")         // Zahrnutie vášho :app modulu

