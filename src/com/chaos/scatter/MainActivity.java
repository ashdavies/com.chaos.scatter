package com.chaos.scatter;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
	
	// Store context reference
	final Context context = this;
	
	// WiFi manager
	WifiManager manager;
	
	// Broadcast receiver
	Receiver receiver;
	
	// Multicast lock and broadcast address
	MulticastLock multicastLock;
	InetAddress inetAddress;
	
	// A stored list of received signal strengths
	Map<String, Integer> signals = new HashMap<String, Integer>( );
	
	// Ticker management
	Handler handler;
	Runnable runnable;
	
	// Canvas view reference
	SurfaceView surface;
	
	// Painter
	Paint paint;
	
	@Override protected void onCreate( Bundle savedInstanceState ) {
		
		// Call parent method
		super.onCreate( savedInstanceState );
		
		// Hide title view
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		getWindow( ).setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
		
		// Set the layout view
		setContentView( R.layout.activity_main );
        
		// Set references
		manager = (WifiManager)getSystemService( Context.WIFI_SERVICE );
		surface = (SurfaceView)findViewById( R.id.surface );
		
		// Get broadcast address
		try {
			
			DhcpInfo dhcp = manager.getDhcpInfo( );
			int broadcast = ( dhcp.ipAddress & dhcp.netmask ) | ~dhcp.netmask;
			byte[] quads = new byte[4];
		
			for ( int k = 0; k < 4; k++ )
				quads[k] = (byte)( ( broadcast >> k * 8 ) & 0xFF);
			inetAddress = InetAddress.getByAddress( quads );
		    
		}
		
		// Handle exception
		catch ( Exception exception ) { exception.printStackTrace( ); }
		
		// Acquire multicast lock
		multicastLock = manager.createMulticastLock( "scatter" );
		multicastLock.setReferenceCounted( Boolean.TRUE );
		multicastLock.acquire( );
		
		// Set the painter
		paint = new Paint( Paint.ANTI_ALIAS_FLAG );
		paint.setColor( Color.GREEN );
		paint.setStyle( Paint.Style.STROKE );
		paint.setStrokeWidth( (float)2.0 );
		paint.setTextSize( 14 );
		
        // Create new Receiver
        receiver = new Receiver( );
        
        // Create new intent filter for the receiver and add action
        IntentFilter intentFilter = new IntentFilter( );
        intentFilter.addAction( WifiManager.SCAN_RESULTS_AVAILABLE_ACTION );
        
        // Register the receiver
        registerReceiver( receiver, intentFilter );
        
        // Begin scanning
        manager.startScan( );
        
        // Create a runnable to send the network pulse
        runnable = new Runnable( ) {
			@Override public void run( ) {
				
				// Call pulse
				pulse( context );
				
				// Repeat runnable
				handler.postDelayed( runnable, 1000 );

			}
        };
        
        // Register the ticker to send pulse signals
        handler = new Handler( );
        handler.postDelayed( runnable, 1000 );
        
	}
	
	// Check for WiFi on application resume
	protected void onResume( ) {
		
		// Call parent method
		super.onResume( );
		
        // Show a dialog to prompt the user to enable the WiFi
        if ( manager.isWifiEnabled( ) != Boolean.TRUE )
        	new AlertDialog.Builder( this )
            	.setTitle( "This application uses WiFi" )
                .setMessage( "Would you like to enable WiFi on your device?" )
                .setPositiveButton( "Yes", new DialogInterface.OnClickListener( ) {
                	public void onClick( DialogInterface dialog, int which )
                    	{ startActivity( new Intent( Settings.ACTION_WIFI_SETTINGS ) ); }
                } )
                .setCancelable( Boolean.FALSE )
                .show( );
        
	}
	
	// Draw the access points on the canvas
	protected void draw( ) {
		
		// Check surface exists
		if ( surface == null ) return;
		
		// Get the surface holder and lock the canvas
		Canvas canvas = surface.getHolder( ).lockCanvas( );
		
		// Check for surface lock
		if ( canvas == null ) return;
		
        // Clear the canvas
        canvas.drawColor( Color.BLACK );
        
		// Get the maximum visible radius
		int maximum = canvas.getWidth( ) > canvas.getHeight( ) ? canvas.getHeight( ) : canvas.getWidth( );
		maximum = maximum / 2;
		
		// Iterate over hash map to find the ceiling value
		int ceiling = 0;
		for ( Integer value : signals.values( ) )
			if ( value > ceiling ) ceiling = value;
		
		// Now iterate again and draw values as circles
		@SuppressWarnings( "rawtypes" )
		Iterator entries = (Iterator)signals.entrySet( ).iterator( );
		
		while( entries.hasNext( ) ) {
			
			// Calculate radius
			@SuppressWarnings( "unchecked" )
			Entry<String, Integer> entry = ( Entry<String, Integer> )entries.next( );
			float radius = ( (float)entry.getValue( ) / (float)ceiling ) * (float)maximum;
			
			// Draw the received signal strength
			canvas.drawCircle( canvas.getWidth( ) / 2, canvas.getHeight( ) / 2, radius, paint );
			
			// Draw the SSID
			canvas.drawText( entry.getKey( ), ( canvas.getWidth( ) / 2 ) + radius + 5, ( canvas.getHeight( ) / 2 ) + ( radius / 3 ), paint );
			
		}
		
		// Release the canvas
		surface.getHolder( ).unlockCanvasAndPost( canvas );
		
	}
	
	// Broadcast receiver registers received signal strength
	private class Receiver extends BroadcastReceiver {

        @Override public void onReceive( Context context, Intent intent ) {
        	
        	// Finish if WiFi is disabled
        	if ( manager.isWifiEnabled( ) == Boolean.FALSE ) return;
        	
        	// Get scan results
        	List<ScanResult> results = manager.getScanResults( );
        	
        	// Iterate over scan results and store the received signal strength
        	for( int i = 0; i < results.size( ); i++ )
        		signals.put( results.get( i ).SSID,  -results.get( i ).level );
        	
        	// Draw the canvas
        	draw( );
        	
        	// Repeat the scan
        	manager.startScan( );
        	
        }

	}
	
	// Pulse signal strengths to receiving devices
	private void pulse( Context context ) {

		// Create a datagram socket
		DatagramSocket socket = new DatagramSocket( 8000 );
		socket.setBroadcast(true);
		
		
		// Send packets via socket
		//DatagramPacket packet = new DatagramPacket( signals.getBytes( ), data.length(), getBroadcastAddress( ), DISCOVERY_PORT );
		//socket.send( packet );

		// Receive and update
		//byte[] buffer = new byte[1024];
		//DatagramPacket packet = new DatagramPacket( buffer, buffer.length );
		//socket.receive( packet );

	}
	
}