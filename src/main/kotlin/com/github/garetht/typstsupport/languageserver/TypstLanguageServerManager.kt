package com.github.garetht.typstsupport.languageserver

import com.github.garetht.typstsupport.previewserver.TinymistPreviewServerManager
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerState
import com.intellij.platform.lsp.api.LspServerSupportProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val LOG = logger<TypstLanguageServerManager>()


class TypstLanguageServerManager : LanguageServerManager {
  override suspend fun initialStart(project: Project) {
    val providerClass = TypstLspServerSupportProvider::class.java
    val manager = LspServerManager.getInstance(project)

    // Restarting the language server kills its preview servers, so forget the
    // addresses cached for the previous process.
    TinymistPreviewServerManager.getInstance().invalidateServers()

    manager.stopAndRestartIfNeeded(providerClass)
    repaintOnIntialize(manager, project, providerClass)
  }

  companion object {
    private val languageServerPollDelay = 300.milliseconds
    private val languageServerPollTimeout = 15.seconds

    suspend fun repaintOnIntialize(
      manager: LspServerManager,
      project: Project,
      cls: Class<out LspServerSupportProvider>
    ) {
      waitForServer(manager, cls)
      restartCodeAnalyzer(project)
    }

    private suspend fun restartCodeAnalyzer(project: Project) {
      withContext(Dispatchers.EDT) { DaemonCodeAnalyzer.getInstance(project).restart() }
    }

    suspend fun waitForServer(
      manager: LspServerManager,
      cls: Class<out LspServerSupportProvider>
    ): LspServer? = withTimeoutOrNull(languageServerPollTimeout) {
      var result: LspServer? = null
      while (result == null) {
        val servers = manager.getServersForProvider(cls)
        val targetServer =
          servers.find { server -> server.providerClass.canonicalName == cls.canonicalName }

        LOG.info("Tinymist target server: $targetServer, state: ${targetServer?.state}, class: ${cls.canonicalName}")

        if (targetServer?.state == LspServerState.Running) {
          result = targetServer
        } else {
          delay(languageServerPollDelay)
        }
      }
      result
    }
  }
}

