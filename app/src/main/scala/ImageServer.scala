import java.io.FileInputStream
import java.net.InetSocketAddress
import java.io.File

import com.twitter.util.{Await, Future, Time}
import com.twitter.finagle.Service
import com.twitter.finagle.builder.{ServerBuilder, Server}
import com.twitter.finagle.http._
import com.twitter.finagle.http.service.RoutingService
import com.twitter.server.TwitterServer

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferOutputStream, ChannelBuffer}
import org.jboss.netty.handler.codec.http.HttpMethod._

object ImageServer extends TwitterServer {

  val imageMap = Map(("xsmall", imageLoader("choco-xsmall.jpg")), ("small", imageLoader("choco-small.jpg")), ("medium", imageLoader("choco-medium.jpg")), ("large", imageLoader("choco-large.jpg")))

  def imageLoader(fileName: String): ChannelBuffer = {
    val img = new File(fileName)
    val imgStream = new FileInputStream(img)
    val byteArray = new Array[Byte](img.length().asInstanceOf[Int])

    imgStream.read(byteArray)

    val dynBuffer: ChannelBuffer = ChannelBuffers.dynamicBuffer()
    dynBuffer.clear()
    val buffOutStream = new ChannelBufferOutputStream(dynBuffer)

    buffOutStream.write(byteArray, 0, byteArray.length)
    buffOutStream.buffer()
  }

  class imageRetrieval(name: String) extends Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
      val response = Response()

      val xWarningHeader = request.headers().get("X-WARNING")

      val byteArray = imageMap.get(name).get

      response.setContent(imageMap.get(name).get)
      response.headers().add("Content-Type", "application/image")
//      response.headers().add("Content-Length", byteArray.length)
      if (xWarningHeader != null && xWarningHeader.length > 0)
        response.headers().add("X-WARNING", xWarningHeader)
      Future.value(response)
    }
  }

  class defaultPath extends Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
//      log.info("General request at ", Time.now)
      val xWarningHeader = request.headers().get("X-WARNING")
      val response = Response()
      response.setContentString("Nothing to show")
      if (xWarningHeader != null && xWarningHeader.length > 0)
        response.headers().add("X-WARNING", xWarningHeader)
      Future(response)
    }
  }

  class fileUpload extends Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
      val response = Response()
      response.setStatusCode(204)
      val xWarningHeader = request.headers().get("X-WARNING")
      if (xWarningHeader != null && xWarningHeader.length > 0)
        response.headers().add("X-WARNING", xWarningHeader)
      Future(response)
    }
  }

  val routingService = RoutingService.byMethodAndPath {
    case (GET, "/images/xsmall") => new imageRetrieval("xsmall")
    case (GET, "/images/small") => new imageRetrieval("small")
    case (GET, "/images/medium") => new imageRetrieval("medium")
    case (GET, "/images/large") => new imageRetrieval("large")
    case (GET, _) => new defaultPath
    case (POST, _) => new fileUpload
  }

  def main {
    val server: Server = ServerBuilder()
      .codec(RichHttp[Request](Http()))
      .bindTo(new InetSocketAddress(8888))
      .name("ServerTest")
      .build(routingService)

    onExit {
      server.close()
    }
    Await.ready(server)
  }
}
