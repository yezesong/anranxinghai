package com.anranxinghai.musicplayer.activity;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.anranxinghai.musicplayer.MusicPlayerApplication;
import com.anranxinghai.musicplayer.R;
import com.anranxinghai.musicplayer.constant.Constant;
import com.anranxinghai.musicplayer.db.DatabaseHelper;
import com.anranxinghai.musicplayer.runnable.FileTransThread;

/**
 * 这个类是播放音乐的主界面
 * 
 * @author anranxinghai
 *
 */
public class MusicPlayerActivity extends Activity implements
		OnCompletionListener, OnErrorListener, OnSeekBarChangeListener,
		OnItemClickListener, Runnable {

	protected static final int SEARCH_MUSIC_SUCCESS = 0;
	private SeekBar playSeekBar;
	private ListView musicList;
	private ImageButton btnPlay;
	private TextView currentTime, totalTime, showName;
	private ArrayList<String> list;
	private ProgressDialog progressDialog;// 进度条对话框。
	private MusicListAdapter mla;// 适配器
	private MediaPlayer mediaPlayer;
	private final String MUSIC_DB = "music_list_database";

	DatabaseHelper dataDatabaseHelper = null;
	SQLiteDatabase dataDatabase = null;

	// 定义线程池（同时只能有一个线程运行）

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music_player);

		Constant.musicInfos = scanAllAudioFiles();
		dataDatabaseHelper = new DatabaseHelper(this, MUSIC_DB);
		/* SQLiteDatabase createdbSqLiteDatabase = */
		dataDatabase = dataDatabaseHelper.getWritableDatabase();
		mla = new MusicListAdapter();
		list = Constant.list;
		if (isTableExist("music")) {
			// 游标, , , , null
			list.clear();
			Cursor cursor = dataDatabase.query("music", new String[] { "id",
					"music_name" }, null, null, null, null, null);
			while (cursor.moveToNext()) {
				String name = cursor.getString(cursor
						.getColumnIndex("music_name"));
				System.out.println("query-->" + name);
				list.add(name);
			}
			handler.sendEmptyMessage(SEARCH_MUSIC_SUCCESS);
		}

		mediaPlayer = MusicPlayerApplication.getInstance().getMediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		initView();
	}

	// 初始化控件
	private void initView() {
		btnPlay = (ImageButton) findViewById(R.id.btn_play);
		playSeekBar = (SeekBar) findViewById(R.id.seek_play_seek_bar);
		playSeekBar.setOnSeekBarChangeListener(this);
		musicList = (ListView) findViewById(R.id.list_music_list);
		musicList.setOnItemClickListener(this);
		currentTime = (TextView) findViewById(R.id.text_current_time);
		totalTime = (TextView) findViewById(R.id.text_total_time);
		showName = (TextView) findViewById(R.id.text_show_name);
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case SEARCH_MUSIC_SUCCESS:
				musicList.setAdapter(mla);
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
				break;
			case Constant.CURR_TIME_VALUE:
				currentTime.setText(msg.obj.toString());
				break;
			case Constant.DOWNLOAD_SUCCESS:
				Bundle bundle = new Bundle();
				Intent intent = new Intent();
				String utfSongName = Constant.musicInfos[0].get(list.get(Constant.currentIndex));

				String lyricUrl = Constant.HOME + utfSongName + ".lrc";
				bundle.putString("lyricUrl", lyricUrl);
				intent.putExtras(bundle);
				intent.setClass(MusicPlayerActivity.this, LyricActivity.class);
				startActivity(intent);
				break;
			default:
				break;
			}
		}

	};

	public boolean isTableExist(String tabName) {
		boolean result = false;
		if (tabName == null) {
			return false;
		}
		Cursor cursor = null;
		try {

			String sql = "select count(*) as c from sqlite_master where type ='table' and name ='"
					+ tabName.trim() + "' ";
			cursor = dataDatabase.rawQuery(sql, null);
			if (cursor.moveToNext()) {
				int count = cursor.getInt(0);
				if (count > 0) {
					result = true;
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return result;
	}
	

	class MusicListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.list_item,
						null);
			}
			TextView musicName = (TextView) convertView
					.findViewById(R.id.text_music_name);
			musicName.setText(Constant.musicInfos[0].get(list.get(position)) + "--" + Constant.musicInfos[1].get(list.get(position)));
			return convertView;
		}

	}

	private void playOrPause() {
		switch (Constant.currentState) {
		case Constant.IDLE:
			start();
			break;
		case Constant.PAUSE:
			mediaPlayer.pause();
			btnPlay.setImageResource(R.drawable.ic_media_play);
			Constant.currentState = Constant.START;
			break;
		case Constant.START:
			mediaPlayer.start();
			downloadLyric(null);
			btnPlay.setImageResource(R.drawable.ic_media_pause);
			Constant.currentState = Constant.PAUSE;
		default:
			break;
		}
	}

	private void start() {

		if (list.size() > 0 && Constant.currentIndex < list.size()) {
			String songPath = list.get(Constant.currentIndex);
			mediaPlayer.reset();
			try {
				mediaPlayer.setDataSource(songPath);
				mediaPlayer.prepare();
				mediaPlayer.start();
				initSeekBar();
				Constant.es.execute(this);
				String fileName = new File(songPath).getName();
				showName.setText(fileName);
				btnPlay.setImageResource(R.drawable.ic_media_pause);
				Constant.currentState = Constant.PAUSE;
				
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		} else {
			Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
		}
	}

	private void initSeekBar() {
		// TODO Auto-generated method stub
		int time = mediaPlayer.getDuration();
		playSeekBar.setMax(time);
		playSeekBar.setProgress(0);
		totalTime.setText(toTime(time));
	}

	private String toTime(int time) {
		// TODO Auto-generated method stub
		int minute = time / 1000 / 60;
		int s = time / 1000 % 60;
		String mm = null;
		String ss = null;
		if (minute < 10) {
			mm = "0" + minute;
		} else {
			mm = minute + "";
		}
		if (s < 10) {
			ss = "0" + s;
		} else {
			ss = s + "";
		}

		return mm + ":" + ss;

	}

	private void previous() {
		if ((Constant.currentIndex - 1) >= 0) {
			Constant.currentIndex--;
			start();
			downloadLyric(null);
		} else {
			Toast.makeText(this, "现在已经是第一首歌了", Toast.LENGTH_SHORT).show();
		}
	}

	private void next() {
		if ((Constant.currentIndex + 1) < list.size()) {
			Constant.currentIndex++;
			start();
			downloadLyric(null);
		} else {
			Toast.makeText(this, "现在已经是最后首歌了", Toast.LENGTH_SHORT).show();
		}
	}

	// 播放按钮
	public void playOrPause(View v) {
		playOrPause();
	}

	// 上一首按钮
	public void playPrevious(View v) {
		previous();
	}

	// 下一首按钮
	public void playNext(View v) {
		next();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.music_player, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.

		switch (item.getItemId()) {
		case R.id.item_search:
			list.clear();
			// 是否有外部存储
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				progressDialog = ProgressDialog
						.show(this, "", "正在搜索音乐文件", true);
				new Thread(new Runnable() {

					String[] ext = { ".mp3" };
					File file = Environment.getExternalStorageDirectory();

					@Override
					public void run() {
						// TODO Auto-generated method stub
						search(file, ext);
						handler.sendEmptyMessage(SEARCH_MUSIC_SUCCESS);
					}
				}).start();
			} else {
				Toast.makeText(this, "请插入内存卡……", Toast.LENGTH_LONG).show();
			}
			break;

		case R.id.item_clear:
			if (list != null) {
				list.clear();
				clearTable(Constant.TABLE_NAME);
				mla.notifyDataSetChanged();
			}
			break;
		case R.id.item_exit:
			Constant.flag = false;
			finish();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void search(File file, String[] ext) {
		if (file != null) {
			if (file.isDirectory()) {
				File[] listFile = file.listFiles();
				if (listFile != null) {
					for (int i = 0; i < listFile.length; i++) {
						search(listFile[i], ext);
					}
				}
			} else {
				String filename = file.getAbsolutePath();
				for (int i = 0; i < ext.length; i++) {
					if (filename.endsWith(ext[i])) {
						list.add(filename);
						ContentValues values = new ContentValues();
						values.put("id", i + 1);
						values.put("music_name", filename);
						dataDatabase.insert("music", null, values);
						break;
					}
				}
			}
		}
	}

	public void clearTable(String tableName) {
		String sql = "DELETE FROM " + tableName + ";";
		dataDatabase.execSQL(sql);
		//revertSeq(tableName);
	}

	private void revertSeq(String tableName) {
		String sql = "update sqlite_sequence set seq=0 where name= '"
				+ tableName + "'";
		dataDatabase.execSQL(sql);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		if (fromUser) {
			mediaPlayer.seekTo(progress);
		}

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		mediaPlayer.reset();
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		if (list.size() > 0) {
			next();
		} else {
			Toast.makeText(this, "当前列表为空", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		
		if (!mediaPlayer.isPlaying() || Constant.currentIndex != position) {

			Constant.currentIndex = position;
			start();
			
		}
		Intent intent = new Intent();
		intent.setClass(this, LyricActivity.class);
		String utfSongName = Constant.musicInfos[0].get(list.get(position));
		String songName = null;

		String lyricUrl = Constant.HOME + utfSongName + ".lrc";
		Bundle bundle = null;
		bundle = new Bundle();
		bundle.putString("lyricUrl", lyricUrl);
		intent.putExtras(bundle);

		if (downloadLyric(handler)) {
			return;
		}else {
			startActivity(intent);
		}
		
	}
	
	
	public boolean downloadLyric(Handler handler){
		String utfSongName = Constant.musicInfos[0].get(list.get(Constant.currentIndex));
		String songName = null;

		String lyricUrl = Constant.HOME + utfSongName + ".lrc";
			try {
				songName = new String(utfSongName.getBytes("utf-8"), "iso-8859-1");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (!new File(lyricUrl).exists()) {
				Map<String, String> params = new HashMap<String, String>();
				params.put("musicName", songName);
				new FileTransThread(params,handler).start();
				return true;
			}
			
			return false;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Constant.flag = true;
		while (Constant.flag) {
			int currentTime = mediaPlayer.getCurrentPosition();
			if (currentTime < playSeekBar.getMax()) {
				playSeekBar.setProgress(currentTime);
				Message message = handler.obtainMessage(
						Constant.CURR_TIME_VALUE, toTime(currentTime));
				// handler.handleMessage(message);//Handler使用必须是sendMessage
				handler.sendMessage(message);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			} else {
				Constant.flag = false;
			}
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			Constant.flag = false;
			mediaPlayer.release();
		}
		super.onDestroy();
	}

	public HashMap<String, String>[] scanAllAudioFiles() {
		// 生成动态数组，并且转载数据
		HashMap<String, String>[] maps = new HashMap[2];
		maps[0] = new HashMap<String, String>();
		maps[1] = new HashMap<String, String>();
		// 查询媒体数据库
		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		// 遍历媒体数据库
		if (cursor.moveToFirst()) {

			while (!cursor.isAfterLast()) {

				// 歌曲编号
				int id = cursor.getInt(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
				// 歌曲标题
				String tilte = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
				// 歌曲的专辑名：MediaStore.Audio.Media.ALBUM
				String album = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
				// 歌曲的歌手名： MediaStore.Audio.Media.ARTIST
				String artist = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
				// 歌曲文件的路径 ：MediaStore.Audio.Media.DATA
				String url = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
				// 歌曲的总播放时长 ：MediaStore.Audio.Media.DURATION
				int duration = cursor
						.getInt(cursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
				// 歌曲文件的大小 ：MediaStore.Audio.Media.SIZE
				Long size = cursor.getLong(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));

				if (size > 1024 * 800) {// 大于800K

					maps[0].put(url, tilte);
					maps[1].put(url, artist);
				}
				cursor.moveToNext();
			}
		}
		return maps;
	}

}
