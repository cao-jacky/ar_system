package fi.fivegear.remar.network;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.provider.ContactsContract;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.List;

import fi.fivegear.remar.MainActivity;
import fi.fivegear.remar.Constants;
import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.RequestEntry;
import fi.fivegear.remar.models.ServerInfo;

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
    private byte[] frameData;
    private double latitude;
    private double longtitude;
    private double timeCaptured;
    private double timeSend;
    private byte[] Latitude;
    private byte[] Longtitude;
    private byte[] TimeCaptured;
    private byte[] TimeSend;
    private DatagramChannel datagramChannel;
    private SocketAddress serverAddress;

    public Mat YUVMatTrans, YUVMatScaled, GrayScaled;
    private long time;
    private long true_time;

    private Context context;
    private SharedPreferences sharedPreferencesSession, sharedPreferencesLocation;
    private String currSessionNumber;

    private DatabaseHelper requestsDatabase;
    private String serverIP;
    private int serverPort;

    private String currLocation;

    private MainActivity mainActivity = new MainActivity();

    public TransmissionTask(DatagramChannel datagramChannel, SocketAddress serverAddress, Context context, DatabaseHelper requestsDatabase, String serverIP, int serverPort) {
        this.datagramChannel = datagramChannel;
        this.serverAddress = serverAddress;
        this.context = context;
        this.requestsDatabase = requestsDatabase;
        this.serverIP = serverIP;
        this.serverPort = serverPort;

        YUVMatTrans = new Mat(Constants.previewHeight + Constants.previewHeight / 2, Constants.previewWidth, CvType.CV_8UC1);
        YUVMatScaled = new Mat((Constants.previewHeight + Constants.previewHeight / 2) / Constants.recoScale, Constants.previewWidth / Constants.recoScale, CvType.CV_8UC1);
        GrayScaled = new Mat(Constants.previewHeight / Constants.recoScale, Constants.previewWidth / Constants.recoScale, CvType.CV_8UC1);
    }


    public void setData(int frmID, byte[] frameData, double timeCaptured, double timeSend){
        this.frmID = frmID;
        this.frameData = frameData;
        this.timeCaptured = timeCaptured;
        this.timeSend = timeSend;

        if (this.frmID <= 5) dataType = MESSAGE_META;
        else dataType = IMAGE_DETECT;
    }
    public void setData(int frmID, byte[] frameData){
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

        MainActivity.uploadStatus.setImageAlpha(0);
        if (dataType == IMAGE_DETECT) {
            YUVMatTrans.put(0, 0, frameData);

            Imgproc.resize(YUVMatTrans, YUVMatScaled, YUVMatScaled.size(), 0, 0, Imgproc.INTER_LINEAR);
            Imgproc.cvtColor(YUVMatScaled, GrayScaled, Imgproc.COLOR_YUV420sp2GRAY);
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
            datagramChannel.send(buffer, serverAddress);
            time = System.currentTimeMillis();

            // Appending the sent information into the database table
            RequestEntry newRequestEntry = new RequestEntry("", Integer.parseInt(currSessionNumber),
                    frmID, String.valueOf(time), serverIP, serverPort, datasize+12,
                    currLocation, "");
            long newRequestsEntry_id = requestsDatabase.createRequestEntry(newRequestEntry);

            MainActivity.uploadStatus.setImageAlpha(255);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
