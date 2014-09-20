package com.bobamason.openglply;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity
{

	private GLSurface surface;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
		surface = (GLSurface) findViewById(R.id.surface);
    }

	@Override
	protected void onPause()
	{
		surface.onPause();
		super.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		surface.onResume();
	}
	
}
