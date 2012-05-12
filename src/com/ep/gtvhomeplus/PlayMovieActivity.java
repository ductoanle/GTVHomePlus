package com.ep.gtvhomeplus;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONObject;

import com.ep.gtvhomeplus.utils.SrtManager;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.Message;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class PlayMovieActivity extends Activity {
	
	private static final String TAG = "PlayMovieActivity";
	
	public static final String MEDIA_FILE = "media_file";
	public static final String MEDIA_FOLDER = "media_folder";
	
	private String mMediaFileUrl;
	private String mMediaFolderUrl;
	private File mMediaFolder;
	private ArrayList<File> mSubtitles;
	private SrtManager mCurrentSubtitle;
	
	private MediaController mMediaController;
	private MediaPlayerListener mMediaPlayerListener;
	private VideoView mVideoView;
	private TextView mSubtitleView;
	private final SubtitleUpdateHandler subUpdate = new SubtitleUpdateHandler();
	
	private int mCurrentTime;
	
	@Override
	public void onCreate(Bundle savedInstanceStates){
		super.onCreate(savedInstanceStates);
		loadMediaInfo();
		loadSubtitles();
		setContentView(R.layout.play_media);
		initializeVideoView();
		mSubtitleView = (TextView)findViewById(R.id.tv_subtitle);
	}
	
	private void loadMediaInfo(){
		Intent intent = getIntent();
		Bundle bundle  = intent.getExtras();
		mMediaFileUrl = bundle.getString(MEDIA_FILE);
		mMediaFolderUrl = bundle.getString(MEDIA_FOLDER);
	}
	
	private void loadSubtitles(){
		mMediaFolder = new File(mMediaFolderUrl);
		if (!mMediaFolder.isDirectory()){
			return;
		}
		
		class SrtFilter implements FilenameFilter{
			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".srt");
			}
		}
		mSubtitles = new ArrayList<File>(Arrays.asList(mMediaFolder.listFiles(new SrtFilter())));
	}
	
	private void initializeVideoView(){
		Log.i(TAG, mMediaFileUrl); 
		mMediaController = new MediaController(this);
		mMediaPlayerListener = new MediaPlayerListener();
		mVideoView = (VideoView)findViewById(R.id.media_view);
		mVideoView.setVideoURI(Uri.parse(mMediaFileUrl));
		mVideoView.setMediaController(mMediaController);
		mVideoView.setOnPreparedListener(mMediaPlayerListener);
		mVideoView.setOnCompletionListener(mMediaPlayerListener);
		mVideoView.setOnErrorListener(mMediaPlayerListener);
		mVideoView.requestFocus();
	}
	
	private void prepareSubtitles(){
		String subName;
		String mediaFileName = new File(mMediaFileUrl).getName();
		if (mSubtitles.size() > 0){
			mCurrentSubtitle = new SrtManager(mSubtitles.get(0).getAbsolutePath());
			for (int i = 1; i < mSubtitles.size(); i++){
				subName = mSubtitles.get(i).getName();
				if (subName.substring(0, subName.length() - 4).equals(mediaFileName.substring(0, mediaFileName.length() - 4))){
					mCurrentSubtitle = new SrtManager(mSubtitles.get(i).getAbsolutePath());
					break;
				}
			}
		}
		
	}
	
	private void playVideo(){
		mVideoView.start();
		subUpdate.playSubtitles();
	}
	
	private class MediaPlayerListener
	implements OnPreparedListener, OnCompletionListener, OnErrorListener{
		@Override public void onPrepared(MediaPlayer player) {
			prepareSubtitles();
			playVideo();
		}

		@Override public void onCompletion(MediaPlayer player) {
			mVideoView.seekTo(0);
			PlayMovieActivity.this.finish();
		}
		
		@Override public boolean onError(MediaPlayer player, int what, int extra) {
//			mIsMediaPlayerErrorOccurred = true;
//			if(what==MediaPlayer.MEDIA_ERROR_SERVER_DIED)
//				mMediaPlayerErrorMessage = R.string.media_error_server_died;
//			else if(what==MediaPlayer.MEDIA_ERROR_UNKNOWN)
//				mMediaPlayerErrorMessage = R.string.media_error_unknown;
//			else if(what==MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK)
//				mMediaPlayerErrorMessage=R.string.media_error_progressive_playback;
			return true;
		}
	}
	
	/** Subtitle Update Handler **/
	private class SubtitleUpdateHandler extends Handler {
		private long mSubtitlePeriod = 20L;

		public void playSubtitles() {
			this.sendEmptyMessage(0);
		}

		public void pauseSubtitles() {
			this.removeMessages(0);
		}

		@Override
		public void handleMessage(Message msg) {
			this.sendEmptyMessageDelayed(0, mSubtitlePeriod);
			mCurrentTime = mVideoView.getCurrentPosition();

			if (mCurrentSubtitle!=null && !mCurrentSubtitle.isEmpty()) {
				//update subtitles portion if we do not have to show the ads
				if(mVideoView.isPlaying()) {
					String content = mCurrentSubtitle.getSubtitleByTime(mCurrentTime);
					mSubtitleView.setText(Html.fromHtml(content));
				}
			}
		}
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		subUpdate.pauseSubtitles();
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		subUpdate.playSubtitles();
	}
}
