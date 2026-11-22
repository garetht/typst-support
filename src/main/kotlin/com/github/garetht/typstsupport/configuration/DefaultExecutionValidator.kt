package com.github.garetht.typstsupport.configuration

import com.github.garetht.typstsupport.languageserver.locations.Version
import com.intellij.execution.configurations.GeneralCommandLine
import java.io.File

class DefaultExecutionValidator(
  private val processExecutor: ProcessExecutor,
  private val pathValidator: PathValidator
) : ExecutionValidator {
  override fun validateBinaryExecution(binaryPath: String): ExecutionValidation {
    when (val result = pathValidator.validateBinaryFile(binaryPath)) {
      is PathValidation.Failed -> return ExecutionValidation.Failed(result.message)
      PathValidation.Success -> {}
    }

    val file = File(binaryPath)
    return try {
      val commandLine = GeneralCommandLine(binaryPath, "-V")
      commandLine.workDirectory = file.parentFile

      val result = processExecutor.executeProcess(commandLine)

      if (result.exitCode == 0) {
        val output = result.stdout.trim()
        val version = Version.Companion.parseVersion(output)
        if (version == null) {
          ExecutionValidation.Failed("No version information could be found.")
        } else if (version >= VersionRequirement.minimumVersion) {
          ExecutionValidation.Success(version)
        } else if (version < VersionRequirement.minimumVersion) {
          ExecutionValidation.Failed("Tinymist version ${version.toPathString()} is below required version ${VersionRequirement.minimumVersion.toPathString()}")
        } else {
          ExecutionValidation.Failed("Invalid binary output format")
        }
      } else {
        val errorOutput = result.stderr.ifEmpty { result.stdout }.trim()

        val errorMessage =
          when {
            errorOutput.contains("cannot be opened because the developer cannot be verified") ||
                    errorOutput.contains("malware") ||
                    errorOutput.contains("not trusted") ||
                    result.exitCode == 126 -> {
              "macOS blocked execution - check Security & Privacy settings"
            }

            errorOutput.contains("Permission denied") -> {
              "Permission denied - make binary executable"
            }

            errorOutput.contains("No such file") -> {
              "Binary not found at specified path"
            }

            else -> {
              "Binary validation failed: " + errorOutput.take(50)
            }
          }
        ExecutionValidation.Failed(errorMessage)
      }
    } catch (e: Exception) {
      val message =
        when {
          e.message?.contains("Permission denied") == true ->
            "Permission denied - check file permissions"

          e.message?.contains("No such file") == true -> "Binary file not found"
          e.message?.contains("cannot execute") == true ->
            "Cannot execute binary - may be blocked by macOS security"

          else -> "Failed to execute binary: ${e.message?.take(50)}"
        }
      ExecutionValidation.Failed(message)
    }
  }
}
