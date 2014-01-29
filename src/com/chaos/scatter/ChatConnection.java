package com.chaos.scatter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ChatConnection {

    private Handler updateHandler;
    private ChatServer chatServer;
    private ChatClient chatClient;

    private static final String TAG = "ChatConnection";

    private Socket socket;
    private int port = -1;

    public ChatConnection( Handler handler ) {
        updateHandler = handler;
        chatServer = new ChatServer( handler );
    }

    public void tearDown( ) {
        chatServer.tearDown( );
        chatClient.tearDown( );
    }

    public void connectToServer( InetAddress address, int port ) {
        chatClient = new ChatClient( address, port );
    }

    public void sendMessage( String message ) {
        if ( chatClient != null ) chatClient.sendMessage( message );
    }
    
    public int getPort( ) { return port; }    
    public void setPort( int port ) { this.port = port; }

    public synchronized void updateMessages( String string, boolean local ) {
    	
        Log.e( TAG, "Updating message: " + string );
        if ( local ) string = "me: " + string;
        else string = "them: " + string;

        Bundle messageBundle = new Bundle( );
        messageBundle.putString( "msg", string );

        Message message = new Message( );
        message.setData( messageBundle );
        updateHandler.sendMessage( message );

    }

    private Socket getSocket( ) { return socket; }
    private synchronized void setSocket( Socket socket ) {
    	
        Log.d( TAG, "setSocket being called." );
        if ( socket == null ) Log.d( TAG, "Setting a null socket." );

        if ( socket != null && socket.isConnected( ) ) try { socket.close( ); }
        catch ( Exception exception ) { exception.printStackTrace( ); }

        this.socket = socket;
        
    }


    private class ChatServer {
    	
        ServerSocket serverSocket = null;
        Thread thread = null;

        public ChatServer( Handler handler ) {
            thread = new Thread( new ServerThread( ) );
            thread.start( );
        }

        public void tearDown( ) {
            thread.interrupt();
            try { serverSocket.close( ); }
            catch ( Exception exception ) { Log.e( TAG, "Error when closing server socket." ); }
        }

        class ServerThread implements Runnable {

            @Override public void run( ) {

                try {
                	
                    // Since discovery will happen via Nsd, we don't need to care which port is used.
                	// Just grab an available one  and advertise it via network service discovery.
                	
                    serverSocket = new ServerSocket( 0 );
                    setPort( serverSocket.getLocalPort( ) );
                    
                    while ( !Thread.currentThread( ).isInterrupted( ) ) {
                        Log.d( TAG, "ServerSocket Created, awaiting connection" );
                        setSocket( serverSocket.accept( ) );
                        Log.d( TAG, "Connected." );
                        if ( chatClient == null) {
                            int port = socket.getPort( );
                            InetAddress address = socket.getInetAddress( );
                            connectToServer( address, port );
                        }
                    }
                    
                }
                
                catch ( Exception exception ) {
                    Log.e( TAG, "Error creating ServerSocket: ", exception );
                    exception.printStackTrace( );
                }
                
            }
        }
    }

    private class ChatClient {

        private InetAddress inetAddress;
        private int port;

        private final String CLIENT_TAG = "ChatClient";

        private Thread sendThread;
        private Thread recThread;

        public ChatClient( InetAddress address, int port ) {

            Log.d( CLIENT_TAG, "Creating chatClient" );
            this.inetAddress = address;
            this.port = port;

            sendThread = new Thread( new SendingThread( ) );
            sendThread.start( );
            
        }

        class SendingThread implements Runnable {

            BlockingQueue<String> messageQueue;
            private int capacity = 10;

            public SendingThread( ) {
                messageQueue = new ArrayBlockingQueue<String>( capacity );
            }

            @Override public void run( ) {
                try {
                	if ( getSocket( ) == null ) {
                		setSocket( new Socket( inetAddress, port ) );
                        Log.d( CLIENT_TAG, "Client-side socket initialized." );

                    }
                	
                	else Log.d( CLIENT_TAG, "Socket already initialized. skipping!" );

                    recThread = new Thread( new ReceivingThread( ) );
                    recThread.start( );

                }
                
                catch ( Exception exception ) { Log.d( CLIENT_TAG, "Initializing socket failed", exception ); }


                while ( true ) {
                    try { sendMessage( messageQueue.take( ) ); }
                    catch ( Exception exception ) { Log.d( CLIENT_TAG, "Message sending loop interrupted, exiting" ); }
                }
            }
        }

        class ReceivingThread implements Runnable {

            @Override public void run( ) {

                BufferedReader input;
                try {
                    input = new BufferedReader( new InputStreamReader( socket.getInputStream( ) ) );
                    while ( !Thread.currentThread( ).isInterrupted( ) ) {

                        String line = null;
                        line = input.readLine( );
                        if ( line != null ) {
                            Log.d( CLIENT_TAG, "Read from the stream: " + line );
                            updateMessages( line, false );
                        }
                        
                        else {
                            Log.d( CLIENT_TAG, "The nulls! The nulls!" );
                            break;
                        }
                        
                    }
                    
                    input.close( );

                }
                
                catch ( Exception exception ) { Log.e(CLIENT_TAG, "Server loop error: ", exception ); }
                
            }
        }

        public void tearDown( ) {
            try { getSocket( ).close( ); }
            catch ( Exception exception ) { Log.e( CLIENT_TAG, "Error when closing server socket." ); }
        }

        public void sendMessage( String message ) {
        	
            try {
                Socket socket = getSocket( );
                if ( socket == null ) Log.d( CLIENT_TAG, "Socket is null, wtf?" );
                else if ( socket.getOutputStream( ) == null) Log.d( CLIENT_TAG, "Socket output stream is null, wtf?" );

                PrintWriter out = new PrintWriter( new BufferedWriter( new OutputStreamWriter( getSocket( ).getOutputStream( ) ) ), true );
                out.println( message );
                out.flush( );
                updateMessages( message, true );
            }
            
            catch ( Exception exception ) { Log.d( CLIENT_TAG, "Exception", exception ); }
            Log.d( CLIENT_TAG, "Client sent message: " + message );
            
        }
    }
}
