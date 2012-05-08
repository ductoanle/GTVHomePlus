package com.ep.gtvhomeplus.fragments;

import java.io.File;

import com.ep.gtvhomeplus.R;
import com.ep.gtvhomeplus.PlayMovieActivity;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class PlayMovieFragment extends Fragment{
	
	private static final String FOLDER_URL_VALUE = "The.Silence.Of.The.Lambs.mkv";
	private static final String FILENAME_VALUE = "/mnt/media/usb.BCFCAAF2FCAAA5DC/Movies/Actions";
	
	private static final String TAG = "PlayMovieFragment";
	@Override 
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		File file = Environment.getDataDirectory();
		Log.i(TAG, "" + file.getName());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		View v = inflater.inflate(R.layout.play_movie, container, false);
		Button play = (Button)v.findViewById(R.id.play_movie_btn);
		play.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				Intent intent = new Intent(getActivity(), PlayMovieActivity.class);		
				intent.putExtra(PlayMovieActivity.MEDIA_FILE, FOLDER_URL_VALUE);
				intent.putExtra(PlayMovieActivity.MEDIA_FOLDER, FILENAME_VALUE);
				getActivity().startActivity(intent);
			}
		});
		return v;
	}
}
