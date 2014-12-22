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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class SelfTestActivity extends Activity {

	
	Timer refreshTimer;
	TextView resultView;
	Button buttonRet;
	
	 SignalChannel channel;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setTitle("�����Լ�");
		setContentView(R.layout.activity_self_test);
		
		resultView=(TextView)findViewById(R.id.textView2);
		buttonRet=(Button)findViewById(R.id.buttonRet);
		
		buttonRet.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				channel.setMode(1); //����Ϊ�Լ�ģʽ
				SelfTestActivity.this.finish();
			}
		});
		 channel=SignalModuleManager.getInstance().getChannel(0);
		
		channel.setMode(0); //����Ϊ�Լ�ģʽ
		
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
				resultView.setText("���δ֪");
			}
			else if(r<40)
			{
				resultView.setTextColor(android.graphics.Color.GREEN);
				resultView.setText("�Լ���ֵΪ:"+r+"\r\n����ͨ�������Կ�ʼ�����ˣ�");
			}
			else
			{
				resultView.setTextColor(android.graphics.Color.RED);
				resultView.setText("����ʧ��!");
			}
			
		}
	};

}
