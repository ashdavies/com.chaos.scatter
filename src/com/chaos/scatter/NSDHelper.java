package com.chaos.scatter;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.util.Log;

public class NSDHelper {

	// Context storage
    Context context;

    // Network service discovery manager storage
    NsdManager nsdManager;
    NsdManager.ResolveListener resolveListener;
    NsdManager.DiscoveryListener discoveryListener;
    NsdManager.RegistrationListener registrationListener;

    // Service type definition
    public static final String SERVICE_TYPE = "_http._tcp.";

    public static final String TAG = "NsdHelper";
    public String serviceName = "Scatter";

    // Service information
    NsdServiceInfo nsdServiceInfo;

    public NSDHelper( Context context ) {
    	this.context = context;
    	nsdManager = (NsdManager)context.getSystemService( Context.NSD_SERVICE );
    }

    public void initialise( ) {

    	// Initialise the discovery listener service
        discoveryListener = new NsdManager.DiscoveryListener( ) {

        	// On discovery started
            @Override public void onDiscoveryStarted( String serviceType ) {
                Log.d( TAG, "Service discovery started" );
            }

            // On service found
            @Override public void onServiceFound( NsdServiceInfo serviceInfo ) {
            	
                Log.d( TAG, "Service discovery success" + serviceInfo );
                if ( !serviceInfo.getServiceType( ).equals( SERVICE_TYPE ) )
                    Log.d( TAG, "Unknown Service Type: " + serviceInfo.getServiceType( ) );
                
                else if ( serviceInfo.getServiceName( ).equals( serviceName ) )
                    Log.d( TAG, "Same machine: " + serviceName );
                    
                else if ( serviceInfo.getServiceName( ).contains( serviceName ) )
                    nsdManager.resolveService( serviceInfo, resolveListener );

            }

            // On service lost
            @Override public void onServiceLost( NsdServiceInfo serviceInfo ) {
                Log.e( TAG, "service lost" + serviceInfo );
                if ( nsdServiceInfo == serviceInfo ) nsdServiceInfo = null;
            }
            
            // On discovery stopped
            @Override public void onDiscoveryStopped( String serviceType ) {
                Log.i( TAG, "Discovery stopped: " + serviceType );        
            }

            // On start discovery failed
            @Override public void onStartDiscoveryFailed( String serviceType, int errorCode ) {
                Log.e( TAG, "Discovery failed: Error code:" + errorCode );
                nsdManager.stopServiceDiscovery( this );
            }

            // On stop discovery failed
            @Override public void onStopDiscoveryFailed( String serviceType, int errorCode ) {
                Log.e( TAG, "Discovery failed: Error code:" + errorCode );
                nsdManager.stopServiceDiscovery( this );
            }
        };

        // Initialise the resolve listener service
        resolveListener = new NsdManager.ResolveListener( ) {

            @Override public void onResolveFailed( NsdServiceInfo serviceInfo, int errorCode ) {
                Log.e( TAG, "Resolve failed" + errorCode );
            }

            @Override public void onServiceResolved( NsdServiceInfo serviceInfo ) {
                Log.e( TAG, "Resolve Succeeded. " + serviceInfo );

                if ( serviceInfo.getServiceName( ).equals( nsdServiceInfo ) ) {
                    Log.d( TAG, "Same IP." );
                    return;
                }
                
                nsdServiceInfo = serviceInfo;
                
            }
        };
        
        // Initialise the registration listener
        registrationListener = new NsdManager.RegistrationListener( ) {

            @Override public void onServiceRegistered(NsdServiceInfo nsdServiceInfo ) {
                serviceName = nsdServiceInfo.getServiceName( );
            }
            
            @Override public void onRegistrationFailed( NsdServiceInfo nsdServiceInfo, int arg1 ) { }

            @Override public void onServiceUnregistered( NsdServiceInfo nsdServiceInfo) { }
            
            @Override public void onUnregistrationFailed( NsdServiceInfo nsdServiceInfo, int errorCode ) { }
            
        };

    }
    
    /**
     * Register the service on a specified port
     */
    public void registerService( int port ) {
        
        // Begin registering the service
        NsdServiceInfo serviceInfo  = new NsdServiceInfo( );
        serviceInfo.setPort( port );
        serviceInfo.setServiceName( serviceName );
        serviceInfo.setServiceType( SERVICE_TYPE );
        
        nsdManager.registerService( serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener );
        
    }

    /**
     * Discover available services
     */
    public void discoverServices( ) {
        nsdManager.discoverServices( SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener );
    }
    
    /**
     * Stop discovering available services
     */
    public void stopDiscovery() { nsdManager.stopServiceDiscovery( discoveryListener ); }

    /**
     * Return service info
     */
    public NsdServiceInfo getChosenServiceInfo( ) { return nsdServiceInfo; }
    
    /**
     * Unregister the service registration listener
     */
    public void tearDown( ) { nsdManager.unregisterService( registrationListener ); }
    
}
