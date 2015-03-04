package com.anranxinghai.musicplayer.constant;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;

public class Constant {
	public static int currentIndex = -1;// 当前播放文件索引
	public static boolean flag = true;// 进度条线程标记

	// 定义当前播放器状态
	public static final int IDLE = 0;// 未工作状态
	public static final int PAUSE = 1;// 暂停状态
	public static final int START = 2;// 播放状态
	public static final int CURR_TIME_VALUE = 1;// 当前时间值
	public static ArrayList<String> list = new ArrayList<String>();
	public static Map<String, String>[] musicInfos = null;
	public static int currentState = IDLE;// 当前状态
	public static ExecutorService es = Executors.newSingleThreadExecutor();
	public static String TABLE_NAME = "music";
	public static final String HOME = "/mnt/sdcard/anranxinghai/music/lyrics/";

	public static final int REGISTER_SUCCESS = 0;
	public static final int REGISTER_FAILED = 1;

	
	public static final int DOWNLOAD_SUCCESS = 3;
	public static final int DOWNLOAD_FAILED = 4;
}
