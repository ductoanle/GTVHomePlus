package com.ep.gtvhomeplus;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.ep.gtvhomeplus.utils.ImageLoader;

public class PhotoGalleryActivity extends Activity{
	private static final String TAG= "PhotoGalleryActivity";
	
	private static double GALLERY_VS_SCREEN_HEIGHT_RATIO =1.0/5.0;
	private static double THUMBNAIL_VS_GALLERY_WIDTH_RATION = 1.0/7.0;
	private static double SPACING_VS_THUMBNAIL_WIDTH_RATIO =1.0/10.0;
	
	////views
    ImageView mFocusedImage;
	Gallery mBottomGallery;
	View mParentView;
	
	Uri mInitialPhotoUri;
	File[] mPhotoFiles;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.photo_gallery);
		mFocusedImage =(ImageView) findViewById(R.id.iv_focused);
		mBottomGallery= (Gallery) findViewById(R.id.gallery_bottom);
		mParentView = findViewById(R.id.main_content);
		
		mInitialPhotoUri = getIntent().getData();
		File photoFile= new File(mInitialPhotoUri.getPath());
		File directory = photoFile.getParentFile();
    	mPhotoFiles = directory.listFiles();
    	
    	setBottomGallery();
    	
	}
    
	private void setBottomGallery(){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int mScreenWidth = metrics.widthPixels;
    	int mScreenHeight = metrics.heightPixels;
    
        int galleryWidth = mScreenWidth;
		int galleryHeight = (int) (mScreenHeight *GALLERY_VS_SCREEN_HEIGHT_RATIO);
        Log.d(TAG,"gallery Height: "+galleryHeight);
		
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

	}
	
	private class ImageAdapter extends BaseAdapter{

    	Context context;
    	File[] imageList;
    	int width,height;
    	LayoutInflater inflater;
    	int resource;
        ImageLoader mImageLoader;
    	public ImageAdapter(Context context, File[] imageList, int width ,int height){
    		this.context=context;
    		this.imageList=imageList;
    		this.inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		this.resource=R.layout.thumbnail_item;
    		this.width=width;
    		this.height=height;
    		this.mImageLoader = new ImageLoader(PhotoGalleryActivity.this, width, height);
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
