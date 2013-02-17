package com.invisiblek.cm.nightlies;

import java.util.ArrayList;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import com.invisiblek.cm.nightlies.model.Change;
import com.invisiblek.cm.nightlies.model.ListItem;
import com.invisiblek.cm.nightlies.model.Section;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class NightliesActivity extends SherlockListActivity {

	private LayoutInflater mInflater;
	private SharedPreferences prefs;
	private String currentDevice;
	private static final String defaultDevice = android.os.Build.DEVICE;
	private static final String TAG = "app-cm-nightlies";
	private ProgressDialog dialog = null;
	private boolean showTranslations = false;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);
		mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		currentDevice = prefs.getString("device", defaultDevice);
		load(currentDevice);
	}

	public void load(String device) {
		currentDevice = device;
		new GetChanges().execute(device);
		getSupportActionBar().setSubtitle(device);
		setSupportProgressBarIndeterminateVisibility(Boolean.TRUE);
	}


	public void gotDataEvent(ArrayList<ListItem> changes) {
		setSupportProgressBarIndeterminateVisibility (Boolean.FALSE);
		setListAdapter(new ArrayAdapter<ListItem>(this, R.layout.list_item, changes) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View row;
				ListItem li = getItem(position);

				row = convertView;

				if(!li.isSection()) {
					if(convertView == null || convertView.getId() != R.layout.list_item) {
						row = mInflater.inflate(R.layout.list_item, null);
					}

					Change change = (Change)li;

					((TextView) row.findViewById(R.id.subject)).setText(change.subject);
					((TextView) row.findViewById(R.id.project)).setText("("+change.project + ")");

				} else {
					if(convertView == null || convertView.getId() != R.layout.list_section) {
						row = mInflater.inflate(R.layout.list_section, null);
					}

					Section section = (Section)li;
					row = mInflater.inflate(R.layout.list_section, null);

					((TextView) row.findViewById(R.id.list_item_section_text)).setText(section.getDate());
				}

				return row;
			}
		});
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ListItem li = (ListItem)l.getItemAtPosition(position);
		String url = "";
		if(!li.isSection()) {

			Change c = (Change)li;
			url = "http://review.cyanogenmod.org/" + c.id;

		} else {

			url = "http://download.cyanogenmod.org/?device=" + currentDevice;
		}

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);

	}

	private class GetChanges extends AsyncTask<String, Void, ArrayList<ListItem>>{

		@Override
		protected ArrayList<ListItem> doInBackground(String... params) {
			Service s = new Service();
			try {
				ArrayList<ListItem> liste = s.getChanges(params[0], showTranslations);
				return liste;
			}

			catch(Exception e) {
				Log.d(TAG, "getChanges exception", e);
			}

			return null;
		}

		protected void onPostExecute(ArrayList<ListItem> result) {
			if(result == null) {
				Toast.makeText(NightliesActivity.this,"Problem loading data. Try choosing a device using the wrench in the upper right corner.",1000).show();
				setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);

			} else {
				 NightliesActivity.this.gotDataEvent(result);
			}
		}

	}
	private class GetDevices extends AsyncTask<Void, Void, ArrayList<String>> {

		@Override
		protected ArrayList<String> doInBackground(Void... params) {
			Service s = new Service();
			try
			{
				ArrayList<String> liste = s.getDevices();
				return liste;
			} catch(Exception e)
			{
				Log.d(TAG, "getDevices exception", e);
			}
			return null;
		}

		protected void onPostExecute(ArrayList<String> result) {
			if(NightliesActivity.this.dialog != null) {
				NightliesActivity.this.dialog.hide();
				NightliesActivity.this.dialog = null;

			}

			if(result == null) {
				Toast.makeText(NightliesActivity.this,"Problem loading data",1000).show();

			} else {
				NightliesActivity.this.gotDevices(result);
			}

		}
	}

	public void gotDevices(ArrayList<String> liste) {

		openDeviceSelector(liste);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getSupportMenuInflater().inflate(R.menu.menu, menu);

		// Configure the search info and add any event listeners
		return super.onCreateOptionsMenu(menu);
	}

	public void openDeviceSelector(final ArrayList<String> items) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose device");
		builder.setItems(items.toArray(new CharSequence[items.size()]), new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int item) {
				Editor e = NightliesActivity.this.prefs.edit();
				e.putString("device", items.get(item));
				e.commit();
				NightliesActivity.this.load(items.get(item));
			}
		});
		AlertDialog alert = builder.create();
		alert.show();

	}

	public void getDevices() {

		dialog = ProgressDialog.show(this, "", "Getting device list. \nHold on...", true);
		dialog.show();
		new GetDevices().execute();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_pref:
				getDevices();
				return true;
			case R.id.show_translations:
				if (showTranslations) {
					Toast.makeText(NightliesActivity.this,"Hiding Translations...",1000).show();
					showTranslations = false;
				} else {
					Toast.makeText(NightliesActivity.this,"Showing Translations...",1000).show();
					showTranslations = true;
				}
				load(currentDevice);
				return true;
			case R.id.menu_refresh:
				load(currentDevice);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
