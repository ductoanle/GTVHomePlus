package com.ep.gtvhomeplus;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

public class PlayMovieActivity extends Activity {
	
	public static final String MEDIA_FILE = "media_file";
	public static final String MEDIA_FOLDER = "media_folder";
	
	private String mMediaFileUrl;
	private String mMediaFolderUrl;
	private File mMediaFolder;
	private ArrayList<File> mSubtitles;
	
	private MediaController mMediaController;
	private MediaPlayerListener mMediaPlayerListener;
	private VideoView mVideoView;
	
	@Override
	public void onCreate(Bundle savedInstanceStates){
		super.onCreate(savedInstanceStates);
		loadMediaInfo();
		loadSubtitles();
		setContentView(R.layout.play_media);
		initializeVideoView();
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
		mMediaController = new MediaController(this);
		mMediaPlayerListener = new MediaPlayerListener();
		mVideoView = (VideoView)findViewById(R.id.media_view);
		mVideoView.setVideoURI(Uri.parse(mMediaFolderUrl + "/" + mMediaFileUrl));
		mVideoView.setMediaController(mMediaController);
		mVideoView.setOnPreparedListener(mMediaPlayerListener);
		mVideoView.setOnCompletionListener(mMediaPlayerListener);
		mVideoView.setOnErrorListener(mMediaPlayerListener);
		mVideoView.requestFocus();
		
	}
	
	private void prepareSubtitles(){
		
	}
	
	private void playVideo(){
		mVideoView.start();
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
}
