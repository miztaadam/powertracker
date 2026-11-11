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

// docs/library/sources.md is generated from the catalogue by SourcesDocTest, and only rewritten with -Psources.update=true.
val sourcesDoc = rootProject.layout.projectDirectory.file("docs/library/sources.md")
tasks.named<Test>("jvmTest") {
    systemProperty("sources.file", sourcesDoc.asFile.absolutePath)
    systemProperty("sources.update", (project.findProperty("sources.update") ?: "false").toString())
    inputs.files(sourcesDoc).withPropertyName("sourcesDoc")
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
        // Across forced rebuilds only two third-party files vary, and only in two constructs: the member lines
        // of an `import { ... } from` block, and the trailing interface list of `initMetadataForClass(...)`.
        // Those are compared order-insensitively; everything else, and every other file (in particular
        // powertracker-domain.mjs and powertracker-domain.d.mts), must match byte for byte.
        val tolerant = setOf("kotlin-kotlin-stdlib.mjs", "kotlinx-serialization-kotlinx-serialization-json.mjs")
        val interfaces = Regex("^(initMetadataForClass\\(.*, )\\[([^\\[\\]]*)]\\);$")
        fun normalised(bytes: ByteArray): List<String> {
            val out = mutableListOf<String>()
            val members = mutableListOf<String>()
            var inImport = false
            for (line in bytes.decodeToString().lines()) {
                when {
                    line == "import {" -> { inImport = true; out += line }
                    inImport && line.startsWith("} from ") -> {
                        out += members.sorted(); members.clear(); inImport = false; out += line
                    }
                    inImport -> members += line
                    else -> out += interfaces.replace(line) { m ->
                        m.groupValues[1] + m.groupValues[2].split(", ").sorted().joinToString(", ", "[", "]") + ");"
                    }
                }
            }
            return out
        }
        fun same(name: String, a: ByteArray?, b: ByteArray?) =
            a contentEquals b || (name in tolerant && a != null && b != null && normalised(a) == normalised(b))
        val stale = (expected.keys + actual.keys).filter { !same(it, expected[it], actual[it]) }.sorted()
        if (stale.isNotEmpty()) {
            throw GradleException(
                "web/src/domain/generated is out of date (${stale.joinToString()}). " +
                    "Run ./gradlew :domain:syncWebDomain and commit the result.",
            )
        }
    }
}
