package models.export.format

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.iteratee.{Enumerator,Iteratee}
import play.api.test.{FutureAwaits,DefaultAwaitTimeout}

import models.export.rows.Rows

class CsvFormatSpec extends Specification with FutureAwaits with DefaultAwaitTimeout {
  trait StringScope extends Scope {
    val headers: Array[String] = Array("col1", "col2", "col3")
    val rowRows: Enumerator[Array[String]] = Enumerator(
      Array("val1", "val2", "val3"),
      Array("val4", "val5", "val6")
    )

    val rows = Rows(headers, rowRows)

    def bytes: Array[Byte] = await(CsvFormat.bytes(rows).run(Iteratee.consume()))

    lazy val parsedCsv: Seq[Array[String]] = {
      import scala.collection.JavaConverters.asScalaBufferConverter
      // Drop the UTF-8 BOM when reading, so we can do string comparisons
      val csvString = new String(bytes.drop(3), "utf-8")
      csvString.split("\r?\n").map(_.split(","))
    }
  }

  "CsvFormat" should {
    "have the correct content-type" in {
      CsvFormat.contentType must beEqualTo("""text/csv; charset="utf-8"""")
    }

    "export a UTF-8 byte-order marker, to help MS Excel on Windows" in new StringScope {
      bytes.slice(0, 3) must beEqualTo(Array(0xef, 0xbb, 0xbf).map(_.toByte))
    }

    "export headers" in new StringScope {
      parsedCsv(0) must beEqualTo(Array("col1", "col2", "col3"))
    }

    "export values" in new StringScope {
      parsedCsv(1) must beEqualTo(Array("val1", "val2", "val3"))
      parsedCsv(2) must beEqualTo(Array("val4", "val5", "val6"))
      parsedCsv.length must beEqualTo(3)
    }
  }
}
