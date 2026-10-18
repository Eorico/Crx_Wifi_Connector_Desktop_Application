package com.crxpulse.serial;

public interface SerialEvents {
 
    /** One complete newline-terminated line arrived from the device. */
    void onLine(String line);
 
    /** Something happened worth telling the user about (opened, closed, failed). */
    void onStatus(String message, boolean isError);
}
 