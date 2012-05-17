package com.ep.gtvhomeplus.file.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public final class ImageUtils {

	/**
	 * Resizes specific a Bitmap with keeping ratio.
	 */
	public static Bitmap resizeBitmap(Bitmap drawable, int desireWidth,
			int desireHeight) {
		int width = drawable.getWidth();
		int height = drawable.getHeight();

		if (0 < width && 0 < height && desireWidth < width
				|| desireHeight < height) {
			// Calculate scale
			float scale;
			if (width < height) {
				scale = (float) desireHeight / (float) height;
				if (desireWidth < width * scale) {
					scale = (float) desireWidth / (float) width;
				}
			} else {
				scale = (float) desireWidth / (float) width;
			}

			// Draw resized image
			Matrix matrix = new Matrix();
			matrix.postScale(scale, scale);
			Bitmap bitmap = Bitmap.createBitmap(drawable, 0, 0, width, height,
					matrix, true);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawBitmap(bitmap, 0, 0, null);

			drawable = bitmap;
		}

		return drawable;
	}

	/**
	 * Resizes specific a Drawable with keeping ratio.
	 */
	public static Drawable resizeDrawable(Drawable drawable, int desireWidth,
			int desireHeight) {
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();

		if (0 < width && 0 < height && desireWidth < width
				|| desireHeight < height) {
			drawable = new BitmapDrawable(resizeBitmap(
					((BitmapDrawable) drawable).getBitmap(), desireWidth,
					desireHeight));
		}

		return drawable;
	}
	
    /**
     * 
     * @param f: File
     * @param required_size: 0 => no scaling
     * @return
     */
    public static Bitmap decodeFile(File f,int required_size){
    	//no scaling
    	if(required_size == 0){
    		try {
				return BitmapFactory.decodeStream(new FileInputStream(f)); 
			} catch (FileNotFoundException e) {
                return null;
			}    		
    	}
    	
    	//scaling
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);
            
            //Find the correct scale value. It should be the power of 2.
            
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;
            while(true){
                if(width_tmp/2<required_size || height_tmp/2<required_size)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }
            
            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
        }
        return null;
    }

}
