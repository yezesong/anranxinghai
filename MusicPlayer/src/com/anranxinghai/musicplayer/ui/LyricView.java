package com.anranxinghai.musicplayer.ui;

import java.text.AttributedCharacterIterator.Attribute;
import java.util.List;

import com.anranxinghai.musicplayer.serializable.Lyric;
import com.anranxinghai.musicplayer.serializable.Sentence;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class LyricView extends TextView {


	private Paint notCurrentPaint;//非当前歌词画笔
	private Paint currentPaint;//当前歌词画笔
	
	private int notCurrentPaintColor = Color.WHITE;
	private int currentPaintColor = Color.RED;
	
	private Typeface notCurrentTypeface = Typeface.SERIF;//字体
	private Typeface currentTypeface = Typeface.SERIF;//字体
	
	private float width;
	private static Lyric lyric;
	private int backgroundColor = 0xffffffff;
	private float lrcTextSize = 22;//歌词大小
	private float currentTextSize = 24;
	// private Align = Paint.Align.CENTER；
	
	public float mTouchHistoryY;
	
	public int height;
	private long currentDuringTime;// 当前行歌词持续的时间，用该时间来sleep
	// private float middleY;// y轴中间
	private int textHeight = 50; // 每一行的间隔
	private boolean lrcInitDone = false; // 是否初始化完毕了
	public int index = 0;
	private int lastIndex = 0;
	private List<Sentence> sentences;//存放一首歌的所有歌词
	
	private long currentTime;
	private long sentenceTime;
	
	
	public Paint getNotCurrentPaint() {
		return notCurrentPaint;
	}
	public void setNotCurrentPaint(Paint notCurrentPaint) {
		this.notCurrentPaint = notCurrentPaint;
	}
	public Paint getCurrentPaint() {
		return currentPaint;
	}
	public void setCurrentPaint(Paint currentPaint) {
		this.currentPaint = currentPaint;
	}
	public int getNotCurrentPaintColor() {
		return notCurrentPaintColor;
	}
	public void setNotCurrentPaintColor(int notCurrentPaintColor) {
		this.notCurrentPaintColor = notCurrentPaintColor;
	}
	public int getCurrentPaintColor() {
		return currentPaintColor;
	}
	public void setCurrentPaintColor(int currentPaintColor) {
		this.currentPaintColor = currentPaintColor;
	}
	public Typeface getNotCurrentTypeface() {
		return notCurrentTypeface;
	}
	public void setNotCurrentTypeface(Typeface notCurrentTypeface) {
		this.notCurrentTypeface = notCurrentTypeface;
	}
	public Typeface getCurrentTypeface() {
		return currentTypeface;
	}
	public void setCurrentTypeface(Typeface currentTypeface) {
		this.currentTypeface = currentTypeface;
	}
	public float getCurrentTextSize() {
		return currentTextSize;
	}
	public void setCurrentTextSize(float currentTextSize) {
		this.currentTextSize = currentTextSize;
	}
	public long getCurrentDuringTime() {
		return currentDuringTime;
	}
	public void setCurrentDuringTime(long currentDuringTime) {
		this.currentDuringTime = currentDuringTime;
	}
	public long getCurrentTime() {
		return currentTime;
	}
	public void setCurrentTime(long currentTime) {
		this.currentTime = currentTime;
	}
	public int getBackgroundColor() {
		return backgroundColor;
	}
	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
	public int getTextHeight() {
		return textHeight;
	}
	public void setTextHeight(int textHeight) {
		this.textHeight = textHeight;
	}
	public List<Sentence> getSentences() {
		return sentences;
	}
	public void setSentences(List<Sentence> sentences) {
		this.sentences = sentences;
	}
	public float getLrcTextSize() {
		return lrcTextSize;
	}
	public void setLrcTextSize(float lrcTextSize) {
		this.lrcTextSize = lrcTextSize;
	}
	public boolean isLrcInitDone() {
		return lrcInitDone;
	}
	public void setLrcInitDone(boolean lrcInitDone) {
		this.lrcInitDone = lrcInitDone;
	}
	
		
	public static Lyric getLyric() {
		return lyric;
	}
	public void setLyric(Lyric lyric) {
		LyricView.lyric = lyric;
	}
	public LyricView(Context context) {
		super(context);
		init();
	}
	
	public LyricView(Context context, AttributeSet attributeSet){
		super(context,attributeSet);
		init();
	}
	
	public LyricView(Context context, AttributeSet attributeSet, int i){
		super(context,attributeSet,i);
		init();
	}
	
	
	
	private void init() {
		setFocusable(true);
		//非高亮部分
		notCurrentPaint = new Paint();
		notCurrentPaint.setAntiAlias(true);
		
		notCurrentPaint.setTextAlign(Paint.Align.CENTER);
		
		//高亮部分 当前正在播放的歌词
		currentPaint = new Paint();
		currentPaint.setAntiAlias(true);
		currentPaint.setTextAlign(Paint.Align.CENTER);
		//list = mLyric.list;
	}
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(backgroundColor);
		notCurrentPaint.setColor(notCurrentPaintColor);
		currentPaint.setColor(currentPaintColor);
		
		notCurrentPaint.setTextSize(lrcTextSize);
		//notCurrentPaint.setColor(notCurrentPaintColor);
		notCurrentPaint.setTypeface(notCurrentTypeface);
		
		currentPaint.setTextSize(lrcTextSize);
		//currentPaint.setColor(currentPaintColor);
		currentPaint.setTypeface(currentTypeface);
		
		// // 首先看是不是初始化完毕了
				// if (!Lyric.initDone) {
				// Sentence temp = new Sentence("Search Lyric...");
				// canvas.drawText(temp.getContent(), width / 2, height / 2,
				// CurrentPaint);
				// return;
				// }
		if (index == -1) {
			return;
		}
		float plus = currentDuringTime == 0 ? 30 : 
			30 + (((float) currentTime - (float) sentenceTime) / (float) currentDuringTime) * (float)30;
		// 向上滚动 这个是根据歌词的时间长短来滚动，整体上移
		canvas.translate(0, -plus);
		// 先画当前行，之后再画他的前面和后面，这样就保持当前行在中间的位置
		try {
			canvas.drawText(sentences.get(index).getContent(), width / 2, height / 2, currentPaint);
			//canvas.translate(0, plus);
			float tempY = height /2;
			// 画出本句之前的句子
			for (int i = index - 1; i >= 0; i--) {
				// Sentence sen = list.get(i);
				// 向上推移
				tempY -= textHeight;
				if (tempY < 0) {
					break;
				}
				canvas.drawText(sentences.get(i).getContent(), width / 2, tempY, notCurrentPaint);
				// canvas.translate(0, TextHeight);
			}
			tempY = height / 2;
			// 画出本句之后的句子
			for (int i = index + 1; i < sentences.size(); i++) {
				tempY += textHeight;
				if (tempY > height) {
					return;
				}
				canvas.drawText(sentences.get(i).getContent(), width/2, tempY, notCurrentPaint);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(w, h, oldw, oldh);
		width = w;
		height = h;
	}
	
	//
	/**
	 * @param time
	 *            当前歌词的时间轴
	 * 
	 * @return null
	 */
	public void updateIndex(long time){
		this.currentTime = time;
		//歌词序号
		index = lyric.getNowSentenceIndex(time);
		if (index != -1) {
			Sentence sentence = sentences.get(index);
			sentenceTime = sentence.getStartTime();
			currentDuringTime = sentence.getDuring();
			
		}
	}
	

}