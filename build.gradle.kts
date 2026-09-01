plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
}

version = providers.gradleProperty("version").get()
group = providers.gradleProperty("group").get()

base {
	archivesName = providers.gradleProperty("archives_base_name")
}

repositories {
	mavenCentral()
	maven {
		name = "Shedaniel"
		url = uri("https://maven.shedaniel.me/")
	}
	maven {
		name = "TerraformersMC"
		url = uri("https://maven.terraformersmc.com/")
	}
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

fabricApi {
	configureTests {
		createSourceSet = false
		enableGameTests = true
		enableClientGameTests = false
		eula = true
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

	testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Optional client extras. compileOnly so dedicated servers do not need them.
	compileOnly("me.shedaniel.cloth:cloth-config-fabric:${providers.gradleProperty("cloth_config_version").get()}") {
		exclude(group = "net.fabricmc.fabric-api")
	}
	compileOnly("com.terraformersmc:modmenu:${providers.gradleProperty("modmenu_version").get()}")
}

tasks.test {
	useJUnitPlatform()
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
