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
        maven(url = "https://jitpack.io")
        maven(url = "https://devrepo.kakao.com/nexus/content/groups/public/") // 카카오 로그인
        maven (url = "https://devrepo.kakao.com/nexus/repository/kakaomap-releases/") //카카오맵
    }
}

rootProject.name = "LifeSharing"
include(":app")
