package controllers

import java.io.File
import java.nio.file.attribute.PosixFilePermission.{OWNER_READ, OWNER_WRITE}
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Path}
import java.util
import javax.inject._

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.ByteString
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.i18n.MessagesApi
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.api.{Logger, i18n}
import play.core.parsers.Multipart.FileInfo

import scala.concurrent.Future

case class FormData(name: String)

/**
  * Home controller
  */
@Singleton
class HomeController @Inject()(val messagesApi: MessagesApi) extends Controller with i18n.I18nSupport {

  val form = Form(
    mapping(
      "name" -> nonEmptyText
    )(FormData.apply)(FormData.unapply)
  )

  /**
    * Index page
    */
  def index =
    Action { implicit request =>
      Logger.info("index page")
      Ok(views.html.index(form))
    }

  type FilePartHandler[A] = FileInfo => Accumulator[ByteString, FilePart[A]]

  private def handleFilePartAsFile: FilePartHandler[File] = {
    case FileInfo(partName, filename, contentType) =>
      val attr = PosixFilePermissions.asFileAttribute(util.EnumSet.of(OWNER_READ, OWNER_WRITE))
      val path: Path = Files.createTempFile("multipartBody", "tempFile", attr)
      val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(path)
      val accumulator: Accumulator[ByteString, IOResult] = Accumulator(fileSink)
      accumulator.map {
        case IOResult(count, status) =>
          Logger.info(s"count = $count, status = $status")
          FilePart(partName, filename, contentType, path.toFile)
      }(play.api.libs.concurrent.Execution.defaultContext)
  }

  /**
    * Uploads a file.
    *
    * @return
    */
  def upload = Action(parse.multipartFormData(handleFilePartAsFile)) { implicit request =>
    val fileOption = request.body.file("name").map {
      case FilePart(key, filename, contentType, file) =>
        Logger.info(s"key = ${key}, filename = ${filename}, contentType = ${contentType}, file = $file")
        val data = processFile(file)
        data
    }

    Ok(s"file size = ${fileOption.getOrElse("no file")}")
  }

  private def processFile(file: File) = {
    val size = Files.size(file.toPath)
    Logger.info(s"size = ${size}")
    Files.deleteIfExists(file.toPath)
    size
  }
}
