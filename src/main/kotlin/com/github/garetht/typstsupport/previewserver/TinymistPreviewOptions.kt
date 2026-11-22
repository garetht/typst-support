package com.github.garetht.typstsupport.previewserver

import com.github.garetht.typstsupport.configuration.VersionRequirement
import com.github.garetht.typstsupport.languageserver.locations.Version
import com.intellij.openapi.diagnostic.logger
import java.nio.file.Path
import java.util.UUID

private val LOG = logger<TinymistPreviewServerManager>()

data class TinymistPreviewOptions(
  val partialRendering: Boolean? = null,
  val invertColors: InvertColors? = null,
  val root: Path? = null,
  val inputs: Map<String, String> = emptyMap(),
  val taskId: UUID? = null,
  val fontPaths: List<Path> = emptyList(),
  val ignoreSystemFonts: Boolean = false,
  val packagePath: Path? = null,
  val packageCachePath: Path? = null,
  val cert: Path? = null,
  val previewMode: PreviewMode? = null,
  val host: String = "",
  val dataPlaneHostPort: Int? = null,
  val controlPlaneHostPort: Int? = null,
  // Version of the running tinymist server; decides the flag syntax for
  // `--partial-rendering` (see `partialRenderingFlag`). `null` (unknown) falls
  // back to the modern `=true` form, which is what the auto-downloaded binary uses.
  val tinymistVersion: Version? = null,
) {
  fun toCommandParamsArguments(filename: String): List<List<String>> {
    return listOf(this.toCommandList(filename))
  }

  fun toCommandList(filename: String): List<String> {
    val command = mutableListOf(filename)

    if (partialRendering == true) {
      command.add(partialRenderingFlag(tinymistVersion))
    }

    when (val colors = invertColors) {
      is InvertColors.Auto -> command.add("--invert-colors=auto")
      is InvertColors.Never -> command.add("--invert-colors=never")
      is InvertColors.Always -> command.add("--invert-colors=always")
      is InvertColors.ByElement -> {
        val json = buildString {
          append("{")
          append("\"rest\": \"${colors.rest}\", ")
          append("\"image\": \"${colors.image}\"")
          append("}")
        }
        command.add("--invert-colors=$json")
      }

      null -> {}
    }

    root?.let { command.addAll(listOf("--root", it.toString())) }

    inputs.forEach { (key, value) ->
      command.addAll(listOf("--input", "$key=$value"))
    }

    fontPaths.forEach { path ->
      command.addAll(listOf("--font-path", path.toString()))
    }

    if (ignoreSystemFonts) {
      command.add("--ignore-system-fonts")
    }

    packagePath?.let { command.addAll(listOf("--package-path", it.toString())) }
    packageCachePath?.let { command.addAll(listOf("--package-cache-path", it.toString())) }
    cert?.let { command.addAll(listOf("--cert", it.toString())) }

    if (previewMode != null) {
      command.addAll(listOf("--preview-mode", previewMode.value))
    }

    if (taskId != null) {
      command.addAll(listOf("--task-id", taskId.toString()))
    }

    if (host.isNotEmpty()) {
      command.addAll(listOf("--host", host))
    }

    command.add("--no-open")

    dataPlaneHostPort?.let { command.addAll(listOf("--data-plane-host", "127.0.0.1:$it")) }
    controlPlaneHostPort?.let { command.addAll(listOf("--control-plane-host", "127.0.0.1:$it")) }

    LOG.info("Tinymist preview command: $command")

    return command
  }

  sealed class InvertColors {
    object Auto : InvertColors()
    object Never : InvertColors()
    object Always : InvertColors()
    data class ByElement(
      val rest: InvertStrategy,
      val image: InvertStrategy
    ) : InvertColors()
  }

  enum class InvertStrategy {
    Always, Never, Auto;

    override fun toString(): String = name.lowercase()
  }

  enum class PreviewMode(val value: String) {
    Document("document"),
    Slide("slide")
  }
}

// tinymist v0.13.17 changed the preview's `--partial-rendering` flag from a bare
// boolean flag to one that requires an explicit value. Older builds reject the
// valued form ("unexpected value 'true' for '--partial-rendering' found; no more
// were expected"), newer builds reject the bare form ("a value is required for
// '--partial-rendering <ENABLE_PARTIAL_RENDERING>' but none was supplied").
internal fun partialRenderingFlag(tinymistVersion: Version?): String =
  if (tinymistVersion != null && tinymistVersion < VersionRequirement.valuedPartialRenderingVersion) {
    "--partial-rendering"
  } else {
    "--partial-rendering=true"
  }
