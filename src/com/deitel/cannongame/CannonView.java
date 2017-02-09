// CannonView.java
// Displays the Cannon Game
package com.deitel.cannongame;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CannonView extends SurfaceView implements SurfaceHolder.Callback
{
	private CannonThread cannonThread; // controls the game loop
	private Activity activity; // to display Game Over dialog in GUI thread
	private boolean dialogIsDisplayed = false;   

	// constants for game play
	public static  int TARGET_PIECES = 5; // sections in the target 目标数量
	public static final int MISS_PENALTY = 2; // seconds deducted on a miss
	public static final int HIT_REWARD = 3; // seconds added on a hit

	// variables for the game loop and tracking statistics
	private boolean gameOver; // is the game over?
	private double timeLeft; // the amount of time left in seconds
	private int shotsFired; // the number of shots the user has fired
	private double totalElapsedTime; // the number of seconds elapsed

	// variables for the blocker and target
	private Line blocker; // start and end points of the blocker
	private int blockerDistance; // blocker distance from left
	private int blockerBeginning; // blocker distance from top
	private int blockerEnd; // blocker bottom edge distance from top
	private int initialBlockerVelocity; // initial blocker speed multiplier
	private float blockerVelocity; // blocker speed multiplier during game

	private Line target; // start and end points of the target
	private int targetDistance; // target distance from left
	private int targetBeginning; // target distance from top
	private double pieceLength; // length of a target piece
	private int targetEnd; // target bottom's distance from top
	private int initialTargetVelocity; // initial target speed multiplier
	private float targetVelocity; // target speed multiplier during game

	private int lineWidth; // width of the target and blocker
	private boolean[] hitStates; // is each target piece hit?
	private int targetPiecesHit; // number of target pieces hit (out of 7)

	// variables for the cannon and cannonball
	private Point cannonball; // cannonball image's upper-left corner
	private int cannonballVelocityX; // cannonball's x velocity
	private int cannonballVelocityY; // cannonball's y velocity
	private boolean cannonballOnScreen; // is the cannonball on the screen
	private int cannonballRadius; // cannonball radius
	private int cannonballSpeed; // cannonball speed
	private int cannonBaseRadius; // cannon base radius
	private int cannonLength; // cannon barrel length
	private Point barrelEnd; // the endpoint of the cannon's barrel
	private int screenWidth; // width of the screen
	private int screenHeight; // height of the screen
	
	private int cannonHeight; 

	// constants and variables for managing sounds
	private static final int TARGET_SOUND_ID = 0;
	private static final int CANNON_SOUND_ID = 1;
	private static final int BLOCKER_SOUND_ID = 2;
	private static final double TIMELEFT = 20; //时间
	private SoundPool soundPool; // plays sound effects
	private Map<Integer, Integer> soundMap; // maps IDs to SoundPool

	// Paint variables used when drawing each item on the screen
	private Paint textPaint; // Paint used to draw text
	private Paint cannonballPaint; // Paint used to draw the cannonball
	private Paint cannonPaint; // Paint used to draw the cannon
	private Paint blockerPaint; // Paint used to draw the blocker
	private Paint targetPaint; // Paint used to draw the target
	private Paint backgroundPaint; // Paint used to clear the drawing area
	private Line blocker2;
	private Bitmap back_pic;
	private MediaPlayer mMediaPlayer;
	private ArrayList<Line> blockers;
	private int LEVELS = 1;
	private int w;
	private int oldw;
	private int oldh;
	private int h;
	private InterListener inter;

	public interface InterListener{
		void onClick();
	}
	
	public void setInter(InterListener inter) {
		this.inter = inter;
	}
	
	public void setLevels(int lv) {
		LEVELS = lv;
		if (LEVELS>0) {
			cannonballSpeed = w * 3 / 4;
			blockerDistance = w * 5 / 12;
		}else{
			cannonballSpeed = w * 3 / 2; // cannonball speed multiplier
			blockerDistance = w * 5 / 8; // blocker 5/8 screen width from left
		}
		newGame();
		startMusic();
	}
	
	public void startMusic() {
		if (mMediaPlayer != null) {
			mMediaPlayer.start();  
		}
	}
	// public constructor
	public CannonView(Context context, AttributeSet attrs)
	{
		super(context, attrs); // call super's constructor
		activity = (Activity) context; 

		// register SurfaceHolder.Callback listener
		getHolder().addCallback(this); 

		// initialize Lines and points representing game items
		
	/*	blockers = new ArrayList<Line>();
		for (int i = 0; i < 3; i++) {
			blockers.add(new Line());
		}*/
		blocker = new Line(); // create the blocker as a Line
		
		target = new Line(); // create the target as a Line
		cannonball = new Point(); // create the cannonball as a point

		// initialize hitStates as a boolean array
		hitStates = new boolean[TARGET_PIECES];

		// initialize SoundPool to play the app's three sound effects
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

		// create Map of sounds and pre-load sounds
		soundMap = new HashMap<Integer, Integer>(); // create new HashMap
		soundMap.put(TARGET_SOUND_ID,
				soundPool.load(context, R.raw.target_hit, 1));
		soundMap.put(CANNON_SOUND_ID,
				soundPool.load(context, R.raw.cannon_fire, 1));
		soundMap.put(BLOCKER_SOUND_ID,
				soundPool.load(context, R.raw.blocker_hit, 1));
		
		mMediaPlayer = MediaPlayer.create(context, R.raw.shuaicong);  //背景音乐
		mMediaPlayer.setLooping(true);
		
		InputStream inputStream = context.getResources().openRawResource(R.raw.back1); //背景图片
		back_pic = BitmapFactory.decodeStream(inputStream);

		// construct Paints for drawing text, cannonball, cannon,
		// blocker and target; these are configured in method onSizeChanged
		textPaint = new Paint(); // Paint for drawing text
		cannonPaint = new Paint(); // Paint for drawing the cannon
		cannonballPaint = new Paint(); // Paint for drawing a cannonball
		blockerPaint = new Paint(); // Paint for drawing the blocker
		targetPaint = new Paint(); // Paint for drawing the target
		backgroundPaint = new Paint(); // Paint for drawing the target //背景颜色
	} // end CannonView constructor
	// called when the size of this View changes--including when this
	// view is first added to the view hierarchy
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		this.w = w;
		this.h = h;
		this.oldw = oldw;
		this.oldh = oldh;

		super.onSizeChanged(w, h, oldw, oldh);
		screenWidth = w; // store the width
		screenHeight = h; // store the height
		
		cannonHeight = screenHeight / 2;
		
		cannonBaseRadius = h / 18; // cannon base radius 1/18 screen height
		cannonLength = w / 8; // cannon length 1/8 screen width
		cannonballRadius = w / 36; // cannonball radius 1/36 screen width
		
		cannonballSpeed = w * 3 / 2; // cannonball speed multiplier
		
		lineWidth = w / 24; // target and blocker 1/24 screen width
		// configure instance variables related to the blocker
		blockerDistance = w * 5 / 8; // blocker 5/8 screen width from left
		
		blockerBeginning = h / 8; // distance from top 1/8 screen height
		blockerEnd = h * 3 / 8; // distance from top 3/8 screen height
		initialBlockerVelocity = h / 2; // initial blocker speed multiplier
		
		
		blocker.start = new Point(blockerDistance, blockerBeginning);
		blocker.end = new Point(blockerDistance, blockerEnd);
		
	/*	for (int i = 0; i < blockers.size(); i++) {
			blockers.get(i).start = new Point(blockerDistance, blockerBeginning);
			blockers.get(i).end = new Point(blockerDistance, blockerEnd);
		}*/
		
		
		// configure instance variables related to the target
		targetDistance = w * 7 / 8; // target 7/8 screen width from left
		targetBeginning = h / 8; // distance from top 1/8 screen height
		targetEnd = h * 7 / 8; // distance from top 7/8 screen height
		pieceLength = (targetEnd - targetBeginning) / TARGET_PIECES;
		initialTargetVelocity = -h / 4; // initial target speed multiplier
		target.start = new Point(targetDistance, targetBeginning);
		target.end = new Point(targetDistance, targetEnd);
		// endpoint of the cannon's barrel initially points horizontally
		barrelEnd = new Point(cannonLength, h / 2);
		// configure Paint objects for drawing game elements
		textPaint.setTextSize(w / 20); // text size 1/20 of screen width
		textPaint.setAntiAlias(true); // smoothes the text
		cannonPaint.setStrokeWidth(lineWidth * 1.5f); // set line thickness
		blockerPaint.setStrokeWidth(lineWidth); // set line thickness
		blockerPaint.setColor(Color.CYAN);
		targetPaint.setStrokeWidth(lineWidth); // set line thickness
		backgroundPaint.setColor(Color.WHITE); // set background color
			
		newGame(); // set up and start a new game
	} // end method onSizeChanged
	//reset all the screen elements and start a new game
	public void newGame()
	{// set every element of hitStates to false--restores target pieces
		for (int i = 0; i < TARGET_PIECES; ++i)
			hitStates[i] = false;
		targetPiecesHit = 0; // no target pieces have been hit
		blockerVelocity = initialBlockerVelocity; // set initial velocity
		targetVelocity = initialTargetVelocity; // set initial velocity
		timeLeft = TIMELEFT; // start the countdown at 10 seconds 时间
		cannonballOnScreen = false; // the cannonball is not on the screen
		shotsFired = 0; // set the initial number of shots fired
		totalElapsedTime = 0.0; // set the time elapsed to zero
		
		blocker.start.set(blockerDistance, blockerBeginning);
		blocker.end.set(blockerDistance, blockerEnd);
		
		/*for (int i = 0; i < blockers.size(); i++) {
			blockers.get(i).start.set(blockerDistance, blockerBeginning);
			blockers.get(i).end.set(blockerDistance, blockerEnd);
		}*/
		
		target.start.set(targetDistance, targetBeginning);
		target.end.set(targetDistance, targetEnd);
		if (gameOver)
		{
			if (cannonThread != null) {
				if (cannonThread.threadIsRunning) {
					cannonThread.setRunning(false);
				}
			}
			gameOver = false; // the game is not over
			cannonThread = new CannonThread(getHolder());
			cannonThread.start();
		} // end if
	} // end method newGame
	//called repeatedly by the CannonThread to update game elements
	private void updatePositions(double elapsedTimeMS)
	{
		double interval = elapsedTimeMS / 1000.0; // convert to seconds
		if (cannonballOnScreen) // if there is currently a shot fired
		{
			//update cannonball position
			cannonball.x += interval * cannonballVelocityX;
			cannonball.y += interval * cannonballVelocityY;
			//check for collision with blocker

			if (cannonball.x + cannonballRadius > blockerDistance &&
					cannonball.x - cannonballRadius < blockerDistance &&
					cannonball.y + cannonballRadius > blocker.start.y &&
					cannonball.y - cannonballRadius < blocker.end.y)
			{
				cannonballVelocityX *= -1; // reverse cannonball's direction
//				timeLeft -= MISS_PENALTY; // penalize the user
				// play blocker sound
				soundPool.play(soundMap.get(BLOCKER_SOUND_ID), 1, 1, 1, 0, 1f);
			} // end if
			//check for collisions with left and right walls
			else if (cannonball.x + cannonballRadius > screenWidth ||
					cannonball.x - cannonballRadius < 0)
				cannonballOnScreen = false; // remove cannonball from screen
			//check for collisions with top and bottom walls
			else if (cannonball.y + cannonballRadius > screenHeight ||
					cannonball.y - cannonballRadius < 0)
				cannonballOnScreen = false; // make the cannonball disappear
			//check for cannonball collision with target
			else if (cannonball.x + cannonballRadius > targetDistance &&
					cannonball.x - cannonballRadius < targetDistance &&
					cannonball.y + cannonballRadius > target.start.y &&
					cannonball.y - cannonballRadius < target.end.y)
			{
				//determine target section number (0 is the top)
				int section =
						(int) ((cannonball.y - target.start.y) / pieceLength);
				//check if the piece hasn't been hit yet
				if ((section >= 0 && section < TARGET_PIECES) &&
						!hitStates[section])
				{
					hitStates[section] = true; // section was hit
					cannonballOnScreen = false; // remove cannonball
					timeLeft += HIT_REWARD; // add reward to remaining time
					//play target hit sound
					soundPool.play(soundMap.get(TARGET_SOUND_ID), 1,1, 1, 0, 1f);
					//if all pieces have been hit
					if (++targetPiecesHit == TARGET_PIECES)
					{
						cannonThread.setRunning(false);
						showGameOverDialog(R.string.win); // show winning dialog
						gameOver = true; // the game is over
					} // end if
				} // end if
			} // end else if
		} // end if
		//update the blocker's position
		double blockerUpdate = interval * blockerVelocity;
		blocker.start.y += blockerUpdate;
		blocker.end.y += blockerUpdate;
		//update the target's position
		double targetUpdate = interval * targetVelocity;
		target.start.y += targetUpdate;
		target.end.y += targetUpdate;
		//if the blocker hit the top or bottom, reverse direction
		if (blocker.start.y < 0 || blocker.end.y > screenHeight)
			blockerVelocity *= -1;
		//if the target hit the top or bottom, reverse direction
		if (target.start.y < 0 || target.end.y > screenHeight)
			targetVelocity *= -1;
		timeLeft -= interval; // subtract from time left
		//if the timer reached zero
		if (timeLeft <= 0)
		{
			timeLeft = 0.0;
			gameOver = true; // the game is over
			cannonThread.setRunning(false);
			showGameOverDialog(R.string.lose); // show the losing dialog
		} // end if
	} // end method updatePositions
	//fires a cannonball
	
	public void fireCannonball(MotionEvent event){
		System.out.println("CannonView.fireCannonball()");
		if (cannonballOnScreen) // if a cannonball is already on the screen
			return; // do nothing
		double angle = alignCannon(event); // get the cannon barrel's angle
		//move the cannonball to be inside the cannon
		cannonball.x = cannonballRadius; // align x-coordinate with cannon
		cannonball.y = screenHeight / 2; // centers ball vertically
		
		if (LEVELS>1) {
			cannonballVelocityX = cannonballSpeed;//直飞
			cannonballVelocityY = cannonballSpeed;
		}else{
			//get the x component of the total velocity
			cannonballVelocityX = (int) (cannonballSpeed * Math.sin(angle));
			//get the y component of the total velocity
			cannonballVelocityY = (int) (-cannonballSpeed * Math.cos(angle));
		}
		
		cannonballOnScreen = true; // the cannonball is on the screen
		++shotsFired; // increment shotsFired
		//play cannon fired sound
		soundPool.play(soundMap.get(CANNON_SOUND_ID), 1, 1, 1, 0, 1f);
	} // end method fireCannonball
	//aligns the cannon in response to a user touch
	public double alignCannon(MotionEvent event)
	{
		//get the location of the touch in this view
		Point touchPoint = new Point((int) event.getX(), (int) event.getY());
		//compute the touch's distance from center of the screen
		//on the y-axis
		double centerMinusY = (screenHeight / 2 - touchPoint.y);
		double angle = 0; // initialize angle to 0
		//calculate the angle the barrel makes with the horizontal
		if (centerMinusY != 0) // prevent division by 0
			angle = Math.atan((double) touchPoint.x / centerMinusY);
		//if the touch is on the lower half of the screen
		if (touchPoint.y > screenHeight / 2)
			angle += Math.PI; // adjust the angle
		//calculate the endpoint of the cannon barrel
		
		if (LEVELS>1) {
			cannonHeight = touchPoint.y;//活动炮台
		}else{
			barrelEnd.x = (int) (cannonLength * Math.sin(angle));
			barrelEnd.y = (int) (-cannonLength * Math.cos(angle) + screenHeight / 2); //瞄准炮台
		}
		
		return angle; // return the computed angle
	} // end method alignCannon
	public void drawGameElements(Canvas canvas)
	{
		if (canvas == null) {
			return;
		}
		// clear the background
		canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(),backgroundPaint);		
		
		RectF rectF = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());//底图
		canvas.drawBitmap(back_pic, null, rectF, null);
		
		canvas.drawText(getResources().getString(R.string.time_remaining_format, timeLeft), 30, 50, textPaint);
		if (cannonballOnScreen){
			if (LEVELS>1) {
				canvas.drawCircle(cannonball.x, cannonHeight, cannonballRadius,	cannonballPaint);//活动子弹
			}else{
				canvas.drawCircle(cannonball.x, cannonball.y, cannonballRadius,	cannonballPaint);
			}
		}
		
		if (LEVELS>1) {
			canvas.drawLine(0, cannonHeight, barrelEnd.x, cannonHeight, cannonPaint);//barrelEnd.x barrelEnd.y	//活动炮台			
			canvas.drawCircle(0, (int) cannonHeight, (int) cannonBaseRadius, cannonPaint);//活动炮台
		}else{
			canvas.drawLine(0, screenHeight / 2, barrelEnd.x, barrelEnd.y,cannonPaint);
			canvas.drawCircle(0, (int) screenHeight / 2,(int) cannonBaseRadius, cannonPaint);	
		}

		canvas.drawLine(blocker.start.x, blocker.start.y, blocker.end.x,
				blocker.end.y, blockerPaint);
		
