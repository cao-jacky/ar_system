package fi.fivegear.remar.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.opencv.core.Point;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import fi.fivegear.remar.MainActivity;
import fi.fivegear.remar.Constants;
import fi.fivegear.remar.Detected;
import fi.fivegear.remar.activities.AugmentedRealityActivity;
import fi.fivegear.remar.helpers.DatabaseHelper;
import fi.fivegear.remar.models.ResultsEntry;

import static fi.fivegear.remar.Constants.ACK_SIZE;
import static fi.fivegear.remar.Constants.PACKET_STATUS;
import static fi.fivegear.remar.Constants.RESULTS_STATUS;
import static fi.fivegear.remar.Constants.RES_SIZE;
import static fi.fivegear.remar.Constants.TAG;

public class ReceivingTask implements Runnable{

    private ByteBuffer resPacket = ByteBuffer.allocate(RES_SIZE);

    private byte[] res;
    private byte[] tmp = new byte[4];
    private byte[] name = new byte[56];
    private double resultTimecap;
    private double resultTimesend;
    private int newMarkerNum;
    private int lastSentID;

    private String selectedProtocol;
    private SocketChannel socketChannel;
    private DatagramChannel datagramChannel;

    private long time;
    private double timeReceived;

    private Context context;
    private SharedPreferences sharedPreferencesSession, sharedPreferencesLocation;
    private String currSessionNumber;

    private DatabaseHelper resultsDatabase;
    private String serverIP;
    private int serverPort;
    private String resultItems;

    private String currLocation;

    public ReceivingTask(String selectedProtocol, DatagramChannel datagramChannel, SocketChannel socketChannel,
                         Context context, DatabaseHelper resultsDatabase, String serverIP, int serverPort){
        this.selectedProtocol = selectedProtocol;
        this.datagramChannel = datagramChannel;
        this.socketChannel = socketChannel;
        this.context = context;
        this.resultsDatabase = resultsDatabase;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void updateLatestSentID(int lastSentID){
        this.lastSentID = lastSentID;
    }

    @Override
    public void run() {
        // pulling session number
        sharedPreferencesSession = context.getSharedPreferences("currSessionSetting", Context.MODE_PRIVATE);
        currSessionNumber = sharedPreferencesSession.getString("currSessionNumber", "0");

        sharedPreferencesLocation = context.getSharedPreferences("currLocationSetting", Context.MODE_PRIVATE);
        currLocation = sharedPreferencesLocation.getString("currLocation", "0");

        resPacket.clear();

        if (selectedProtocol.contains("UDP")) {
            try {
                time = System.currentTimeMillis();
                timeReceived = (double) time;
                if (datagramChannel.receive(resPacket) != null) {
                    res = resPacket.array();
                } else {
                    res = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (selectedProtocol.contains("TCP")) {
            try {
                if (socketChannel.read(resPacket) != 0) {
                    res = resPacket.array();
                } else {
                    res = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (res != null) {
            AugmentedRealityActivity.downloadStatus.setImageAlpha(0);

            System.arraycopy(res, 0, tmp, 0, 4);
            int messageType = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

//            SharedPreferences udpAckSP = context.getSharedPreferences("udpAckSP", Context.MODE_PRIVATE);
//            Boolean udpAckStatus = udpAckSP.getBoolean("currUDPAck", false);
//            Log.d(TAG, "ack status " + udpAckStatus);

            if (messageType == RESULTS_STATUS) {
                System.arraycopy(res, 4, tmp, 0, 4);
                int resultID = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

                System.arraycopy(res, 12, tmp, 0, 4);
                newMarkerNum = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

                if (newMarkerNum >= 0) {
                    Detected detected[] = new Detected[newMarkerNum];
                    ArrayList<String> detectedStrings = new ArrayList<String>();

                    int i = 0;
                    while (i < newMarkerNum) {
                        Log.d(TAG, "results received");
                        detected[i] = new Detected();

                        detected[i].tcap = resultTimecap;
                        detected[i].tsend = resultTimesend;
                        System.arraycopy(res, 16 + i * 100, tmp, 0, 4);
                        detected[i].prob = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                        System.arraycopy(res, 20 + i * 100, tmp, 0, 4);
                        detected[i].left = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                        System.arraycopy(res, 24 + i * 100, tmp, 0, 4);
                        detected[i].right = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                        System.arraycopy(res, 28 + i * 100, tmp, 0, 4);
                        detected[i].top = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
                        System.arraycopy(res, 32 + i * 100, tmp, 0, 4);
                        detected[i].bot = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();

                        System.arraycopy(res, 36 + i * 100, name, 0, 20);

                        String nname = new String(name);
                        String objectName = nname.substring(0, nname.indexOf("."));
                        detected[i].name = objectName;

                        detectedStrings.add(objectName);

                        i++;
                    }

                    resultItems = String.join(",", detectedStrings);

                    // submit results item into table
                    ResultsEntry newResultsEntry = new ResultsEntry("", Integer.parseInt(currSessionNumber),
                            resultID, String.valueOf(time), serverIP, serverPort, currLocation, resultItems);
                    long newResultsEntry_id = resultsDatabase.createResultsEntry(newResultsEntry);

                    if (callback != null){
                        callback.onReceive(resultID, detected);
                    }
                }

                AugmentedRealityActivity.downloadStatus.setImageAlpha(255);
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
