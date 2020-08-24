package fi.fivegear.remar.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fi.fivegear.remar.models.RequestEntry;
import fi.fivegear.remar.models.ResultsEntry;
import fi.fivegear.remar.models.ServerInfo;
import fi.fivegear.remar.models.SessionInfo;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Logcat tag
    private static final String LOG = "DatabaseHelper";

    // Database version
    private static final int DATABASE_VERSION = 1;

    // Database name
    private static final String DATABASE_NAME = "remarManager";

    // Table names
    private static final String TABLE_SERVER = "server_info";
    private static final String TABLE_REQUEST = "request_items";
    private static final String TABLE_RESULTS = "result_items";
    private static final String TABLE_SESSIONS = "sessions";

    // Common column names
    private static final String KEY_RECOGNITION_ID = "recognitionID";
    private static final String KEY_SERVER_IP = "serverIP";
    private static final String KEY_SERVER_PORT = "serverPort";
    private static final String KEY_SESSION_ID = "sessionID";
    private static final String KEY_FRAME_ID = "frameID";

    // server_info table - column names
    private static final String KEY_SERVER_ID = "serverID";
    private static final String KEY_UNIX_TIME_ADDED = "unixTimeAdded";

    // request_items table - column names
    private static final String KEY_REQUEST_ID = "requestID";
    private static final String KEY_UNIX_TIME_REQUEST_SENT = "unixTimeRequestSent";
    private static final String KEY_REQUEST_ARRAY_LENGTH = "requestArrayLength";
    private static final String KEY_REQUEST_GPS_COORD = "requestGPSCoord";
    private static final String KEY_PROTOCOL = "protocol";
    private static final String KEY_RESOLUTION = "resolution";
    private static final String KEY_PRE_PROCESSING_TIME = "preProcessingTime";

    // result_items table - column names
    private static final String KEY_RESULTS_ID = "resultsID";
    private static final String KEY_UNIX_TIME_RESULTS_REC = "unixTimeResultsRec";
    private static final String KEY_RESULTS_GPS_COORD = "resultsGPSCoord";
    private static final String KEY_RECEIVED_RESULTS = "receivedResults";
    private static final String KEY_POST_PROCESSING_TIME = "postProcessingTime";

    // sessions table - column names
    private static final String KEY_SESSION_TABLE_ID = "sessionTableID";
    private static final String KEY_UNIX_TIME_INITIATED = "unixTimeInitiated";
    private static final String KEY_UNIX_TIME_ENDED = "unixTimeEnded";
    private static final String KEY_BYTES_RECEIVED = "bytesReceived";
    private static final String KEY_PACKETS_RECEIVED = "packetsReceived";
    private static final String KEY_BYTES_TRANSMITTED = "bytesTransmitted";
    private static final String KEY_PACKETS_TRANSMITTED = "packetsTransmitted";
    private static final String KEY_AVG_RTT = "avgRTT";
    private static final String KEY_NUM_REQUESTS = "numRequests";
    private static final String KEY_NUM_RESULTS = "numResults";

    // Table create statements
    // server_info
    private static final String CREATE_TABLE_SERVER_INFO = "CREATE TABLE "
            + TABLE_SERVER + "(" + KEY_SERVER_ID + " INTEGER PRIMARY KEY," + KEY_UNIX_TIME_ADDED
            + " INTEGER," + KEY_SERVER_IP + " TEXT," + KEY_SERVER_PORT
            + " INTEGER" + ")";

    // request_items
    private static final String CREATE_TABLE_REQUEST_ITEMS = "CREATE TABLE "
            + TABLE_REQUEST + "(" + KEY_REQUEST_ID + " INTEGER PRIMARY KEY," + KEY_RECOGNITION_ID
            + " TEXT," + KEY_SESSION_ID + " INTEGER," + KEY_FRAME_ID + " INTEGER,"
            + KEY_UNIX_TIME_REQUEST_SENT + " INTEGER," + KEY_SERVER_IP + " TEXT," + KEY_SERVER_PORT
            + " INTEGER," + KEY_REQUEST_ARRAY_LENGTH + " INTEGER," + KEY_REQUEST_GPS_COORD
            + " TEXT," + KEY_PROTOCOL + " TEXT," + KEY_RESOLUTION  + " TEXT," + KEY_PRE_PROCESSING_TIME
            + " TEXT" + ")";

    // result_items
    private static final String CREATE_TABLE_RESULT_ITEMS = "CREATE TABLE "
            + TABLE_RESULTS + "(" + KEY_RESULTS_ID + " INTEGER PRIMARY KEY," + KEY_RECOGNITION_ID
            + " TEXT," + KEY_SESSION_ID + " INTEGER," + KEY_FRAME_ID + " INTEGER,"
            + KEY_UNIX_TIME_RESULTS_REC + " INTEGER," + KEY_SERVER_IP + " TEXT," + KEY_SERVER_PORT
            + " INTEGER," + KEY_RESULTS_GPS_COORD + " TEXT," + KEY_RECEIVED_RESULTS + " TEXT,"
            + KEY_POST_PROCESSING_TIME + " TEXT" + ")";

    private static final String CREATE_TABLE_SESSIONS = "CREATE TABLE "
            + TABLE_SESSIONS + "(" + KEY_SESSION_TABLE_ID + " INTEGER PRIMARY KEY," + KEY_SESSION_ID
            + " INTEGER," + KEY_UNIX_TIME_INITIATED + " INTEGER," + KEY_UNIX_TIME_ENDED
            + " INTEGER," + KEY_BYTES_RECEIVED + " REAL," + KEY_PACKETS_RECEIVED
            + " REAL," + KEY_BYTES_TRANSMITTED + " REAL," + KEY_PACKETS_TRANSMITTED
            + " REAL," + KEY_AVG_RTT + " REAL," + KEY_NUM_REQUESTS + " INTEGER,"
            + KEY_NUM_RESULTS + " INTEGER" + ")";

    // sessions

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating tables
        db.execSQL(CREATE_TABLE_SERVER_INFO);
        db.execSQL(CREATE_TABLE_REQUEST_ITEMS);
        db.execSQL(CREATE_TABLE_RESULT_ITEMS);
        db.execSQL(CREATE_TABLE_SESSIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REQUEST);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESULTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);

        // create new tables
        onCreate(db);
    }

    // Methods to create, read, update and delete from the tables
    public long createServerInfo(ServerInfo serverInfo) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_UNIX_TIME_ADDED, serverInfo.getUnixTimeAdded());
        values.put(KEY_SERVER_IP, serverInfo.getServerIP());
        values.put(KEY_SERVER_PORT, serverInfo.getServerPort());

        long server_id = db.insert(TABLE_SERVER, null, values);
        return server_id;
    }

    /**
     * Creating entries
     */
    public long createRequestEntry(RequestEntry requestEntry) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_RECOGNITION_ID, requestEntry.getRecognitionID());
        values.put(KEY_SESSION_ID, requestEntry.getSessionID());
        values.put(KEY_FRAME_ID, requestEntry.getFrameID());
        values.put(KEY_UNIX_TIME_REQUEST_SENT, requestEntry.getUnixTimeRequestSent());
        values.put(KEY_SERVER_IP, requestEntry.getServerIP());
        values.put(KEY_SERVER_PORT, requestEntry.getServerPort());
        values.put(KEY_REQUEST_ARRAY_LENGTH, requestEntry.getRequestArrayLength());
        values.put(KEY_REQUEST_GPS_COORD, requestEntry.getRequestGPSCoord());
        values.put(KEY_PROTOCOL, requestEntry.getProtocol());
        values.put(KEY_RESOLUTION, requestEntry.getResolution());
        values.put(KEY_PRE_PROCESSING_TIME, requestEntry.getPreProcessingTime());

        long request_id = db.insert(TABLE_REQUEST, null, values);
        return request_id;
    }

    public long createResultsEntry(ResultsEntry resultsEntry) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_RECOGNITION_ID, resultsEntry.getRecognitionID());
        values.put(KEY_SESSION_ID, resultsEntry.getSessionID());
        values.put(KEY_FRAME_ID, resultsEntry.getFrameID());
        values.put(KEY_UNIX_TIME_RESULTS_REC, resultsEntry.getUnixTimeResultsRec());
        values.put(KEY_SERVER_IP, resultsEntry.getServerIP());
        values.put(KEY_SERVER_PORT, resultsEntry.getServerPort());
        values.put(KEY_RESULTS_GPS_COORD, resultsEntry.getResultsGPSCoord());
        values.put(KEY_RECEIVED_RESULTS, resultsEntry.getReceivedResults());
        values.put(KEY_POST_PROCESSING_TIME, resultsEntry.getPostProcessingTime());

        long results_id = db.insert(TABLE_RESULTS, null, values);
        return results_id;
    }

    public long createSessionsEntry(SessionInfo sessionInfo) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_SESSION_ID, sessionInfo.getSessionID());
        values.put(KEY_UNIX_TIME_INITIATED, sessionInfo.getUnixTimeInitiated());
        values.put(KEY_UNIX_TIME_ENDED, sessionInfo.getUnixTimeEnded());
        values.put(KEY_BYTES_RECEIVED, sessionInfo.getBytesReceived());
        values.put(KEY_PACKETS_RECEIVED, sessionInfo.getPacketsReceived());
        values.put(KEY_BYTES_TRANSMITTED, sessionInfo.getBytesTransmitted());
        values.put(KEY_PACKETS_TRANSMITTED, sessionInfo.getPacketsTransmitted());
        values.put(KEY_AVG_RTT, sessionInfo.getAvgRTT());
        values.put(KEY_NUM_REQUESTS, sessionInfo.getNumRequests());
        values.put(KEY_NUM_RESULTS, sessionInfo.getNumResults());

        long sessions_id = db.insert(TABLE_SESSIONS, null, values);
        return sessions_id;

    }

    /**
     * Fetching all entries
     */
    public List<ServerInfo> getAllServerInfo() {
        List<ServerInfo> servers = new ArrayList<ServerInfo>();
        String selectQuery = "SELECT * FROM " + TABLE_SERVER;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // loop through rows and append to list
        if (c.moveToFirst()) {
            do {
                ServerInfo si = new ServerInfo();
                si.setId(c.getInt((c.getColumnIndex(KEY_SERVER_ID))));
                si.setUnixTimeAdded(c.getString(c.getColumnIndex(KEY_UNIX_TIME_ADDED)));
                si.setServerIP(c.getString(c.getColumnIndex(KEY_SERVER_IP)));
                si.setServerPort(c.getInt(c.getColumnIndex(KEY_SERVER_PORT)));

                servers.add(si);
            } while (c.moveToNext());
        }
        return servers;
    }

    public List<RequestEntry> getAllRequestEntry() {
        List<RequestEntry> requests = new ArrayList<RequestEntry>();
        String selectQuery = "SELECT * FROM " + TABLE_REQUEST;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if (c.moveToFirst()) {
            do {
                RequestEntry re = new RequestEntry();
                re.setRecognitionID(c.getString(c.getColumnIndex(KEY_RECOGNITION_ID)));
                re.setSessionID(c.getInt(c.getColumnIndex(KEY_SESSION_ID)));
                re.setFrameID(c.getInt(c.getColumnIndex(KEY_FRAME_ID)));
                re.setUnixTimeRequestSent(c.getString(c.getColumnIndex(KEY_UNIX_TIME_REQUEST_SENT)));
                re.setServerIP(c.getString(c.getColumnIndex(KEY_SERVER_IP)));
                re.setServerPort(c.getInt(c.getColumnIndex(KEY_SERVER_PORT)));
                re.setRequestArrayLength(c.getInt(c.getColumnIndex(KEY_REQUEST_ARRAY_LENGTH)));
                re.setRequestGPSCoord(c.getString(c.getColumnIndex(KEY_REQUEST_GPS_COORD)));
                re.setProtocol(c.getString(c.getColumnIndex(KEY_PROTOCOL)));
                re.setResolution(c.getString(c.getColumnIndex(KEY_RESOLUTION)));
                re.setPreProcessingTime(c.getString(c.getColumnIndex(KEY_PRE_PROCESSING_TIME)));

                requests.add(re);
            } while (c.moveToNext());
        }
        return requests;
    }

    public List<ResultsEntry> getAllResultsEntry() {
        List<ResultsEntry> results = new ArrayList<ResultsEntry>();
        String selectQuery = "SELECT * FROM " + TABLE_RESULTS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if (c.moveToFirst()) {
            do {
                ResultsEntry re = new ResultsEntry();
                re.setRecognitionID(c.getString(c.getColumnIndex(KEY_RECOGNITION_ID)));
                re.setSessionID(c.getInt(c.getColumnIndex(KEY_SESSION_ID)));
                re.setFrameID(c.getInt(c.getColumnIndex(KEY_FRAME_ID)));
                re.setUnixTimeResultsRec(c.getString(c.getColumnIndex(KEY_UNIX_TIME_RESULTS_REC)));
                re.setServerIP(c.getString(c.getColumnIndex(KEY_SERVER_IP)));
                re.setServerPort(c.getInt(c.getColumnIndex(KEY_SERVER_PORT)));
                re.setReceivedResults(c.getString(c.getColumnIndex(KEY_RECEIVED_RESULTS)));
                re.setPostProcessingTime(c.getString(c.getColumnIndex(KEY_POST_PROCESSING_TIME)));

                results.add(re);
            } while (c.moveToNext());
        }
        return results;
    }

    /*
     * Deleting entries
     */
    public void deleteServer(ServerInfo server) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_SERVER, KEY_SERVER_ID + " =  ?",
                new String[] { String.valueOf(server.getServerID()) });
    }

    /*
     * Closing database
     */
    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }


}
