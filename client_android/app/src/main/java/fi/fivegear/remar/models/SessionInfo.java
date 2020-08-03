package fi.fivegear.remar.models;

public class SessionInfo {
    int tableSessionID;
    int sessionID;
    String unixTimeInitiated;
    String unixTimeEnded;
    long bytesReceived;
    long packetsReceived;
    long bytesTransmitted;
    long packetsTransmitted;
    float avgRTT;
    int numRequests;
    int numResults;

    // constructors
    public SessionInfo() {
    }

    public SessionInfo(int sessionID, String unixTimeInitiated, String unixTimeEnded,
                       long bytesReceived, long packetsReceived, long bytesTransmitted,
                       long packetsTransmitted, float avgRTT, int numRequests, int numResults) {
        this.sessionID = sessionID;
        this.unixTimeInitiated = unixTimeInitiated;
        this.unixTimeEnded = unixTimeEnded;
        this.bytesReceived = bytesReceived;
        this.packetsReceived = packetsReceived;
        this.bytesTransmitted = bytesTransmitted;
        this.packetsTransmitted = packetsTransmitted;
        this.avgRTT = avgRTT;
        this.numRequests = numRequests;
        this.numResults = numResults;
    }

    // setters
    public void setTableSessionID(int tableSessionID) {
        this.tableSessionID = tableSessionID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }

    public void setUnixTimeInitiated(String unixTimeInitiated) {
        this.unixTimeInitiated = unixTimeInitiated;
    }

    public void setUnixTimeEnded(String unixTimeEnded) {
        this.unixTimeEnded = unixTimeEnded;
    }

    public void setBytesReceived(long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public void setPacketsReceived(long packetsReceived) {
        this.packetsReceived = packetsReceived;
    }

    public void setBytesTransmitted(long bytesTransmitted) {
        this.bytesTransmitted = bytesTransmitted;
    }

    public void setPacketsTransmitted(long packetsTransmitted) {
        this.packetsTransmitted = packetsTransmitted;
    }

    public void setAvgRTT(float avgRTT) {
        this.avgRTT = avgRTT;
    }

    public void setNumRequests(int numRequests) {
        this.numRequests = numRequests;
    }

    public void setNumResults(int numResults) {
        this.numResults = numResults;
    }

    // getters
    public int getTableSessionID() {
        return tableSessionID;
    }

    public int getSessionID() {
        return sessionID;
    }

    public String getUnixTimeInitiated() {
        return unixTimeInitiated;
    }

    public String getUnixTimeEnded() {
        return unixTimeEnded;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public long getPacketsReceived() {
        return packetsReceived;
    }

    public long getBytesTransmitted() {
        return bytesTransmitted;
    }

    public long getPacketsTransmitted() {
        return packetsTransmitted;
    }

    public float getAvgRTT() {
        return avgRTT;
    }

    public int getNumRequests() {
        return numRequests;
    }

    public int getNumResults() {
        return numResults;
    }
}
