package fi.fivegear.remar;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.InetAddresses;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.opencv.core.Mat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.network.ReceivingTask;
import fi.fivegear.remar.network.TransmissionTask;

public class ARManager {

    static private ARManager instance;

    private Handler handlerUtil;
    private Handler handlerNetwork;
    private Handler handlerTCPNetwork;

    private TransmissionTask taskTransmission;
    private ReceivingTask taskReceiving;

    private DatagramChannel dataChannel;
    private SocketChannel socketChannel;

    private SocketAddress serverAddressUDP;
    private SocketAddress serverAddressTCP;

    private String selectedProtocol;

    SharedPreferences sharedPreferencesSetup;
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

    private void initConnection(String selectedProtocol, String serverIP, int serverPort) {
        try {
            serverAddressUDP = new InetSocketAddress(serverIP, 50000);
            serverAddressTCP = new InetSocketAddress(serverIP, 55000);

            // create UDP datagram channel
            dataChannel = DatagramChannel.open();
            dataChannel.configureBlocking(false);
            dataChannel.bind(new InetSocketAddress(40000));

            // create TCP socket channel
            socketChannel = SocketChannel.open();
        } catch (Exception e) {
            Log.d("DEBUG", "DataChannel creation error");
            System.out.println(e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void init(Context context){
        // using SharedPreferences to set current server IP and port
        sharedPreferencesSetup = context.getSharedPreferences("currSetupSettings", Context.MODE_PRIVATE);
        String serverIP = sharedPreferencesSetup.getString("currServerIP", "0.0.0.0");
        int serverPort = sharedPreferencesSetup.getInt("currServerPort", 0);

        // obtain selected protocol from settings
        selectedProtocol = sharedPreferencesSetup.getString("currProtocol", "UDP");

        // load database tables to pass to the threads dealing with requests and results
        requestsDatabase = new DatabaseHelper(context.getApplicationContext());
        resultsDatabase = new DatabaseHelper(context.getApplicationContext());

        // validating whether IP address is valid, if not, default to 0.0.0.0 and change preferences
        boolean isValid = InetAddresses.isNumericAddress(serverIP);
        if(isValid == false) {
            serverIP = "0.0.0.0";

            SharedPreferences.Editor editor = sharedPreferencesSetup.edit();
            editor.putString("currServerIP", serverIP);
            editor.apply();
        }

        System.loadLibrary("opencv_java");
        initConnection(selectedProtocol, serverIP, serverPort);

        this.handlerUtil = createAndStartThread("Utility thread", Process.THREAD_PRIORITY_DEFAULT); //start util thread
        this.handlerNetwork = createAndStartThread("Network thread", 1);

        taskTransmission = new TransmissionTask(selectedProtocol, dataChannel, socketChannel,
                serverAddressUDP, serverAddressTCP, context, requestsDatabase, serverIP, serverPort);
        taskTransmission.setData(0, new Mat());
        handlerNetwork.post(taskTransmission);

        taskReceiving = new ReceivingTask(selectedProtocol, dataChannel, socketChannel, context,
                resultsDatabase, serverIP, serverPort);
        taskReceiving.setCallback((resultID, detected) -> callback.onObjectsDetected(detected));

    }

    public void stop() {
        handlerNetwork.post(() -> {
            try {
                dataChannel.close();
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            handlerUtil.getLooper().quitSafely();
            handlerNetwork.getLooper().quitSafely();
        }
    }

    public void recognise(int frameID, Mat frameData){
        taskTransmission.setData(frameID, frameData);
        handlerNetwork.post(taskTransmission);
        taskReceiving.updateLatestSentID(frameID);
    }
    public void driveFrame() {
        handlerNetwork.post(taskReceiving);
    }

    private ARManager.Callback callback;

    public void setCallback(ARManager.Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void onObjectsDetected(Detected[] detected);
    }
}
