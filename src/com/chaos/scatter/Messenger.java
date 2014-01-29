package com.chaos.scatter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public abstract class Messenger {
	
	// WiFi manager
	WifiManager wifiManager;
	
	// Buffer size represents 4kB
	protected static final Integer BUFFER_SIZE = 4096;
	
	// Port number
	protected int port;
	
	// Receiving mode
	private boolean receiveMessages = false;
	
	// Context and socket
	protected Context context;
	private DatagramSocket socket;
	
	protected abstract Runnable getIncomingMessageAnalyseRunnable( );
	private final Handler incomingMessageHandler;
	protected Message incomingMessage;
	private Thread receiverThread;
	
	/**
	 * Class constructor
	 * @param context the application's context
	 * @param port the port to multi cast to. Must be between 1025 and 49151 (inclusive)
	 */
	public Messenger( Context context, int port ) throws IllegalArgumentException {
		
		// Store WiFi manager service
		wifiManager = (WifiManager)context.getSystemService( Context.WIFI_SERVICE );
		
		if ( context == null || port <= 1024 || port > 49151 )
			throw new IllegalArgumentException( );
		
		this.context = context.getApplicationContext( );
		this.port = port;
		
		incomingMessageHandler = new Handler( Looper.getMainLooper( ) );

	}
	
	/**
	 * Message Class
	 */
	public class Message {
		
		private String tag;
		private String message;
		private long time = 0;
		private InetAddress inetAddress;
		
		public Message( String message ) throws IllegalArgumentException {
			this( message, (InetAddress)null );
		}
		
		public Message( String message, InetAddress inetAddress ) throws IllegalArgumentException {
			
			String split[] = message.split( " " );
			if ( split.length < 3 ) throw new IllegalArgumentException( );
			
			this.tag = split[ 0 ];
			this.time = Integer.parseInt( split[ 1 ] );
			this.inetAddress = inetAddress;
			
			this.message = "";
			for ( int i = 2; i < split.length - 1; i++ )
				this.message += message.concat( split[ i ] + " " );
			
		}
		
		public Message( String tag, String message ) {
			this( tag, message, null );
		}
		
		public Message( String tag, String message, InetAddress inetAddress ) {
			this( tag, message, inetAddress, System.currentTimeMillis( ) / 1000 );
		}
		
		public Message( String tag, String message, InetAddress inetAddress, long time) {
			this.tag = tag;
			this.message = message;
			this.time = time;
			this.inetAddress = inetAddress;
		}
		
		public String getTag( ) { return tag; }
		public String getMessage( ) { return message; }
		public long getEpochTime( ) { return time; }
		public InetAddress getSrcIp( ) { return inetAddress; }
		
		public String toString( ) { return tag + " " + time + " " + message; }
		
	}
	
	/**
	 * Sends a broadcast message (TAG EPOCH_TIME message). Opens a new socket in case it's closed.
	 * @param message the message to send. It can't be null or 0-characters long.
	 * @throws IllegalArgumentException
	 */
	public boolean send( String string ) throws IllegalArgumentException {
		
		// Validate message
		if ( string == null || string.length( ) == 0 ) throw new IllegalArgumentException();
	
		// Check for IP address
		int ipAddress = wifiManager.getConnectionInfo( ).getIpAddress( );
		if ( ipAddress <= 0 ) return Boolean.FALSE;
		
		// Create the send socket
		if ( socket == null ) {
			try { socket = new DatagramSocket( ); }
			catch ( SocketException exception ) {
				Log.d( getClass( ).getSimpleName( ), "There was a problem creating the sending socket. Aborting." );
				exception.printStackTrace( );
				return Boolean.FALSE;
			}
		}
		
		// Build the packet
		DatagramPacket packet;
		Message message = new Message( getClass( ).getSimpleName( ), string );
		byte data[] = message.toString( ).getBytes( );
		
		// Create the datagram packet
		try { packet = new DatagramPacket( data, data.length, InetAddress.getByName( ipToString( ipAddress, true ) ), port ); }
		catch ( UnknownHostException exception ) {
			Log.d( getClass( ).getSimpleName( ), "It seems that " + ipToString( ipAddress, true ) + " is not a valid ip! Aborting." );
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
				MulticastLock multiCastLock = wifiManager.createMulticastLock( "Scatter" );
				multiCastLock.acquire( );
				
				// Create new datagram packet
				byte[] buffer = new byte[ BUFFER_SIZE ];
				DatagramPacket datagramPacket = new DatagramPacket( buffer, buffer.length );
				MulticastSocket multiCastSocket;
				
				// Create new multicast socket
				try { multiCastSocket = new MulticastSocket( port ); }
				catch ( IOException exception ) {
					Log.d( getClass( ).getSimpleName( ), "Impossible to create a new MulticastSocket on port " + port );
					exception.printStackTrace( );
					return;
				}
				
				// Process receiving messages
				while ( receiveMessages ) {
					
					try { multiCastSocket.receive( datagramPacket ); }
					catch ( IOException exception ) {
						Log.d( getClass( ).getSimpleName( ), "There was a problem receiving the incoming message." );
						exception.printStackTrace( );
						continue;
					}
					
					if( !receiveMessages ) break;
					
					// Check for end of file
					byte data[] = datagramPacket.getData( ); int i;
					for ( i = 0; i < data.length; i++ ) if ( data[ i ] == '\0' ) break;
					
					// Create message
					String message;
					try { message = new String( data, 0, i, "UTF-8" ); }
					catch ( UnsupportedEncodingException exception ) {
						Log.d( getClass( ).getSimpleName( ), "UTF-8 encoding is not supported. Can't receive the incoming message." );
						exception.printStackTrace( );
						continue;
					}
					
					try { incomingMessage = new Message( message, datagramPacket.getAddress( ) ); }
					catch ( IllegalArgumentException exception ) {
						Log.d( getClass( ).getSimpleName( ), "There was a problem processing the message: " + message );
						exception.printStackTrace( );
						continue;
					}
					
					incomingMessageHandler.post( getIncomingMessageAnalyseRunnable( ) );
					
				}
				
				// Close the multicast socket
				multiCastSocket.close( );
				
			}
			
		};
		
		// Create new receiver thread
		receiveMessages = true;
		if ( receiverThread == null ) receiverThread = new Thread( receiver );
		if ( !receiverThread.isAlive( ) ) receiverThread.start( );
		
	}
	
	public void stopReceiver() {
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