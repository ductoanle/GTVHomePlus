package com.ep.gtvhomeplus.adapter;

import java.io.File;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;

import com.ep.gtvhomeplus.PhotoGalleryActivity;
import com.ep.gtvhomeplus.R;
import com.ep.gtvhomeplus.utils.GalleryThumbnailLoader;

public class GalleryImageAdapter extends BaseAdapter{

	Context context;
	File[] imageList;
	int width,height;
	LayoutInflater inflater;
	int resource;
    GalleryThumbnailLoader mImageLoader;
	public GalleryImageAdapter(Context context, File[] imageList, int width ,int height){
		this.context=context;
		this.imageList=imageList;
		this.inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.resource=R.layout.thumbnail_item;
		this.width=width;
		this.height=height;
		this.mImageLoader = new GalleryThumbnailLoader(context, width, height);
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