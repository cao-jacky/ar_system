package fi.fivegear.remar;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.InetAddresses;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.RequestEntry;
import fi.fivegear.remar.models.ServerInfo;
import fi.fivegear.remar.network.ReceivingTask;
import fi.fivegear.remar.network.TransmissionTask;


import static fi.fivegear.remar.activities.settingsServer.currServerSettings;

public class ARManager {

    static private ARManager instance;

    private Handler handlerUtil;
    private Handler handlerNetwork;

    private TransmissionTask taskTransmission;
    private ReceivingTask taskReceiving;

    private DatagramChannel dataChannel;
    private SocketAddress serverAddr;

    private static boolean isCloudBased;

    SharedPreferences sharedPreferences;
    DatabaseHelper requestsDatabase, resultsDatabase;

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

    private void initConnection(String serverIP, int serverPort) {
        try {
            serverAddr = new InetSocketAddress(serverIP, serverPort);
            dataChannel = DatagramChannel.open();
            dataChannel.configureBlocking(false);
            dataChannel.bind(new InetSocketAddress(51919));
            //dataChannel.socket().connect(serverAddr);
        } catch (Exception e) {
            Log.d(Constants.TAG, "config file error");
        }
    }

    public void init(Context context, boolean isCloudBased){
        ARManager.isCloudBased = isCloudBased;

        // using SharedPreferences to set current server IP and port
        sharedPreferences = context.getSharedPreferences(currServerSettings, Context.MODE_PRIVATE);
        String serverIP = sharedPreferences.getString("currServerIP", "0.0.0.0");
        int serverPort = sharedPreferences.getInt("currServerPort", 0);

        // load database tables to pass to the threads dealing with requests and results
        requestsDatabase = new DatabaseHelper(context.getApplicationContext());
        resultsDatabase = new DatabaseHelper(context.getApplicationContext());

        // validating whether IP address is valid, if not, default to 0.0.0.0 and change preferences
        boolean isValid = InetAddresses.isNumericAddress(serverIP);
        if(isValid == false) {
            serverIP = "0.0.0.0";

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("currServerIP", serverIP);
            editor.apply();

            // Add a message about this somewhere, in a log? Create another table for logging messages?
            // could be easy as a text file log
        }

        System.loadLibrary("opencv_java");
        if(isCloudBased) initConnection(serverIP, serverPort);

        this.handlerUtil = createAndStartThread("util thread", Process.THREAD_PRIORITY_DEFAULT); //start util thread
        this.handlerNetwork = createAndStartThread("network thread", 1);

        if(isCloudBased) {
            taskTransmission = new TransmissionTask(dataChannel, serverAddr, context, requestsDatabase,
                    serverIP, serverPort);
	        //pengzhou
            taskTransmission.setData(0,"a".getBytes());
            handlerNetwork.post(taskTransmission);
            taskReceiving = new ReceivingTask(dataChannel, context, resultsDatabase, serverIP,
                    serverPort);
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
