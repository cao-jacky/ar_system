package fi.fivegear.remar.network;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.FFmpeg;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Objects;

import fi.fivegear.remar.Constants;
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
import static org.opencv.imgcodecs.Imgcodecs.IMWRITE_JPEG_QUALITY;
import static org.opencv.imgcodecs.Imgcodecs.IMWRITE_PNG_COMPRESSION;
import static org.opencv.imgcodecs.Imgcodecs.imencode;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;

public class TransmissionTask extends Activity implements Runnable {
    private float MAX_UDP_LENGTH = 50000;
    private float currByteBufferLength;

    private int dataType;
    private int frmID;
    private int encodingType;
    private byte[] encodingTypeByte;
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
    private String currEncoding;

    private DatabaseHelper requestsDatabase;
    private String serverIP;
    private int serverPort;

    private String currLocation;

    private int currHeight, currWidth;
    private Size imageResolution;
    private Size imageSize;
    private String imageExtension;
    private MatOfInt imageParams;

    private ByteBuffer ackPacket = ByteBuffer.allocate(ACK_SIZE);
    private byte[] ack;
    private boolean ackBool;
    private byte[] tmp = new byte[4];

    private boolean isTCPConnectedServer = false;

    private double preProcessingBegin;
    private double preProcessingEnd;

    public TransmissionTask(String selectedProtocol, DatagramChannel datagramChannel, SocketChannel socketChannel,
                            SocketAddress serverAddressUDP, SocketAddress serverAddressTCP,
                            Context context, DatabaseHelper requestsDatabase, String serverIP, int serverPort) {
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

        YUVMatTrans = new Mat(Constants.previewHeight * 3 / 2, Constants.previewWidth, CvType.CV_8UC1);
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
        currEncoding = sharedPreferencesSetup.getString("currEncoding", "JPEG");

        // obtaining the user selected resolutions
        currHeight = sharedPreferencesSetup.getInt("currHeight", 1920);
        currWidth = sharedPreferencesSetup.getInt("currWidth", 1080);

        // set MAX_UDP_LENGTH with user selected variable
        MAX_UDP_LENGTH = Float.parseFloat(Objects.requireNonNull(sharedPreferencesSetup.getString("currUDPPayload", "50000")));

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
            // start of client preprocessing
            preProcessingBegin = System.currentTimeMillis();

            YUVMatTrans = frameData; // setting new mat array with the data from mat from camera data

            imageResolution = originalDataShape.size(); // this is the maximum possible size

//            imageSize = new Size(imageResolution.width,imageResolution.height); //the dst image size
            imageSize = new Size(currHeight,currWidth);

            Imgproc.resize(YUVMatTrans, YUVMatScaled, imageSize, 0, 0, Imgproc.INTER_LINEAR);
            Imgproc.cvtColor(YUVMatScaled, GrayScaled, Imgproc.COLOR_YUV420sp2GRAY);
            Core.flip(GrayScaled.t(), GrayScaled, 1); // rotate 90 deg clockwise
        }

