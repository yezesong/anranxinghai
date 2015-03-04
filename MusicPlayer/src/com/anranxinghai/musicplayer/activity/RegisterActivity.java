package com.anranxinghai.musicplayer.activity;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.anranxinghai.musicplayer.MusicPlayerApplication;
import com.anranxinghai.musicplayer.R;
import com.anranxinghai.musicplayer.R.id;
import com.anranxinghai.musicplayer.R.layout;
import com.anranxinghai.musicplayer.R.menu;
import com.anranxinghai.musicplayer.constant.Constant;
import com.anranxinghai.musicplayer.runnable.RegisterThread;
import com.anranxinghai.musicplayer.util.NetWork;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends Activity implements OnClickListener {

	private AutoCompleteTextView userTelText;
	private EditText userNameText;
	private Button registerButton = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		MusicPlayerApplication.getInstance().addActivity(this);
		registerButton = (Button) findViewById(R.id.register_button);
		registerButton.setOnClickListener(this);
		userTelText = (AutoCompleteTextView) findViewById(R.id.text_user_tel);
		userNameText = (EditText) findViewById(R.id.text_user_name);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		Intent intent = new Intent();
		switch (v.getId()) {
		case R.id.register_button:

			Map<String, String> params = new HashMap<String, String>();
			try {
				params.put("userName", new String(userNameText.getText()
						.toString().getBytes("utf-8"), "iso-8859-1"));
				params.put("userTel", new String(userTelText.getText()
						.toString().getBytes("utf-8"), "iso-8859-1"));
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// 为防止线程还未执行完添加上述线程休眠200ms的代码
			int i = 0;
			while (i < 1000) {
				RegisterThread registerThread = new RegisterThread(params);
				registerThread.start();

				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				boolean isRegisterSuccess = registerThread.isRegisterSuccess();
				isRegisterSuccess = true;
				if (isRegisterSuccess) {

					intent.setClass(RegisterActivity.this,
							MusicPlayerActivity.class);

					// Toast.makeText(this, "注册成功！", Toast.LENGTH_SHORT).show();
					Log.i("RegisterActivity", "注册成功");
					startActivity(intent);
					finish();
				} else {
					Log.i("RegisterActivity", "注册失败");
					// Toast.makeText(this, "注册失败！", Toast.LENGTH_SHORT).show();
				}
				i++;
			}

			break;

		default:
			break;
		}
	}

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { // Inflate the
	 * menu; this adds items to the action bar if it is present.
	 * getMenuInflater().inflate(R.menu.register, menu); return true; }
	 * 
	 * @Override public boolean onOptionsItemSelected(MenuItem item) { // Handle
	 * action bar item clicks here. The action bar will // automatically handle
	 * clicks on the Home/Up button, so long // as you specify a parent activity
	 * in AndroidManifest.xml. int id = item.getItemId(); if (id ==
	 * R.id.action_settings) { return true; } return
	 * super.onOptionsItemSelected(item); }
	 */
}