/*		for (int i = 0; i < blockers.size(); i++) {			
			int yyy = new Random().nextInt()%50;
			canvas.drawLine(blockers.get(i).start.x + i*50, blockers.get(i).start.y + yyy, blockers.get(i).end.x + i*50,
					blockers.get(i).end.y + yyy, blockerPaint);
		}*/
		
		Point currentPoint = new Point(); // start of current target section
		// initialize curPoint to the starting point of the target
		currentPoint.x = target.start.x;
		currentPoint.y = target.start.y;
		// draw the target
		for (int i = 1; i <= TARGET_PIECES; ++i)
		{
			// if this target piece is not hit, draw it
			if (!hitStates[i - 1])
			{
				// alternate coloring the pieces yellow and blue
				if (i % 2 == 0)
					targetPaint.setColor(Color.YELLOW);//颜色
				else
					targetPaint.setColor(Color.BLUE);
				canvas.drawLine(currentPoint.x, currentPoint.y, target.end.x,
						(int) (currentPoint.y + pieceLength), targetPaint);
			}
			// move curPoint to the start of the next piece
			currentPoint.y += pieceLength;
		} // end for
		
		
	} // end method drawGameElements
	//display an AlertDialog when the game ends
	private void showGameOverDialog(int messageId)
	{
		if (mMediaPlayer != null) {
			if (mMediaPlayer.isPlaying()){  
				mMediaPlayer.pause();  
			}  
		}
		
		//create a dialog displaying the given String
		final AlertDialog.Builder dialogBuilder =
				new AlertDialog.Builder(getContext());
		dialogBuilder.setTitle(getResources().getString(messageId));
		dialogBuilder.setCancelable(false);
		//display number of shots fired and total time elapsed
		dialogBuilder.setMessage(getResources().getString(
				R.string.results_format, shotsFired, totalElapsedTime));
		
		
		if (messageId == R.string.win) {
			dialogBuilder.setPositiveButton("下一关",//R.string.reset_game
					new DialogInterface.OnClickListener(){
				//called when "Reset Game" Button is pressed
				@Override
				public void onClick(DialogInterface dialog, int which){
					dialogIsDisplayed = false;
					//newGame(); // set up and start a new game
					if (LEVELS == 0) {
						setLevels(1);
					}else{
						inter.onClick();
					}
				} // end method onClick
			}); // end call to setPositiveButton
		}
		
		dialogBuilder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialogIsDisplayed = false;
				inter.onClick();
			}
		});
		activity.runOnUiThread(
				new Runnable() {
					public void run()
					{
						dialogIsDisplayed = true;
						try {
							dialogBuilder.show(); // display the dialog
						} catch (Exception e) {
							// TODO: handle exception
						}
					} // end method run
				} // end Runnable
				);
	} 
	//pauses the game
	public void stopGame()
	{
		if (mMediaPlayer != null) {
			if (mMediaPlayer.isPlaying()){  
				mMediaPlayer.pause();  
			}  
		}
		if (cannonThread != null )
			cannonThread.setRunning(false);
	} // end method stopGame
	//releases resources; called by CannonGame's onDestroy method
	public void releaseResources(){
		soundPool.release(); // release all resources used by the SoundPool
		soundPool = null ;
		if (mMediaPlayer != null) {
			if (mMediaPlayer.isPlaying()){  
				mMediaPlayer.stop();  
			}  
		}
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format,int width, int height)
	{
	} // end method surfaceChanged
	//called when surface is first created
	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		cannonThread = new CannonThread(holder);
		cannonThread.setRunning(true);
		cannonThread.start(); // start the game loop thread
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// ensure that thread terminates properly
		boolean retry = true;
		cannonThread.setRunning(false);
		while (retry)
		{
			try
			{
				cannonThread.join();
				retry = false;
			} // end try
			catch (InterruptedException e)
			{
			} // end catch
		} // end while
	} // end method surfaceDestroyed
	//Thread subclass to control the game loop
	private class CannonThread extends Thread
	{
		private SurfaceHolder surfaceHolder; // for manipulating canvas
		private boolean threadIsRunning = true; // running by default
		public CannonThread(SurfaceHolder holder)
		{
			surfaceHolder = holder;
			setName("CannonThread");
		} // end constructor
		// changes running state
		public void setRunning(boolean running)
		{
			threadIsRunning = running;
		} // end method setRunning
		// controls the game loop
		@Override
		public void run()
		{
			Canvas canvas = null ; // used for drawing
			long previousFrameTime = System.currentTimeMillis();
			while (threadIsRunning)
			{
				try
				{canvas = surfaceHolder.lockCanvas(null);
				synchronized(surfaceHolder)
				{
					long currentTime = System.currentTimeMillis();
					double elapsedTimeMS = currentTime - previousFrameTime;
					totalElapsedTime += elapsedTimeMS / 1000.0;
					updatePositions(elapsedTimeMS); // update game state
					drawGameElements(canvas); // draw
					previousFrameTime = currentTime; // update previous time
				}
				}
				finally
				{
					if (canvas != null )
						surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}
	//****************************************************************************
	// This is only a good start... Complete from handout...
	//****************************************************************************


} // end class CannonView

/*********************************************************************************
 * (C) Copyright 1992-2012 by Deitel & Associates, Inc. and * Pearson Education, *
 * Inc. All Rights Reserved. * * DISCLAIMER: The authors and publisher of this   *
 * book have used their * best efforts in preparing the book. These efforts      *
 * include the * development, research, and testing of the theories and programs *
 * * to determine their effectiveness. The authors and publisher make * no       *
 * warranty of any kind, expressed or implied, with regard to these * programs   *
 * or to the documentation contained in these books. The authors * and publisher *
 * shall not be liable in any event for incidental or * consequential damages in *
 * connection with, or arising out of, the * furnishing, performance, or use of  *
 * these programs.                                                               *
 *********************************************************************************/
