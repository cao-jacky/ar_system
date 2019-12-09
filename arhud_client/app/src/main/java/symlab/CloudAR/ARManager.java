package symlab.CloudAR;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import symlab.CloudAR.network.ReceivingTask;
import symlab.CloudAR.network.TransmissionTask;

/**
 * Created by st0rm23 on 2017/2/18.
 */

public class ARManager {

    static private ARManager instance;

    private Handler handlerUtil;
    private Handler handlerNetwork;

    private TransmissionTask taskTransmission;
    private ReceivingTask taskReceiving;

    private DatagramChannel dataChannel;
    private SocketAddress serverAddr;
    private String ip;
    private int port;

    private static boolean isCloudBased;

    private ARManager(){ super(); }

    static public ARManager getInstance() {
        synchronized (ARManager.class){
            if (instance == null) instance = new ARManager();
        }
        return instance;
    }

    private Handler createAndStartThread(String name, int priority){
        HandlerThread handlerThread = new HandlerThread(name, priority);
        handlerThread.start();
        return new Handler(handlerThread.getLooper());
    }

    private void initConnection() {
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard,"CloudAR/cloudConfigarhud.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            ip = br.readLine();
            port = Integer.parseInt(br.readLine());
            br.close();

            serverAddr = new InetSocketAddress(ip, port);
            dataChannel = DatagramChannel.open();
            dataChannel.configureBlocking(false);
            // pengzhou: the receiving phone needs the following sentence
            dataChannel.bind(new InetSocketAddress(51919));
            //dataChannel.socket().connect(serverAddr);
        } catch (IOException e) {
            Log.d(Constants.TAG, "config file error");
        } catch (Exception e) {}
    }

    public void init(Context context, boolean isCloudBased){
        ARManager.isCloudBased = isCloudBased;

        System.loadLibrary("opencv_java");
        if(isCloudBased) initConnection();

        this.handlerUtil = createAndStartThread("util thread", Process.THREAD_PRIORITY_DEFAULT); //start util thread
        this.handlerNetwork = createAndStartThread("network thread", 1);

        if(isCloudBased) {
            taskTransmission = new TransmissionTask(dataChannel, serverAddr);
	        //pengzhou
            taskTransmission.setData(0,"a".getBytes());
            handlerNetwork.post(taskTransmission);
            taskReceiving = new ReceivingTask(dataChannel);
            taskReceiving.setCallback(new ReceivingTask.Callback() {
                @Override
                public void onReceive(int resultID, Detected[] detected) {
                    callback.onObjectsDetected(detected);
                }
            });
        }
    }

    public void start() {
    }

    public void stop() {
        if(isCloudBased) handlerNetwork.post(new Runnable() {
            @Override
            public void run() {
                try {
                    dataChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            handlerUtil.getLooper().quitSafely();
            handlerNetwork.getLooper().quitSafely();
        }
    }

    public void recognize(int frameID, byte[] frameData) {
        if(ARManager.isCloudBased) {
            taskTransmission.setData(frameID, frameData);
            handlerNetwork.post(taskTransmission);
            taskReceiving.updateLatestSentID(frameID);
        }
    }
    //------------------pengzhou: location-------------------
    /*public void recognizeLocation(int frameID, byte[] frameData, double latitude, double longtitude) {
        if(ARManager.isCloudBased) {
            taskTransmission.setData(frameID, frameData, latitude, longtitude);
            handlerNetwork.post(taskTransmission);
            taskReceiving.updateLatestSentID(frameID);
        }
    }*/
    //------------------pengzhou: timestamp-------------------
    public void recognizeTime(int frameID, byte[] frameData){// double timeCaptured, double timeSend) {
        if(ARManager.isCloudBased) {
            taskTransmission.setData(frameID, frameData);//, timeCaptured, timeSend);
            handlerNetwork.post(taskTransmission);
            taskReceiving.updateLatestSentID(frameID);
        }
    }
    public void driveFrame(byte[] frameData) {
        if(isCloudBased) handlerNetwork.post(taskReceiving);
    }

    private ARManager.Callback callback;

    public void setCallback(ARManager.Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void onObjectsDetected(Detected[] detected);
    }
}
