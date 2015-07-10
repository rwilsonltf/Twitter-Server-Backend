import java.io.FileInputStream
import java.net.InetSocketAddress
import java.time.Instant
import java.io.File

import com.twitter.util.{Await, Future}
import com.twitter.finagle.Service
import com.twitter.finagle.builder.{ServerBuilder, Server}
import com.twitter.finagle.http._
import com.twitter.finagle.http.service.RoutingService
import com.twitter.server.TwitterServer

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferOutputStream, ChannelBuffer}
import org.jboss.netty.handler.codec.http.HttpMethod._

object ImageServer extends TwitterServer {

  class imageRetrievalService(fileName: String) extends Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
//      log.info("Request at " + Time.now + " on " + fileName)
      val response = Response()
      val img = new File(fileName)

      val imgStream = new FileInputStream(img)

      val dynBuffer: ChannelBuffer = ChannelBuffers.dynamicBuffer()
      dynBuffer.clear()
      dynBuffer.ensureWritableBytes(img.length().toInt)
      val buffOutStream = new ChannelBufferOutputStream(dynBuffer)

      var byteBuf = new Array[Byte](4096)
      var bytesRead = 0

      while (bytesRead != -1) {
        bytesRead = imgStream.read(byteBuf)
        if (bytesRead != -1)
          buffOutStream.write(byteBuf, 0, bytesRead)
      }

      response.setContent(buffOutStream.buffer())
      response.headers().add("Content-Type", "application/image")
      response.headers().add("Content-Length", img.length())
      Future.value(response)
    }
  }

  class defaultPath extends Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
      log.info("General request at ", Instant.now())

      val response = Response()
      response.setContentString("Nothing to show")
      Future(response)
    }
  }

  class fileUpload extends Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
      val response = Response()
      response.setStatusCode(204)
      Future(response)
    }
  }

  val routingService = RoutingService.byMethodAndPath {
    case (GET, "/images/xsmall") => new imageRetrievalService("choco-xsmall.jpg")
    case (GET, "/images/small") => new imageRetrievalService("choco-small.jpg")
    case (GET, "/images/medium") => new imageRetrievalService("choco-medium.jpg")
    case (GET, "/images/large") => new imageRetrievalService("choco-large.jpg")
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
