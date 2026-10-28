package net.kdt.pojavlaunch.server;

public class CsServerRuntimeStatus {
    public String serverId;
    public CsServerState state = CsServerState.STOPPED;
    public long uptimeMs;
    public int playersOnline;
    public int playersMax;
    public long memoryUsedMb;
    public long memoryMaxMb;
    public String lastError;
    public Long pid;

    public CsServerRuntimeStatus() {}

    public CsServerRuntimeStatus(String serverId, CsServerState state) {
        this.serverId = serverId;
        this.state = state;
    }
}
