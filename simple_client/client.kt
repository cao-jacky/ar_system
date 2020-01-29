import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

// Server details
class ServerDetails {
    var ServerIP: String = "10.42.0.1"
    var ServerPort: Int = 51717

    constructor()
    init{}
}

val ServerSettings = ServerDetails()

//val threadWithRunnable = Thread(udp_DataArrival())
//threadWithRunnable.start()

fun main() {
    println("Hello, World!")

    // Preparing UDP socket
    val socket = DatagramSocket()
    socket.broadcast = true

    // load query.jpg
    var queryImage: BufferedImage = ImageIO.read(File("./query.jpg"))

    // convert loaded image to bytes array
    var queryImageBytes: ByteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.write(queryImage, "jpg", queryImageBytes)
    queryImageBytes.flush()
    var imageBytes: ByteArray = queryImageBytes.toByteArray() 
    queryImageBytes.close()

    println(imageBytes)

    // send image as UDP packet to server at an interval
    val ses = Executors.newScheduledThreadPool(10)
    ses.scheduleAtFixedRate(Runnable {
        val sendPacket = DatagramPacket(imageBytes, imageBytes.size, 
            InetAddress.getByName(ServerSettings.ServerIP), ServerSettings.ServerPort)
        socket.send(sendPacket)
        println("sent at " + System.currentTimeMillis())
    }, 0, 2, TimeUnit.SECONDS)

    // receiving reply from server
    var buffer: ByteArray = ByteArray(512)
    // Keep a socket open to listen to all the UDP trafic that is destined for this port
    val packet = DatagramPacket(buffer, buffer.size)
    while (true){
        var socket_receive = socket.receive(packet)
        if (socket_receive != null) {
            println("received at " + System.currentTimeMillis())
        }
    }

}