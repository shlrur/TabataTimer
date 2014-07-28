package shlrur.sap.shlrurtabatatimer.shlrurtabatatimer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class TabataTimerActivity extends Activity {
	private ViewPager tabataPager;
	
	private AutoResizeTextView tv_time;
	private Button btn_timer, btn_send_email;
	private AutoResizeTextView tv_state;
	
	private SeekBar sk_exer_time, sk_rest_time, sk_sets;
	private TextView tv_exer_time, tv_rest_time, tv_sets;
	private TextView tv_exer_time_title, tv_rest_time_title, tv_sets_title;
	private TabataTimerProgressView v_progress;
	
	private double tabataTime;
	
	private TabataCountRuannable tabataCountRunnable;
	private TabataCountHandler tabataCountHandler;
	
	private Object mPauseLock;
	private int mState;	// 0: stop(reseted), 1: start, 2: give up, 3: done
	
	private boolean threadRunning;
	private boolean isRunning;
	
	private SharedPreferences mPrefs;
	
	private int tabataExerciseSec;
	private int tabataRestSec;
	private int tabataSetsNum;
	
	private ToneGenerator tg;
	
	private Typeface futuraFont;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tabata_timer_layout);
		
		Log.d("SAP", "onCreate");
		
		tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
		
//		futuraFont = Typeface.createFromAsset(getAssets(), "futura_condensed_extrabold.ttf");
		futuraFont = Typeface.createFromAsset(getAssets(), "Futura BdCn BT Bold.ttf");
		
		mPrefs = getSharedPreferences("SAPpref", MODE_PRIVATE);
		
		tabataExerciseSec = Integer.parseInt(mPrefs.getString("TabataTimerExerciseSecond", "20"));
		tabataRestSec = Integer.parseInt(mPrefs.getString("TabataTimerRestSecond", "10"));
		tabataSetsNum = Integer.parseInt(mPrefs.getString("TabataTimerSetsNumber", "8"));
		
		tabataPager = (ViewPager)findViewById(R.id.tabata_pager);
		PagerAdapterClass pagerAdapterClass = new PagerAdapterClass(getApplicationContext());
		tabataPager.setAdapter(pagerAdapterClass);
		tabataPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if(position==0){
					tabataExerciseSec = sk_exer_time.getProgress()+1;
					tabataRestSec = sk_rest_time.getProgress()+1;
					tabataSetsNum = sk_sets.getProgress()+1;
				}
				if(position==1){
					// 0: stop(reseted), 1: start, 2: give up, 3: done
					switch(mState){
					case 1: // start -> give up
						btn_timer.setText("Don't give up!!!");
						isRunning = false;
				        mState = 2;
						break;
					}

			        Log.d("SAP", tabataExerciseSec + " " + tabataRestSec + " " + tabataSetsNum);
			        
			        tv_exer_time.setText(String.valueOf(tabataExerciseSec) + " Sec");
			        tv_rest_time.setText(String.valueOf(tabataRestSec) + " Sec");
			        tv_sets.setText(String.valueOf(tabataSetsNum) + " Sets");
			        
			        sk_exer_time.setProgress(tabataExerciseSec-1);
			        sk_rest_time.setProgress(tabataRestSec-1);
			        sk_sets.setProgress(tabataSetsNum-1);
				}
			}
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
		
		tabataCountHandler = new TabataCountHandler();
		
		tabataCountRunnable = new TabataCountRuannable();
		Thread t = new Thread(tabataCountRunnable);
		t.setDaemon(true);
		t.start();
	}
	
	protected void onPause() {
		super.onPause();
				
		finish();
	}
	
	/**
	 * Message to Handler
	 * @param what
	 * @param arg
	 * @param obj
	 */
	private void sendMessage(int what, int arg, String obj) {
		Message timeMsg = tabataCountHandler.obtainMessage();
		timeMsg.what = what;
		timeMsg.arg1 = arg;
		timeMsg.obj = obj;
		tabataCountHandler.sendMessage(timeMsg);
	}
	
	class TabataCountRuannable implements Runnable {
		private double startTime;
		
		public TabataCountRuannable() {
			mPauseLock = new Object();
	        mState = 0;
	        
	        threadRunning = true;
	        
	        counterInit();
		}
		
		private void counterInit() {
			tabataTime = 0;
			startTime = 0;
		}
		
		@Override
		public void run() {
			int nowSet;
			while(threadRunning){
				nowSet = 0;
				
				// stop, give up
				synchronized (mPauseLock) {
	                while (mState==0 || mState==2 || mState==3) {
	                    try {
	                        mPauseLock.wait();
	                    } catch (InterruptedException e) { }
	                }
	            }
				
				isRunning = true;
				// running
				startTime = System.currentTimeMillis();
				while(isRunning){
					tabataTime = System.currentTimeMillis() - startTime;
					sendMessage(1, nowSet, null);
//					Message timeMsg = tabataCountHandler.obtainMessage();
//					timeMsg.what = 1;
//					timeMsg.arg1 = nowSet;
//					tabataCountHandler.sendMessage(timeMsg);
					
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					// exercise over
					if(tabataTime>=tabataExerciseSec*1000 && nowSet%2==0){
						tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
						if(nowSet/2 == tabataSetsNum-1){ // start -> done
							sendMessage(2, 0, "Good Job");
							
							isRunning = false;
							mState = 3;
							
							sendMessage(3, 0, "Complete");
							
							sendMessage(4, 0, null);
						}
						else{
							sendMessage(2, 0, (nowSet+2)/2 + "/" + tabataSetsNum + "\n[Resting]");
						}
						startTime = System.currentTimeMillis();
						tabataTime = 0;
						
						nowSet++;
						v_progress.nextRount();
						sendMessage(5, 0, null);
					}
					
					// rest over
					if(tabataTime>=tabataRestSec*1000 && nowSet%2==1){
						tg.startTone(ToneGenerator.TONE_PROP_BEEP);
						startTime = System.currentTimeMillis();
						
						nowSet++;
						v_progress.nextRount();
						sendMessage(5, 0, null);
						
						sendMessage(2, 0, (nowSet+2)/2 + "/" + tabataSetsNum + "\n[Exercising]");
						Log.d("SAP", "nowSet: " + nowSet);
					}
				}
			}
		}
	}
	
	class TabataCountHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.what){
			case 1:
				double showingTime=0;
				if(tabataTime!=0)
					if(msg.arg1%2 == 0)
						showingTime = tabataExerciseSec - (tabataTime/1000);
					else // msg.arg1%2 == 1
						showingTime = tabataRestSec - (tabataTime/1000);
				
				if(showingTime >= 0)
					tv_time.setText(String.format("%5.2f", showingTime));
				break;
			case 2:
				tv_state.setText(msg.obj.toString());
				break;
			case 3:
				btn_timer.setText(msg.obj.toString());
				break;
			case 4:
				tv_time.setText("0.00");
				break;
			case 5:
				v_progress.invalidate();
				break;
			}
		}
	};
	
	private class PagerAdapterClass extends PagerAdapter {

		private LayoutInflater lInflater;
		
		public PagerAdapterClass(Context applicationContext) {
			super();
			lInflater = LayoutInflater.from(applicationContext);
		}

		@Override
		public int getCount() {
			return 2;
		}

		public Object instantiateItem(View pager, int position) {
			View v = null;
			
			if(position == 0) {
				v = lInflater.inflate(R.layout.tabata_inflate_timer, null);
				tv_time = (AutoResizeTextView)v.findViewById(R.id.tabata_timer_time);
				btn_timer = (Button)v.findViewById(R.id.tabata_timer_btn);
				tv_state = (AutoResizeTextView)v.findViewById(R.id.tabata_timer_state_tv);
				v_progress = (TabataTimerProgressView)v.findViewById(R.id.tabata_timer_surface);
//				tv_state.setText("state test");
				tv_time.setText("00.00");
				btn_timer.setText("start");
				v_progress.progressBarInit(tabataExerciseSec, tabataRestSec, tabataSetsNum);
				sendMessage(5, 0, null);
				
				// font setting
//				tv_time.setTypeface(futuraFont);
				tv_state.setTypeface(futuraFont);
				btn_timer.setTypeface(futuraFont);
				
				btn_timer.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// 0: stop(reseted), 1: start, 2: give up, 3: done
						switch(mState){
						case 0: // stop(reseted) -> start
							tg.startTone(ToneGenerator.TONE_PROP_BEEP);
							v_progress.progressBarInit(tabataExerciseSec, tabataRestSec, tabataSetsNum);
							v_progress.nextRount();
							sendMessage(5, 0, null);
							sendMessage(2, 0, "1/" + tabataSetsNum + "\n[Exercising]");
							btn_timer.setText("give up");
							
							synchronized (mPauseLock) {
					            mState = 1;
					            mPauseLock.notifyAll();
					        }
							break;
						case 1: // start -> give up
							btn_timer.setText("Don't give up!!!");
							isRunning = false;
					        mState = 2;
							break;
						case 2: // give up -> stop(reseted)
//							v_progress.progressBarInit(tabataExerciseSec, tabataRestSec, tabataSetsNum);
//							sendMessage(5, 0, null);
//							sendMessage(2, 0, "");
//							btn_timer.setText("start");
//							tv_time.setText("00.00");
//					        mState = 0;
//							break;
						case 3: // done -> stop(reseted)
							v_progress.progressBarInit(tabataExerciseSec, tabataRestSec, tabataSetsNum);
							sendMessage(5, 0, null);
							sendMessage(2, 0, "");
							btn_timer.setText("start");
							tv_time.setText("00.00"); 
							mState = 0;
							break;
						}
					}
				});
			}
			else if(position == 1) {
				v = lInflater.inflate(R.layout.tabata_inflate_setting, null);
				
				sk_exer_time = (SeekBar)v.findViewById(R.id.tabata_setting_exercise_time_seekbar);
				sk_rest_time = (SeekBar)v.findViewById(R.id.tabata_setting_rest_time_seekbar);
				sk_sets = (SeekBar)v.findViewById(R.id.tabata_setting_sets_seekbar);
				tv_exer_time = (TextView)v.findViewById(R.id.tabata_setting_exercise_time_text);
				tv_rest_time = (TextView)v.findViewById(R.id.tabata_setting_rest_time_text);
				tv_sets = (TextView)v.findViewById(R.id.tabata_setting_sets_text);
				btn_send_email = (Button)v.findViewById(R.id.tabata_setting_send_email);
				tv_exer_time_title = (TextView)v.findViewById(R.id.tabata_setting_exercise_time_title);
				tv_rest_time_title = (TextView)v.findViewById(R.id.tabata_setting_rest_time_title);
				tv_sets_title = (TextView)v.findViewById(R.id.tabata_setting_sets_title);
				
				tv_exer_time.setTypeface(futuraFont);
				tv_rest_time.setTypeface(futuraFont);
				tv_sets.setTypeface(futuraFont);
				tv_exer_time_title.setTypeface(futuraFont);
				tv_rest_time_title.setTypeface(futuraFont);
				tv_sets_title.setTypeface(futuraFont);
				btn_send_email.setTypeface(futuraFont);
				
				sk_exer_time.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						final SharedPreferences.Editor editor = mPrefs.edit();
						editor.putString("TabataTimerExerciseSecond", String.valueOf(sk_exer_time.getProgress()+1));
						editor.commit();
					}
					
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {}
					
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						tv_exer_time.setText(String.valueOf(progress+1) + " Sec");
					}
				});
				sk_rest_time.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						final SharedPreferences.Editor editor = mPrefs.edit();
						editor.putString("TabataTimerRestSecond", String.valueOf(sk_rest_time.getProgress()+1));
						editor.commit();
					}
					
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {}
					
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						tv_rest_time.setText(String.valueOf(progress+1) + " Sec");
					}
				});
				sk_sets.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						final SharedPreferences.Editor editor = mPrefs.edit();
						editor.putString("TabataTimerSetsNumber", String.valueOf(sk_sets.getProgress()+2));
						editor.commit();
					}
					
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {}
					
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						tv_sets.setText(String.valueOf(progress+1) + " Sets");
					}
				});
				btn_send_email.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Uri uri = Uri.parse("mailto:shlrur123@gmail.com");
						Intent it = new Intent(Intent.ACTION_SENDTO, uri);
						
						SimpleDateFormat formatter = new SimpleDateFormat ( "yyyy.MM.dd HH:mm:ss", Locale.KOREA );
						Date currentTime = new Date ( );
						String today = formatter.format ( currentTime );
						it.putExtra(Intent.EXTRA_SUBJECT, "[SAP app] "+today);
						startActivity(it);
					}
				});
				
			}
			
			((ViewPager) pager).addView(v, 0);
			return v;
		}
		
		@Override
		public void destroyItem(View pager, int position, Object view) {
			((ViewPager) pager).removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(View pager, Object obj) {
			return pager == obj;
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {}

		@Override
		public Parcelable saveState() {	return null;}

		@Override
		public void startUpdate(View arg0) {}

		@Override
		public void finishUpdate(View arg0) {}
		
		@Override
		public int getItemPosition(Object object){
		     return POSITION_NONE;
		}
	}
}