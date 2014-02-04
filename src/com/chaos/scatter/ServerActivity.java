package com.chaos.scatter;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.chaos.scatter.Messenger.Message;
import com.chaos.scatter.Messenger.OnMessageReceived;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

public class ServerActivity extends Activity {
	
	// Final reference to self context
	final private Context context = this;
	
	// WiFi manager
	WifiManager wifiManager;

	// Broadcast receiver
	Receiver receiver;
	
	// Stores signals from connected clients
	Map<String, Map<String, Integer>> signals = new HashMap<String, Map<String, Integer>>( );

	// Stores anchor and point references
	Map<String, Point> points = new HashMap<String, Point>( );
	Map<String, Point> anchors = new HashMap<String, Point>( );
	
	// Canvas view reference
	SurfaceView surface;

	// Painter
	Paint dark;
	Paint light;
	
	// UDP Messenger protocol
	Messenger messenger;
	
	// Runnable and handler
	Handler handler = new Handler( );
	Runnable runnable = new Runnable( ) {
		@Override public void run( ) {
			
			// Get screen size
			Display display = ( (Activity)context ).getWindowManager( ).getDefaultDisplay( );
			Point screen = new Point( );
			display.getSize( screen );
			
			// Begin iterating
			Iterator<Entry<String, Map<String, Integer>>> signalPointIterator = signals.entrySet( ).iterator( );
			while( signalPointIterator.hasNext( ) ) {
				
				// Get entry and anchor iterator
				Entry<String, Map<String, Integer>> signalPointEntry = signalPointIterator.next( );
				Iterator<Entry<String, Integer>> signalAnchorIterator = ( (Map<String, Integer>)signalPointEntry.getValue( ) ).entrySet( ).iterator( );
				
				// Check point exists
				Point point;
				if ( !points.containsKey( signalPointEntry.getKey( ) ) ) {
					
					// Create new point
					point = new Point(
						new Random( ).nextInt( (int)( screen.x * 0.9 ) ) + (int)( screen.x * 0.1 ),
						new Random( ).nextInt( (int)( screen.y * 0.9 ) ) + (int)( screen.y * 0.1 )
					);
					
					// Add to the points
					points.put( signalPointEntry.getKey( ), point );
					
				}
				
				else point = points.get( signalPointEntry.getKey( ) );
				
				// Iterate over anchors
				while( signalAnchorIterator.hasNext( ) ) {
					
					// Get anchor point
					Map.Entry<String, Integer> signalAnchorEntry = (Map.Entry<String, Integer>)signalAnchorIterator.next( );
					int signal = -signalAnchorEntry.getValue( );
					
					// Calculate amplitude
					int amplitude = (int)( ( (float)signal / (float)80 ) * ( Math.PI * 2 ) );
					
					// Calculate anchor destination
					Point anchorDestination = new Point(
						(int)( point.x + ( signal * 5 ) * Math.cos( amplitude ) ),
						(int)( point.y + ( signal * 5 ) * Math.sin( amplitude ) )
					);
					
					// Put the new anchor
					//if ( !anchors.containsKey( signalAnchorEntry.getKey( ) ) )
						anchors.put( signalAnchorEntry.getKey( ), anchorDestination );
					
				}
				
			}
			
			// Check for a surface
			if ( surface == null ) return;
			
			// Get the surface holder and lock the canvas
			Canvas canvas = surface.getHolder( ).lockCanvas( );
			if ( canvas == null ) return;

	        // Clear the canvas
	        canvas.drawColor( Color.BLACK );
			
			// Iterate and draw points
			Iterator<Entry<String, Point>> pointIterator = points.entrySet( ).iterator( );			
			while( pointIterator.hasNext( ) ) {
				Entry<String, Point> pointEntry = pointIterator.next( );
				Point point = pointEntry.getValue( );
				canvas.drawRect( point.x - 15, point.y - 15, point.x + 15, point.y + 15, light );
			}
			
			// Iterate and draw anchor
			Iterator<Entry<String, Point>> anchorIterator = anchors.entrySet( ).iterator( );
			while( anchorIterator.hasNext( ) ) {
				
				// Get anchor point
				Entry<String, Point> anchorEntry = anchorIterator.next( );
				Point point = anchorEntry.getValue( );
				
				// Draw circle
				canvas.drawCircle( point.x, point.y, 10, light );
				
				// Get origin
				Entry<String, Point> originPoint = points.entrySet( ).iterator( ).next( );
				Point origin = originPoint.getValue( );

				// Get individual signal
				Map<String, Integer> signal = signals.get( originPoint.getKey( ) );
				if ( signal.containsKey( anchorEntry.getKey( ) ) ) {
				
					int level = -signal.get( anchorEntry.getKey( ) );
					canvas.drawLine( origin.x, origin.y, point.x, point.y, light );
					canvas.drawCircle( origin.x, origin.y, level * 5, dark );
					
				}
			}
			
			// Unlock canvas and post
			surface.getHolder( ).unlockCanvasAndPost( canvas );
			
			// Repost posting runnable
			handler.postDelayed( runnable, 1000 );
			
		}
	};
	
