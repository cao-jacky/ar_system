package fi.fivegear.remar.network;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import fi.fivegear.remar.Constants;
import fi.fivegear.remar.MainActivity;
import fi.fivegear.remar.activities.AugmentedRealityActivity;
import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.RequestEntry;

import static fi.fivegear.remar.Constants.ACK_SIZE;
import static fi.fivegear.remar.Constants.IMAGE_DETECT;
import static fi.fivegear.remar.Constants.IMAGE_DETECT_COMPLETE;
import static fi.fivegear.remar.Constants.IMAGE_DETECT_SEGMENTED;
import static fi.fivegear.remar.Constants.MESSAGE_META;
import static fi.fivegear.remar.Constants.PACKET_STATUS;
import static fi.fivegear.remar.activities.AugmentedRealityActivity.currFrame;

public class TransmissionTask extends Activity implements Runnable {
    private float MAX_UDP_LENGTH = 50000;
    private float currByteBufferLength;

    private int dataType;
    private int frmID;
    private byte[] frmdataToSend;
    private byte[] frmid;
    private byte[] frmsize;
    private byte[] packetContent;
    private byte[] messageType;
    private int datasize;
    private Mat frameData;
    private String selectedProtocol;
    private final DatagramChannel datagramChannel;
    private final SocketChannel socketChannel;

    private SocketAddress serverAddressUDP;
    private SocketAddress serverAddressTCP;

    public Mat YUVMatTrans, YUVMatScaled, GrayScaled;
    public Mat originalDataShape;
    private long time;

    private Context context;
    private SharedPreferences sharedPreferencesSetup;
    private String currSessionNumber;

    private DatabaseHelper requestsDatabase;
    private String serverIP;
    private int serverPort;

    private String currLocation;

    private int currHeight, currWidth;
    private Size imageResolution;
    private Size imageSize;

    private ByteBuffer ackPacket = ByteBuffer.allocate(ACK_SIZE);
    private byte[] ack;
    private byte[] tmp = new byte[4];

    private MainActivity mainActivity = new MainActivity();

    private boolean isTCPConnectedServer = false;

    public TransmissionTask(String selectedProtocol, DatagramChannel datagramChannel, SocketChannel socketChannel,
                            SocketAddress serverAddressUDP, SocketAddress serverAddressTCP, Context context, DatabaseHelper requestsDatabase,
                            String serverIP, int serverPort) {
        this.selectedProtocol = selectedProtocol;
        this.datagramChannel = datagramChannel;
        this.socketChannel = socketChannel;
        this.serverAddressUDP = serverAddressUDP;
        this.serverAddressTCP = serverAddressTCP;
        this.context = context;
        this.requestsDatabase = requestsDatabase;
        this.serverIP = serverIP;
        this.serverPort = serverPort;

        originalDataShape = new Mat(Constants.previewHeight * 3 / 2, Constants.previewWidth, CvType.CV_8UC1);

//        YUVMatTrans = new Mat(Constants.previewHeight + Constants.previewHeight / 2, Constants.previewWidth, CvType.CV_8UC1);
        YUVMatTrans = new Mat(Constants.previewHeight * 3 / 2, Constants.previewWidth, CvType.CV_8UC1);
//        YUVMatScaled = new Mat((Constants.previewHeight + Constants.previewHeight / 2), Constants.previewWidth, CvType.CV_8UC1);
        YUVMatScaled = new Mat((Constants.previewHeight * 3 / 2) / Constants.recoScale, Constants.previewWidth / Constants.recoScale, CvType.CV_8UC1);
        GrayScaled = new Mat(Constants.previewHeight / Constants.recoScale, Constants.previewWidth / Constants.recoScale, CvType.CV_8UC1);
    }

    public void setData(int frmID, Mat frameData){
        this.frmID = frmID;
        this.frameData = frameData;

        if (this.frmID <= 5) {
            dataType = MESSAGE_META;
        }
        else dataType = IMAGE_DETECT;
    }

    public static byte[] toBytes(String data, int length) {
        byte[] result = new byte[length];
        System.arraycopy(data.getBytes(), 0, result, length - data.length(), data.length());
        return result;
    }

