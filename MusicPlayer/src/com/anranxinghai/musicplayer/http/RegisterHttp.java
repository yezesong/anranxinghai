package com.anranxinghai.musicplayer.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.anranxinghai.musicplayer.util.HttpUtil;


public class RegisterHttp {

	private String url = "http://10.132.12.165:8080/Server/RegisterServlet";
	//private String url = "http://192.168.0.103:8080/Server/RegisterServlet";
	public boolean postSuccess(Map<String,String> params){
		String result = HttpUtil.send(url,params);
		if ("registerSuccess".equals(result)) {
			return true;
		} else {
			return false;
		}
	}
	
	
	 
}