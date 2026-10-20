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
        instance = new MIBLogger("/fs/sda0/MIBLogger");
        return instance;
    }

    public MIBLogger(String configFilePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(configFilePath));
            String line = br.readLine();
            if (line != null) {
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
            this.info("No config file found at " + configFilePath);
            System.out.println("Error reading the log configuration file.");
        }

        this.info("Starting with log level of " + logLevel);
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