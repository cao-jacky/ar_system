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

    // Common column names
    private static final String KEY_RECOGNITION_ID = "recognitionID";
    private static final String KEY_SERVER_IP = "serverIP";
    private static final String KEY_SERVER_PORT = "serverPort";

    // server_info table - column names
    private static final String KEY_SERVER_ID = "serverID";
    private static final String KEY_UNIX_TIME_ADDED = "unixTimeAdded";

    // request_items table - column names
    private static final String KEY_REQUEST_ID = "requestID";
    private static final String KEY_UNIX_TIME_REQUEST_SENT = "unixTimeRequestSent";
    private static final String KEY_REQUEST_ARRAY_LENGTH = "requestArrayLength";
    private static final String KEY_REQUEST_GPS_COORD = "requestGPSCoord";
    private static final String KEY_PROTOCOL = "protocol";

    // result_items table - column names
    private static final String KEY_RESULTS_ID = "resultsID";
    private static final String KEY_UNIX_TIME_RESULTS_REC = "unixTimeResultsRec";
    private static final String KEY_RECEIVED_RESULTS = "receivedResults";

    // Table create statements
    // server_info
    private static final String CREATE_TABLE_SERVER_INFO = "CREATE TABLE "
            + TABLE_SERVER + "(" + KEY_SERVER_ID + " INTEGER PRIMARY KEY," + KEY_UNIX_TIME_ADDED
            + " INTEGER," + KEY_SERVER_IP + " TEXT," + KEY_SERVER_PORT
            + " INTEGER" + ")";

    // request_items
    private static final String CREATE_TABLE_REQUEST_ITEMS = "CREATE TABLE "
            + TABLE_REQUEST + "(" + KEY_REQUEST_ID + " INTEGER PRIMARY KEY," + KEY_RECOGNITION_ID
            + " TEXT," + KEY_UNIX_TIME_REQUEST_SENT + " INTEGER," + KEY_SERVER_IP
            + " TEXT," + KEY_SERVER_PORT + " INTEGER," + KEY_REQUEST_ARRAY_LENGTH
            + " INTEGER," + KEY_REQUEST_GPS_COORD + " TEXT," + KEY_PROTOCOL
            + " TEXT" + ")";

    // result_items
    private static final String CREATE_TABLE_RESULT_ITEMS = "CREATE TABLE "
            + TABLE_RESULTS + "(" + KEY_RESULTS_ID + " INTEGER PRIMARY KEY," + KEY_RECOGNITION_ID
            + " TEXT," + KEY_UNIX_TIME_RESULTS_REC + " INTEGER," + KEY_SERVER_IP
            + " TEXT," + KEY_SERVER_PORT + " INTEGER," + KEY_RECEIVED_RESULTS
            + " TEXT" + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating tables
        db.execSQL(CREATE_TABLE_SERVER_INFO);
        db.execSQL(CREATE_TABLE_REQUEST_ITEMS);
        db.execSQL(CREATE_TABLE_RESULT_ITEMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REQUEST);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESULTS);

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
        values.put(KEY_UNIX_TIME_REQUEST_SENT, requestEntry.getUnixTimeRequestSent());
        values.put(KEY_SERVER_IP, requestEntry.getServerIP());
        values.put(KEY_SERVER_PORT, requestEntry.getServerPort());
        values.put(KEY_REQUEST_ARRAY_LENGTH, requestEntry.getRequestArrayLength());
        values.put(KEY_REQUEST_GPS_COORD, requestEntry.getRequestGPSCoord());
        values.put(KEY_PROTOCOL, requestEntry.getProtocol());

        long request_id = db.insert(TABLE_REQUEST, null, values);
        return request_id;
    }

    public long createResultsEntry(ResultsEntry resultsEntry) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_RECOGNITION_ID, resultsEntry.getRecognitionID());
        values.put(KEY_UNIX_TIME_RESULTS_REC, resultsEntry.getUnixTimeResultsRec());
        values.put(KEY_SERVER_IP, resultsEntry.getServerIP());
        values.put(KEY_SERVER_PORT, resultsEntry.getServerPort());
        values.put(KEY_RECEIVED_RESULTS, resultsEntry.getReceivedResults());

        long results_id = db.insert(TABLE_RESULTS, null, values);
        return results_id;
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
                re.setRequestID(c.getInt(c.getColumnIndex(KEY_REQUEST_ID)));
                re.setRecognitionID(c.getString(c.getColumnIndex(KEY_RECOGNITION_ID)));
                re.setUnixTimeRequestSent(c.getString(c.getColumnIndex(KEY_UNIX_TIME_REQUEST_SENT)));
                re.setServerIP(c.getString(c.getColumnIndex(KEY_SERVER_IP)));
                re.setServerPort(c.getInt(c.getColumnIndex(KEY_SERVER_PORT)));
                re.setRequestArrayLength(c.getInt(c.getColumnIndex(KEY_REQUEST_ARRAY_LENGTH)));
                re.setRequestGPSCoord(c.getString(c.getColumnIndex(KEY_REQUEST_GPS_COORD)));
                re.setProtocol(c.getString(c.getColumnIndex(KEY_PROTOCOL)));

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
                re.setResultsID(c.getInt(c.getColumnIndex(KEY_RESULTS_ID)));
                re.setRecognitionID(c.getString(c.getColumnIndex(KEY_RECOGNITION_ID)));
                re.setUnixTimeResultsRec(c.getString(c.getColumnIndex(KEY_UNIX_TIME_RESULTS_REC)));
                re.setServerIP(c.getString(c.getColumnIndex(KEY_SERVER_IP)));
                re.setServerPort(c.getInt(c.getColumnIndex(KEY_SERVER_PORT)));
                re.setReceivedResults(c.getString(c.getColumnIndex(KEY_RECEIVED_RESULTS)));

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
