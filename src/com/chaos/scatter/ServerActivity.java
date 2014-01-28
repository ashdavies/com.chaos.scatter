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

    NsdHelper mNsdHelper;

    private TextView mStatusView;
    private Handler mUpdateHandler;

    public static final String TAG = "NsdChat";

    ChatConnection mConnection;
	
	@Override protected void onCreate( Bundle savedInstanceState ) {
		
		super.onCreate( savedInstanceState);
		setContentView( R.layout.activity_server );
		
		
        mStatusView = (TextView) findViewById(R.id.status);

        mUpdateHandler = new Handler() {
            @Override public void handleMessage( Message message ) {
                addChatLine( message.getData( ).getString( "msg" ) );
            }
        };

        mConnection = new ChatConnection( mUpdateHandler );

        mNsdHelper = new NsdHelper( context );
        mNsdHelper.initializeNsd( );
        
    }

    public void clickAdvertise( View view ) {
    	
        if( mConnection.getLocalPort( ) > -1 )
            mNsdHelper.registerService( mConnection.getLocalPort( ) );
        
        else Log.d( TAG, "ServerSocket isn't bound." );
        
    }

    public void clickDiscover( View view ) {
        mNsdHelper.discoverServices( );
    }

    public void clickConnect( View view ) {
    	
        NsdServiceInfo service = mNsdHelper.getChosenServiceInfo( );
        if ( service != null ) {
            Log.d( TAG, "Connecting." );
            mConnection.connectToServer( service.getHost( ), service.getPort( ) );
        }
        
        else Log.d( TAG, "No service to connect to!" );

    }

    public void clickSend( View view ) {
    	
        EditText messageView = (EditText)findViewById( R.id.chatInput );
        if ( messageView != null ) {
            String messageString = messageView.getText( ).toString( );
            if ( !messageString.isEmpty( ) )
                mConnection.sendMessage( messageString );

            messageView.setText( "" );
        }
        
    }

    public void addChatLine( String line ) {
        mStatusView.append( "\n" + line );
    }

    @Override protected void onPause( ) {
        if ( mNsdHelper != null ) mNsdHelper.stopDiscovery( );
        super.onPause( );
    }
    
    @Override protected void onResume( ) {
        super.onResume( );
        if ( mNsdHelper != null ) mNsdHelper.discoverServices( );
    }
    
    @Override protected void onDestroy( ) {
        mNsdHelper.tearDown( );
        mConnection.tearDown( );
        super.onDestroy( );
    }
    
}