        if (dataType == MESSAGE_META) {
            datasize = 0;
            frmdataToSend = null;
        } else if (dataType == IMAGE_DETECT) {
            MatOfByte imgbuff = new MatOfByte();

            // dealing with the selected encoding
            if (currEncoding.contains("JPEG") || currEncoding.contains("MP4") || currEncoding.contains("MPEG-TS")) {
                encodingType = 0; // setting encodingType as JPEG, will be rewritten later on as needed
                imageExtension = ".jpg";
                int jpegQuality = Integer.parseInt(Objects.requireNonNull(sharedPreferencesSetup.getString("currEncodingJPEG", "70")));
                imageParams = new MatOfInt(IMWRITE_JPEG_QUALITY, jpegQuality);
            }
            if (currEncoding.contains("PNG")) {
                encodingType = 1;
                imageExtension = ".png";
                int pngQuality = Integer.parseInt(Objects.requireNonNull(sharedPreferencesSetup.getString("currEncodingPNG", "3")));
                imageParams = new MatOfInt(IMWRITE_PNG_COMPRESSION, pngQuality);
            }

            imencode(imageExtension, GrayScaled, imgbuff, imageParams);
            datasize = (int) (imgbuff.total() * imgbuff.channels());
            frmdataToSend = new byte[datasize];

            imgbuff.get(0, 0, frmdataToSend);

            if (currEncoding.contains("MP4")) {
                encodingType = 2;
                File sdcard = new File(Environment.getExternalStorageDirectory(), "/ReMAR/image_transmission/");
                if (!sdcard.exists()) { sdcard.mkdirs(); }

                // saving image as jpg then converting to mp4
                String jpgImage = sdcard + "/requestImage.jpg";
                imwrite(jpgImage, GrayScaled);

                String mp4Image = sdcard + "/requestImage.mp4";
                int convertJpgMp4 = FFmpeg.execute(new String[]{"-loglevel", "panic", "-loop", "1",
                        "-y", "-i", jpgImage, "-codec:v", "libx264", "-preset", "ultrafast", "-t", "1", "-pix_fmt", "yuv420p",
                        mp4Image});

                File mp4File = new File(mp4Image);
                int mp4Size = (int) mp4File.length();
                byte[] mp4Bytes = new byte[mp4Size];

                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(mp4File));
                    buf.read(mp4Bytes, 0, mp4Bytes.length);
                    buf.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                datasize = mp4Size;
                frmdataToSend = mp4Bytes;
            }
            if (currEncoding.contains("MPEG-TS")) {
                encodingType = 3;
                File sdcard = new File(Environment.getExternalStorageDirectory(), "/ReMAR/image_transmission/");
                if (!sdcard.exists()) { sdcard.mkdirs(); }

                // saving image as jpg then converting to mpeg-ts
                String jpgImage = sdcard + "/requestImage.jpg";
                imwrite(jpgImage, GrayScaled);

                String mp4Image = sdcard + "/requestImage.mp4";
                String tsImage = sdcard + "/requestImage.ts";

                int convertJpgMp4 = FFmpeg.execute(new String[]{"-loglevel", "panic", "-loop", "1",
                        "-y", "-i", jpgImage, "-codec:v", "libx264", "-t", "1", "-pix_fmt", "yuv420p",
                        mp4Image});
                int convertMp4Ts = FFmpeg.execute(new String[]{"-loglevel", "panic", "-i", mp4Image,
                        "-c", "copy", "-bsf", "h264_mp4toannexb", tsImage});

                File tsFile = new File(tsImage);
                int tsSize = (int) tsFile.length();
                byte[] tsBytes = new byte[tsSize];

                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(tsFile));
                    buf.read(tsBytes, 0, tsBytes.length);
                    buf.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                datasize = tsSize;
                frmdataToSend = tsBytes;
            }
        }

        packetContent = new byte[16 + datasize];

        frmid = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(frmID).array();
        encodingTypeByte = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(encodingType).array();
        frmsize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(datasize).array();

        System.arraycopy(frmid, 0, packetContent, 4, 4);
        System.arraycopy(frmsize, 0, packetContent, 12, 4);

        if (frmdataToSend != null) {
            System.arraycopy(frmdataToSend, 0, packetContent, 16, datasize);
            System.arraycopy(encodingTypeByte, 0, packetContent, 8, 4);
        }

        try {
            currByteBufferLength = packetContent.length;
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

                    ackBool = true; // set bool to true, allow segments to be sent
                    while (currOffset < currByteBufferLength) {
                        if (ackBool) {
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
                            if (numPackets == currSegmentNumber) {
                                preProcessingEnd = System.currentTimeMillis();
                            }

                            SharedPreferences udpAckSP = context.getSharedPreferences("udpAckSP", Context.MODE_PRIVATE);
                            Boolean udpAckStatus = udpAckSP.getBoolean("currUDPAck", false);

                            // release the lock
                            while(!udpAckStatus) {
                                // constantly checking the variable and prevents new data from being sent
                                udpAckStatus = udpAckSP.getBoolean("currUDPAck", false);
                                try {
                                    if (datagramChannel.receive(ackPacket) != null) {
                                        ack = ackPacket.array();

                                        System.arraycopy(ack, 0, tmp, 0, 4);
                                        int messageType = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                                        if (messageType == PACKET_STATUS) {
                                            System.arraycopy(ack, 12, tmp, 0, 4);
                                            int packetStatus = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

                                            if (packetStatus == 1) {
                                                udpAckSP.edit().putBoolean("currUDPAck", true).apply();
                                            }
                                            if (packetStatus == 2) {
                                                ackBool = false; // set boolean to false, skip to next frame
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
                    }
                } else if (dataType != MESSAGE_META) {
                    messageType = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(IMAGE_DETECT_COMPLETE).array();

                    System.arraycopy(messageType, 0, packetContent, 0, 4);

                    ByteBuffer buffer = ByteBuffer.allocate(packetContent.length).put(packetContent);
                    buffer.flip();
                    datagramChannel.send(buffer, serverAddressUDP);
                    preProcessingEnd = System.currentTimeMillis();

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
                    preProcessingEnd = System.currentTimeMillis();
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

            String currSelectedResolution = currHeight + "x" + currWidth;
            float totalPreProcessingTime = (float) (preProcessingEnd-preProcessingBegin);
            String totalPreProcessingTimeString = String.valueOf(totalPreProcessingTime);

            // Appending the sent information into the database table
            RequestEntry newRequestEntry = new RequestEntry("", Integer.parseInt(currSessionNumber),
                    frmID, String.valueOf(time), serverIP, serverPort, datasize+12,
                    currLocation, selectedProtocol, currSelectedResolution, totalPreProcessingTimeString);
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
                    runOnUiThread(() -> currFrame.setText("F" + frmID));
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
