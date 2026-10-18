package com.crxpulse.constants;

public final class WifiProtocol {
    private WifiProtocol() {}

    public static final String CMD_SSID   = ">SSID:";
    public static final String CMD_PASS   = ">PASS:";
    public static final String CMD_SAVE   = ">SAVE";
    public static final String CMD_STATUS = ">STATUS";
    public static final String CMD_CLEAR  = ">CLEAR";
    public static final String CMD_REBOOT = ">REBOOT";

    /** Replies the firmware sends back, used to colour the console. */
    public static final String REPLY_PREFIX = "<";
    public static final String REPLY_OK     = "<OK:";
    public static final String REPLY_ERROR  = "<ERR:";
}
