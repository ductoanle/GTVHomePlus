package com.ep.gtvhomeplus.fragments;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ep.gtvhomeplus.PlayMovieActivity;
import com.ep.gtvhomeplus.R;
import com.ep.gtvhomeplus.file.DirectoryContents;
import com.ep.gtvhomeplus.file.DirectoryScanner;
import com.ep.gtvhomeplus.file.IconifiedText;
import com.ep.gtvhomeplus.file.IconifiedTextListAdapter;
import com.ep.gtvhomeplus.file.ThumbnailLoader;
import com.ep.gtvhomeplus.file.utils.FileUtils;
import com.ep.gtvhomeplus.file.utils.MimeTypeParser;
import com.ep.gtvhomeplus.file.utils.MimeTypes;

public class USBStorageFragment extends Fragment implements OnItemClickListener{

	private static final String TAG = "InternalStoragesFragment";

	private int mState;

	private static final int STATE_BROWSE = 1;
	private static final int STATE_PICK_FILE = 2;
	private static final int STATE_PICK_DIRECTORY = 3;
	private static final int STATE_MULTI_SELECT = 4;

	private static final String BUNDLE_CURRENT_DIRECTORY = "current_directory";
	private static final String BUNDLE_CONTEXT_FILE = "context_file";
	private static final String BUNDLE_CONTEXT_TEXT = "context_text";
	private static final String BUNDLE_STEPS_BACK = "steps_back";
	private static final String BUNDLE_DIRECTORY_ENTRIES = "directory_entries";
	/** Shows whether activity state has been restored (e.g. from a rotation). */
	private static boolean mRestored = false;


	/** Contains directories and files together */
	private ArrayList<IconifiedText> directoryEntries = new ArrayList<IconifiedText>();

	/** Dir separate for sorting */
	List<IconifiedText> mListDir = new ArrayList<IconifiedText>();

	/** Files separate for sorting */
	List<IconifiedText> mListFile = new ArrayList<IconifiedText>();

	/** SD card separate for sorting */
	List<IconifiedText> mListSdCard = new ArrayList<IconifiedText>();
    
	/** the initial directory being browsed */
	private File mDefaultDirectory;
	
	private File currentDirectory = new File(""); 
    
	protected String mSdCardPath = "";

	private MimeTypes mMimeTypes;
	/** Files shown are filtered using this extension */
	private String mFilterFiletype = "";
	/** Files shown are filtered using this mimetype */
	private String mFilterMimetype = null;

	private String mContextText;
	private File mContextFile = new File("");

	/** How many steps one can make back using the back key. */
	private int mStepsBack;          
	private DirectoryScanner mDirectoryScanner;
	private ThumbnailLoader mThumbnailLoader;
	private File mPreviousDirectory;

	private Handler currentHandler;

	private boolean mWritableOnly;

	private IconifiedText[] mDirectoryEntries;

	static final public int MESSAGE_SHOW_DIRECTORY_CONTENTS = 500;	// List of contents is ready, obj = DirectoryContents
	static final public int MESSAGE_SET_PROGRESS = 501;	// Set progress bar, arg1 = current value, arg2 = max value
	static final public int MESSAGE_ICON_CHANGED = 502;	// View needs to be redrawn, obj = IconifiedText

	///////Views
	private TextView mCurrentDirectoryText;
	private TextView mEmptyText;
	private ProgressBar mProgressBar;
    private ListView mFilesList;
    ListAdapter mFileListAdapter;
	
