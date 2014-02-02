package com.chaos.scatter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.util.Log;

public class Messenger {
	
	// WiFi manager
	WifiManager wifiManager;
	
	// Buffer size represents 4kB
	protected static final Integer BUFFER_SIZE = 4096;
	
	// IP address
	protected String ip;
	
	// Port number
	protected Integer port;
	
	// Receiving mode
	private boolean receiveMessages = false;
	
	// Context and socket
	protected Context context;
	private MulticastSocket socket;
	
	// Message received listeners
	ArrayList<OnMessageReceived> messageReceivedListeners = new ArrayList<OnMessageReceived>( );
	interface OnMessageReceived { void messageReceived( Message message ); }
	
	// Set message received listener
	public void setOnMessageReceived( OnMessageReceived onMessageReceived ) {
		if ( !messageReceivedListeners.contains( onMessageReceived ) ) messageReceivedListeners.add( onMessageReceived );
	}
	
	protected Message incoming;
	private Thread thread;
	
	/**
	 * Class constructor
	 * @param context the application's context
	 * @param port the port to multi cast to. Must be between 1025 and 49151 (inclusive)
	 */
	public Messenger( Context context, String ip, int port ) throws IllegalArgumentException {
		
		// Store WiFi manager service
		wifiManager = (WifiManager)context.getSystemService( Context.WIFI_SERVICE );
		
		if ( context == null || port <= 1024 || port > 49151 )
			throw new IllegalArgumentException( );
		
		this.context = context.getApplicationContext( );
		this.ip = ip;
		this.port = port;

	}
	
	/**
	 * Message Class
	 */
	public class Message {
		
		// Private variables
		private String tag;
		private String message;
		private long time = 0;
		private InetAddress inetAddress;

		/**
		 * Delegate constructors
		 */
		public Message( String message ) throws IllegalArgumentException { this( message, (InetAddress)null ); }
		public Message( String tag, String message ) { this( tag, message, null ); }
		public Message( String tag, String message, InetAddress inetAddress ) { this( tag, message, inetAddress, System.currentTimeMillis( ) / 1000 ); }
		
		// Reforms the message from a parsed string
		public Message( String message, InetAddress inetAddress ) throws IllegalArgumentException {
			
			String[] split = message.split( " " );
			if ( split.length < 3 ) throw new IllegalArgumentException( );
			
			this.tag = split[ 0 ];
			this.time = Integer.parseInt( split[ 1 ] );
			this.inetAddress = inetAddress;
			
			this.message = "";
			for ( int i = 2; i < split.length; i++ )
				this.message += split[ i ] + " ";
			
		}
		
		public Message( String tag, String message, InetAddress inetAddress, long time ) {
			this.tag = tag;
			this.message = message;
			this.time = time;
			this.inetAddress = inetAddress;
		}
		
		// Getter methods
		public String getTag( ) { return tag; }
		public String getMessage( ) { return message; }
		public long getTime( ) { return time; }
		public InetAddress getSource( ) { return inetAddress; }
		
		// Formats the message as a string
		public String toString( ) { return tag + " " + time + " " + message; }
		
	}
	
	/**
	 * Sends a broadcast message (TAG EPOCH_TIME message). Opens a new socket in case it's closed.
	 * @param message the message to send. It can't be null or 0-characters long.
	 * @throws IllegalArgumentException
	 */
	public void send( String string ) throws IllegalArgumentException {
		
		// Create an asynchronous task to send network data
		AsyncTask<String, Void, Boolean> asyncTask = new AsyncTask<String, Void, Boolean>( ) {
			@Override protected Boolean doInBackground( String... messages ) {
				for( String message : messages ) transmit( message );
				return null;
			}
		};
		
		// Execute the task
		asyncTask.execute( string );
		
	}
		
