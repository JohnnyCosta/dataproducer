package controllers

import java.io.File
import java.nio.file.Files

import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import org.scalatestplus.play._
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData
import play.api.test.Helpers._
import play.api.test._

/**
  * Spec test for home controller
  */
class HomeControllerSpec extends PlaySpec with OneServerPerSuite {

  "HomeController GET" should {

    "render the index page from a new instance of controller" in {
      val messages = app.injector.instanceOf[MessagesApi]
      val controller = new HomeController(messages)
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

      val tmpFile = java.io.File.createTempFile("prefix", "txt")
      tmpFile.deleteOnExit()
      val msg = "hello world"
      Files.write(tmpFile.toPath, msg.getBytes())

      val url = s"http://localhost:${Helpers.testServerPort}/upload"
      val responseFuture = ws.url(url).post(postSource(tmpFile))
      val response = await(responseFuture)
      response.status mustBe OK
      response.body mustBe "file size = 11"

      val request = FakeRequest(POST, "/upload").withHeaders("Host" -> "localhost")
      val home = route(app, request).get
    }

  }

  def postSource(tmpFile: File): Source[MultipartFormData.Part[Source[ByteString, _]], _] = {
    import play.api.mvc.MultipartFormData._
    Source(FilePart("name", "hello.txt", Option("text/plain"),
      FileIO.fromPath(tmpFile.toPath)) :: DataPart("key", "value") :: List())
  }

  def ws = app.injector.instanceOf(classOf[WSClient])
}
