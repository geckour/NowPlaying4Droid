import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/geckour/NowPlayingSubjectBuilder")

            credentials {
                username = githubProperties["gpr.usr"] as String?
                password = githubProperties["gpr.key"] as String?
            }
        }
    }
}

val githubProperties
    get() = Properties().apply {
        File(settingsDir, "github.properties").inputStream().use { stream ->
            Properties().load(stream)
        }
    }

include(":app")
