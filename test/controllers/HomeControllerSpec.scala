package controllers

import java.io.File
import java.nio.file.Files

import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import org.scalatestplus.play._
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData
import play.api.test.Helpers._
import play.api.test._


/**
  * Spec test for home controller
  */
class HomeControllerSpec extends PlaySpec with OneServerPerSuite {

  implicit override lazy val app
  = new GuiceApplicationBuilder().configure(Map("kafka.enable" -> "false")).build()

  "HomeController GET" should {

    "render the index page from a new instance of controller" in {
      val messages = app.injector.instanceOf[MessagesApi]
      val conf = app.injector.instanceOf[Configuration]
      val controller = new HomeController(messages, conf)
      val home = controller.index().apply(FakeRequest())

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include("Upload File")
    }

    "render the index page from the application" in {
      val controller = app.injector.instanceOf[HomeController]
      val home = controller.index().apply(FakeRequest())

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include("Upload File")
    }

    "render the index page from the router" in {
      // Need to specify Host header to get through AllowedHostsFilter
      val request = FakeRequest(GET, "/").withHeaders("Host" -> "localhost")
      val home = route(app, request).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include("Upload File")

    }
  }

  "HomeController POST" should {

    "upload a file successfully" in {

      val filename = "hello.txt"
      val msg = "hello world"

      val tmpFile = java.io.File.createTempFile("prefix", "txt")
      tmpFile.deleteOnExit()
      Files.write(tmpFile.toPath, msg.getBytes())

      val url = s"http://localhost:${Helpers.testServerPort}/upload"
      val responseFuture = ws.url(url).post(postSource(tmpFile, filename))
      val response = await(responseFuture)
      response.status mustBe OK
      response.body mustBe "file size = 11"

      app.configuration.getBoolean("kafka.enable") mustBe Some(false)

      val request = FakeRequest(POST, "/upload").withHeaders("Host" -> "localhost")
      val home = route(app, request).get

      // Check output file
      val checkFile = new File(app.configuration.getString("output.folder").get + "/" + filename)
      checkFile.exists() mustBe true
      checkFile.length() mustBe 11

      // Remove file
      checkFile.delete()
    }

  }

  def postSource(tmpFile: File, filename: String): Source[MultipartFormData.Part[Source[ByteString, _]], _] = {
    import play.api.mvc.MultipartFormData._
    Source(FilePart("name", filename, Option("text/plain"),
      FileIO.fromPath(tmpFile.toPath)) :: DataPart("key", "value") :: List())
  }

  def ws = app.injector.instanceOf(classOf[WSClient])
}
