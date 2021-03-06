package com.ep.gtvhomeplus;

import java.io.File;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.ep.gtvhomeplus.adapter.GalleryImageAdapter;

public class PhotoGalleryActivity extends Activity implements OnItemSelectedListener{
	private static final String TAG= "PhotoGalleryActivity";
	
	private static double GALLERY_VS_SCREEN_HEIGHT_RATIO =1.0/5.0;
	private static double THUMBNAIL_VS_GALLERY_WIDTH_RATION = 1.0/7.0;
	private static double SPACING_VS_THUMBNAIL_WIDTH_RATIO =1.0/10.0;
	
	private static long SHOW_HIDE_ANIMATION_DURATION =100;
	////views
    ImageView mFocusedImage;
	Gallery mBottomGallery;
	View mParentView;
	ProgressBar mSpinner;
	
	
	boolean mBottomGalleryShow=false;
	
	FocusedImageUpdater mImageUpdaterThread;
	Handler mHandler;
	Uri mInitialPhotoUri;
	File[] mPhotoFiles;
	String mSelectedFilePath;
	
	ObjectAnimator mHideAnimator, mShowAnimator;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.photo_gallery);
		mHandler =new Handler();
		
		
		mFocusedImage =(ImageView) findViewById(R.id.iv_focused);
		mBottomGallery= (Gallery) findViewById(R.id.gallery_bottom);
		mParentView = findViewById(R.id.main_content);
		mSpinner = (ProgressBar) findViewById(R.id.progress_bar);
	    
		getPhotoFiles();
    	initializeBottomGallery();
    	mBottomGallery.setOnItemSelectedListener(this);
    	
    	//hide the bottom gallery at the start of the activity
    	//this animation should happen in a short time
    	mHideAnimator.setDuration(20).start();
    	//reset duration
    	mHideAnimator.setDuration(SHOW_HIDE_ANIMATION_DURATION);
	}
		
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		mSelectedFilePath=mPhotoFiles[position].getAbsolutePath();
		if (mImageUpdaterThread !=null && mImageUpdaterThread.isAlive())
			mImageUpdaterThread.cancel();		
		else{
		mImageUpdaterThread= new FocusedImageUpdater(mSelectedFilePath);
        mImageUpdaterThread.start();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		
	}
		
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode){
		case KeyEvent.KEYCODE_DPAD_UP:
			toggleBottomGallery();
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			toggleBottomGallery();
			break;
		default:
			return super.onKeyUp(keyCode, event);
		}
		return true;
	}
	/**
	 * get all the files in the current directory
	 * the file at index 0 is the file user selected
	 */
	private void getPhotoFiles(){
		mInitialPhotoUri = getIntent().getData();
		File photoFile= new File(mInitialPhotoUri.getPath());
		File directory = photoFile.getParentFile();
    	mPhotoFiles = directory.listFiles();
        int indexOfSelectedFile =0;
        while(indexOfSelectedFile< mPhotoFiles.length && 
        		!mPhotoFiles[indexOfSelectedFile].getAbsolutePath().equals(photoFile.getAbsolutePath())){
        	indexOfSelectedFile ++;
        }
        if(indexOfSelectedFile<mPhotoFiles.length){
        	File tempFile = mPhotoFiles[indexOfSelectedFile];
        	mPhotoFiles[indexOfSelectedFile]=mPhotoFiles[0];
        	mPhotoFiles[0]=tempFile;
        }
        	
	}
    
	private void initializeBottomGallery(){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int mScreenWidth = metrics.widthPixels;
    	int mScreenHeight = metrics.heightPixels;
    
        int galleryWidth = mScreenWidth;
		int galleryHeight = (int) (mScreenHeight *GALLERY_VS_SCREEN_HEIGHT_RATIO);
		
		int imageHeight = galleryHeight;
		int imageWidth = (int) (galleryWidth * THUMBNAIL_VS_GALLERY_WIDTH_RATION);
		int spacing = (int) (imageWidth * SPACING_VS_THUMBNAIL_WIDTH_RATIO);		
	    int offset = galleryWidth - imageWidth - 2 * spacing;
	   
        GalleryImageAdapter imageAdapter=new GalleryImageAdapter(this,mPhotoFiles,imageWidth,imageHeight);	    
		mBottomGallery.setSpacing(spacing);
        mBottomGallery.setAdapter(imageAdapter);
        
    	// set gallery to left side
        MarginLayoutParams mlp = (MarginLayoutParams) mBottomGallery.getLayoutParams();
        mlp.setMargins(-offset, mlp.topMargin,
                mlp.rightMargin, mlp.bottomMargin);
        mHideAnimator = ObjectAnimator.ofFloat(mBottomGallery, "translationY", 0, (float)galleryHeight* 1.2f);
        mShowAnimator = ObjectAnimator.ofFloat(mBottomGallery, "translationY", (float)galleryHeight *1.2f, 0);
        mHideAnimator.setDuration(SHOW_HIDE_ANIMATION_DURATION);
        mShowAnimator.setDuration(SHOW_HIDE_ANIMATION_DURATION);
	}
	
    private void toggleBottomGallery(){
    	if(mBottomGalleryShow){
    		mHideAnimator.start();
    		mBottomGalleryShow=false;
    	}
    	else{
    		mShowAnimator.start();
    		mBottomGalleryShow=true;    		
    	}
    }

	private class FocusedImageUpdater extends Thread{
		String filePath;
		boolean cancel=false;
		
		FocusedImageUpdater(String filePath){
			this.filePath=filePath;
		}
		public void cancel(){
			this.cancel=true;
		}
		
		@Override
		public void run(){
			mHandler.post(new Runnable() {					
				@Override
				public void run() {
					mSpinner.setVisibility(View.VISIBLE);
				}
			});

			final Bitmap bitmap = BitmapFactory.decodeFile(filePath);
			if(bitmap!=null && !cancel)
				mHandler.post(new Runnable() {						
					@Override
					public void run() {
						mSpinner.setVisibility(View.GONE);
						mFocusedImage.setImageBitmap(bitmap);
					}
				});
			
			if (!filePath.equals(mSelectedFilePath)){
				mImageUpdaterThread = new FocusedImageUpdater(mSelectedFilePath);
				mImageUpdaterThread.start();
			}
			
		}
	}
	
}
