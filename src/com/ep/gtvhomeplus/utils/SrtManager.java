package com.ep.gtvhomeplus.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class SrtManager {
	
	private static final String TAG = "SrtManager";
	private static final String SRT_TIME_SEPARATOR = " --> ";
	
	private String mSrtUri = "";
	private ArrayList<Subtitle> mSubtitlesCache = new ArrayList<Subtitle>();
	private int mSubIndex;
	
	public SrtManager(String srtUri){
		this.mSrtUri = srtUri;
		this.mSubIndex = 0;
		generateSubtitleCache();
		Log.i(TAG, "Subtitle path " + srtUri);
	}
	
	public void reset(){
		this.mSubIndex = 0;
	}
	
	private void generateSubtitleCache(){
		try{
			FileInputStream fstream = new FileInputStream(mSrtUri);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
	
			Subtitle sub = null;
			while ((strLine = br.readLine()) != null) {
				Log.i(TAG, strLine);
			   String[] atoms = strLine.split(SRT_TIME_SEPARATOR);
			   if (atoms.length == 1){
				   // Either content text or empty line
				   String line = atoms[0];
				   if (line.trim().isEmpty() && sub != null) {
					   sub.print();
					   if (sub.isComplete()){
						   mSubtitlesCache.add(sub);
					   }
					   sub = null;
				   }
				   else if (sub != null){
					   if (sub.content == null){
						   sub.content = atoms[0];
					   }
					   else{
						   sub.content += "\n" + atoms[0];
					   }
				   }
			   }
			   else if (atoms.length == 2) {
				  sub = new Subtitle();
				  sub.startTime = handleTime(atoms[0]);
				  sub.endTime = handleTime(atoms[1]);
			   }
			}
		}
		catch (FileNotFoundException e){
			Log.e(TAG, "Subtitle file not exist", e);
		}
		catch (IOException e){
			Log.e(TAG, "Subtitle file read error", e);
		}
	}
	
	public String getSubtitleByTime(int time){
		try{
			Subtitle sub = mSubtitlesCache.get(mSubIndex);
			if (time >= sub.startTime && time < sub.endTime){
				return sub.content;
			}
			else if (time > sub.endTime && mSubIndex < mSubtitlesCache.size()){
				mSubIndex++;
			}
			else if (time < sub.startTime && mSubIndex > 0){
				mSubIndex--;
			}
		}
		catch (Exception e){
			Log.e(TAG, "Error getting subtitle from json",e );
		}
		return "";
	}
	
	private int handleTime(String time){
		final int HOUR = 60 * 60 * 1000;
		final int MIN = 60 * 1000;
		final int SEC = 1000;
		int timeInMilisecs = 0;
		String[] atoms = time.split(",");
		try{
			if (atoms.length != 2){
				return timeInMilisecs;
			}
			else{
				int milisec = Integer.parseInt(atoms[1]);
				atoms = atoms[0].split(":");
				if (atoms.length != 3){
					return timeInMilisecs;
				}
				else{
					timeInMilisecs += Integer.parseInt(atoms[0]) * HOUR + 
									  Integer.parseInt(atoms[1]) * MIN + 
									  Integer.parseInt(atoms[2]) * SEC + 
									  milisec;
				}
			}
		}
		catch(Exception e){
			Log.e(TAG, "Error parsing time", e);
		}
		return timeInMilisecs;
	}

	public String getSrtUri() {
		return mSrtUri;
	}

	public void setSrtUri(String srtUri) {
		this.mSrtUri = srtUri;
	}
	
	public boolean isEmpty(){
		return mSubtitlesCache.isEmpty();
	}

	private class Subtitle{
		public String content;
		public int startTime;
		public int endTime;
		
		public boolean isComplete(){
			return (content != null && startTime > 0 && endTime > 0 && startTime < endTime);
		}
		
		public void print(){
			Log.i(TAG, "Content: " + content + " - Start Time: " + startTime + " - End Time: " + endTime);
		}
	}
}
