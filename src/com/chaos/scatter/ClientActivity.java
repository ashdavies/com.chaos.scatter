package com.chaos.scatter;

import java.util.ArrayList;
import java.util.List;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ClientActivity extends Activity {
	
	// Store self referential context
	final private Context context = this;

	// WiFi manager
	WifiManager manager;
	
	// UDP Messenger protocol
	Messenger messenger;

	// Broadcast receiver
	Receiver receiver;

	// Text fields
	TextView update;
	
	@Override protected void onCreate( Bundle savedInstanceState ) {
		
		// Set the layout
		super.onCreate( savedInstanceState);
		setContentView( R.layout.activity_client );
        
		// Set references
		manager = (WifiManager)getSystemService( Context.WIFI_SERVICE );
		update = (TextView)findViewById( R.id.update );
		
		// Create a new messenger
		messenger = new Messenger( context, "224.0.0.0", 9000 );
		
        // Create new Receiver
        receiver = new Receiver( );
        
        // Create new intent filter for the receiver and add action
        IntentFilter intentFilter = new IntentFilter( );
        intentFilter.addAction( WifiManager.SCAN_RESULTS_AVAILABLE_ACTION );
        
        // Register the receiver
        registerReceiver( receiver, intentFilter );
        
        // Begin scanning
        manager.startScan( );
		
	}

	// Unregister the receiver
	@Override protected void onDestroy( ) {
		unregisterReceiver( receiver );
		super.onDestroy( );
	}

	// Broadcast receiver registers received signal strength
	private class Receiver extends BroadcastReceiver {
        @Override public void onReceive( Context context, Intent intent ) {
        	
        	// Finish if WiFi is disabled
        	if ( manager.isWifiEnabled( ) == Boolean.FALSE ) return;
        	
        	// Get scan results
        	List<ScanResult> results = manager.getScanResults( );
        	
        	// UDP transmission string
        	List<String> signals = new ArrayList<String>( );
        	
        	// Reset text
        	update.setText( "" );
        	
        	// Iterate over scan results and push ping over network
        	for( int i = 0; i < results.size( ); i++ ) {
        		
        		// Get result item
        		ScanResult signal = results.get( i );
        
        		// Only process if signal is high enough
        		if ( signal.level < -80 ) continue;
        		
        		// Format pulse signal
        		String pulse = signal.SSID + ":" + signal.level;
        		
        		// Update text field
        		update.setText( update.getText( ) + pulse + "\r\n" );
        		
        		// Append the pulse to the message
        		signals.add( pulse );
        		
        	}
        	
        	// Post update to UDP messenger client
        	messenger.send( TextUtils.join( ",", signals ) + ";" );
        	
        	// Repeat the scan
        	manager.startScan( );
        	
        }
	}

}
