package com.github.garetht.typstsupport.languageserver.locations

import com.github.garetht.typstsupport.mockIntelliJEnvironment
import com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.nio.file.Path
import java.util.UUID
import java.util.stream.Stream
import kotlin.streams.asStream

class TinymistLocationResolverTest {

  @AfterEach
  fun `Remove all mocks`() {
    unmockkAll()
  }

  @ParameterizedTest
  @MethodSource("generateUrlArgs")
  fun shouldGetUrl(version: String, osName: String, archName: String, expectedUrl: URI) {
    restoreSystemProperties {
      System.setProperty("os.name", osName)
      System.setProperty("os.arch", archName)

      mockIntelliJEnvironment {
        plugin {
          this.version = version
        }
        appDirs {
          this.userDataDir = ""
        }
      }

      val resolver = TinymistLocationResolver()

      val url = resolver.downloadUrl()

      Assertions.assertEquals(expectedUrl, url)
    }

  }

  @Test
  fun shouldRestoreJnaNoClassPath() {
    restoreSystemProperties {
      System.setProperty("jna.noclasspath", "true")

      mockIntelliJEnvironment {
        plugin {
          this.version = "1.10"
        }
        appDirs {
          this.userDataDir = ""
        }
      }


      val resolver = TinymistLocationResolver()
      resolver.binaryPath()

      Assertions.assertEquals(System.getProperty("jna.noclasspath"), "true")
    }

  }

  @Test
  fun shouldRestoreJnaNoClassPathNull() {
    restoreSystemProperties {
      System.clearProperty("jna.noclasspath")

      mockIntelliJEnvironment {
        appDirs {
          this.userDataDir = ""
        }
      }
      val resolver = TinymistLocationResolver()
      resolver.binaryPath()

      Assertions.assertEquals(System.getProperty("jna.noclasspath"), null)
    }

  }

  @ParameterizedTest
  @MethodSource("generatePathArgs")
  fun shouldGetPath(basePath: String, version: String, osName: String, expectedPath: String) {
    restoreSystemProperties {
      System.setProperty("os.name", osName)

      mockIntelliJEnvironment {
        plugin {
          this.version = version
        }
        appDirs {
          this.userDataDir = basePath
        }
      }

      val resolver = TinymistLocationResolver()

      val path = resolver.binaryPath()

      Assertions.assertEquals(Path.of(expectedPath), path)
    }

  }

  companion object {

    data class OperatingSystem(
      val osNameProperty: String,
      val platformId: String,
      val downloadExtension: String = "",
      val executableExtension: String = ""
    )

    data class Architecture(
      val osArchProperty: String,
      val platformId: String
    )

    data class TestConfiguration(
      val version: String,
      val os: OperatingSystem,
      val architecture: Architecture,
      val expectedUrl: URI
    )

    private val supportedOperatingSystems = listOf(
      OperatingSystem(
        osNameProperty = "Windows",
        platformId = "pc-windows-msvc",
        downloadExtension = ".zip",
        executableExtension = ".exe"
      ),
      OperatingSystem(
        osNameProperty = "MacOs X",
        platformId = "apple-darwin",
        downloadExtension = ".tar.gz",

        ),
      OperatingSystem(
        osNameProperty = UUID.randomUUID().toString(),
        platformId = "unknown-linux-gnu",
        downloadExtension = ".tar.gz",
      )
    )

    private val supportedArchitectures = listOf(
      Architecture(
        osArchProperty = "arch64",
        platformId = "aarch64"
      ),
      Architecture(
        osArchProperty = UUID.randomUUID().toString(),
        platformId = "x86_64"
      )
    )

    private val supportedVersions = listOf(
      "0.15.12",
    )

    @JvmStatic
    fun generateUrlArgs(): Stream<Arguments> {
      return generateTestConfigurations()
        .map { config ->
          Arguments.of(
            config.version,
            config.os.osNameProperty,
            config.architecture.osArchProperty,
            config.expectedUrl
          )
        }
        .asStream()
    }

    private fun generateTestConfigurations(): Sequence<TestConfiguration> = sequence {
      for (os in supportedOperatingSystems) {
        for (architecture in supportedArchitectures) {
          for (version in supportedVersions) {
            val expectedUrl = buildDownloadUrl(
              version = version,
              platformId = os.platformId,
              architectureId = architecture.platformId,
              downloadExtension = os.downloadExtension
            )

            yield(
              TestConfiguration(
                version = version,
                os = os,
                architecture = architecture,
                expectedUrl = expectedUrl
              )
            )
          }
        }
      }
    }

    private fun buildDownloadUrl(
      version: String,
      platformId: String,
      architectureId: String,
      downloadExtension: String
    ): URI {
      val urlTemplate = "https://github.com/Myriad-Dreamin/tinymist/releases/download/v%s/tinymist-%s-%s%s"
      return URI(urlTemplate.format(version, architectureId, platformId, downloadExtension))
    }

    @JvmStatic
    fun generatePathArgs(): Stream<Arguments> {
      val seq = sequence {
        supportedOperatingSystems.forEach { osOpt ->
          supportedVersions.forEach { version ->
            val basePath = "/%s".format(UUID.randomUUID().toString())
            val expectedPath = "%s/language-server/%s/tinymist%s".format(
              basePath,
              "v0.15.12",
              osOpt.executableExtension
            )
            yield(
              Arguments.of(
                basePath,
                version,
                osOpt.osNameProperty,
                expectedPath
              )
            )
          }
        }
      }

      return seq.asStream()
    }
  }
}
