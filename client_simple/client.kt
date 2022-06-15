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

import java.time.*

// Server details
var ServerIP: String = ""
var ServerPort: Int = 0

var input_file: String = ""

//val threadWithRunnable = Thread(udp_DataArrival())
//threadWithRunnable.start()

fun getRandomString(length: Int) : String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun get_curr_time(): String {
    return Instant.now().toEpochMilli().toString()
}

fun main(args: Array<String>) {
    // Generate random 4 character alphanumeric string for client ID
    var client_id = getRandomString(4)
    println(get_curr_time() + " [STATUS $client_id client]: Client created and assigned ID of $client_id")

    //Printing all arguments passed through command line
    var arg_loop: Int = 0
    for (item in args){
        // setting server IP and then port 
        if (arg_loop == 0) {
            ServerIP = item
        }
        if (arg_loop == 1) {
            ServerPort = item.toInt()
        }
        if (arg_loop == 2) {
            input_file = item
        }
        arg_loop++  
    }
    println(get_curr_time() + " [STATUS $client_id client]: Details of main service provided: IP: $ServerIP, and port: $ServerPort")

    // Preparing UDP socket
    val socket = DatagramSocket()
    socket.broadcast = true
    println(get_curr_time() + " [STATUS $client_id client]: Client local host has binded port " + socket.getLocalPort())

    // establish an initial echo message
    val echoArray = ByteArray(30000)
    val echoPacket = DatagramPacket(echoArray, echoArray.size, 
        InetAddress.getByName(ServerIP), ServerPort)
    socket.send(echoPacket)
    println(get_curr_time() + " [STATUS $client_id client 0]: Sending initial echo message to main to self-register client details")

    // loading file provided by user
    var queryImage: BufferedImage = ImageIO.read(File(input_file))
    println(get_curr_time() + " [STATUS $client_id client]: Loading file $input_file to be sent to the system for analysis")

    // // convert loaded image to bytes array
    // var queryImageBytes: ByteArrayOutputStream = ByteArrayOutputStream()
    // ImageIO.write(queryImage, "jpg", queryImageBytes)
    // queryImageBytes.flush()
    // var imageBytes: ByteArray = queryImageBytes.toByteArray() 
    // queryImageBytes.close()

    // var packetToSend = ByteArray(12+imageBytes.size)
    // val dataType: Int = 2 // IMAGE_DETECT is given by 2

    // var loopCounter: Int = 1 

    // // send image as UDP packet to server at an interval
    // val ses = Executors.newScheduledThreadPool(10)
    // ses.scheduleAtFixedRate(Runnable {
    //     println("Current frame ID is " + loopCounter)
    //     // var frameIDBytes: ByteArray = loopCounter.toString().toByteArray(charset) 
    //     var frameIDBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(loopCounter).array()
    //     frameIDBytes.copyInto(packetToSend, 0, 0, 4)

    //     var dataTypeBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dataType).array()
    //     dataTypeBytes.copyInto(packetToSend, 4, 0, 4)

    //     var frameSizeBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(imageBytes.size).array()
    //     frameSizeBytes.copyInto(packetToSend, 8, 0, 4)
        
    //     imageBytes.copyInto(packetToSend, 12, 0, imageBytes.size)
        
    //     val sendPacket = DatagramPacket(packetToSend, packetToSend.size, 
    //         InetAddress.getByName(ServerSettings.ServerIP), ServerSettings.ServerPort)
    //     socket.send(sendPacket)

    //     loopCounter = loopCounter + 1
    //     println("sent at " + System.currentTimeMillis())
    // }, 0, 2, TimeUnit.SECONDS)

    // // receiving reply from server
    // var buffer: ByteArray = ByteArray(512)
    // // Keep a socket open to listen to all the UDP trafic that is destined for this port
    // val packet = DatagramPacket(buffer, buffer.size)
    // while (true){
    //     var socket_receive = socket.receive(packet)
    //     if (socket_receive != null) {
    //         println("received at " + System.currentTimeMillis())
    //     }
    // }

}