	private boolean transmit( String string ) throws IllegalArgumentException {
		
		// Validate message
		if ( string == null || string.length( ) == 0 ) throw new IllegalArgumentException( );
	
		// Check for IP address
		if ( ip == null ) ip = ipToString( wifiManager.getConnectionInfo( ).getIpAddress( ), true );
		
		// Create the send socket
		if ( socket == null ) {
			try {
				socket = new MulticastSocket( port );
				socket.joinGroup( InetAddress.getByName( ip ) );
			}
			catch ( IOException exception ) {
				Log.d( getClass( ).getSimpleName( ), "Impossible to create a new MulticastSocket on port " + port );
				exception.printStackTrace( );
				return Boolean.FALSE;
			}
		}
		
		// Build the packet
		DatagramPacket packet;
		Message message = new Message( getClass( ).getSimpleName( ), string );
		byte data[] = message.toString( ).getBytes( );
		
		// Create the datagram packet
		try { packet = new DatagramPacket( data, data.length, InetAddress.getByName( ip ), port ); }
		catch ( UnknownHostException exception ) {
			Log.d( getClass( ).getSimpleName( ), "It seems that " + ip + " is not a valid ip! Aborting." );
			exception.printStackTrace( );
			return false;
		}
		
		// Send the datagram packet
		try { socket.send( packet ); }
		catch ( IOException exception ) {
			Log.d( getClass( ).getSimpleName( ), "There was an error sending the UDP packet. Aborted." );
			exception.printStackTrace( );
			return false;
		}
		
		return true;
		
	}
	
	public void startReceiver( ) {
		
		Runnable receiver = new Runnable( ) {
			@Override public void run( ) {
				
				// Acquire multicast lock
				MulticastLock multiCastLock = wifiManager.createMulticastLock( "SCATTER" );
				multiCastLock.acquire( );
				
				// Create new datagram packet
				byte[] buffer = new byte[ BUFFER_SIZE ];
				DatagramPacket datagramPacket = new DatagramPacket( buffer, buffer.length );
				MulticastSocket multiCastSocket;
				
				// Create new multicast socket
				try {
					multiCastSocket = new MulticastSocket( port );
					multiCastSocket.joinGroup( InetAddress.getByName( ip ) );
				}
				
				catch ( IOException exception ) {
					Log.d( getClass( ).getSimpleName( ), "Impossible to create a new MulticastSocket on port " + port );
					exception.printStackTrace( );
					return;
				}
				
				// Process receiving messages
				while ( true ) {

					try { multiCastSocket.receive( datagramPacket ); }
					catch ( Exception exception ) {
						Log.d( getClass( ).getSimpleName( ), "There was a problem receiving the incoming message." );
						exception.printStackTrace( );
						continue;
					}
					
					if ( !receiveMessages ) break;
					
					// Check for end of file
					byte data[] = datagramPacket.getData( ); int i = 0;
					for ( i = 0; i < data.length; i++ ) if ( data[ i ] == '\0' ) break;
					
					// Create message					
					String message;
					try { message = new String( data, 0, i, "UTF-8" ); }
					catch ( UnsupportedEncodingException exception ) {
						Log.d( getClass( ).getSimpleName( ), "UTF-8 encoding is not supported. Can't receive the incoming message." );
						exception.printStackTrace( );
						continue;
					}

					try { incoming = new Message( message, datagramPacket.getAddress( ) ); }
					catch ( IllegalArgumentException exception ) {
						Log.d( getClass( ).getSimpleName( ), "There was a problem processing the message: " + message );
						exception.printStackTrace( );
						continue;
					}

					// Check for a message
					if ( incoming == null ) break;

					// Post to any registered listeners
					for( OnMessageReceived onMessageReceived : messageReceivedListeners )
						onMessageReceived.messageReceived( incoming );
					
					
				}
				
				// Close the multicast socket
				multiCastSocket.close( );
				
			}
			
		};
		
		// Create new receiver thread
		receiveMessages = true;
		if ( thread == null ) thread = new Thread( receiver );
		if ( !thread.isAlive( ) ) thread.start( );
		
	}
	
	public void stopReceiver( ) {
		receiveMessages = false;
	}
	
	public static String ipToString( int ip, boolean broadcast ) {
		
		String result = new String( );
		Integer[] address = new Integer[ 4 ];
		
		for ( int i = 0; i < 4; i++ ) address[ i ] = ( ip >> 8 * i ) & 0xFF;
		for ( int i = 0; i < 4; i++) result = ( i != 3 ? result.concat( address[ i ] + "." ) : result.concat( "255." ) );
		return result.substring( 0, result.length( ) - 2 );
		
	}
}