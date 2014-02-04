package com.chaos.scatter;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity implements OnClickListener {
	
	// Store context reference
	final Context context = this;
	
	// WiFi manager
	WifiManager wifiManager;
	
	@Override protected void onCreate( Bundle savedInstanceState ) {
		
		// Call parent method
		super.onCreate( savedInstanceState );
		
		// Hide title view
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		getWindow( ).setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
		
		// Store WiFiManager reference
		wifiManager = (WifiManager)getSystemService( Context.WIFI_SERVICE );
		if ( wifiManager.isWifiEnabled( ) == Boolean.FALSE )
			wifiManager.setWifiEnabled( Boolean.TRUE );
		
		// Set the layout view
		setContentView( R.layout.activity_main );
        
		// Register button call backs
		findViewById( R.id.server ).setOnClickListener( this );
		findViewById( R.id.client ).setOnClickListener( this );
		
	}
	
	// Check for WiFi on application resume
	/*protected void onResume( ) {
		
		// Call parent method
		super.onResume( );
		
        // Show a dialog to prompt the user to enable the WiFi
        if ( wifiManager.isWifiEnabled( ) != Boolean.TRUE )
        	new AlertDialog.Builder( this )
            	.setTitle( "This application uses WiFi" )
                .setMessage( "Would you like to enable WiFi on your device?" )
                .setPositiveButton( "Yes", new DialogInterface.OnClickListener( ) {
                	public void onClick( DialogInterface dialog, int which )
                    	{ wifiManager.setWifiEnabled( Boolean.TRUE ); }
                } )
                .setCancelable( Boolean.FALSE )
                .show( );
        
	}*/

	@Override public void onClick( View view ) {

		// Switch over resource id's
		switch ( view.getId( ) ) {
		
			// Start the server activity
			case R.id.server:
				startActivity( new Intent( this, ServerActivity.class ) );
				break;
				
			// Start the client activity
			case R.id.client:
				startActivity( new Intent( this, ClientActivity.class ) );
				break;
			
		}
		
	}
	
}