package controllers

import java.io.File
import java.util.Properties
import javax.inject._

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
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
    var part = request.body.file("name")
    val fileOption = part.map {
      case FilePart(key, filename, contentType, file) =>
        Logger.info(s"key = ${key}, filename = ${filename}, contentType = ${contentType}, file = $file")
        processTempFile(file, filename)
    }
    val fileSize = if (fileOption.isDefined)
      if (fileOption.get.isDefined) fileOption.get.get.length() else "Cannot process file"
    else "No file"
    Ok(s"file size = ${fileSize}")
  }

  private def processTempFile(tempFile: TemporaryFile, fileName: String): Option[File] = {
    val fileMove = moveToOutputFolder(tempFile, fileName)
    if (fileMove.isDefined) {
      sendMessage(fileMove.get.getAbsolutePath)
    }
    fileMove
  }

  private def moveToOutputFolder(tempFile: TemporaryFile, fileName: String): Option[File] = {
    val outputFolderConfig = configuration.getString("output.folder")
    if (outputFolderConfig.isDefined) {
      val outputFolderStr = outputFolderConfig.get
      val outputFolder = new File(outputFolderStr)
      if (outputFolder.exists && outputFolder.isDirectory) {
        Logger.info(s"'output.folder' = ${outputFolderStr}")
        val checkNewFile = new File(s"${outputFolder.getAbsolutePath}/${fileName}")
        if (checkNewFile.exists()) {
          Logger.error(s"File '${checkNewFile.getAbsolutePath}' already exists")
          None
        } else {
          val newFile = tempFile.moveTo(checkNewFile)
          Logger.info(s"new file = ${newFile.getAbsolutePath}")
          Option(newFile)
        }
      } else {
        Logger.error(s"invalid 'output.folder' = ${outputFolderStr}")
        None
      }
    } else {
      Logger.error("variable 'output.folder' is missing")
      None
    }
  }


  private def sendMessage(message: String): Unit = {

    if (configuration.getBoolean("kafka.enable").get) {
      Logger.info(s"Sending message: $message")

      val topicName = "datatopic"

      val props = new Properties()
      props.put("bootstrap.servers", "localhost:9092")
      props.put("acks", "all")
      props.put("retries", 0.asInstanceOf[Integer])
      props.put("batch.size", 16384.asInstanceOf[Integer])
      props.put("linger.ms", 0.asInstanceOf[Integer])
      props.put("buffer.memory", 33554432.asInstanceOf[Integer])
      props.put("max.block.ms", long2Long(10000))
      props.put("request.timeout.ms", 10000.asInstanceOf[Integer])
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

      val producer = new KafkaProducer[String, String](props);
      val sendFuture = producer.send(new ProducerRecord[String, String](topicName, null, message))
      val meta = sendFuture.get()
      Logger.info(s"send checksum '${meta.checksum()}'")
      producer.close()
    } else {
      Logger.info("Kafka configuration is disabled")
    }

  }


}
