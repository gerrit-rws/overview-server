package com.overviewdocs.jobhandler.filegroup.task

import java.nio.file.Path
import java.nio.file.Paths

/** Refers to the temporary directory where the documentset-worker stores files during document conversion */
object TempDirectory {
  private val tmpDir = System.getProperty("java.io.tmpdir")

  val path: Path = Paths.get(tmpDir, "overview-documentset-worker")

  def create: Unit = DirectoryCleaner.createCleanDirectory(path)
}
