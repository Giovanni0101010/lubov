package hi

import spray.json._
import com.github.tototoshi.csv._
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.io.{BufferedWriter, File, FileWriter}

object Parser extends App {

  //{"fullName": "", "phoneNumber": "", "email": ""}
  case class Result(fullName: String, phoneNumber: String, email: String)


  implicit val personJsonFormat = new RootJsonFormat[Result] {
    def write(foo: Result) =
      JsObject(
        "fullName" -> Option(foo.fullName).map(JsString(_)).getOrElse(JsNull),
        "phoneNumber" -> Option(foo.phoneNumber).map(JsString(_)).getOrElse(JsNull),
        "email" -> Option(foo.email).map(JsString(_)).getOrElse(JsNull)
      )

    def read(value: JsValue): Result = value match {
      case _ => Result("1", "2", "3")
    }
  }

  val d = new File("file")

  var i = 1
  d.listFiles.filter(_.isFile).filterNot(_.getName.startsWith(".")).toList.foreach{
    file =>

      val csv = scala.io.Source.fromFile(file.getPath)
      case class Source(email: String, phone: String)
      val reader = CSVReader.open(csv)

      val res = reader.all().map{
        list =>
          try {

            //val name = s"${list(1)} ${list(2)} ${list(3)}"
            //val name = s"${list(6)} ${list(7)}"
            val name = s"${list(0)}"

            var email = if(list(2).nonEmpty) list(2) else list(2)
            email = if (email.contains("@")) email else null
            //var email = null

            //email = if(email.contains(",")) email.split(",").head else email

            //Some(Result(name, null, email))

            //var phone = if(list(5).nonEmpty) list(5) else list(4)
            var phone = list(1)

            //phone = if (phone.contains(",")) phone.split(",").head else phone

            phone = phone.replaceAll("\u00a0", "")
              .replaceAll("-", "")
              .replaceAll("\\(", "")
              .replaceAll("\\)", "")
              .replaceAll("\\+", "")
              .replaceAll("\t", "")
              .replaceAll(" ", "").trim

            //code 7495

            if (phone.matches("^([0-9]+)$") && phone.length > 4) {
              if(phone.length > 4 && phone.length <= 7) {
                phone = "7495" + phone
              }

              if (!phone.startsWith("7")) {
                phone = "7" + phone
              }
              if (!phone.startsWith("+")) {
                phone = "+" + phone
              }

              Some(Result(name, phone, email))
            } else
              None
          } catch {
            case _ =>
              None
          }

      }.filter(_.nonEmpty).toJson

      val w = new BufferedWriter(new FileWriter("file/" + s"${file.getName}.json"))

      w.write(res.prettyPrint)
      w.close

      i = i + 1
  }

}
