package com.chaos.scatter;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;

public class ClientActivity extends Activity {
	
	final private Context context = this;

	@Override protected void onCreate( Bundle savedInstanceState ) {
		
		super.onCreate( savedInstanceState);
		setContentView( R.layout.activity_client );

	}

}
