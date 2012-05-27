package com.ep.gtvhomeplus.utils;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

public class GalleryThumbnailLoader {
	
	private static final String TAG = "ImageLoader";
	
	// Both hard and soft caches are purged after 40 seconds idling. 
	private static final int DELAY_BEFORE_PURGE = 40000;
	//TODO: fine tune MAX_CACHE_CAPACITY
	private static final int MAX_CACHE_CAPACITY = 10;
	
	// Maximum number of threads in the executor pool.
	private static final int POOL_SIZE = 5;
	
    private boolean cancel;
    private Context mContext;
   
    private int imageWidth = 64;
    private int imageHeight = 64;
    
    private Runnable purger;
    private Handler purgeHandler;
    private ExecutorService mExecutor;
    
    // Soft bitmap cache for thumbnails removed from the hard cache.
    // This gets cleared by the Garbage Collector everytime we get low on memory.
    private ConcurrentHashMap<String, SoftReference<Bitmap>> mSoftBitmapCache;
    private LinkedHashMap<String, Bitmap> mHardBitmapCache;
    
    /**
     * Used for loading and decoding thumbnails from files.
     * 
     * @author PhilipHayes
     * @param context Current application context.
     */
	public GalleryThumbnailLoader(Context context) {
		mContext = context;
		
		purger = new Runnable(){
			@Override
			public void run() {
				Log.d(TAG, "Purge Timer hit; Clearing Caches.");
				clearCaches();
			}
		};
		
		purgeHandler = new Handler();
		mExecutor = Executors.newFixedThreadPool(POOL_SIZE);
		
		mSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(MAX_CACHE_CAPACITY / 2);
		mHardBitmapCache = new LinkedHashMap<String, Bitmap>(MAX_CACHE_CAPACITY / 2, 0.75f, true){
			
			/***/
			private static final long serialVersionUID = 1347795807259717646L;
			
			@Override
			protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest){
				// Moves the last used item in the hard cache to the soft cache.
				if(size() > MAX_CACHE_CAPACITY){
					mSoftBitmapCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
					return true;
				} else {
					return false;
				}
			}
		};
	}
	
	public GalleryThumbnailLoader(Context context, int width, int height){
	    this(context);
		this.imageHeight=height;
		this.imageWidth=width;
	}

	
	/**
	 * @param filename.
	 */
	public void loadImage(String fileName, ImageView imageView) {
		if(!cancel){
			// We reset the caches after every 40 or so seconds of inactivity for memory efficiency.
			resetPurgeTimer();
			
			Bitmap bitmap = getBitmapFromCache(fileName);
			if(bitmap != null){
				imageView.setImageBitmap(bitmap);
			} else {
				if (!cancel) {
					// Submit the file for decoding.
					PhotoToLoad thumbnail = new PhotoToLoad(fileName, imageView);
					WeakReference<ThumbnailRunner> runner = new WeakReference<ThumbnailRunner>(new ThumbnailRunner(thumbnail));
					mExecutor.submit(runner.get());
				}
			}
		}
	}
	/**
	 * Cancels any downloads, shuts down the executor pool,
	 * and then purges the caches.
	 */
	public void cancel(){
		cancel = true;
		
		// We could also terminate it immediately,
		// but that may lead to synchronization issues.
		if(!mExecutor.isShutdown()){
			mExecutor.shutdown();
		}
		
		stopPurgeTimer();
		
		mContext = null;
		clearCaches();
	}
	
	/**
	 * Stops the cache purger from running until it is reset again.
	 */
	public void stopPurgeTimer(){
		purgeHandler.removeCallbacks(purger);
	}
	
	/**
	 * Purges the cache every (DELAY_BEFORE_PURGE) milliseconds.
	 * @see DELAY_BEFORE_PURGE
	 */
	private void resetPurgeTimer() {
		purgeHandler.removeCallbacks(purger);
		purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
	}
	
	private void clearCaches(){
		mSoftBitmapCache.clear();
		mHardBitmapCache.clear();
	}
	
	/**
	 * @param key In this case the file name (used as the mapping id).
	 * @return bitmap The cached bitmap or null if it could not be located.
	 * 
	 * As the name suggests, this method attemps to obtain a bitmap stored
	 * in one of the caches. First it checks the hard cache for the key.
	 * If a key is found, it moves the cached bitmap to the head of the cache
	 * so it gets moved to the soft cache last.
	 * 
	 * If the hard cache doesn't contain the bitmap, it checks the soft cache
	 * for the cached bitmap. If neither of the caches contain the bitmap, this
	 * returns null.
	 */
	private Bitmap getBitmapFromCache(String key){
		synchronized(mHardBitmapCache) {
			Bitmap bitmap = mHardBitmapCache.get(key);
			if(bitmap != null){
				// Put bitmap on top of cache so it's purged last.
				mHardBitmapCache.remove(key);
				mHardBitmapCache.put(key, bitmap);
				return bitmap;
			}
		}
		
		SoftReference<Bitmap> bitmapRef = mSoftBitmapCache.get(key);
		if(bitmapRef != null){
			Bitmap bitmap = bitmapRef.get();
			if(bitmap != null){
				return bitmap;
			} else {
				// Must have been collected by the Garbage Collector 
				// so we remove the bucket from the cache.
				mSoftBitmapCache.remove(key);
			}
		}
		
		// Could not locate the bitmap in any of the caches, so we return null.
		return null;
	}
	
	/**@param filePath
	 * @return The resized and resampled bitmap, if can not be decoded it returns null.
	 */
	private Bitmap decodeFile(String filePath) {
		if(!cancel){
			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				
				options.inJustDecodeBounds = true;
				options.outWidth = 0;
				options.outHeight = 0;
				options.inSampleSize = 1;
		
				BitmapFactory.decodeFile(filePath, options);
				
				if(options.outWidth > 0 && options.outHeight > 0){
					if (!cancel) {
						// Now see how much we need to scale it down.
						int widthFactor = (options.outWidth + imageWidth - 1)
								/ imageWidth;
						int heightFactor = (options.outHeight + imageHeight - 1)
								/ imageHeight;
						widthFactor = Math.max(widthFactor, heightFactor);
						widthFactor = Math.max(widthFactor, 1);
						// Now turn it into a power of two.
						if (widthFactor > 1) {
							if ((widthFactor & (widthFactor - 1)) != 0) {
								while ((widthFactor & (widthFactor - 1)) != 0) {
									widthFactor &= widthFactor - 1;
								}

							}
						}
						
						BitmapFactory.Options options2 =new BitmapFactory.Options();
						options2.inSampleSize=widthFactor;
						Bitmap bitmap = BitmapFactory.decodeFile(filePath,options2);
						
						if (bitmap != null) {
							return bitmap;
						}
					}
				} 
			} catch(Exception e) { }
		}
		return null;
	}
	
	/**
	 * Holder object for photo information.
	 */
	private class PhotoToLoad {
		public String filePath;
		public ImageView imageView;
		
		public PhotoToLoad(String parentFile, ImageView imageView) {
			this.filePath = parentFile;
			this.imageView = imageView;
		}
	}
	
	/**
	 * @see ImageUpdater
	 */
	private class ThumbnailRunner implements Runnable {
		PhotoToLoad thumb;
		ThumbnailRunner(PhotoToLoad thumb){
			this.thumb = thumb;
		}
		
		@Override
		public void run() {
			if(!cancel){
				Bitmap bitmap = decodeFile(thumb.filePath);
				if(bitmap != null && !cancel){
					// Bitmap was successfully decoded so we place it in the hard cache.
					mHardBitmapCache.put(thumb.filePath, bitmap);
					Activity activity = ((Activity) mContext);
					Log.d(TAG,"updating on UI thread");
					activity.runOnUiThread(new ImageUpdater(bitmap, thumb));
					thumb = null;
				}
			}
		}
	}
	

	private class ImageUpdater implements Runnable {
		private Bitmap bitmap;
		private PhotoToLoad thumb;
		
		public ImageUpdater(Bitmap bitmap, PhotoToLoad thumb) {
			this.bitmap = bitmap;
			this.thumb = thumb;
		}
		
		@Override
		public void run() {
			if(bitmap != null && mContext != null && !cancel){
				thumb.imageView.setImageBitmap(bitmap);
			}
		}
	}
}
