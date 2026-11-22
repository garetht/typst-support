package com.github.garetht.typstsupport.languageserver.locations

import com.github.garetht.typstsupport.configuration.BinarySource
import com.github.garetht.typstsupport.configuration.DefaultPathValidator
import com.github.garetht.typstsupport.configuration.PathValidation
import com.github.garetht.typstsupport.configuration.SettingsState
import com.github.garetht.typstsupport.configuration.VersionRequirement
import com.github.garetht.typstsupport.notifier.Notifier
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import net.harawata.appdirs.AppDirsFactory
import java.net.URI
import java.nio.file.Path

private const val TYPST_SUPPORT_ID = "com.github.garetht.typstsupport"
class TinymistLocationResolver : LocationResolver {
  private val jnaNoClassPathKey = "jna.noclasspath"
  private var jnaNoClassPath: String? = null
  private val pathValidator = DefaultPathValidator()

  private val binary =
    TinymistBinary(
      version = VersionRequirement.downloadVersion,
      osName = OsName.fromString(System.getProperty("os.name")),
      osArchitecture = OsArchitecture.fromString(System.getProperty("os.arch")),
    )

  private fun pushJnaNoClassPathFalse() {
    jnaNoClassPath = System.getProperty(jnaNoClassPathKey)
    System.setProperty(jnaNoClassPathKey, "false")
  }

  private fun popJnaNoClassPath() {
    jnaNoClassPath?.let { System.setProperty(jnaNoClassPathKey, it) }
      ?: run { System.clearProperty(jnaNoClassPathKey) }
  }

  override fun downloadUrl(): URI = binary.downloadUrl

  override fun binaryPath(): Path {
    val settings = SettingsState.getInstance()
    if (settings.state.binarySource == BinarySource.USE_CUSTOM_BINARY) {
      when (val result = pathValidator.validateBinaryFile(settings.state.customBinaryPath)) {
        is PathValidation.Failed -> {
          Notifier.warn(
            "Your specified Tinymist binary (${settings.state.customBinaryPath}) is invalid: ${result.message}.\n\n Falling back to automatically downloaded Tinymist."
          )
        }

        PathValidation.Success -> return Path.of(settings.state.customBinaryPath)
      }
    }

    PluginManagerCore.getPlugin(PluginId.getId(TYPST_SUPPORT_ID))!!.run {
      pushJnaNoClassPathFalse()

      val appDirs = AppDirsFactory.getInstance()
      val path =
        Path.of(appDirs.getUserDataDir("TypstSupport", null, "com.github.garetht.typstsupport"))
          .resolve("language-server")
          .resolve(binary.versionPath)
          .resolve(binary.binaryFilename)

      popJnaNoClassPath()
      return path
    }
  }
}
