package com.github.garetht.typstsupport.previewserver

import com.intellij.openapi.project.Project

interface PreviewServerManager {
  fun createServer(filepath: String, project: Project, callback: (String?) -> Unit)
  fun shutdownServer(filepath: String, project: Project)
  fun invalidateServers()
}
