package com.chaos.scatter;

import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.app.Activity;
import android.content.Context;

public class ServerActivity extends Activity {
	
	// Final reference to self context
	final private Context context = this;

	// Network service discovery helper
	NSDHelper nsdHelper;

    // Status text view
    private TextView statusTextView;
    
    // Update handler
    private Handler updateHandler;

    ChatConnection chatConnection;
	
	@Override protected void onCreate( Bundle savedInstanceState ) {
		
		// Set the activity layout
		super.onCreate( savedInstanceState);
		setContentView( R.layout.activity_server );
		
		// Set the status text view
		statusTextView = (TextView)findViewById( R.id.status );

		// Create an update handler to add chat line
		updateHandler = new Handler( ) {
            @Override public void handleMessage( Message message ) {
                addChatLine( message.getData( ).getString( "msg" ) );
            }
        };

        // Create new chat connection with the update handler
        chatConnection = new ChatConnection( updateHandler );

        // Create the network service helper and initialise
        nsdHelper = new NSDHelper( context );
        nsdHelper.initialise( );
        
    }

	/**
	 * Register the service
	 */
    public void clickAdvertise( View view ) {
    	
        if( chatConnection.getPort( ) > -1 )
        	nsdHelper.registerService( chatConnection.getPort( ) );
        
        else Log.d( getClass( ).getSimpleName( ), "ServerSocket isn't bound." );
        
    }

    /**
     * Discover the network services
     */
    public void clickDiscover( View view ) {
    	nsdHelper.discoverServices( );
    }

    public void clickConnect( View view ) {
    	
        NsdServiceInfo service = nsdHelper.getChosenServiceInfo( );
        if ( service != null ) {
            Log.d( getClass( ).getSimpleName( ), "Connecting." );
            chatConnection.connectToServer( service.getHost( ), service.getPort( ) );
        }
        
        else Log.d( getClass( ).getSimpleName( ), "No service to connect to!" );

    }

    public void clickSend( View view ) {
    	
        EditText messageView = (EditText)findViewById( R.id.chatInput );
        if ( messageView != null ) {
            String messageString = messageView.getText( ).toString( );
            if ( !messageString.isEmpty( ) )
                chatConnection.sendMessage( messageString );

            messageView.setText( "" );
        }
        
    }

    public void addChatLine( String line ) {
        statusTextView.append( "\n" + line );
    }

    @Override protected void onPause( ) {
        if ( nsdHelper != null ) nsdHelper.stopDiscovery( );
        super.onPause( );
    }
    
    @Override protected void onResume( ) {
        super.onResume( );
        if ( nsdHelper != null ) nsdHelper.discoverServices( );
    }
    
    @Override protected void onDestroy( ) {
    	nsdHelper.tearDown( );
    	nsdHelper.tearDown( );
        super.onDestroy( );
    }
    
}