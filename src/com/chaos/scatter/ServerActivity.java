package com.chaos.scatter;

import java.util.HashMap;
import java.util.Map;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;

public class ServerActivity extends Activity {

	// WiFi P2P Management
	WifiP2pManager wifiP2pManager;
	Channel channel;
	
	@Override protected void onCreate( Bundle savedInstanceState ) {
		
		super.onCreate( savedInstanceState);
		setContentView( R.layout.activity_server );

		// Store the manager and channel
		wifiP2pManager = (WifiP2pManager)getSystemService( Context.WIFI_P2P_SERVICE );
		channel = wifiP2pManager.initialize( this, getMainLooper( ), null );
	    
	}

    private void startRegistration() {
    	
        //  Create a string map containing information about your service.
        Map<String, String> record = new HashMap<String, String>( );
        record.put( "listenport", "9000" );

        // Service information.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance( "ScatterService", "_presence._tcp", record );

        // Add the local service, sending the service info, network channel, and listener.
        wifiP2pManager.addLocalService( channel, serviceInfo, new ActionListener() {
            @Override public void onSuccess( ) { }
            @Override public void onFailure( int code ) { }
        } );
        
    }
}