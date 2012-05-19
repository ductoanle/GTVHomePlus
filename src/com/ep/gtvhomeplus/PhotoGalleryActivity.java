package com.ep.gtvhomeplus;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.ep.gtvhomeplus.file.utils.ImageUtils;

public class PhotoGalleryActivity extends Activity{
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
		int galleryHeight = mScreenHeight/5;

		
		int imageHeight = galleryHeight;
		int imageWidth = galleryWidth * 1 / 7;
		int spacing = imageWidth /10;
		
	    int offset = galleryWidth - imageWidth - 2 * spacing;
	   
        ImageAdapter imageAdapter=new ImageAdapter(this,mPhotoFiles);	    
		imageAdapter.setImageSize(imageWidth, imageHeight);
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
    	public ImageAdapter(Context context, File[] imageList){
    		this.context=context;
    		this.imageList=imageList;
    		inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		resource=R.layout.thumbnail_item;
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

		public void setImageSize(int mWidth,int mHeight){
			width=mWidth;
			height=mHeight;
		}
		
		@Override
		public View getView(int position, View converView, ViewGroup parent) {
			LinearLayout ll=new LinearLayout(context);
			inflater.inflate(resource, ll,true);
			ll.setLayoutParams(new Gallery.LayoutParams(width,height));
			ImageView iv=(ImageView)ll.findViewById(R.id.iv_item);
			iv.setImageBitmap(ImageUtils.decodeFile(imageList[position], Math.max(width, height)));
			iv.setScaleType(ScaleType.CENTER_INSIDE);			
			return ll;
		}    	
    }
}
