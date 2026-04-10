package com.xwal.adapter.cli

// Note: CLI directly uses engine transformers (not via ports) — this is an accepted
// hexagonal architecture exception for a standalone offline tool that does not use
// the Micronaut application context or dependency injection.
import com.xwal.adapter.engine.camunda7.transformer.IwmToBpmnTransformer as Camunda7Transformer
import com.xwal.adapter.engine.flowable.transformer.IwmToBpmnTransformer as FlowableTransformer
import com.xwal.adapter.engine.transformer.BpmnToIwmTransformer
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import kotlin.system.exitProcess

@Command(
    name = "x-wal-migrate",
    description = ["x-wal Migration Tool — transforms workflow definitions between engines"],
    version = ["x-wal-migrate 2.0.0"],
    mixinStandardHelpOptions = true
)
class MigrationToolCommand : Runnable {

    @Parameters(index = "0", description = ["Input file (IWM JSON or BPMN XML)"])
    lateinit var inputFile: File

    @Option(names = ["-t", "--target"], description = ["Target engine: camunda7, flowable"], required = true)
    lateinit var targetEngine: String

    @Option(names = ["-o", "--output"], description = ["Output file (default: stdout)"])
    var outputFile: File? = null

    @Option(names = ["-n", "--name"], description = ["Process name"], defaultValue = "migrated-process")
    var processName: String = "migrated-process"

    @Option(names = ["--process-version"], description = ["Process version"], defaultValue = "1.0.0")
    var processVersion: String = "1.0.0"

    override fun run() {
        try {
            if (!inputFile.exists()) {
                System.err.println("Error: Input file not found: ${inputFile.absolutePath}")
                return
            }

            val input = inputFile.readText()
            val isIwm = inputFile.name.endsWith(".json") || input.trimStart().startsWith("{")

            val result = if (isIwm) {
                when (targetEngine.lowercase()) {
                    "camunda7" -> Camunda7Transformer().transformToBpmn(input, processName, processVersion)
                    "flowable" -> FlowableTransformer().transformToBpmn(input, processName, processVersion)
                    else -> {
                        System.err.println("Error: Unsupported target engine: $targetEngine. Use: camunda7, flowable")
                        return
                    }
                }
            } else {
                BpmnToIwmTransformer().transformToIwm(input, targetEngine)
            }

            if (outputFile != null) {
                outputFile!!.writeText(result)
                System.err.println("Migration complete: ${inputFile.name} -> ${outputFile!!.name} (target: $targetEngine)")
            } else {
                println(result)
            }
        } catch (e: Exception) {
            System.err.println("Error: ${e.message}")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            exitProcess(CommandLine(MigrationToolCommand()).execute(*args))
        }
    }
}
