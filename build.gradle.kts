plugins {
	id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
	`maven-publish`
}

version = providers.gradleProperty("version").get()
group = providers.gradleProperty("group").get()

base {
	archivesName = providers.gradleProperty("archives_base_name")
}

repositories {
	mavenCentral()
}

loom {
	splitEnvironmentSourceSets()
	mods {
		register("extrahardmode") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.named("client").get())
		}
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")

	val nightConfig = "com.electronwill.night-config:toml:${providers.gradleProperty("night_config_version").get()}"
	implementation(nightConfig)
	include(nightConfig)
	include("com.electronwill.night-config:core:${providers.gradleProperty("night_config_version").get()}")
}

tasks.processResources {
	val version = project.version.toString()
	inputs.property("version", version)
	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to version))
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release = 25
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)
	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}
	repositories {
	}
}
