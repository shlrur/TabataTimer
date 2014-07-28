package shlrur.sap.shlrurtabatatimer.shlrurtabatatimer;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class TabataTimerProgressView extends View {

	
	private int exerTime;
	private int restTime;
	private int setsNum;
	private int nowSetCount; // if setsNum is 8, max value of nowSetCount is 15.{8+(8-1)}
	
	private int canvasHeight;
	private int canvasWidth;
	
	private Paint beforeExer, doingExer, afterExer;
	private Paint beforeRest, doingRest, afterRest;
	
	public TabataTimerProgressView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
//		DRAW_MODE = 0;
		nowSetCount = -1;
		
		progressBarInit(20, 10, 8);
		paintInit();
	}
	
	public void progressBarInit(int exertime, int resttime, int setsnum) {
		exerTime = exertime;
		restTime = resttime;
		setsNum = setsnum;
		
		nowSetCount = -1;
	}
	
	private void paintInit() {
		beforeExer = new Paint();
		beforeRest = new Paint();
		doingExer = new Paint();
		doingRest = new Paint();
		afterExer = new Paint();
		afterRest = new Paint();
		
		beforeExer.setColor(Color.argb(40, 33, 133, 197));
		beforeRest.setColor(Color.argb(40, 255, 146, 122));
		doingExer.setColor(Color.argb(255, 33, 133, 197));
		doingRest.setColor(Color.argb(255, 255, 146, 122));
		afterExer.setColor(Color.argb(255, 33, 133, 197));
		afterRest.setColor(Color.argb(255, 255, 146, 122));
//		afterExer.setColor(Color.argb(255, 62, 153, 55));
//		afterRest.setColor(Color.argb(255, 62, 153, 55));
		
		beforeExer.setStyle(Paint.Style.FILL_AND_STROKE);
		beforeRest.setStyle(Paint.Style.FILL_AND_STROKE);
		doingExer.setStyle(Paint.Style.FILL_AND_STROKE);
		doingRest.setStyle(Paint.Style.FILL_AND_STROKE);
		afterExer.setStyle(Paint.Style.FILL_AND_STROKE);
		afterRest.setStyle(Paint.Style.FILL_AND_STROKE);
	}
	
	public void nextRount() {
		nowSetCount++;
	}
	
//	public void drawBranch(int mode) {
//		DRAW_MODE = mode;
//	}
	
	public void drawTabataTimerProgressBar(Canvas canvas) {
		int allTabataTime = setsNum*exerTime + (setsNum-1)*restTime;
		ArrayList<Float> pointByTime = new ArrayList<Float>();
				
		for(int i=0 ; i<setsNum*2 ; i++) {
			if(i==0)
				pointByTime.add(0.0f);
			else if(i==setsNum*2-1)
				pointByTime.add((float)canvasWidth);
			else {
				if(i%2 == 0) {
					// plus rest time
					pointByTime.add(pointByTime.get(i-1)+restTime*canvasWidth/allTabataTime);
				}
				else {
					// plus exer time
					pointByTime.add(pointByTime.get(i-1)+exerTime*canvasWidth/allTabataTime);
				}
			}
		}
		
		float x1, y1, x2, y2;
		y1 = 0;
		y2 = canvasHeight;
		
		for(int i=0 ; i<2*setsNum-1 ; i++) {
			x1 = pointByTime.get(i);
			x2 = pointByTime.get(i+1);
			
			// before state
			if(i>nowSetCount) {
				// Exer state
				if(i%2 == 0)
					canvas.drawRect(x1, y1, x2, y2, beforeExer);
				// rest state
				else
					canvas.drawRect(x1, y1, x2, y2, beforeRest);
			}
			// doing state
			else if(i==nowSetCount) {
				// Exer state
				if(i%2 == 0)
					canvas.drawRect(x1, y1, x2, y2, doingExer);
				// rest state
				else
					canvas.drawRect(x1, y1, x2, y2, doingRest);
					
			}
			// after state
			else {
				// Exer state
				if(i%2 == 0)
					canvas.drawRect(x1, y1, x2, y2, afterExer);
				// rest state
				else
					canvas.drawRect(x1, y1, x2, y2, afterRest);
					
			}
		}
	}
	
	protected void onDraw(Canvas canvas) {
		canvasHeight = this.getMeasuredHeight();
		canvasWidth = this.getMeasuredWidth();
		
		drawTabataTimerProgressBar(canvas);
		
//		switch(DRAW_MODE){
//		case 0: // Init progressbar
//			
//			break;
//		case 1: // go to the next round
//			break;
//		}
	}
}
