package com.github.garetht.typstsupport.previewserver

import com.github.garetht.typstsupport.configuration.ProcessExecutor
import com.github.garetht.typstsupport.languageserver.TinymistLanguageServerDescriptor
import com.github.garetht.typstsupport.languageserver.locations.Version
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerDescriptor
import io.mockk.every
import io.mockk.mockk
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerInfo
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeProcessExecutor(
  private val output: String = "",
  private val exitCode: Int = 0
) : ProcessExecutor {
  val executedCommands = mutableListOf<GeneralCommandLine>()

  override fun executeProcess(commandLine: GeneralCommandLine): ProcessOutput {
    executedCommands.add(commandLine)
    return ProcessOutput(output, "", exitCode, false, false)
  }
}

class TinymistPreviewServerManagerTest {
  private fun mockedServer(serverInfoVersion: String?, descriptor: LspServerDescriptor): LspServer {
    val server = mockk<LspServer>()
    every { server.initializeResult } answers {
      if (serverInfoVersion == null) {
        null
      } else {
        InitializeResult().apply { serverInfo = ServerInfo("tinymist", serverInfoVersion) }
      }
    }
    every { server.descriptor } returns descriptor
    return server
  }

  private fun mockedDescriptor(binaryPath: Path): TinymistLanguageServerDescriptor {
    val descriptor = mockk<TinymistLanguageServerDescriptor>()
    every { descriptor.languageServerPath } returns binaryPath
    return descriptor
  }

  @Test
  fun `version should be read from the LSP serverInfo when present`() {
    val executor = FakeProcessExecutor()
    val server = mockedServer("0.15.12", mockedDescriptor(Path.of("/usr/bin/tinymist")))

    val version = tinymistVersionOf(server, executor, mutableMapOf())

    assertEquals(Version(0, 15, 12), version)
    assertEquals(0, executor.executedCommands.size)
  }

  @Test
  fun `version should be probed from the binary when LSP serverInfo is missing`() {
    val executor = FakeProcessExecutor(output = "tinymist 0.13.14")
    val server = mockedServer(null, mockedDescriptor(Path.of("/usr/bin/tinymist")))

    val version = tinymistVersionOf(server, executor, mutableMapOf())

    assertEquals(Version(0, 13, 14), version)
    assertEquals(1, executor.executedCommands.size)
    assertEquals("/usr/bin/tinymist", executor.executedCommands[0].exePath)
    assertEquals("-V", executor.executedCommands[0].parametersList.list.first())
  }

  @Test
  fun `probed version should be cached per binary path`() {
    val executor = FakeProcessExecutor(output = "tinymist 0.13.14")
    val server = mockedServer(null, mockedDescriptor(Path.of("/usr/bin/tinymist")))
    val cache = mutableMapOf<Path, Version>()

    tinymistVersionOf(server, executor, cache)
    val second = tinymistVersionOf(server, executor, cache)

    assertEquals(Version(0, 13, 14), second)
    assertEquals(1, executor.executedCommands.size)
  }

  @Test
  fun `version should be null when the descriptor is not a tinymist descriptor`() {
    val executor = FakeProcessExecutor()
    val server = mockedServer(null, mockk<LspServerDescriptor>())

    assertNull(tinymistVersionOf(server, executor, mutableMapOf()))
    assertEquals(0, executor.executedCommands.size)
  }

  @Test
  fun `version should be null when the binary probe fails`() {
    val executor = FakeProcessExecutor(output = "", exitCode = 1)
    val server = mockedServer(null, mockedDescriptor(Path.of("/usr/bin/tinymist")))

    assertNull(tinymistVersionOf(server, executor, mutableMapOf()))
    assertEquals(1, executor.executedCommands.size)
  }
}
