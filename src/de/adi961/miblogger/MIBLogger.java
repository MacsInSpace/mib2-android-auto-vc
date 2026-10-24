package de.adi961.miblogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MIBLogger {

    static String logPrefix = "MIBLogger";

    // System.out of the HMI is not saved anywhere, so also keep a log file
    static String logFilePath = "/tmp/VCAndroidAuto.log";
    static long logFileMaxBytes = 1024 * 1024;

    public static final int TRACE = 1;
    public static final int DEBUG = 2;
    public static final int INFO = 3;
    public static final int ERROR = 4;
    public static final int SILENT = 5;

    private int logLevel = INFO;

    static MIBLogger instance;

    public static MIBLogger getInstance() {
        if (instance != null) {
            return instance;
        }
        instance = new MIBLogger();
        return instance;
    }

    // Either card slot, any name starting with MIBLogger (MIBLogger,
    // MIBLogger.txt, MIBLogger.text ...)
    static final String[] cardDirs = { "/fs/sda0", "/fs/sdb0" };

    private boolean configFound = false;

    public MIBLogger() {
        findConfig();
        if (!configFound) {
            this.info("No config file MIBLogger* found on the SD card");
        }
        this.info("Starting with log level of " + logLevel);
    }

    public MIBLogger(String configFilePath) {
        readConfig(configFilePath);
        this.info("Starting with log level of " + logLevel);
    }

    // lsd starts about 14 s after boot; look again once Android Auto
    // connects in case the card was not readable yet
    public void recheckConfig() {
        if (configFound) {
            return;
        }
        findConfig();
        if (configFound) {
            this.info("Config file found later, log level now " + logLevel);
        }
    }

    private void findConfig() {
        for (int d = 0; d < cardDirs.length && !configFound; d++) {
            String[] names = new File(cardDirs[d]).list();
            if (names == null) {
                continue;
            }
            for (int i = 0; i < names.length && !configFound; i++) {
                if (names[i].toLowerCase().startsWith("miblogger")) {
                    readConfig(cardDirs[d] + "/" + names[i]);
                    if (configFound) {
                        this.info("Config file " + cardDirs[d] + "/" + names[i]);
                    }
                }
            }
        }
    }

    private void readConfig(String configFilePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(configFilePath));
            String line = br.readLine();
            configFound = true;
            if (line != null) {
                line = lettersOnly(line);
                if (line.equalsIgnoreCase("TRACE")) {
                    logLevel = TRACE;
                } else if (line.equalsIgnoreCase("DEBUG")) {
                    logLevel = DEBUG;
                } else if (line.equalsIgnoreCase("INFO")) {
                    logLevel = INFO;
                } else if (line.equalsIgnoreCase("ERROR")) {
                    logLevel = ERROR;
                } else if (line.equalsIgnoreCase("SILENT")) {
                    logLevel = SILENT;
                }
            }
            br.close();
        } catch (IOException e) {
            // not on this path
        }
    }

    // drops spaces, line ends and a Notepad byte-order mark
    private static String lettersOnly(String line) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public void trace(String message) {
        if (logLevel <= TRACE) {
            log("TRACE", message);
        }
    }

    public void debug(String message) {
        if (logLevel <= DEBUG) {
            log("DEBUG", message);
        }
    }

    public void info(String message) {
        if (logLevel <= INFO) {
            log("INFO", message);
        }
    }

    public void error(String message) {
        if (logLevel <= ERROR) {
            log("ERROR", message);
        }
    }

    void log(String level, String message) {
        String line = "[" + logPrefix + "] [" + level + "] [" + getCallingClassName() + "]: " + message;
        System.out.println(line);
        writeToFile(System.currentTimeMillis() + " " + line);
    }

    private synchronized void writeToFile(String line) {
        try {
            File file = new File(logFilePath);
            FileWriter writer = new FileWriter(logFilePath, file.length() < logFileMaxBytes);
            writer.write(line + "\n");
            writer.close();
        } catch (IOException e) {
            // logging must never break the HMI
        }
    }

    private String getCallingClassName() {
        try {
            throw new Exception();
        } catch (Exception e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace.length >= 4) {
                StackTraceElement caller = stackTrace[3];
                return caller.getClassName() + "." + caller.getMethodName();
            }
        }
        return "UnknownClass";
    }

    public void setLogLevel(int level) {
        this.logLevel = level;
    }

    public int getLogLevel() {
        return logLevel;
    }
}