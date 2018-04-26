package com.qingcheng.home.util;

import android.util.Log;

public class QCLog {
	private static final String TAG = "Launcher3";
	private static final String TAG_ADD = "Launcher3_ADD";
	private static final String name1 = "";
	public static final boolean DEBUG = true;
	
    private static final QCLog INSTANCE = new QCLog();
    
    /**
     * private constructor here, It is a singleton class.
     */
    private QCLog() {
		// TODO Auto-generated constructor stub
	}

    /**
     * The FileManagerLog is a singleton class, this static method can be used
     * to obtain the unique instance of this class.
     *
     * @return The global unique instance of FileManagerLog.
     */
    public static QCLog getInstance() {
        return INSTANCE;
    }

    /**
     * The method prints the log, level error.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void e(String tag, String msg) {
        Log.e(TAG+name1, tag + ", --------" + msg + "!--------");
    }

    /**
     * The method prints the log, level error.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t an exception to log.
     */
    public static void e(String tag, String msg, Throwable t) {
        Log.e(TAG+name1, tag + ", --------" + msg + "!--------", t);
    }

    /**
     * The method prints the log, level warning.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void w(String tag, String msg) {
        Log.w(TAG+name1, tag + ", --------" + msg + "!--------");
    }

    /**
     * The method prints the log, level warning.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t an exception to log.
     */
    public static void w(String tag, String msg, Throwable t) {
        Log.w(TAG+name1, tag + ", --------" + msg + "!--------", t);
    }

    /**
     * The method prints the log, level debug.
     * 
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void i(String tag, String msg,boolean f) {
        Log.i(TAG_ADD+name1, tag + ", --------" + msg + "!--------");
    }
    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void i(String tag, String msg ) {
        Log.i(TAG+name1, tag + ", --------" + msg + "!--------");
    }


    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t an exception to log.
     */
    public static void i(String tag, String msg, Throwable t) {
        Log.i(TAG+name1, tag + ", --------" + msg + "!--------", t);
    }

    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void d(String tag, String msg) {
        Log.d(TAG+name1, tag + ", --------" + msg + "!--------");
    }

    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t An exception to log.
     */
    public static void d(String tag, String msg, Throwable t) {
        Log.d(TAG+name1, tag + ", --------" + msg + "!--------", t);
    }

    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void v(String tag, String msg) {
        Log.v(TAG+name1, tag + ", --------" + msg + "!--------");
    }

    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t An exception to log.
     */
    public static void v(String tag, String msg, Throwable t) {
        Log.v(TAG+name1, tag + ", --------" + msg + "!--------", t);
    }
}
