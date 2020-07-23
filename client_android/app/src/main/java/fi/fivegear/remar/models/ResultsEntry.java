package fi.fivegear.remar.models;

public class ResultsEntry {
    int resultsID;
    String recognitionID;
    long unixTimeResultsRec;
    String serverIP;
    int serverPort;
    String receivedResults;

    // constructors
    public ResultsEntry() {
    }

    public ResultsEntry(int id, String recognitionID, long unixTimeResultsRec, String serverIP,
                        int serverPort, String receivedResults) {
        this.resultsID = id;
        this.recognitionID = recognitionID;
        this.unixTimeResultsRec = unixTimeResultsRec;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.receivedResults = receivedResults;
    }

    // setter
    public void setResultsID(int resultsID) {
        this.resultsID = resultsID;
    }

    public void setRecognitionID(String recognitionID) {
        this.recognitionID = recognitionID;
    }

    public void setUnixTimeResultsRec(long unixTimeResultsRec) {
        this.unixTimeResultsRec = unixTimeResultsRec;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
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

    public long getUnixTimeResultsRec() {
        return unixTimeResultsRec;
    }

    public String getServerIP() {
        return serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getReceivedResults() {
        return receivedResults;
    }
}
