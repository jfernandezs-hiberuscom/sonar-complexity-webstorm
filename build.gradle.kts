plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij.platform") version "2.0.0-beta6"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        intellijDependencies()
    }
}

dependencies {
    intellijPlatform {
        webstorm(providers.gradleProperty("platformVersion").get())
        bundledPlugins("JavaScript")
        instrumentationTools()
    }


    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.sonarcomplexity.webstorm"
        name = "Sonar Complexity"
        version = providers.gradleProperty("pluginVersion").get()
        description = """
            Calculates Cognitive and Cyclomatic complexity metrics for functions in JavaScript and TypeScript, 
            displaying them inline in WebStorm editors just like the popular VS Code sonar-complexity plugin.
        """.trimIndent()
        
        vendor {
            name = "jopo79"
            url = "https://github.com/jfernandezs-hiberuscom/sonar-complexity-webstorm"
        }
        
        ideaVersion {
            sinceBuild = "241"
            untilBuild = "262.*"
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    test {
        useJUnitPlatform()
    }
}
