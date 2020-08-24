package fi.fivegear.remar.models;

public class RequestEntry {
    int requestID;
    String recognitionID;
    int sessionID;
    int frameID;
    String unixTimeRequestSent;
    String serverIP;
    int serverPort;
    int requestArrayLength;
    String requestGPSCoord;
    String protocol;
    String resolution;
    String preProcessingTime;

    // constructors
    public RequestEntry() {
    }

    public RequestEntry(String recognitionID, int sessionID, int frameID, String unixTimeRequestSent,
                        String serverIP, int serverPort, int requestArrayLength, String requestGPSCoord,
                        String protocol, String resolution, String preProcessingTime) {
        this.recognitionID = recognitionID;
        this.sessionID = sessionID;
        this.frameID = frameID;
        this.unixTimeRequestSent = unixTimeRequestSent;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.requestArrayLength = requestArrayLength;
        this.requestGPSCoord = requestGPSCoord;
        this.protocol = protocol;
        this.resolution = resolution;
        this.preProcessingTime = preProcessingTime;
    }

    // setter
    public void setRequestID(int requestID) {
        this.requestID = requestID;
    }

    public void setRecognitionID(String recognitionID) {
        this.recognitionID = recognitionID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }

    public void setFrameID(int frameID) {
        this.frameID = frameID;
    }

    public void setUnixTimeRequestSent(String unixTimeRequestSent) {
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

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public void setPreProcessingTime(String preProcessingTime) {
        this.preProcessingTime = preProcessingTime;
    }

    // getter
    public int getRequestID() {
        return requestID;
    }

    public String getRecognitionID() {
        return recognitionID;
    }

    public int getSessionID() {
        return sessionID;
    }

    public int getFrameID() {
        return frameID;
    }

    public String getUnixTimeRequestSent() {
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

    public String getResolution() {
        return resolution;
    }

    public String getPreProcessingTime() {
        return preProcessingTime;
    }
}