    @Override
    public void run() {
        // pulling session number
        sharedPreferencesSetup = context.getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSetup.getString("currSessionNumber", "0");
        currLocation = sharedPreferencesSetup.getString("currLocation", "0");

        // obtaining the user selected resolutions
        currHeight = sharedPreferencesSetup.getInt("currHeight", 1920);
        currWidth = sharedPreferencesSetup.getInt("currWidth", 1080);

        // set MAX_UDP_LENGTH with user selected variable
        MAX_UDP_LENGTH = Float.parseFloat(sharedPreferencesSetup.getString("currUDPPayload", "50000"));

        if (!isTCPConnectedServer) {
            try {
                socketChannel.connect(serverAddressTCP);
                socketChannel.configureBlocking(false);
                isTCPConnectedServer = true;
            } catch (IOException e) {
                e.printStackTrace();
                isTCPConnectedServer = false;
            }
        }

        AugmentedRealityActivity.uploadStatus.setImageAlpha(0);
        if (dataType == IMAGE_DETECT) {
            YUVMatTrans = frameData; // setting new mat array with the data from mat from camera data

            imageResolution = originalDataShape.size(); // this is the maximum possible size

//            imageSize = new Size(imageResolution.width,imageResolution.height); //the dst image size
            imageSize = new Size(currHeight,currWidth);

//            Imgproc.resize(YUVMatTrans, YUVMatScaled, YUVMatScaled.size(), 0, 0, Imgproc.INTER_LINEAR);
            Imgproc.resize(YUVMatTrans, YUVMatScaled, imageSize, 0, 0, Imgproc.INTER_LINEAR);
            Imgproc.cvtColor(YUVMatScaled, GrayScaled, Imgproc.COLOR_YUV420sp2GRAY);
            Core.flip(GrayScaled.t(), GrayScaled, 1); // rotate 90 deg clockwise
        }

        if (dataType == MESSAGE_META) {
            datasize = 0;
            frmdataToSend = null;
        } else if (dataType == IMAGE_DETECT) {
            MatOfByte imgbuff = new MatOfByte();
            Highgui.imencode(".jpg", GrayScaled, imgbuff, Constants.Image_Params);

            datasize = (int) (imgbuff.total() * imgbuff.channels());
            frmdataToSend = new byte[datasize];

            imgbuff.get(0, 0, frmdataToSend);
        }

        packetContent = new byte[12 + datasize];

        frmid = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(frmID).array();
        frmsize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(datasize).array();

        System.arraycopy(frmid, 0, packetContent, 4, 4);
        System.arraycopy(frmsize, 0, packetContent, 8, 4);

        if (frmdataToSend != null)
            System.arraycopy(frmdataToSend, 0, packetContent, 12, datasize);

        try {
            currByteBufferLength = packetContent.length; //buffer.remaining();
            if (selectedProtocol.contains("UDP")) {
                if (dataType == MESSAGE_META) {
                    messageType = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(MESSAGE_META).array();
                    System.arraycopy(messageType, 0, packetContent, 0, 4);

                    ByteBuffer buffer = ByteBuffer.allocate(packetContent.length).put(packetContent);
                    buffer.flip();
                    datagramChannel.send(buffer, serverAddressUDP);
                }

                if (currByteBufferLength >= MAX_UDP_LENGTH) {
                    int numPackets = (int)Math.ceil(currByteBufferLength / MAX_UDP_LENGTH);

                    int currOffset = 0;
                    int currSegmentNumber = 1;
                    byte[] totalSegments = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(numPackets).array();

                    messageType = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(IMAGE_DETECT_SEGMENTED).array();

                    while (currOffset < currByteBufferLength) {
                        if (currOffset == 0) {
                            // if first segment, set SharedPreferences boolean to false, i.e., not acknowledged
                            SharedPreferences udpAckSP = context.getSharedPreferences("udpAckSP", Context.MODE_PRIVATE);
                            udpAckSP.edit().putBoolean("currUDPAck", false).apply();
                        }

                        byte[] currPacketSegment = Arrays.copyOfRange(packetContent, currOffset, (int)(currOffset+MAX_UDP_LENGTH));
                        byte[] currSegmentNumberInBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(currSegmentNumber).array();
                        byte[] currSegmentContent = new byte[24 + currPacketSegment.length];

                        byte[] segmentPacketLength;

                        if (numPackets == currSegmentNumber) {
                            segmentPacketLength = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int)(currByteBufferLength-currOffset)).array();
                        } else {
                            segmentPacketLength = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(currPacketSegment.length).array();
                        }

                        System.arraycopy(messageType, 0, currSegmentContent, 0, 4);
                        System.arraycopy(frmid, 0, currSegmentContent, 4, 4);
                        System.arraycopy(segmentPacketLength, 0, currSegmentContent, 8,4);
                        System.arraycopy(totalSegments, 0, currSegmentContent, 12, 4);
                        System.arraycopy(currSegmentNumberInBytes, 0, currSegmentContent, 16, 4);
                        System.arraycopy(currPacketSegment, 0, currSegmentContent, 20, (int)MAX_UDP_LENGTH);

                        // add integer to end of segment to verify on server whether full segment received
                        byte[] endInteger = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(33).array();

                        // change location of end byte depending on if final packet
                        if (numPackets == currSegmentNumber) {
                            System.arraycopy(endInteger, 0, currSegmentContent, (int)(currByteBufferLength-currOffset)+20, 4);
                        } else {
                            System.arraycopy(endInteger, 0, currSegmentContent, (int)MAX_UDP_LENGTH+20, 4);
                        }

                        ByteBuffer buffer = ByteBuffer.allocate(currSegmentContent.length).put(currSegmentContent);
                        buffer.flip();
                        datagramChannel.send(buffer, serverAddressUDP);

                        SharedPreferences udpAckSP = context.getSharedPreferences("udpAckSP", Context.MODE_PRIVATE);
                        Boolean udpAckStatus = udpAckSP.getBoolean("currUDPAck", false);
                        while(!udpAckStatus) {
                            // constantly checking the variable and prevents new data from being sent
                            udpAckStatus = udpAckSP.getBoolean("currUDPAck", false);
                            try {
                                if (datagramChannel.receive(ackPacket) != null) {
                                    ack = ackPacket.array();

                                    System.arraycopy(ack, 0, tmp, 0, 4);
                                    int messageType = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                                    if (messageType == PACKET_STATUS) {
//                                        System.arraycopy(ack, 4, tmp, 0, 4);
//                                        int frameID = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

                                        System.arraycopy(ack, 12, tmp, 0, 4);
                                        int packetStatus = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

                                        if (packetStatus == 1) {
//                                            SharedPreferences udpAckSetting = context.getSharedPreferences("udpAckSP", Context.MODE_PRIVATE);
                                            udpAckSP.edit().putBoolean("currUDPAck", true).apply();
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        currOffset += MAX_UDP_LENGTH;
                        currSegmentNumber ++;
                    }
                } else if (dataType != MESSAGE_META) {
                    messageType = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(IMAGE_DETECT_COMPLETE).array();

                    System.arraycopy(messageType, 0, packetContent, 0, 4);

                    ByteBuffer buffer = ByteBuffer.allocate(packetContent.length).put(packetContent);
                    buffer.flip();
                    datagramChannel.send(buffer, serverAddressUDP);

                    SharedPreferences udpAckSP = context.getSharedPreferences("udpAckSP", Context.MODE_PRIVATE);
                    Boolean udpAckStatus = udpAckSP.getBoolean("currUDPAck", false);
                    while(!udpAckStatus) {
                        // constantly checking the variable and prevents new data from being sent
                        udpAckStatus = udpAckSP.getBoolean("currUDPAck", false);

                        try {
                            if (datagramChannel.receive(ackPacket) != null) {
                                ack = ackPacket.array();

                                System.arraycopy(ack, 0, tmp, 0, 4);
                                int messageType = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                                if (messageType == PACKET_STATUS) {
//                                    System.arraycopy(ack, 4, tmp, 0, 4);
//                                    int frameID = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

                                    System.arraycopy(ack, 12, tmp, 0, 4);
                                    int packetStatus = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

                                    if (packetStatus == 1) {
                                        udpAckSP.edit().putBoolean("currUDPAck", true).apply();
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }

            if (selectedProtocol.contains("TCP")) {
                // attempt TCP connection, if failure, change the selected protocol to UDP and declare to user
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(packetContent.length).put(packetContent);
                    buffer.flip();
                    socketChannel.write(buffer);
                } catch (Exception e) {
                    sharedPreferencesSetup = context.getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);
                    SharedPreferences.Editor sessionEditor = sharedPreferencesSetup.edit();
                    sessionEditor.putString("currProtocol", "UDP");
                    sessionEditor.apply();

                    Toast alertMessage = Toast.makeText(context, "TCP connection failed, reverted to UDP - please restart app", Toast.LENGTH_LONG);
                    TextView v = alertMessage.getView().findViewById(android.R.id.message);
                    if( v != null) v.setGravity(Gravity.CENTER);
                    alertMessage.show();
                }
            }

            time = System.currentTimeMillis();

            // Appending the sent information into the database table
            RequestEntry newRequestEntry = new RequestEntry("", Integer.parseInt(currSessionNumber),
                    frmID, String.valueOf(time), serverIP, serverPort, datasize+12,
                    currLocation, selectedProtocol);
            long newRequestsEntry_id = requestsDatabase.createRequestEntry(newRequestEntry);

            AugmentedRealityActivity.uploadStatus.setImageAlpha(255);
            runThread();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runThread() {
        new Thread() {
            public void run() {
                try {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            currFrame.setText("F" + frmID);
                        }
                    });
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
