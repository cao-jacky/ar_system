package fi.fivegear.remar.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.Point;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;

import fi.fivegear.remar.MainActivity;
import fi.fivegear.remar.Constants;
import fi.fivegear.remar.Detected;
import fi.fivegear.remar.R;


/**
 * Created by st0rm23 on 2017/2/20.
 */

public class ReceivingTask implements Runnable{

    private ByteBuffer resPacket = ByteBuffer.allocate(Constants.RES_SIZE);

    private byte[] res;
    private float[] floatres = new float[8];
    private Point[] pointArray = new Point[4];
    private byte[] tmp = new byte[4];
    private byte[] Tmp = new byte[8];
    private byte[] name = new byte[56];
    private double resultLat;
    private double resultLong;
    private double resultTimecap;
    private double resultTimesend;
    private int newMarkerNum;
    private int lastSentID;
    private int recoTrackRatio = Constants.scale / Constants.recoScale;

    private DatagramChannel datagramChannel;
    private long time;
    private long true_time;
    private double resultdelay;
    private double timeReceived;

    private Context context;
    private SharedPreferences sharedPreferencesSession;
    private String currSessionNumber;

    public ReceivingTask(DatagramChannel datagramChannel, Context context){
        this.datagramChannel = datagramChannel;
        this.context = context;
    }

    public void updateLatestSentID(int lastSentID){
        this.lastSentID = lastSentID;
    }

    @Override
    public void run() {
        // pulling session number
        sharedPreferencesSession = context.getSharedPreferences("currSessionSetting", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSession.getString("currSessionNumber", "0");

        resPacket.clear();
        try {
            time = System.currentTimeMillis();
            timeReceived = (double)time;
            if (datagramChannel.receive(resPacket) != null) {
                res = resPacket.array();
            } else {
                res = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (res != null) {
            MainActivity.downloadStatus.setImageAlpha(0);

            System.arraycopy(res, 0, tmp, 0, 4);
            int resultID = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

            System.arraycopy(res, 8, tmp, 0, 4);
            newMarkerNum = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();


            if (newMarkerNum >= 0) {
                Detected detected[] = new Detected[newMarkerNum];

                int i = 0;
                while (i < newMarkerNum) {
                    detected[i] = new Detected();
                    //detected[i].lati = resultLat;
                    //detected[i].longti = resultLong;
                    detected[i].tcap = resultTimecap;
                    detected[i].tsend = resultTimesend;
                    System.arraycopy(res, 12 + i * 100, tmp, 0, 4);
                    detected[i].prob = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                    System.arraycopy(res, 16 + i * 100, tmp, 0, 4);
                    detected[i].left = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    System.arraycopy(res, 20 + i * 100, tmp, 0, 4);
                    detected[i].right = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    System.arraycopy(res, 24 + i * 100, tmp, 0, 4);
                    detected[i].top = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    System.arraycopy(res, 28 + i * 100, tmp, 0, 4);
                    detected[i].bot = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    System.arraycopy(res, 32 + i * 100, name, 0, 20);
                    String nname = new String(name);
                    detected[i].name = nname.substring(0, nname.indexOf("."));
                    i++;
                }

                if (callback != null){
                    callback.onReceive(resultID, detected);
                }
            }

            MainActivity.downloadStatus.setImageAlpha(255);
        }
    }

    private Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void onReceive(int resultID, Detected[] detected);
    }
}
