package com.vis.smartaudio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

/** Handles all connections and data transmission between device and server. Device registration is done over TCP, once ip is assigned and 
 * device is registered, udp packages are sent between server and device for communication
 * 
 * @author tilman
 *
 */
public class ServerConnection {
	
	private String ip;
	private int port;
	private String serverIp; //base server ip
	private int serverTCPPort; //base server port
	private Boolean registered;
	private UDPConnection updc;
	private ServerEventListener serverEventListener;
	
	private static final String LOG_TAG = "ServerConnection";
	public static final int UDP_PORT = 1336;
	private static final String SERVER_COMMAND_REGISTER = "register";
	
	public ServerConnection(String ip, int port) {
		Log.v(LOG_TAG, "new ServerConnection()");
		this.serverIp = ip;
		this.serverTCPPort = port;
		registered = false;
	}
	
	/** opens up tcp connection to server and registers its udp socket by sending ip and port
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 * @return String Server response
	 */
	public void register() {
		Log.v(LOG_TAG, "register(): " + this.serverIp + " (port:" + this.serverTCPPort + ")");
		try {
			disableStrictMode();
			String urlString = "http://" + this.serverIp + ":" + this.serverTCPPort + "/smartdevice?cmd=" + SERVER_COMMAND_REGISTER + "&port=" + UDP_PORT;
			Log.v(LOG_TAG, "request url: " + urlString);
			
			URL url = new URL(urlString);
		    URLConnection conn = url.openConnection();
		    conn.setConnectTimeout(2000);
		    conn.setReadTimeout(2000);
		    conn.setDoOutput(true);
		    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		    wr.flush();

		    // Get the response
		    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		    String line;
		    String resp = "";
		    while ((line = rd.readLine()) != null) {
		       resp += line;
		    }
		    Log.v(LOG_TAG, "Server response: " + resp);
		    String[] r = resp.split(":");
		    this.ip = r[r.length-2];
		    this.port = Integer.parseInt(r[r.length-1]);
		    Log.v(LOG_TAG, "ip set to: " + this.ip);
		    Log.v(LOG_TAG, "port set to: " + this.port);

		    serverEventListener.onServerMessageReceived("Connected. Device IP: " + this.ip);
		    registered = true;
		    
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e(LOG_TAG, "Could not connect to server: " + ip + ":" + port);
			serverEventListener.onServerMessageReceived("Could not connect to server.");
			e.printStackTrace();
		} 
	}
	
	public void setServerEventListener(ServerEventListener listener) {
    	this.serverEventListener = listener;
    }
	
	public void openUDPSocket() {
		Log.v(LOG_TAG, "openUPDSocket()");
		updc = new UDPConnection();
		updc.execute(new String[] {});
	}
	
	public void closeUDPSocket() {
		Log.v(LOG_TAG, "closeUDPSocket()");
		if(updc != null) {
			updc.closeSocket();
		}
	}
	
	public Boolean isRegistered() {
		return this.registered;
	}
	
	/** Hack: Helper method to circumvent strict mode on android device that prevents background activities to freeze up the UI
	 * 
	 * @throws ClassNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 */
	private void disableStrictMode() throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException {
		Log.v(LOG_TAG, "disableStrictMode()");
		//circumvent strictMode (http://android-developers.blogspot.de/2010/12/new-gingerbread-api-strictmode.html)
		Class strictModeClass=Class.forName("android.os.StrictMode");
        Class strictModeThreadPolicyClass=Class.forName("android.os.StrictMode$ThreadPolicy");
        Object laxPolicy = strictModeThreadPolicyClass.getField("LAX").get(null);
        Method method_setThreadPolicy = strictModeClass.getMethod(
                "setThreadPolicy", strictModeThreadPolicyClass );
        method_setThreadPolicy.invoke(null,laxPolicy);
	}
	
	/** Helper class to manage UDP connection socket as asynchronous background task
	 * 
	 * @author tilman
	 *
	 */
	private class UDPConnection extends AsyncTask<String, Void, String> {
		
		private Boolean listening;
		
		public UDPConnection() {
			this.listening = false;
		}
		
		public void closeSocket() {
			this.listening = false;
		}
		
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			
			byte[] message = new byte[256];
			DatagramPacket p = new DatagramPacket(message, message.length);
			DatagramSocket udpSocket;
			try {
				
				disableStrictMode();
				udpSocket = new DatagramSocket(UDP_PORT);
				this.listening = true;
				
				while(this.listening) {
					Log.d(LOG_TAG,"listening on " + UDP_PORT);
					udpSocket.receive(p);
					String cmd = new String(message, 0, p.getLength());
					Log.d(LOG_TAG,"message received: " + cmd);
					String[] msg = cmd.split(":");
					if(msg.length>1) {
						serverEventListener.onAudioStatusChanged(msg[0], msg[1]);
					}
					else {
						serverEventListener.onServerMessageReceived("Server: " + cmd);
					}
					//here we could switch between different server commands (enter, out,..)
//					this.activity.vibrate(SmartWrist.VIBRATION_DURATION_ENTER);
				}
				udpSocket.close();
				
			} catch (SocketException e) {
				Log.d(LOG_TAG, "SocketException: could not create DatagramSocket");
				e.printStackTrace();
			} catch (IOException e) {
				Log.d(LOG_TAG, "IOException (udpSocket.receive())");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				Log.d(LOG_TAG, "ClassNotFoundException (strictModeClass)");
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	    @Override
	    protected void onPostExecute(String result) {
	      System.out.println("UDPConnection open");
	    }
	  }
	
	public interface ServerEventListener {
		public void onServerMessageReceived(String msg);
		public void onAudioStatusChanged(String device, String status);
	}
	
}

