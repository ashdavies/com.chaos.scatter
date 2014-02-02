package com.chaos.scatter;

import java.util.HashMap;
import java.util.Map;

import com.chaos.scatter.Messenger.Message;
import com.chaos.scatter.Messenger.OnMessageReceived;

import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;

public class ServerActivity extends Activity {
	
	// Final reference to self context
	final private Context context = this;
	
	// Stores signals from connected clients
	Map<String, Map<String, Integer>> signals = new HashMap<String, Map<String, Integer>>( );

	// Canvas view reference
	SurfaceView surface;

	// Painter
	Paint paint;
	
	// UDP Messenger protocol
	Messenger messenger;
	
	// Runnable and handler
	Handler handler = new Handler( );
	Runnable runnable = new Runnable( ) {
		@Override public void run( ) {
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
		surface = (SurfaceView)findViewById( R.id.surface );

		// Set the painter
		paint = new Paint( Paint.ANTI_ALIAS_FLAG );
		paint.setColor( Color.GREEN );
		paint.setStyle( Paint.Style.STROKE );
		paint.setStrokeWidth( (float)2.0 );
        
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
		
    }
	
}