	/** Called when the activity is first created. */ 
	@Override 
	public void onCreate(Bundle icicle) { 
		super.onCreate(icicle); 

		currentHandler = new Handler() {
			public void handleMessage(Message msg) {
				USBStorageFragment.this.handleMessage(msg);
			}
		};
		// Create map of extensions:
		getMimeTypes();
		getSdCardPath();      

		mDefaultDirectory = new File("/");
		if (!TextUtils.isEmpty(mSdCardPath)) {
			mDefaultDirectory = new File(mSdCardPath);
		}
		
		// Default state
		mState = STATE_BROWSE;
		mWritableOnly = false;
		mStepsBack = 0;

		// Reset mRestored flag.
		mRestored = false;
		if (icicle != null) {
			mDefaultDirectory = new File(icicle.getString(BUNDLE_CURRENT_DIRECTORY));
			mContextFile = new File(icicle.getString(BUNDLE_CONTEXT_FILE));
			mContextText = icicle.getString(BUNDLE_CONTEXT_TEXT);

			mStepsBack = icicle.getInt(BUNDLE_STEPS_BACK);

			Parcelable tmpDirectoryEntries[] = icicle.getParcelableArray(BUNDLE_DIRECTORY_ENTRIES);
			mDirectoryEntries = new IconifiedText[tmpDirectoryEntries.length];
			for(int i=0; i<tmpDirectoryEntries.length; i++){
				mDirectoryEntries[i] = (IconifiedText) tmpDirectoryEntries[i];
			}
			mRestored = true;
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		View root = inflater.inflate(R.layout.internal_storage_content, container, false);
		mCurrentDirectoryText =(TextView) root.findViewById(R.id.tv_current_directory);
		mEmptyText = (TextView) root.findViewById(R.id.empty_text);
		mProgressBar = (ProgressBar) root.findViewById(R.id.scan_progress);
		mFilesList = (ListView) root.findViewById(R.id.list_files);
		
		mFilesList.setEmptyView(root.findViewById(R.id.empty));
		mFilesList.setTextFilterEnabled(true);
		mFilesList.requestFocus();
		mFilesList.requestFocusFromTouch();
		mFilesList.setOnItemClickListener(this);
        mFilesList.setOnScrollListener(new AbsListView.OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) view.getAdapter();
				if(adapter != null){
					switch (scrollState) {
					case OnScrollListener.SCROLL_STATE_IDLE:
						adapter.toggleScrolling(false);
						adapter.notifyDataSetChanged();
						break;
					case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
						adapter.toggleScrolling(true);
						break;
					case OnScrollListener.SCROLL_STATE_FLING:
						adapter.toggleScrolling(true);
						break;
					}
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
			}
		
		});
        browseTo(mDefaultDirectory);
		return root;
	}
	
	public void onBackPressed(){
		if (mStepsBack > 0) 
			upOneLevel();
		else
			getActivity().finish();
	}

	public void onDestroy() {
		super.onDestroy();

		// Stop the scanner.
		DirectoryScanner scanner = mDirectoryScanner;

		if (scanner != null) {
			scanner.cancel = true;
		}

		mDirectoryScanner = null;

		ThumbnailLoader loader = mThumbnailLoader;

		if (loader != null) {
			loader.cancel();
			mThumbnailLoader = null;
		}

		if(mFilesList != null){
			mFilesList.setAdapter(null);
		}
	}

	private void handleMessage(Message message) {    	 
		switch (message.what) {
		case MESSAGE_SHOW_DIRECTORY_CONTENTS:
			showDirectoryContents((DirectoryContents) message.obj);
			break;

		case MESSAGE_SET_PROGRESS:
			setProgress(message.arg1, message.arg2);
			break;
		}
	}

	private void setProgress(int progress, int maxProgress) {
		mProgressBar.setMax(maxProgress);
		mProgressBar.setProgress(progress);
		mProgressBar.setVisibility(View.VISIBLE);
	}

	private void showDirectoryContents(DirectoryContents contents) {
		mDirectoryScanner = null;

		mListSdCard = contents.listSdCard;
		mListDir = contents.listDir;
		mListFile = contents.listFile;

		if(!mRestored){
			directoryEntries.ensureCapacity(mListSdCard.size() + mListDir.size() + mListFile.size());

			addAllElements(directoryEntries, mListSdCard);
			addAllElements(directoryEntries, mListDir);
			addAllElements(directoryEntries, mListFile);

			mDirectoryEntries = directoryEntries.toArray(new IconifiedText[0]);
		}
		else {
			directoryEntries.clear();
			directoryEntries.ensureCapacity(mDirectoryEntries.length);
			for(int i = 0; i < mDirectoryEntries.length; i++){
				directoryEntries.add(mDirectoryEntries[i]);
			}    		 
			// Once mRestore flag has been used, we should toggle it so that further refreshes don't take it into account
			mRestored = false;
		}

		mFileListAdapter = new IconifiedTextListAdapter(getActivity()); 
		((IconifiedTextListAdapter) mFileListAdapter).setListItems(directoryEntries, mFilesList.hasTextFilter(), currentDirectory, mMimeTypes);          
		mFilesList.setAdapter(mFileListAdapter);
		mFilesList.setTextFilterEnabled(true);

		//update  other views
		selectInList(mPreviousDirectory);
		mCurrentDirectoryText.setText(currentDirectory.getAbsolutePath());
		mProgressBar.setVisibility(View.GONE);
		mEmptyText.setVisibility(View.VISIBLE); 
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// remember file name
		outState.putString(BUNDLE_CURRENT_DIRECTORY, currentDirectory.getAbsolutePath());
		outState.putString(BUNDLE_CONTEXT_FILE, mContextFile.getAbsolutePath());
		outState.putString(BUNDLE_CONTEXT_TEXT, mContextText);
		outState.putInt(BUNDLE_STEPS_BACK, mStepsBack);
		outState.putParcelableArray(BUNDLE_DIRECTORY_ENTRIES, mDirectoryEntries);
	}

	/**
	 * get mimeType info from resource file
	 */
	private void getMimeTypes() {
		MimeTypeParser mtp = null;
		try {
			Activity activity= getActivity();
			mtp = new MimeTypeParser(activity, activity.getPackageName());
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		XmlResourceParser in = getResources().getXml(R.xml.mimetypes);

		try {
			mMimeTypes = mtp.fromXmlResource(in);
		} catch (XmlPullParserException e) {
			Log
			.e(
					TAG,
					"PreselectedChannelsActivity: XmlPullParserException",
					e);
			throw new RuntimeException(
					"PreselectedChannelsActivity: XmlPullParserException");
		} catch (IOException e) {
			Log.e(TAG, "PreselectedChannelsActivity: IOException", e);
			throw new RuntimeException(
					"PreselectedChannelsActivity: IOException");
		}
	} 

	/** 
	 * This function browses up one level 
	 * according to the field: currentDirectory 
	 */ 
	private void upOneLevel(){
		if (mStepsBack > 0) {
			mStepsBack--;
		}
		if(currentDirectory.getParent() != null) 
			browseTo(currentDirectory.getParentFile()); 
	} 


	/**
	 * Browse to some location by clicking on a list item.
	 * @param aDirectory
	 */
	private void browseTo(final File aDirectory){           
		if (aDirectory.isDirectory()){
				mPreviousDirectory = currentDirectory;
				currentDirectory = aDirectory;
				refreshList();
		}else{ 
				openFile(aDirectory); 
		} 
	}


	private void openFile(File aFile) { 
		if (!aFile.exists()) {
			Toast.makeText(getActivity(), R.string.error_file_does_not_exists, Toast.LENGTH_SHORT).show();
			return;
		}

		Intent intent = new Intent(getActivity(), PlayMovieActivity.class);
		Uri data = FileUtils.getUri(aFile);
		String type = mMimeTypes.getMimeType(aFile.getName());
		intent.setDataAndType(data, type);
				
		intent.putExtra(PlayMovieActivity.MEDIA_FILE, aFile.getAbsolutePath());
		intent.putExtra(PlayMovieActivity.MEDIA_FOLDER, aFile.getParent());
		

		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(getActivity(), R.string.application_not_available, Toast.LENGTH_SHORT).show();
		};
	} 

	public void refreshList() {

		boolean directoriesOnly = mState == STATE_PICK_DIRECTORY;    	 
		// Cancel an existing scanner, if applicable.
		DirectoryScanner scanner = mDirectoryScanner;

		if (scanner != null) {
			scanner.cancel = true;
		}

		ThumbnailLoader loader = mThumbnailLoader;

		if (loader != null) {
			loader.cancel();
			mThumbnailLoader = null;
		}

		directoryEntries.clear(); 
		mListDir.clear();
		mListFile.clear();
		mListSdCard.clear();

		// Don't show the "folder empty" text since we're scanning.
		mEmptyText.setVisibility(View.GONE);

		mProgressBar.setVisibility(View.GONE);
		mFilesList.setAdapter(null); 

		mDirectoryScanner = new DirectoryScanner(currentDirectory, getActivity(), currentHandler, mMimeTypes, mFilterFiletype, mFilterMimetype, mSdCardPath, mWritableOnly, directoriesOnly);
		mDirectoryScanner.start();

	} 

	private void selectInList(File selectFile) {
		String filename = selectFile.getName();
		IconifiedTextListAdapter la = (IconifiedTextListAdapter) mFilesList.getAdapter();
		int count = la.getCount();
		for (int i = 0; i < count; i++) {
			IconifiedText it = (IconifiedText) la.getItem(i);
			if (it.getText().equals(filename)) {
				mFilesList.setSelection(i);
				break;
			}
		}
	}

	private void addAllElements(List<IconifiedText> addTo, List<IconifiedText> addFrom) {
		int size = addFrom.size();
		for (int i = 0; i < size; i++) {
			addTo.add(addFrom.get(i));
		}
	}

	@Override 
	public void onItemClick(AdapterView<?> l, View v, int position, long id) { 
		IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) l.getAdapter();
		if (adapter == null) {
			return;
		}

		IconifiedText text = (IconifiedText) adapter.getItem(position);
		String file = text.getText(); 

		String curdir = currentDirectory 
				.getAbsolutePath() ;
		File clickedFile = FileUtils.getFile(curdir, file);
		if (clickedFile != null) {
			if (clickedFile.isDirectory()) {
				// If we click on folders, we can return later by the "back" key.
						mStepsBack++;
			}
			browseTo(clickedFile);
		}
	}
	
	

	private void getSdCardPath() {
		mSdCardPath = mSdCardPath = "/mnt/media/usb.BCFCAAF2FCAAA5DC";
	}
}
