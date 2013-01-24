package com.vis.smartaudio;

import android.app.Activity;
import android.os.Bundle;

public class Settings extends Activity {
	
	private String soundSource;
	private SettingsListener settingsListener;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
	}
	
	public void setSettingsListener(SettingsListener listener) {
		this.settingsListener = listener;
	}
	
	public interface SettingsListener {
		public void onSoundChanged(String soundfile);
	}
	
}
