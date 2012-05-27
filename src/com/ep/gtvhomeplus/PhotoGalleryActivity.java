package com.ep.gtvhomeplus;

import java.io.File;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.ep.gtvhomeplus.utils.GalleryThumbnailLoader;

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
		
		mInitialPhotoUri = getIntent().getData();
		File photoFile= new File(mInitialPhotoUri.getPath());
		File directory = photoFile.getParentFile();
    	mPhotoFiles = directory.listFiles();
    	
    	initializeBottomGallery();
    	mBottomGallery.setOnItemSelectedListener(this);
    	
    	//hide the bottom gallery at the start of the activity
    	//this animation should happen in a short time
    	mHideAnimator.setDuration(20).start();
    	//reset duration
    	mHideAnimator.setDuration(SHOW_HIDE_ANIMATION_DURATION);
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
	   
        ImageAdapter imageAdapter=new ImageAdapter(this,mPhotoFiles,imageWidth,imageHeight);	    
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
	
	private class ImageAdapter extends BaseAdapter{

    	Context context;
    	File[] imageList;
    	int width,height;
    	LayoutInflater inflater;
    	int resource;
        GalleryThumbnailLoader mImageLoader;
    	public ImageAdapter(Context context, File[] imageList, int width ,int height){
    		this.context=context;
    		this.imageList=imageList;
    		this.inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		this.resource=R.layout.thumbnail_item;
    		this.width=width;
    		this.height=height;
    		this.mImageLoader = new GalleryThumbnailLoader(PhotoGalleryActivity.this, width, height);
    	}
    	
		@Override
		public int getCount() {
			return imageList.length;
		}

		@Override
		public Object getItem(int position) {
			return imageList[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View converView, ViewGroup parent) {
			LinearLayout ll=new LinearLayout(context);
			inflater.inflate(resource, ll,true);
			ImageView iv=(ImageView)ll.findViewById(R.id.iv_item);
			iv.setLayoutParams(new LinearLayout.LayoutParams(width,height));
			iv.setScaleType(ScaleType.FIT_XY);			
			mImageLoader.loadImage(imageList[position].getAbsolutePath(), iv);
			return ll;
		}    	
    }

}
