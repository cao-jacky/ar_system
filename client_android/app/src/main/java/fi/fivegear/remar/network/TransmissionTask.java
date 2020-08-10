package fi.fivegear.remar.network;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

import fi.fivegear.remar.MainActivity;
import fi.fivegear.remar.Constants;
import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.RequestEntry;

import static fi.fivegear.remar.MainActivity.currFrame;

public class TransmissionTask extends Activity implements Runnable {

    private final int MESSAGE_META = 0;
    private final int IMAGE_DETECT = 2;

    private int dataType;
    private int frmID;
    private byte[] frmdataToSend;
    private byte[] frmid;
    private byte[] datatype;
    private byte[] frmsize;
    private byte[] packetContent;
    private int datasize;
    private Mat frameData;
    private double timeCaptured;
    private double timeSend;

    private String selectedProtocol;
    private final DatagramChannel datagramChannel;
    private final SocketChannel socketChannel;

    private SocketAddress serverAddressUDP;
    private SocketAddress serverAddressTCP;

    public Mat YUVMatTrans, YUVMatScaled, GrayScaled;
    public Mat convertedData;
    private long time;

    private Context context;
    private SharedPreferences sharedPreferencesSession, sharedPreferencesLocation, sharedPreferencesProtocol;
    private String currSessionNumber;

    private DatabaseHelper requestsDatabase;
    private String serverIP;
    private int serverPort;

    private String currLocation;

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

        convertedData = new Mat(Constants.previewHeight , Constants.previewWidth, CvType.CV_8UC3);
//        YUVMatTrans = new Mat(Constants.previewHeight + Constants.previewHeight / 2, Constants.previewWidth, CvType.CV_8UC1);
        YUVMatTrans = new Mat(Constants.previewHeight * 3 / 2, Constants.previewWidth, CvType.CV_8UC1);
//        YUVMatScaled = new Mat((Constants.previewHeight + Constants.previewHeight / 2), Constants.previewWidth, CvType.CV_8UC1);
        YUVMatScaled = new Mat((Constants.previewHeight + Constants.previewHeight / 2) / Constants.recoScale, Constants.previewWidth / Constants.recoScale, CvType.CV_8UC1);
        GrayScaled = new Mat(Constants.previewHeight / Constants.recoScale, Constants.previewWidth / Constants.recoScale, CvType.CV_8UC1);
    }

    public void setData(int frmID, Mat frameData){
        this.frmID = frmID;
        this.frameData = frameData;

        if (this.frmID <= 5) dataType = MESSAGE_META;
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
        sharedPreferencesSession = context.getSharedPreferences("currSessionSetting", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSession.getString("currSessionNumber", "0");

        sharedPreferencesLocation = context.getSharedPreferences("currLocationSetting", Context.MODE_PRIVATE);
        currLocation = sharedPreferencesLocation.getString("currLocation", "0");

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

        MainActivity.uploadStatus.setImageAlpha(0);
        if (dataType == IMAGE_DETECT) {
//            YUVMatTrans.put(0, 0, frameData);

            YUVMatTrans = frameData;

//            Log.d("TEST", String.valueOf(YUVMatScaled.size()));

//            Size imageSize = new Size(4000,3000);//the dst image size,e.g.100x100
//            Imgproc.resize(YUVMatTrans, YUVMatScaled, imageSize, 0, 0, Imgproc.INTER_LINEAR);

            Imgproc.resize(YUVMatTrans, YUVMatScaled, YUVMatScaled.size(), 0, 0, Imgproc.INTER_LINEAR);
            Imgproc.cvtColor(YUVMatScaled, GrayScaled, Imgproc.COLOR_YUV420sp2GRAY);
            Core.flip(GrayScaled.t(), GrayScaled, 1); // rotate 90 deg clockwise
        }

        if (dataType == IMAGE_DETECT) {
            MatOfByte imgbuff = new MatOfByte();
            Highgui.imencode(".jpg", GrayScaled, imgbuff, Constants.Image_Params);

            datasize = (int) (imgbuff.total() * imgbuff.channels());
            frmdataToSend = new byte[datasize];

            imgbuff.get(0, 0, frmdataToSend);
        } else if (dataType == MESSAGE_META) {
            datasize = 0;
            frmdataToSend = null;
        }
        packetContent = new byte[12 + datasize];

        frmid = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(frmID).array();
        datatype = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dataType).array();
        frmsize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(datasize).array();

        System.arraycopy(frmid, 0, packetContent, 0, 4);
        System.arraycopy(datatype, 0, packetContent, 4, 4);
        System.arraycopy(frmsize, 0, packetContent, 8, 4);

        if (frmdataToSend != null)
            System.arraycopy(frmdataToSend, 0, packetContent, 12, datasize);

        try {
            ByteBuffer buffer = ByteBuffer.allocate(packetContent.length).put(packetContent);
            buffer.flip();

            if (selectedProtocol.contains("UDP")) {
                datagramChannel.send(buffer, serverAddressUDP);
            }
            if (selectedProtocol.contains("TCP")) {
                // attempt TCP connection, if failure, change the selected protocol to UDP and declare to user
                try {
                    socketChannel.write(buffer);
                } catch (Exception e) {
                    sharedPreferencesProtocol = context.getSharedPreferences("currProtocolSetting", Context.MODE_PRIVATE);
                    SharedPreferences.Editor sessionEditor = sharedPreferencesProtocol.edit();
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

            MainActivity.uploadStatus.setImageAlpha(255);
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
