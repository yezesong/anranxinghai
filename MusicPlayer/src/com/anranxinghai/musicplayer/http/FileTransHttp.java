package com.anranxinghai.musicplayer.http;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.anranxinghai.musicplayer.util.HttpUtil;
import com.anranxinghai.musicplayer.util.HttpUtils;

public class FileTransHttp {
	
	
	Map<String, String> params ;
	private String path = "mnt/sdcard/anranxinghai/music/lyrics/";
	private String url = "http://10.132.12.165:8080/Server/FileTransServlet?musicName=";
	//private String url = "http://192.168.0.103:8080/Server/FileTransServlet?musicName=";
	public FileTransHttp(Map<String, String> params) {
		this.params = params;
	}
	public boolean downloadFile(){
		String musicName = null;
		String result = "downloadFailed";
		try {
			musicName = new String(params.get("musicName").getBytes("iso-8859-1"),"utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		url += params.get("musicName");
		path = path + musicName + ".lrc";
		/*try {
			url = new String(url.getBytes(),"utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		result = HttpUtils.downloadFile(url, path);
		if (result == null || "downloadFailed".equals(result)) {
			return false;
		}
		return true;
	}
}
