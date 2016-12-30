package controllers

import java.io.File
import java.nio.file.Files
import javax.inject._

import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.i18n.MessagesApi
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.api.{Logger, i18n}

case class FormData(name: String)

/**
  * Home controller
  */
@Singleton
class HomeController @Inject()(val messagesApi: MessagesApi, configuration: play.api.Configuration) extends Controller with i18n.I18nSupport {

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

  /**
    * Upload action
    */
  def upload = Action(parse.multipartFormData) { request =>
    val fileOption = request.body.file("name").map {
      case FilePart(key, filename, contentType, file) =>
        Logger.info(s"key = ${key}, filename = ${filename}, contentType = ${contentType}, file = $file")
        processFile(file, filename).getOrElse("Error to process")
    }

    Ok(s"file size = ${fileOption.getOrElse("No file")}")
  }

  private def processFile(tempFile: TemporaryFile, fileName: String): Option[Long] = {
    val outputFolderOption = configuration.getString("outputFolder")
    outputFolderOption match {
      case Some(outputFolderStr) => {
        val outputFolder = new File(outputFolderStr)
        if (outputFolder.exists && outputFolder.isDirectory) {
          Logger.info(s"outputFolder = ${outputFolderStr}")
          val checkNewFile = new File(s"${outputFolder.getAbsolutePath}/${fileName}")
          if (checkNewFile.exists()) {
            Logger.error(s"File '${checkNewFile.getAbsolutePath}' already exists")
            None
          } else {
            val newFile = tempFile.moveTo(checkNewFile)
            Logger.info(s"new file = ${newFile.getAbsolutePath}")
            val size = Files.size(newFile.toPath)
            Logger.info(s"size = ${size}")
            Option(size.toLong)
          }
        } else {
          Logger.error(s"invalid outputFolder = ${outputFolderStr}")
          None
        }
      }
      case None => {
        Logger.error("variable 'outputFolder' is missing")
        None
      }
    }
  }
}
