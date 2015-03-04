package com.anranxinghai.musicplayer.serializable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.anranxinghai.musicplayer.constant.Constant;
import com.anranxinghai.musicplayer.runnable.FileTransThread;

public class Lyric implements Serializable {

	private static final long serialVersionUID = 20140723L;

	private static Logger logger = Logger.getLogger(Lyric.class.getName());
	private int width;// 显示宽度
	private int height;// 显示高度
	private long time;// 当前时间（毫秒为单位）
	private long tempTime;// 拖动到的临时时间
	public List<Sentence> list = new ArrayList<Sentence>();// 装载所有歌词（所有句子）

	private boolean isMoving;// 是否在拖动歌词
	private int currentIndex = 0;// 播放的歌词的下标
	private boolean isInitDone;
	private transient PlayListItem musicInfor;// 关于这首歌的信息

	private transient File file;// 歌词所在的文件
	private boolean enabled = true;// 是否使用（启用）了该对象，默认启用
	private long during = Integer.MAX_VALUE;// 歌曲的长度
	private int offset;// 整首歌的偏移量
	private String songName;
	// 用于缓存的一个正则表达式对象
	private static final Pattern pattern = Pattern
			.compile("(?<=\\[).*?(?=\\])");

	/**
	 * 用ID3V1标签的字节和歌名来初始化歌词 歌词将自动在本地或者网络上搜索相关的歌词并建立关联
	 * 本地搜索将硬编码为user.home文件夹下面的Lyrics文件夹 以后改为可以手动设置.
	 * 
	 * @param songName
	 *            歌名
	 * @param data
	 *            ID3V1的数据
	 */
	public Lyric(final PlayListItem musicInfor) {
		this.offset = musicInfor.getOffset();
		this.musicInfor = musicInfor;
		this.file = musicInfor.getLyricFile();
		logger.info("传进来的歌名是:" + musicInfor.toString());
		// 只要有关联好了的，就不用搜索了直接用就是了
		if (file != null && file.exists()) {
			logger.log(Level.INFO, "直接关联到的歌词：" + file);
			init(file);
			isInitDone = true;
			return;
		} else {
			// 否则就起一个线程去找了，先是本地找，然后再是网络上找
			new Thread(){
				@Override
				public void run() {
					doInit(musicInfor);
					isInitDone = true;
				}
			}.start();
		}
	}
	
	/**
	 * 读取某个指定的歌词文件,这个构造函数一般用于 拖放歌词文件到歌词窗口时调用的,拖放以后,两个自动关联
	 * 
	 * @param file
	 *            歌词文件
	 * @param info
	 *            歌曲信息
	 */
	public Lyric(File file, PlayListItem musicInfor) {
		System.out.println(" Lyric file" + file);
		this.offset = musicInfor.getOffset();
		this.file = file;
		this.musicInfor = musicInfor;
		init(file);
		isInitDone = true;
	}

	/**
	 * 根据歌词内容和播放项构造一个 歌词对象
	 * 
	 * @param lyric
	 *            歌词内容
	 * @param info
	 *            播放项
	 */
	public Lyric(String lyric, PlayListItem musicInfo) {
		this.offset = musicInfo.getOffset();
		this.musicInfor = musicInfo;
		init(lyric);
		isInitDone = true;
	}

	private void doInit(PlayListItem musicInfo) {
		init(musicInfo);

		Sentence temp = null;
		// 这个时候就要去网络上找了
		if (list.size() == 1) {
			temp = list.remove(0);
			String lyric = "";
			if (lyric != null) {
				init(lyric);
				// saveLyric(lyric, info);
			} else {// 如果网络也没有找到,就要加回去了
				Map<String, String> params = new HashMap<String, String>();
				songName = "我们都一样";
				params.put("musicName", songName);
				new FileTransThread(params).start();
			}
			list.add(temp);
		}
	}


