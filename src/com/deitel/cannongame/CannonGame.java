package com.deitel.cannongame;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.deitel.cannongame.CannonView.InterListener;

public class CannonGame extends Activity{

	private GestureDetector gestureDetector; // listens for double taps
	private CannonView cannonView; // custom view to display the game
	private View mainView;

	// called when the app first launches
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState); // call super's onCreate method
		setContentView(R.layout.main); // inflate the layout

		// get the CannonView
		cannonView = (CannonView) findViewById(R.id.cannonView);
		cannonView.setInter(new InterListener() {
			
			@Override
			public void onClick() {
				cannonView.stopGame();
				cannonView.setVisibility(View.GONE);
				mainView.setVisibility(View.VISIBLE);	
			}
		});

		mainView = findViewById(R.id.mainView);

		// initialize the GestureDetector
		gestureDetector = new GestureDetector(this, gestureListener);

		// allow volume keys to set game volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	} // end method onCreate

	// when the app is pushed to the background, pause it
	@Override
	public void onPause(){
		super.onPause(); // call the super method
		cannonView.stopGame(); // terminates the game
	} // end method onPause

	// release resources
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		cannonView.releaseResources();
	} // end method onDestroy

	// called when the user touches the screen in this Activity
	@Override
	public boolean onTouchEvent(MotionEvent event){
		// get int representing the type of action which caused this event
		int action = event.getAction();

		// the user user touched the screen or dragged along the screen
		if (action == MotionEvent.ACTION_DOWN ||
				action == MotionEvent.ACTION_MOVE)
		{
			cannonView.alignCannon(event); // align the cannon
		} // end if

		// call the GestureDetector's onTouchEvent method
		return gestureDetector.onTouchEvent(event);
	} // end method onTouchEvent

	// listens for touch events sent to the GestureDetector
	SimpleOnGestureListener gestureListener = new SimpleOnGestureListener(){
		@Override
		public boolean onDown(MotionEvent e) {

			if (cannonView.getVisibility() == View.VISIBLE) {
				cannonView.fireCannonball(e); // fire the cannonball
			}
			return true; // the event was handled 

		};
		/*   // called when the user double taps the screen
      @Override
      public boolean onDoubleTap(MotionEvent e)
      {
         cannonView.fireCannonball(e); // fire the cannonball
         return true; // the event was handled
      } // end method onDoubleTap
		 */   
	}; // end gestureListener

	public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			if (cannonView.getVisibility() == View.VISIBLE) {
				cannonView.stopGame();
				cannonView.setVisibility(View.GONE);
				mainView.setVisibility(View.VISIBLE);
				return false;
			}else{
				return super.onKeyDown(keyCode, event);
			}
		}
		return false;
	};
	
	public void click1(View v) {
		cannonView.setVisibility(View.VISIBLE);
		mainView.setVisibility(View.GONE);
		cannonView.setLevels(0);
	}
	public void click2(View v) {
		cannonView.setVisibility(View.VISIBLE);
		mainView.setVisibility(View.GONE);
		cannonView.setLevels(1);
	}

} // end class CannonGame

