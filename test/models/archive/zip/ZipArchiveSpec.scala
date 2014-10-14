package models.archive.zip

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import models.archive.ArchiveEntry
import java.io.ByteArrayInputStream
import models.archive.StreamReader

class ZipArchiveSpec extends Specification {

  "ZipArchive" should {

    "compute size of archive" in new ZipArchiveContext {
      archive.size must be equalTo archiveSize
    }

    "write to archive to stream" in new ZipArchiveContext {
      val output = readStream(archive.stream)
      
      output.length must be equalTo archiveSize
    }
  }
}

trait ZipArchiveContext extends Scope with StreamReader {

  val fileNameLength = 10
  val numberOfEntries = 10
  val fileSize = 2048

  val archiveSize = numberOfEntries * (30 + fileNameLength + fileSize + 46 + fileNameLength) + 22

  val data = Array.fill(fileSize)(0xda.toByte)

  val entries = Seq.tabulate(numberOfEntries)(n => ArchiveEntry(fileSize, f"file-$n%05d", fileStream _))

  val archive = new ZipArchive(entries)

  def fileStream = new ByteArrayInputStream(data)

}