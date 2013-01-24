package com.vis.smartaudio;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.location.Address;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class SmartAudio extends Activity {
	
	EditText ipField;
	EditText portField;
	Button connectButton;
	TextView statusLabel;
	ListView applianceList;
	ArrayAdapter<String> appliancesAdapter;
	SharedPreferences prefs;
	String selectedAppliance;
	ServerConnection conn;
	MediaPlayer player;
	String soundSource;
	
	private static final String LOG_TAG = "SmartAudio";
//	private static final String SOUND_SOURCE = "clong.mp3";
	private static final String SOUND_BLIP = "blip.mp3";
	private static final String SOUND_CLONG = "clong.mp3";
	private static final String SOUND_ALARM = "alarm.mp3";
	
	public static final String APP_STORAGE_PREFIX = "com.vis.smartaudio";
	public static final String STORAGE_SERVER_IP = APP_STORAGE_PREFIX + ".server.ip";
	public static final String STORAGE_SERVER_PORT = APP_STORAGE_PREFIX + ".server.port";
	private static final int MENU_QUIT = 0;
	private static final int MENU_SETTINGS = 1;
	private static final int MENU_BLIP = 2;
	private static final int MENU_CLONG = 3;
	private static final int MENU_ALARM = 4;
	
	public static final String[] appliances = new String[] {
		"Radio", 
		"TV", 
		"Lampe", 
		"Heizung",
		"Tischlampe"
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_smart_audio);
		
		prefs = getSharedPreferences(APP_STORAGE_PREFIX, Context.MODE_PRIVATE);
		
		ipField = (EditText) findViewById(R.id.edit_server_ip);
		portField = (EditText) findViewById(R.id.edit_server_port);
		connectButton = (Button) findViewById(R.id.button_connect);
		statusLabel = (TextView) findViewById(R.id.statusLabel);
		applianceList = (ListView) findViewById(R.id.applianceList);
		
		Log.v(LOG_TAG, "Retrieving config from SharedPreferences: " + prefs.getString(STORAGE_SERVER_IP, "") + ":" + prefs.getString(STORAGE_SERVER_PORT, ""));
        ipField.setText(prefs.getString(STORAGE_SERVER_IP, ""));
        ipField.setSelected(false);
        portField.setText(prefs.getString(STORAGE_SERVER_PORT, ""));
        portField.setSelected(false);
		
		appliancesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, appliances);
        applianceList.setAdapter(appliancesAdapter);
        applianceList.setOnItemClickListener(applianceListClickHandler);
        
        addButtonListener();
        
        //init sound player
        soundSource = SOUND_BLIP;
        restartMediaPlayer();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_smart_audio, menu);
		menu.add(0, MENU_QUIT, 0, "Quit");
		//TODO: tidy this up:
//		menu.add(1, MENU_SETTINGS, 1, "Settings");
		menu.add(1, MENU_BLIP, 0, "Blip");
		menu.add(1, MENU_CLONG, 0, "Clong");
		menu.add(1, MENU_ALARM, 0, "Alarm");
		return true;
	}
	
	/* Handles item selections */
	@SuppressWarnings("finally")
	public boolean onOptionsItemSelected(MenuItem item) {
		System.out.println("omOptionsItemSelected()");
		System.out.println(item.getItemId());
		try {
		    switch (item.getItemId()) {
			    case MENU_QUIT: {
			    	Log.v(LOG_TAG, "QUIT");
			    	System.exit(0); 
			    }
		    	case MENU_SETTINGS: {
			    	Log.v(LOG_TAG, "Settings");
			    	Intent settings = new Intent(this, Settings.class);
			    	startActivity(settings);
			    }
		    	case MENU_BLIP: {
		    		soundSource = SOUND_BLIP;
		    		restartMediaPlayer();
		    		break;
		    	}
		    	case MENU_CLONG: {
		    		soundSource = SOUND_CLONG;
		    		restartMediaPlayer();
		    		break;
		    	}
		    	case MENU_ALARM: {
		    		soundSource = SOUND_ALARM;
		    		restartMediaPlayer();
		    		break;
		    	}
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
    		return true;
    	}
	}
	
	private void restartMediaPlayer() {
		try {
			player = new MediaPlayer();
		    AssetFileDescriptor afd = getAssets().openFd(soundSource);
		    player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
		    player.prepare();
		} catch (IOException exc) {
			Log.e(LOG_TAG, "MediaPlayer could not be initialized.");
			exc.printStackTrace();
		}
	}
	
	
	private OnItemClickListener applianceListClickHandler = new OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            String appliance = (String) applianceList.getItemAtPosition(position);
        	
        	if(selectedAppliance != null) {
	        	int pos = appliancesAdapter.getPosition(selectedAppliance);
	        	applianceList.getChildAt(pos).setBackgroundResource(R.color.white);
        	}
        	
        	v.setBackgroundResource(R.color.orange);
        	selectedAppliance = appliance;
        	
        	statusLabel.setText("Selected: " + selectedAppliance);
        }
    };
    
    private void setAudio(String status) {
    	Log.v(LOG_TAG, "setAudio(" + status + ")");
    	
    	if(status.equals("on")) {
			//play audio
			System.out.println("ON");
			player.seekTo(0);
			player.start();
    	}
    	else {
			System.out.println("OFF");
			player.pause();
    	}
    }
    
    private void addButtonListener() {
    	System.out.println("addButtonListener()");
    	
    	connectButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String ip = ipField.getText().toString();
				String port = portField.getText().toString();
				
				System.out.println("Setting SharedPreferences to: " + ip + ":" + port);
				prefs.edit().putString(STORAGE_SERVER_IP, ip).commit();
				prefs.edit().putString(STORAGE_SERVER_PORT, port).commit();
				
				if(conn == null) {
					
					try {
						int portI = Integer.parseInt(port);
						Log.v(LOG_TAG, "Connecting to: " + ip + " (port:" + port + ")");
	
						conn = new ServerConnection(ip, portI);
						conn.setServerEventListener(new ServerConnection.ServerEventListener() {
							
							@Override
							public void onServerMessageReceived(String msg) {
								statusLabel.setText(msg);
							}
							
							public void onAudioStatusChanged(String device, String status) {
								Log.v(LOG_TAG, "onAudioStatusChanged(" + device + ", " + status + ")");
								device = device.toLowerCase();
								if(device.equals(selectedAppliance.toLowerCase())) {
									setAudio(status);
								}
							}
						});
						conn.register();
						conn.openUDPSocket();
						
//						compass.setCompassEventListener(new Compass.CompassEventListener() {
//							
//							@Override
//							public void onSensorChanged(float x, float y, float z) {
//								compassLabel.setText("Compass reading: " + x);	
//							}
//						});
						
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
    }

}
