package fi.fivegear.remar.models;

public class ResultsEntry {
    int resultsID;
    String recognitionID;
    int sessionID;
    int frameID;
    String unixTimeResultsRec;
    String serverIP;
    int serverPort;
    String resultsGPSCoord;
    String receivedResults;

    // constructors
    public ResultsEntry() {
    }

    public ResultsEntry(String recognitionID, int sessionID, int frameID, String unixTimeResultsRec,
                        String serverIP, int serverPort, String resultsGPSCoord, String receivedResults) {
        this.recognitionID = recognitionID;
        this.sessionID = sessionID;
        this.frameID = frameID;
        this.unixTimeResultsRec = unixTimeResultsRec;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.resultsGPSCoord = resultsGPSCoord;
        this.receivedResults = receivedResults;
    }

    // setter
    public void setResultsID(int resultsID) {
        this.resultsID = resultsID;
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

    public void setUnixTimeResultsRec(String unixTimeResultsRec) {
        this.unixTimeResultsRec = unixTimeResultsRec;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setResultsGPSCoord(String resultsGPSCoord) {
        this.resultsGPSCoord = resultsGPSCoord;
    }

    public void setReceivedResults(String receivedResults) {
        this.receivedResults = receivedResults;
    }

    // getter
    public int getResultsID() {
        return resultsID;
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

    public String getUnixTimeResultsRec() {
        return unixTimeResultsRec;
    }

    public String getServerIP() {
        return serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getResultsGPSCoord() {
        return resultsGPSCoord;
    }

    public String getReceivedResults() {
        return receivedResults;
    }
}
