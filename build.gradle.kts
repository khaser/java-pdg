import java.io.ByteArrayOutputStream

plugins {
    java
    application
}

group = "ru.itmo.khaser.java_pdg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.8")
    // testImplementation(kotlin("test"))
}

application {
    mainClass.set("ru.itmo.khaser.java_pdg.Main")
}

tasks.register("runExamples") {
    group = "pdg"
    dependsOn("build")
    doLast {
        val examplesDir = file("examples")
        val resultsDir = file("results")

        // Create results directory if it doesn't exist
        resultsDir.mkdirs()

        // Find all .java files in examples directory
        val javaFiles = examplesDir.listFiles { file ->
            file.isFile && file.extension == "java"
        } ?: emptyArray()

        javaFiles.forEach { javaFile ->
            val baseName = javaFile.nameWithoutExtension
            val dotFile = File(resultsDir, "$baseName.dot")
            val svgFile = File(resultsDir, "$baseName.svg")

            val dotOutput = ByteArrayOutputStream()
            val result = exec {
                commandLine = listOf(
                    "./gradlew", "run", "--quiet", "--console=plain",
                    "--args=${javaFile.absolutePath}"
                )
                standardOutput = dotOutput
                isIgnoreExitValue = true
            }

            if (result.exitValue == 0) {
                dotFile.writeText(dotOutput.toString())
                val svgResult = exec {
                    commandLine = listOf("dot", "-Tsvg", dotFile.absolutePath, "-o", svgFile.absolutePath)
                }
                println("  Generated ${dotFile.name} & ${svgFile.name}")
            } else {
                println("  Error: Failed to process ${javaFile.name}")
            }
        }

        println("\nDone! Results saved in ${resultsDir.absolutePath}")
    }
}
