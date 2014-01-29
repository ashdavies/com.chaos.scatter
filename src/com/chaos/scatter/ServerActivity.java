package com.chaos.scatter;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;

public class ServerActivity extends Activity {
	
	// Final reference to self context
	final private Context context = this;
	
	// UDP Messenger protocol
	Messenger messenger;
	
	@Override protected void onCreate( Bundle savedInstanceState ) {
		
		// Set the activity layout
		super.onCreate( savedInstanceState);
		setContentView( R.layout.activity_server );
        
		// Create a new messenger
		// https://github.com/elegos/udpmulticast
		messenger = new Messenger( context, 0 ) {

			@Override protected Runnable getIncomingMessageAnalyseRunnable( ) {
				return null;
			}
			
		}
		
    }
	
}