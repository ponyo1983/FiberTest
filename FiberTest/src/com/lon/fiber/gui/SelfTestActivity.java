package com.lon.fiber.gui;

import java.util.Timer;
import java.util.TimerTask;

import com.lon.fiber.R;
import com.lon.fiber.core.SignalChannel;
import com.lon.fiber.core.SignalModuleManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.view.WindowManager;
import android.widget.TextView;

public class SelfTestActivity extends Activity {

	
	Timer refreshTimer;
	TextView resultView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setTitle("工作自检");
		setContentView(R.layout.activity_self_test);
		
		resultView=(TextView)findViewById(R.id.textView2);
		final SignalChannel channel=SignalModuleManager.getInstance().getChannel(0);
		
		channel.setMode(0); //设置为自检模式
		
		refreshTimer = new Timer();

		refreshTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				int r=channel.getSelfTestResult();
				mHandler.sendEmptyMessage(r);

			}
		}, 1000, 1000);
	}

	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
		
			int r=msg.what;
			if(r<0)
			{
				resultView.setTextColor(android.graphics.Color.YELLOW);
				resultView.setText("结果未知");
			}
			else if(r<40)
			{
				resultView.setTextColor(android.graphics.Color.GREEN);
				resultView.setText("测试结果为:"+r+",测试通过，可以开始工作了！");
			}
			else
			{
				resultView.setTextColor(android.graphics.Color.RED);
				resultView.setText("测试失败!");
			}
			
		}
	};

}
