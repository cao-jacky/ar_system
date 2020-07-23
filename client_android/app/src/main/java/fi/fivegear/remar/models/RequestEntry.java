package fi.fivegear.remar.models;

public class RequestEntry {
    int requestID;
    String recognitionID;
    long unixTimeRequestSent;
    String serverIP;
    int serverPort;
    int requestArrayLength;
    String requestGPSCoord;
    String protocol;

    // constructors
    public RequestEntry() {
    }

    public RequestEntry(int id, String recognitionID, long unixTimeRequestSent, String serverIP,
                        int serverPort, int requestArrayLength, String requestGPSCoord,
                        String protocol) {
        this.requestID = id;
        this.recognitionID = recognitionID;
        this.unixTimeRequestSent = unixTimeRequestSent;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.requestArrayLength = requestArrayLength;
        this.requestGPSCoord = requestGPSCoord;
        this.protocol = protocol;
    }

    // setter
    public void setRequestID(int requestID) {
        this.requestID = requestID;
    }

    public void setRecognitionID(String recognitionID) {
        this.recognitionID = recognitionID;
    }

    public void setUnixTimeRequestSent(long unixTimeRequestSent) {
        this.unixTimeRequestSent = unixTimeRequestSent;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setRequestArrayLength(int requestArrayLength) {
        this.requestArrayLength = requestArrayLength;
    }

    public void setRequestGPSCoord(String requestGPSCoord) {
        this.requestGPSCoord = requestGPSCoord;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    // getter
    public int getRequestID() {
        return requestID;
    }

    public String getRecognitionID() {
        return recognitionID;
    }

    public long getUnixTimeRequestSent() {
        return unixTimeRequestSent;
    }

    public String getServerIP() {
        return serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getRequestArrayLength() {
        return requestArrayLength;
    }

    public String getRequestGPSCoord() {
        return requestGPSCoord;
    }

    public String getProtocol() {
        return protocol;
    }
}
