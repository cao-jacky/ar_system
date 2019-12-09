package symlab.CloudAR.network;

import android.os.Environment;
import android.util.Log;

import org.opencv.core.Point;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.BufferedWriter;

import symlab.CloudAR.Constants;
import symlab.CloudAR.Detected;

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
    private double resultdelay;
    private double timeReceived;



    public ReceivingTask(DatagramChannel datagramChannel){
        this.datagramChannel = datagramChannel;
    }

    public void updateLatestSentID(int lastSentID){
        this.lastSentID = lastSentID;
    }

    @Override
    public void run() {
        resPacket.clear();
        try {
            if (datagramChannel.receive(resPacket) != null) {
                res = resPacket.array();
                //Log.v(Constants.TAG, "something received");
            } else {
                res = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //pengzhou : record result transmission delay
//        File appDirectory = new File( Environment.getExternalStorageDirectory() + "/CloudAR" );
//        File logDirectory = new File( appDirectory + "/receive_logs" );
//        File file = new File(logDirectory,"receive_"+System.currentTimeMillis()+".txt");
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard,"CloudAR/receive.txt");

        if (res != null) {
            time = System.currentTimeMillis();
            timeReceived = (double)time;
            System.arraycopy(res, 0, tmp, 0, 4);
            int resultID = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

//            System.arraycopy(res, 8, Tmp, 0, 8);
            //resultLat = ByteBuffer.wrap(Tmp).order(ByteOrder.LITTLE_ENDIAN).getDouble();
//            resultTimecap = ByteBuffer.wrap(Tmp).order(ByteOrder.LITTLE_ENDIAN).getDouble();

//            System.arraycopy(res, 16, Tmp, 0, 8);
            //resultLong = ByteBuffer.wrap(Tmp).order(ByteOrder.LITTLE_ENDIAN).getDouble();
//            resultTimesend = ByteBuffer.wrap(Tmp).order(ByteOrder.LITTLE_ENDIAN).getDouble();

            System.arraycopy(res, 8, tmp, 0, 4);
            newMarkerNum = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

//            resultdelay = time - resultTimesend;
//            Log.d(Constants.Eval, resultID + " " + time + " " + String.valueOf(resultTimesend) +
//             " " + String.valueOf(resultdelay));
            try{BufferedWriter bw =
                    new BufferedWriter(new FileWriter(file, true));
                bw.write(Integer.toString(resultID));
                bw.write(",");
                bw.write(Double.toString(timeReceived));
                bw.newLine();
                bw.flush();}
            catch (Exception e){
                e.printStackTrace();

            }

            if (newMarkerNum >= 0) {
                Log.d(Constants.Eval, "" + newMarkerNum + " res for frame " + resultID +
                        " received at " + System.currentTimeMillis());
                Detected detected[] = new Detected[newMarkerNum];

//                try{BufferedWriter bw =
//                        new BufferedWriter(new FileWriter(file, true));
//                        bw.write(Double.toString(resultdelay));
//                        bw.newLine();
//                        bw.flush();}
//                catch (Exception e){
//                    e.printStackTrace();
//
//                }

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