	@Override protected void onCreate( Bundle savedInstanceState ) {
		
		// Call parent method
		super.onCreate( savedInstanceState );

		// Hide title view
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		getWindow( ).setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );

		// Set the layout view
		setContentView( R.layout.activity_server );
		
		// Set references
		wifiManager = (WifiManager)getSystemService( Context.WIFI_SERVICE );
		surface = (SurfaceView)findViewById( R.id.surface );

		// Set the dark painter
		dark = new Paint( Paint.ANTI_ALIAS_FLAG );
		dark.setColor( Color.DKGRAY );
		dark.setStyle( Paint.Style.STROKE );
		dark.setStrokeWidth( (float)2.0 );

		// Set the light painter
		light = new Paint( Paint.ANTI_ALIAS_FLAG );
		light.setColor( Color.WHITE );
		light.setStyle( Paint.Style.STROKE );
		light.setStrokeWidth( (float)2.0 );
        
		// Create a new messenger
		messenger = new Messenger( context, "224.0.0.0", 9000 );
		
		// Register message listener
		messenger.setOnMessageReceived( new OnMessageReceived( ) {
			@Override public void messageReceived( Message message ) {
				
				// Parse the string
				String[] parsed = message.getMessage( ) 
					.substring( 0, message.getMessage( ).indexOf( ";" ) )
					.split( "," );

				// Create new signal entry
				Map<String, Integer> entry = new HashMap<String, Integer>( );
				for( String signal : parsed ) {
					String[] split = signal.split( ":" );
					entry.put( split[ 0 ], Integer.valueOf( split[ 1 ] ) );
				}

				
				// Delete any existing key by the host name
				if ( signals.containsKey( message.getSource( ).getHostName( ) ) )
					signals.remove( message.getSource( ).getHostName( ) );
				
				// Add new key
				signals.put( message.getSource( ).getHostName( ), entry );
				
			}		
		} );
		
		// Start receiving datagram packets
		messenger.startReceiver( );
		
        // Create new Receiver
        receiver = new Receiver( );
        
        // Create new intent filter for the receiver and add action
        IntentFilter intentFilter = new IntentFilter( );
        intentFilter.addAction( WifiManager.SCAN_RESULTS_AVAILABLE_ACTION );
        
        // Register the receiver
        registerReceiver( receiver, intentFilter );
        
        // Begin scanning
        wifiManager.startScan( );
		
		// Begin posting runnable
		handler.postDelayed( runnable, 1000 );
		
    }

	// Broadcast receiver registers received signal strength
	private class Receiver extends BroadcastReceiver {
        @Override public void onReceive( Context context, Intent intent ) {
        	
        	// Finish if WiFi is disabled
        	if ( wifiManager.isWifiEnabled( ) == Boolean.FALSE ) return;
        	
        	// Get scan results
        	List<ScanResult> results = wifiManager.getScanResults( );
        	
        	// Create entry for local signals
        	Map<String, Integer> entry = new HashMap<String, Integer>( );
        	
        	// Iterate over scan results and push ping over network
        	for( int i = 0; i < results.size( ); i++ ) {
        		
        		// Get result item
        		ScanResult signal = results.get( i );
        
        		// Only process if signal is high enough
        		if ( signal.level < -80 ) continue;
        		
        		// Add signal to entry map
        		entry.put( signal.SSID, signal.level );
        		
        	}
        	
        	String hostAddress = "localhost";
        	try { hostAddress = InetAddress.getLocalHost( ).getHostAddress( ); }
        	catch( Exception exception ) { }
        	
    		// Add the signal to the array list
    		signals.put( hostAddress, entry );
        	
        	// Repeat the scan
        	wifiManager.startScan( );
        	
        }
	}
}