	/**
	 * 把下载到的歌词保存起来,免得下次再去找
	 * 
	 * @param lyric
	 *            歌词内容
	 * @param info
	 *            歌的信息
	 */
	private void saveLyric(String lyric, PlayListItem info) {
		try {
			// 如果歌手不为空,则以歌手名+歌曲名为最好组合
			String name = info.getFormattedName() + ".lrc";
			File dir = new File(Constant.HOME, File.separator);
			// File dir = Config.getConfig().getSaveLyricDir();
			dir.mkdirs();
			file = new File(dir, name);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), "utf-8"));
			bw.write(lyric);
			bw.close();
			info.setLyricFile(file);
			logger.info("保存完毕,保存在:" + file);
		} catch (Exception exe) {
			logger.log(Level.SEVERE, "保存歌词出错", exe);
		}
	}

	/**
	 * 设置此歌词是否起用了,否则就不动了
	 * 
	 * @param b
	 *            是否起用
	 */
	public void setEnabled(boolean b) {
		this.enabled = b;
	}

	/**
	 * 得到此歌词保存的地方
	 * 
	 * @return 文件
	 */
	public File getLyricFile() {
		return file;
	}

	/**
	 * 调整整体的时间,比如歌词统一快多少 或者歌词统一慢多少,为正说明要快,为负说明要慢
	 * 
	 * @param time
	 *            要调的时间,单位是毫秒
	 */
	public void adjustTime(int time) {
		// 如果是只有一个显示的,那就说明没有什么效对的意义了,直接返回
		if (list.size() == 1) {
			return;
		}
		offset += time;
		musicInfor.setOffset(offset);
	}

	/**
	 * 根据一个文件夹,和一个歌曲的信息 从本地搜到最匹配的歌词
	 * 
	 * @param dir
	 *            目录
	 * @param info
	 *            歌曲信息
	 * @return 歌词文件
	 */
	private File getMathedLyricFile(File dir, PlayListItem info) {
		File matched = null;// 已经匹配的文件
		File[] fs = dir.listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(".lrc");
			}
		});
		for (File f : fs) {
			// 全部匹配或者部分匹配都行
			if (matchAll(info, f) || matchSongName(info, f)) {
				matched = f;
				break;
			}
		}
		return matched;
	}


	/**
	 * 根据歌的信息去初始化,这个时候 可能在本地找到歌词文件,也可能要去网络上搜索了
	 * 
	 * @param info
	 *            歌曲信息
	 */
	private void init(PlayListItem info) {
		File matched = null;
		// 得到歌曲信息后,先搜索HOME文件夹
		// 如果还不存在的话,那建一个目录,然后直接退出不管了

		File dir = new File(Constant.HOME, File.separator);
		if (!dir.exists()) {
			dir.mkdirs();
			// }
			matched = getMathedLyricFile(dir, info);
		}
		logger.info("找到的是:" + matched);
		if (matched != null && matched.exists()) {
			info.setLyricFile(matched);
			file = matched;
			init(matched);
		} else {
			init("");
		}
	}
	
	private void init(File file) {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), "utf-8"));
			StringBuilder stringBuilder = new StringBuilder();
			String temp = null;
			while ((temp = bufferedReader.readLine()) != null) {
				stringBuilder.append(temp).append("\n");
			}
			init(stringBuilder.toString());
		} catch (Exception e) {
			Logger.getLogger(Lyric.class.getName()).log(Level.SEVERE, null, e);
		} finally {
			try {
				bufferedReader.close();
			} catch (Exception ex) {
				Logger.getLogger(Lyric.class.getName()).log(Level.SEVERE, null,
						ex);
			}
		}
	}
	
	/**
	 * 是否完全匹配,完全匹配是指直接对应到ID3V1的标签, 如果一样,则完全匹配了,完全匹配的LRC的文件格式是: 阿木 - 有一种爱叫放手.lrc
	 * 
	 * @param info
	 *            歌曲信息
	 * @param file
	 *            侯选文件
	 * @return 是否合格
	 */
	private boolean matchAll(PlayListItem info, File file) {
		String name = info.getFormattedName();
		String fn = file.getName()
				.substring(0, file.getName().lastIndexOf("."));
		if (name.equals(fn)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 是否匹配了歌曲名
	 * 
	 * @param info
	 *            歌曲信息
	 * @param file
	 *            侯选文件
	 * @return 是否合格
	 */
	private boolean matchSongName(PlayListItem info, File file) {
		String name = info.getFormattedName();
		String rn = file.getName()
				.substring(0, file.getName().lastIndexOf("."));
		if (name.equalsIgnoreCase(rn) || info.getTitle().equalsIgnoreCase(rn)) {
			return true;
		} else {
			return false;
		}
	}


	/**
	 * 最重要的一个方法，它根据读到的歌词内容 进行初始化，比如把歌词一句一句分开并计算好时间
	 * 
	 * @param content
	 *            歌词内容
	 */
	private void init(String content) {
		// 如果歌词的内容为空,则后面就不用执行了
		// 直接显示歌曲名就可以了
		if (content == null || content.trim().equals("")) {
			list.add(new Sentence(musicInfor.getFormattedName(),
					Integer.MAX_VALUE, Integer.MAX_VALUE));
			return;
		}
		try {
			BufferedReader bufferedReader = new BufferedReader(
					new StringReader(content));
			String temp = null;
			while ((temp = bufferedReader.readLine()) != null) {
				parseLine(temp.trim());
			}
			bufferedReader.close();
			// 读进来以后就排序了
			Collections.sort(list,new Comparator<Sentence>(){

				@Override
				public int compare(Sentence object1, Sentence object2) {
					return (int)(object1.getStartTime() - object2.getStartTime());
				}
				
			});
			
			// 处理第一句歌词的起始情况,无论怎么样,加上歌名做为第一句歌词,并把它的
			// 结尾为真正第一句歌词的开始
			if (list.size() == 0) {
				list.add(new Sentence(musicInfor.getFormattedName(), 0, Integer.MAX_VALUE));
				return;
			} else {
				Sentence first = list.get(0);
				list.add(0, new Sentence(musicInfor.getFormattedName(),0,first.getStartTime()));
			}
			int size = list.size();
			for (int i = 0; i < size; i++) {
				Sentence next = null;
				if (i + 1 < size) {
					next = list.get(i + 1);
				}
				Sentence now = list.get(i);
				if (next != null) {
					now.setEndTime(next.getStartTime() - 1);
				}
			}
			// 如果就是没有怎么办,那就只显示一句歌名了
			if (list.size() == 1) {
				list.get(0).setEndTime(Integer.MAX_VALUE);
			} else {
				Sentence last = list.get(list.size() - 1);
				last.setEndTime(musicInfor == null ? Integer.MAX_VALUE : musicInfor.getLength() * 1000 + 1000);
			}
		} catch (Exception e) {
			Logger.getLogger(Lyric.class.getName()).log(Level.SEVERE, null, e);
		}
	}
	
	
	/**
	 * 分析出整体的偏移量
	 * 
	 * @param str
	 *            包含内容的字符串
	 * @return 偏移量，当分析不出来，则返回最大的正数
	 */
	private int parseOffset(String str) {
		String[] ss = str.split("\\:");
		if (ss.length == 2) {
			if (ss[0].equalsIgnoreCase("offset")) {
				int os = Integer.parseInt(ss[1]);
				System.err.println("整体的偏移量：" + os);
				return os;
			} else {
				return Integer.MAX_VALUE;
			}
		} else {
			return Integer.MAX_VALUE;
		}
	}
	
	

	/**
	 * 分析这一行的内容，根据这内容 以及标签的数量生成若干个Sentence对象 当此行中的时间标签分布不在一起时，也要能分析出来 所以更改了一些实现
	 * 20080824更新
	 * 
	 * @param line
	 *            这一行
	 */
	private void parseLine(String line) {
		if ("".equals(line)) {
			return;
		}
		Matcher matcher = pattern.matcher(line);
		List<String> temp = new ArrayList<String>();
		int lastIndex = -1;// 最后一个时间标签
		int lastLength = -1;// 最后一句时间标签的长度
		while (matcher.find()) {
			String string = matcher.group();
			int index = line.indexOf("[" + string + "]");
			if (lastIndex != -1 && index - lastIndex > lastLength + 2) {
				String content = line.substring(lastIndex + lastLength + 2,
						index);
				for (String str : temp) {
					long time = parseTime(str);
					if (time != -1) {
						System.out.println("content" + content);
						System.out.println("time = " + time);
						list.add(new Sentence(content, time));
					}
				}
				temp.clear();
			}
			temp.add(string);
			lastIndex = index;
			lastLength = string.length();
		}
		// 如果列表为空，则表示本行没有分析出任何标签
		if (temp.isEmpty()) {
			return;
		}
		try {
			int length = lastLength + 2 + lastIndex;
			String content = line.substring(length > line.length() ? line.length() : length);
			// if (Config.getConfig().isCutBlankChars()) {
			// content = content.trim();
			// }
			// 当已经有了偏移量的时候，就不再分析了
			if ("".equals(content) && offset == 0) {
				for (String string : temp) {
					int of = parseOffset(string);
					if (of != Integer.MAX_VALUE) {
						offset = of;
						musicInfor.setOffset(offset);
						break;
					}
				}
				return;
			}
			for (String string : temp) {
				long time = parseTime(string);
				if (time != -1) {
					list.add(new Sentence(content, time));
					System.out.println("content" + content);
					System.out.println("time = " + time);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 把如00:00.00这样的字符串转化成 毫秒数的时间，比如 01:10.34就是一分钟加上10秒再加上340毫秒 也就是返回70340毫秒
	 * 
	 * @param time
	 *            字符串的时间
	 * @return 此时间表示的毫秒
	 */
	private long parseTime(String time) {
		String[] ss = time.split("\\:|\\.");
		// 如果 是两位以后，就非法了
		if (ss.length < 2) {
			return -1;
		} else if (ss.length == 2) {
			try {
				// 先看有没有一个是记录了整体偏移量的
				if (offset == 0 && ss[0].equalsIgnoreCase("offset")) {
					offset = Integer.parseInt(ss[1]);
					musicInfor.setOffset(offset);
					System.err.println("整体的偏移量：" + offset);
					return -1;
				}
				int min = Integer.parseInt(ss[0]);
				int sec = Integer.parseInt(ss[1]);
				if (min < 0 || sec < 0 || sec >= 60) {
					throw new RuntimeException("数字不合法");
				}
				return (min * 60 + sec) * 1000L;
			} catch (Exception e) {
				e.printStackTrace();
				return -1;
			}
		} else if (ss.length == 3) {// 如果正好三位，就算分秒，十毫秒
			try {
				int min = Integer.parseInt(ss[0]);
				int sec = Integer.parseInt(ss[1]);
				int mm = Integer.parseInt(ss[2]);
				if (min < 0 || sec < 0 || sec >= 60 || mm < 0 || mm > 99) {
					throw new RuntimeException("数字不合法");
				}
				return (min * 60 + sec) * 1000L + mm * 10;
			} catch (Exception e) {
				return -1;
			}
		} else {
			return -1;
		}
	}

	
	/**
	 * 设置其显示区域的高度
	 * 
	 * @param height
	 *            高度
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * 设置其显示区域的宽度
	 * 
	 * @param width
	 *            宽度
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * 设置时间,设置的时候，要把整体的偏移时间算上
	 * 
	 * @param time
	 *            时间
	 */
	public void setTime(long time) {
		if (!isMoving) {
			tempTime = this.time = time + offset;
		}
	}

	/**
	 * 得到是否初始化完成了
	 * 
	 * @return 是否完成
	 */
	public boolean isInitDone() {
		return isInitDone;
	}

	/**
	 * 得到当前正在播放的那一句的下标 不可能找不到，因为最开头要加一句 自己的句子 ，所以加了以后就不可能找不到了
	 * 
	 * @return 下标
	 */
	public int getNowSentenceIndex(long time){
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isInTime(time)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * 是否能拖动,只有有歌词才可以被拖动,否则没有意义了
	 * 
	 * @return 能否拖动
	 */
	public boolean canMove(){
		return list.size() > 1 && enabled;
	}
	
	/**
	 * 得到当前的时间,一般是由显示面板调用的
	 */
	public long getTime() {
		return tempTime;
	}
	
	/**
	 * 在对tempTime做了改变之后,检查一下它的 值,看是不是在有效的范围之内
	 */
	private void checkTempTime(){
		if (tempTime < 0) {
			tempTime = 0;
		} else if (tempTime > during) {
			tempTime = during;
		}
	}
	
	/**
	 * 告诉歌词,要开始移动了, 在此期间,所有对歌词的直接的时间设置都不理会
	 */
	public void startMove(){
		isMoving = true;
	}
	
	/**
	 * 告诉歌词拖动完了,这个时候的时间改 变要理会,并做更改
	 */
	public void stopMove(){
		isMoving = false;
	}
}
