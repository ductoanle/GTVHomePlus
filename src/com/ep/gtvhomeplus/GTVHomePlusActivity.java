package com.ep.gtvhomeplus;

import com.example.google.tv.leftnavbar.LeftNavBar;
import com.example.google.tv.leftnavbar.LeftNavBarService;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.app.FragmentTransaction;

import com.ep.gtvhomeplus.fragments.*;

public class GTVHomePlusActivity extends Activity {

	private ActionBar mActionBar;

	public static class TabListener<T extends Fragment> implements
	ActionBar.TabListener {
		private final Activity mActivity;
		private final String mTag;
		private final Class<T> mClass;
		private final Bundle mExtras;

		public Fragment getFragment() {
			FragmentManager manager = mActivity.getFragmentManager();
			return manager.findFragmentByTag(mTag);
		}
		
		/**
		 * Constructor used each time a new tab is created.
		 * 
		 * @param activity
		 *            The host Activity, used to instantiate the fragment
		 * @param tag
		 *            The identifier tag for the fragment
		 * @param clz
		 *            The fragment's Class, used to instantiate the fragment
		 */
		public TabListener(GTVHomePlusActivity activity, String tag, Class<T> clz,
				Bundle extras) {
			mActivity = activity;
			mTag = tag;
			mClass = clz;
			mExtras = extras;
		}

		public TabListener(GTVHomePlusActivity activity, String tag, Class<T> clz) {
			this(activity, tag, clz, null);
		}

		/* The following are each of the ActionBar.TabListener callbacks */

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Fragment f = Fragment.instantiate(mActivity, mClass.getName(),
					mExtras);
			ft.add(android.R.id.content, f, mTag);
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			Fragment fragment = getFragment();
			if (fragment != null) {
				// Detach the fragment, because another one is being attached
				ft.remove(fragment);
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// Do nothing.
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		findViewById(android.R.id.content).setBackgroundDrawable(
				getResources().getDrawable(R.drawable.content_background));
		ActionBar actionBar = getActionBar();
		Tab tab;
		tab = actionBar
				.newTab()
				.setIcon(R.drawable.internal_storage)
				.setText(R.string.internal_storage_menu)
				.setTabListener(new TabListener<InternalStoragesFragment>(this, "Internal", InternalStoragesFragment.class));
		actionBar.addTab(tab);
		
		tab = actionBar
				.newTab()
				.setIcon(R.drawable.usb_storage)
				.setText(R.string.usb_storage_menu)
				.setTabListener(new TabListener<PlayMovieFragment>(this, "Internal", PlayMovieFragment.class));
		actionBar.addTab(tab);
		
		tab = actionBar
				.newTab()
				.setIcon(R.drawable.network_storage)
				.setText(R.string.network_storage_menu)
				.setTabListener(new TabListener<InternalStoragesFragment>(this, "Internal", InternalStoragesFragment.class));
		actionBar.addTab(tab);
	}

	@Override
	public ActionBar getActionBar() {
		if (mActionBar == null) {
			ActionBar actionBar = super.getActionBar();

			if (actionBar == null) {
				actionBar =  (LeftNavBarService.instance()).getLeftNavBar(this);

				int options = actionBar.getDisplayOptions();
				options |= LeftNavBar.DISPLAY_AUTO_EXPAND;
				options |= LeftNavBar.DISPLAY_USE_LOGO_WHEN_EXPANDED;
				actionBar.setDisplayOptions(options);
				actionBar.setTitle(R.string.app_name);
			}
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			actionBar.setDisplayShowHomeEnabled(true);
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setDisplayUseLogoEnabled(true);
			actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.leftnav_bar_background_dark));

			mActionBar = actionBar;
		}
		return mActionBar;
	}
	
	// Options menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.universal_menu, menu);
		return true;
	}
}