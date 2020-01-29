import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

import java.nio.ByteBuffer
import java.nio.ByteOrder

// Server details
class ServerDetails {
    var ServerIP: String = "10.42.0.1"
    var ServerPort: Int = 52727

    constructor()
    init{}
}

val ServerSettings = ServerDetails()

//val threadWithRunnable = Thread(udp_DataArrival())
//threadWithRunnable.start()

fun main() {
    // Preparing UDP socket
    val socket = DatagramSocket(51919)
    socket.broadcast = true

    // load query.jpg
    var queryImage: BufferedImage = ImageIO.read(File("./query.jpg"))

    // convert loaded image to bytes array
    var queryImageBytes: ByteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.write(queryImage, "jpg", queryImageBytes)
    queryImageBytes.flush()
    var imageBytes: ByteArray = queryImageBytes.toByteArray() 
    queryImageBytes.close()

    var packetToSend = ByteArray(12+imageBytes.size)
    val dataType: Int = 2 // IMAGE_DETECT is given by 2

    var loopCounter: Int = 1 

    // send image as UDP packet to server at an interval
    val ses = Executors.newScheduledThreadPool(10)
    ses.scheduleAtFixedRate(Runnable {
        println("Current frame ID is " + loopCounter)
        // var frameIDBytes: ByteArray = loopCounter.toString().toByteArray(charset) 
        var frameIDBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(loopCounter).array()
        frameIDBytes.copyInto(packetToSend, 0, 0, 4)

        var dataTypeBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dataType).array()
        dataTypeBytes.copyInto(packetToSend, 4, 0, 4)

        var frameSizeBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(imageBytes.size).array()
        frameSizeBytes.copyInto(packetToSend, 8, 0, 4)
        
        imageBytes.copyInto(packetToSend, 12, 0, imageBytes.size)
        
        val sendPacket = DatagramPacket(packetToSend, packetToSend.size, 
            InetAddress.getByName(ServerSettings.ServerIP), ServerSettings.ServerPort)
        socket.send(sendPacket)

        loopCounter = loopCounter + 1
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