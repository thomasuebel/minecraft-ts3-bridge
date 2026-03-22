package de.thomasuebel.mc.ts3bridge.configuration;

public class PluginConfig {

    private String tsHost = "localhost";
    private int tsQueryPort = 10011;
    private String tsQueryUsername = "";
    private String tsQueryPassword = "";
    private int tsVirtualServerId = 1;
    // Optional: select the virtual server by its voice port instead of its ID.
    // Set this to the port your TS clients connect on (e.g. 10006 on 4netplayers).
    // When set to a value > 0, tsVirtualServerId is ignored.
    private int tsVirtualServerPort = 0;
    private String tsServerAddress = "localhost";
    // Message sent to players on join. Use {address} as a placeholder for tsServerAddress.
    private String advertisementMessage = "Join our TeamSpeak server: {address}";
    private boolean chatBridgeEnabled = true;
    private String tsQueryNickname = "MinecraftTS";
    // Channel ID for the chat bridge. 0 = no channel filter (server-wide messages).
    private int tsBridgeChannelId = 0;
    // When true, raw TS event map contents are logged at INFO for debugging.
    private boolean debugLogging = false;

    public String getTsHost() { return tsHost; }
    public void setTsHost(String tsHost) { this.tsHost = tsHost; }

    public int getTsQueryPort() { return tsQueryPort; }
    public void setTsQueryPort(int tsQueryPort) { this.tsQueryPort = tsQueryPort; }

    public String getTsQueryUsername() { return tsQueryUsername; }
    public void setTsQueryUsername(String tsQueryUsername) { this.tsQueryUsername = tsQueryUsername; }

    public String getTsQueryPassword() { return tsQueryPassword; }
    public void setTsQueryPassword(String tsQueryPassword) { this.tsQueryPassword = tsQueryPassword; }

    public int getTsVirtualServerId() { return tsVirtualServerId; }
    public void setTsVirtualServerId(int tsVirtualServerId) { this.tsVirtualServerId = tsVirtualServerId; }

    public int getTsVirtualServerPort() { return tsVirtualServerPort; }
    public void setTsVirtualServerPort(int tsVirtualServerPort) { this.tsVirtualServerPort = tsVirtualServerPort; }

    public String getTsServerAddress() { return tsServerAddress; }
    public void setTsServerAddress(String tsServerAddress) { this.tsServerAddress = tsServerAddress; }

    public String getAdvertisementMessage() { return advertisementMessage; }
    public void setAdvertisementMessage(String advertisementMessage) { this.advertisementMessage = advertisementMessage; }

    public boolean isChatBridgeEnabled() { return chatBridgeEnabled; }
    public void setChatBridgeEnabled(boolean chatBridgeEnabled) { this.chatBridgeEnabled = chatBridgeEnabled; }

    public String getTsQueryNickname() { return tsQueryNickname; }
    public void setTsQueryNickname(String tsQueryNickname) { this.tsQueryNickname = tsQueryNickname; }

    public int getTsBridgeChannelId() { return tsBridgeChannelId; }
    public void setTsBridgeChannelId(int tsBridgeChannelId) { this.tsBridgeChannelId = tsBridgeChannelId; }

    public boolean isDebugLogging() { return debugLogging; }
    public void setDebugLogging(boolean debugLogging) { this.debugLogging = debugLogging; }
}
