package fi.fivegear.remar.models;

public class ServerInfo {
    int serverID;
    long unixTimeAdded;
    String serverIP;
    int serverPort;

    // constructors
    public ServerInfo() {
    }

    public ServerInfo(long unixTimeAdded, String serverIP, int serverPort) {
        this.unixTimeAdded = unixTimeAdded;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public ServerInfo(int serverID, long unixTimeAdded, String serverIP, int serverPort) {
        this.serverID = serverID;
        this.unixTimeAdded = unixTimeAdded;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    // setters
    public void setId(int serverID) {
        this.serverID = serverID;
    }

    public void setUnixTimeAdded(long unixTimeAdded) {
        this.unixTimeAdded = unixTimeAdded;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    // getters
    public int getServerID() {
        return this.serverID;
    }

    public long getUnixTimeAdded() {
        return this.unixTimeAdded;
    }

    public String getServerIP() {
        return this.serverIP;
    }

    public int getServerPort()  {
        return this.serverPort;
    }

}
