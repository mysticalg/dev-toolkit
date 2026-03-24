pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DevToolkit"

include(":app")
include(":core:ui")
include(":core:data")
include(":core:domain")
include(":feature:json")
include(":feature:regex")
include(":feature:diff")
include(":feature:texttransform")
include(":feature:base64")
include(":feature:url")
include(":feature:hash")
include(":feature:jwt")
include(":feature:epoch")
include(":feature:color")
include(":feature:uuid")
include(":feature:mockdata")
