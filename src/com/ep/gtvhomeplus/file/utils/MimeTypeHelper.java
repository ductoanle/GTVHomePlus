package com.ep.gtvhomeplus.file.utils;

public class MimeTypeHelper {
	public static boolean isPhotoType(String mimeType){
		return mimeType.equals(MimeTypes.JPEG) || mimeType.equals(MimeTypes.BMP) || mimeType.equals(MimeTypes.PNG) ||mimeType.equals(MimeTypes.GIF);
	}
}
