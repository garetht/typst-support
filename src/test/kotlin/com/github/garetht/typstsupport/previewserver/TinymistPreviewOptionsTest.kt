package com.github.garetht.typstsupport.previewserver

import com.github.garetht.typstsupport.languageserver.locations.Version
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TinymistPreviewOptionsTest {
  @Test
  fun `default options should only include basic command`() {
    val options = TinymistPreviewOptions()
    val command = options.toCommandList("file/name.typ")

    assertEquals(listOf("file/name.typ", "--no-open"), command)
  }

  @ParameterizedTest
  @MethodSource("provideInvertColorsOptions")
  fun `invert colors options should generate correct commands`(
    invertColors: TinymistPreviewOptions.InvertColors,
    expectedCommand: String
  ) {
    val options = TinymistPreviewOptions(invertColors = invertColors)
    val command = options.toCommandList("file/name.typ")

    assertTrue(command.contains(expectedCommand))
  }

  @ParameterizedTest
  @MethodSource("providePathOptions")
  fun `path options should generate correct commands`(
    root: Path?,
    fontPaths: List<Path>,
    packagePath: Path?,
    packageCachePath: Path?,
    cert: Path?,
    expectedCommands: List<String>
  ) {
    val options = TinymistPreviewOptions(
      root = root,
      fontPaths = fontPaths,
      packagePath = packagePath,
      packageCachePath = packageCachePath,
      cert = cert
    )
    val command = options.toCommandList("file/name.typ")

    expectedCommands.forEach { expected ->
      assertTrue(command.contains(expected), "Command should contain: $expected")
    }
  }

  @ParameterizedTest
  @MethodSource("provideInputOptions")
  fun `input options should generate correct commands`(
    inputs: Map<String, String>,
    expectedCommands: List<String>
  ) {
    val options = TinymistPreviewOptions(inputs = inputs)
    val command = options.toCommandList("file/name.typ")

    expectedCommands.forEach { expected ->
      assertTrue(command.contains(expected), "Command should contain: $expected")
    }
  }

  @ParameterizedTest
  @MethodSource("providePreviewModeOptions")
  fun `preview mode options should generate correct commands`(
    mode: TinymistPreviewOptions.PreviewMode,
    expectedCommand: String
  ) {
    val options = TinymistPreviewOptions(previewMode = mode)
    val command = options.toCommandList("file/name.typ")

    assertTrue(command.contains(expectedCommand))
  }

  @ParameterizedTest
  @MethodSource("providePortOptions")
  fun `port options should generate correct commands`(
    dataPlanePort: Int?,
    controlPlanePort: Int?,
    expectedCommands: List<String>
  ) {
    val options = TinymistPreviewOptions(dataPlaneHostPort = dataPlanePort, controlPlaneHostPort = controlPlanePort)
    val command = options.toCommandList("file/name.typ")

    expectedCommands.forEach { expected ->
      assertTrue(command.contains(expected), "Command should contain: $expected")
    }
  }

  @Test
  fun `all options should generate complete command`() {
    val options = TinymistPreviewOptions(
      partialRendering = true,
      tinymistVersion = Version(0, 15, 12),
      invertColors = TinymistPreviewOptions.InvertColors.ByElement(
        rest = TinymistPreviewOptions.InvertStrategy.Always,
        image = TinymistPreviewOptions.InvertStrategy.Never
      ),
      root = Path.of("/root"),
      inputs = mapOf("key1" to "value1", "key2" to "value2"),
      fontPaths = listOf(Path.of("/fonts1"), Path.of("/fonts2")),
      ignoreSystemFonts = true,
      packagePath = Path.of("/packages"),
      packageCachePath = Path.of("/cache"),
      cert = Path.of("/cert.pem"),
      previewMode = TinymistPreviewOptions.PreviewMode.Slide,
      host = "localhost",
      dataPlaneHostPort = 8080,
      controlPlaneHostPort = 8081
    )

    val command = options.toCommandList("file/name.typ")
    val expectedCommands = listOf(
      "file/name.typ",
      "--partial-rendering=true",
      "--invert-colors={\"rest\": \"always\", \"image\": \"never\"}",
      "--root", "/root",
      "--input", "key1=value1",
      "--input", "key2=value2",
      "--font-path", "/fonts1",
      "--font-path", "/fonts2",
      "--ignore-system-fonts",
      "--package-path", "/packages",
      "--package-cache-path", "/cache",
      "--cert", "/cert.pem",
      "--preview-mode", "slide",
      "--host", "localhost",
      "--no-open",
      "--data-plane-host", "127.0.0.1:8080",
      "--control-plane-host", "127.0.0.1:8081"
    )

    assertEquals(expectedCommands, command)
  }

  @Test
  fun `modern tinymist versions should use the valued partial rendering flag`() {
    val options = TinymistPreviewOptions(partialRendering = true, tinymistVersion = Version(0, 15, 12))

    val command = options.toCommandList("file/name.typ")
    assertTrue(command.contains("--partial-rendering=true"))
    assertFalse(command.contains("--partial-rendering"))
  }

  @Test
  fun `legacy tinymist versions should use the bare partial rendering flag`() {
    val options = TinymistPreviewOptions(partialRendering = true, tinymistVersion = Version(0, 13, 16))

    val command = options.toCommandList("file/name.typ")
    assertTrue(command.contains("--partial-rendering"))
    assertFalse(command.contains("--partial-rendering=true"))
  }

  @Test
  fun `unknown tinymist version should fall back to the valued partial rendering flag`() {
    val options = TinymistPreviewOptions(partialRendering = true, tinymistVersion = null)

    val command = options.toCommandList("file/name.typ")
    assertTrue(command.contains("--partial-rendering=true"))
  }

  @ParameterizedTest
  @MethodSource("providePartialRenderingVersionThresholds")
  fun `partial rendering flag should respect the version threshold`(
    version: Version,
    expectedFlag: String
  ) {
    val options = TinymistPreviewOptions(partialRendering = true, tinymistVersion = version)

    val command = options.toCommandList("file/name.typ")
    assertTrue(command.contains(expectedFlag), "Version $version should produce $expectedFlag")
  }

  companion object {
    @JvmStatic
    fun provideInvertColorsOptions() = listOf(
      Arguments.of(
        TinymistPreviewOptions.InvertColors.Auto,
        "--invert-colors=auto"
      ),
      Arguments.of(
        TinymistPreviewOptions.InvertColors.Never,
        "--invert-colors=never"
      ),
      Arguments.of(
        TinymistPreviewOptions.InvertColors.Always,
        "--invert-colors=always"
      ),
      Arguments.of(
        TinymistPreviewOptions.InvertColors.ByElement(
          rest = TinymistPreviewOptions.InvertStrategy.Always,
          image = TinymistPreviewOptions.InvertStrategy.Never
        ),
        "--invert-colors={\"rest\": \"always\", \"image\": \"never\"}"
      )
    )

    @JvmStatic
    fun providePathOptions() = listOf(
      Arguments.of(
        Path.of("/root"),
        listOf(Path.of("/fonts")),
        Path.of("/packages"),
        Path.of("/cache"),
        Path.of("/cert.pem"),
        listOf(
          "--root", "/root",
          "--font-path", "/fonts",
          "--package-path", "/packages",
          "--package-cache-path", "/cache",
          "--cert", "/cert.pem"
        )
      ),
      Arguments.of(
        null,
        emptyList<Path>(),
        null,
        null,
        null,
        emptyList<String>()
      )
    )

    @JvmStatic
    fun provideInputOptions() = listOf(
      Arguments.of(
        mapOf("key" to "value"),
        listOf("--input", "key=value")
      ),
      Arguments.of(
        mapOf("key1" to "value1", "key2" to "value2"),
        listOf("--input", "key1=value1", "--input", "key2=value2")
      ),
      Arguments.of(
        emptyMap<String, String>(),
        emptyList<String>()
      )
    )

    @JvmStatic
    fun providePreviewModeOptions() = listOf(
      Arguments.of(
        TinymistPreviewOptions.PreviewMode.Document,
        "--preview-mode"
      ),
      Arguments.of(
        TinymistPreviewOptions.PreviewMode.Slide,
        "--preview-mode"
      )
    )

    @JvmStatic
    fun providePortOptions() = listOf(
      Arguments.of(
        8080,
        8081,
        listOf("--data-plane-host", "127.0.0.1:8080", "--control-plane-host", "127.0.0.1:8081")
      ),
      Arguments.of(
        null,
        null,
        emptyList<String>()
      ),
      Arguments.of(
        8080,
        null,
        listOf("--data-plane-host", "127.0.0.1:8080")
      ),
      Arguments.of(
        null,
        8081,
        listOf("--control-plane-host", "127.0.0.1:8081")
      )
    )

    @JvmStatic
    fun providePartialRenderingVersionThresholds() = listOf(
      Arguments.of(Version(0, 13, 14), "--partial-rendering"),
      Arguments.of(Version(0, 13, 16), "--partial-rendering"),
      Arguments.of(Version(0, 13, 17), "--partial-rendering=true"),
      Arguments.of(Version(0, 14, 0), "--partial-rendering=true"),
      Arguments.of(Version(0, 15, 12), "--partial-rendering=true")
    )
  }
} 
