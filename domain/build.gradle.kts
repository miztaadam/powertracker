import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// The coach is pure Kotlin: the Android app consumes the jvm() variant, and the web app the JS library.
kotlin {
    // Target JVM 17 bytecode using whatever JDK (17+) runs Gradle; avoids toolchain auto-provisioning,
    // which needs network access to a JDK download service.
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    js {
        nodejs()
        binaries.library()
        generateTypeScriptDefinitions()
        useEsModules()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Node.js comes from the repository declared in settings.gradle.kts (project repositories are refused),
// so the Kotlin plugin must not add its own.
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
}

// ---- Generated JS package for the web app -------------------------------------------------------------
// The production library (ES modules + TypeScript declarations) is committed under web/src/domain/generated/
// so web builds need no JDK. Source maps are left out and their trailing comment dropped: they hold build-
// relative paths and nothing in the web build reads them. The output is otherwise byte-for-byte what Kotlin
// emits (no timestamps or absolute paths); checkWebDomain tolerates the compiler's run-to-run line ordering.
val webDomainDir = rootProject.layout.projectDirectory.dir("web/src/domain/generated")
val libraryDistribution = "jsNodeProductionLibraryDistribution"
val libraryOutput = layout.buildDirectory.dir("dist/js/productionLibrary")

fun Sync.fromProductionLibrary() {
    dependsOn(libraryDistribution)
    filteringCharset = "UTF-8"
    from(libraryOutput) {
        include("*.mjs", "*.d.mts")
        filter { line -> if (line.startsWith("//# sourceMappingURL=")) null else line }
    }
}

tasks.register<Sync>("syncWebDomain") {
    group = "web"
    description = "Regenerates web/src/domain/generated/ from the Kotlin/JS production library."
    fromProductionLibrary()
    into(webDomainDir)
}

val regenerateWebDomain = tasks.register<Sync>("regenerateWebDomain") {
    description = "Builds the web domain package into build/ for checkWebDomain."
    fromProductionLibrary()
    into(layout.buildDirectory.dir("webDomainCheck"))
}

tasks.register("checkWebDomain") {
    group = "verification"
    description = "Fails if web/src/domain/generated/ differs from the current Kotlin/JS library."
    dependsOn(regenerateWebDomain)
    val fresh = layout.buildDirectory.dir("webDomainCheck")
    val committed = webDomainDir
    doLast {
        fun files(dir: java.io.File) =
            dir.walkTopDown().filter { it.isFile }.associate { it.relativeTo(dir).path to it.readBytes() }
        val expected = files(fresh.get().asFile)
        val actual = files(committed.asFile)
        // The Kotlin compiler orders some import lines and interface lists (`[Encoder, CompositeEncoder]`) in the
        // stdlib and serialization-json modules differently from run to run, so a file also counts as unchanged
        // when it holds the same lines once those lists are sorted.
        val list = Regex("\\[([^\\[\\]]*)]")
        fun normalised(bytes: ByteArray) = bytes.decodeToString().lines()
            .map { line -> list.replace(line) { m -> m.groupValues[1].split(", ").sorted().joinToString(", ", "[", "]") } }
            .sorted()
        fun sameLines(a: ByteArray?, b: ByteArray?) = a != null && b != null && normalised(a) == normalised(b)
        val stale = (expected.keys + actual.keys)
            .filter { !(expected[it] contentEquals actual[it]) && !sameLines(expected[it], actual[it]) }
            .sorted()
        if (stale.isNotEmpty()) {
            throw GradleException(
                "web/src/domain/generated is out of date (${stale.joinToString()}). " +
                    "Run ./gradlew :domain:syncWebDomain and commit the result.",
            )
        }
    }
}
