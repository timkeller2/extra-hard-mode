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

	// Client-only optional extras. Prefer Loom's remapped client compileOnly so
	// dedicated/main compile cannot see Cloth types.
	val cloth = "me.shedaniel.cloth:cloth-config-fabric:${providers.gradleProperty("cloth_config_version").get()}"
	val modmenu = "com.terraformersmc:modmenu:${providers.gradleProperty("modmenu_version").get()}"
	val clientOnly = sequenceOf("modClientCompileOnly", "clientCompileOnly")
			.firstOrNull { configurations.findByName(it) != null }
	if (clientOnly != null) {
		add(clientOnly, cloth) { exclude(group = "net.fabricmc.fabric-api") }
		add(clientOnly, modmenu)
	} else {
		compileOnly(cloth) { exclude(group = "net.fabricmc.fabric-api") }
		compileOnly(modmenu)
	}
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

val deployDirs = listOf(
	file("C:/Client/minecraftserver/mods"),
	file("${System.getProperty("user.home")}/AppData/Roaming/.minecraft/mods"),
)

tasks.register("deployJar") {
	group = "distribution"
	description = "Copy the remapped Extra Hard Mode jar to the local server and client mods folders."
	dependsOn("assemble")
	doLast {
		val src = layout.buildDirectory
				.dir("libs")
				.get()
				.asFile
				.listFiles()
				?.firstOrNull { file ->
					file.isFile
							&& file.name.startsWith("extrahardmode-")
							&& file.name.endsWith(".jar")
							&& !file.name.contains("sources")
				}
				?: error("No Extra Hard Mode jar in build/libs")
		val failed = mutableListOf<String>()
		for (dir in deployDirs) {
			dir.mkdirs()
			val dest = dir.resolve(src.name)
			val tmp = dir.resolve(src.name + ".tmp")
			tmp.delete()
			src.copyTo(tmp, overwrite = true)
			val stale = dir.listFiles()?.filter { file ->
				file.isFile
						&& file.name.startsWith("extrahardmode-")
						&& file.name.endsWith(".jar")
						&& file.name != src.name
			} ?: emptyList()
			stale.forEach { file ->
				if (!file.delete()) {
					failed += "could not remove ${file.absolutePath} (is Minecraft or the server running?)"
				}
			}
			if (dest.exists() && !dest.delete()) {
				tmp.delete()
				failed += "could not replace ${dest.absolutePath} (stop the server/client, then run deployJar again)"
				continue
			}
			if (!tmp.renameTo(dest)) {
				tmp.copyTo(dest, overwrite = true)
				tmp.delete()
			}
		}
		if (failed.isNotEmpty()) {
			error(failed.joinToString(separator = "\n"))
		}
	}
}

tasks.matching { it.name == "assemble" || it.name == "remapJar" }.configureEach {
	finalizedBy("deployJar")
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
