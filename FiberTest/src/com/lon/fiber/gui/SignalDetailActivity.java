package com.lon.fiber.gui;

import com.lon.fiber.core.ISignal;
import com.lon.fiber.core.ISignalChangedListener;
import com.lon.fiber.core.IWorkModeChangedListener;
import com.lon.fiber.core.SignalChannel;
import com.lon.fiber.core.SignalModuleManager;
import com.lon.fiber.core.WorkMode;
import com.lon.fiber.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

public class SignalDetailActivity extends Activity {

	SignalDetailView signalDetailView;
	ISignalChangedListener signalChangedListener;
	IWorkModeChangedListener workModeChangedListener;
	int channelIndex = 0;

	int secondIndex = 1000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setTitle("信号分析");
		setContentView(R.layout.activity_signal_detail);
		signalDetailView = (SignalDetailView) findViewById(R.id.signalView);

		Intent intent = getIntent();

		channelIndex = intent.getIntExtra("channel", 0);

		signalChangedListener = new ISignalChangedListener() {

			@Override
			public void onSignalChanged(ISignal signal) {
				// TODO Auto-generated method stub

				if (signal == null) {
					signalDetailView.refreshRawData(null);
					signalDetailView.refreshSpectrumData(null);
				} else {
					float[] rawData = signal.getRawData();
					if(rawData==null)
					{
						Log.e("ft", "data null");
						return;
					}
					
					float [] spectrumData=signal.getSpectrumData();
					signalDetailView.refreshRawData(rawData);
					signalDetailView.refreshSpectrumData(spectrumData);
					signalDetailView.addSignalAmpl(signal.getAmpl());
					secondIndex++;
				}
			}
		};

		//工作模式改变事件
		workModeChangedListener=new IWorkModeChangedListener() {
			
			@Override
			public void onWorkModeChanged(WorkMode wkMode) {
				// TODO Auto-generated method stub
				if(wkMode!=null)
				{
					signalDetailView.setLimit(wkMode.getUpper(), wkMode.getLower());
				}
			}
		};
		
		SignalChannel channel=SignalModuleManager.getInstance().getChannel(0);
		
		channel.addSignalChangedListener(signalChangedListener); //信号改变监听
		//channel.addWorkModeChangedListener(workModeChangedListener); //工作模式监听
		
		WorkMode wkMode= channel.getCurrentMode();
		if(wkMode!=null)
		{
			signalDetailView.setLimit(wkMode.getUpper(), wkMode.getLower());
		}
		// 放大按钮
		ImageButton button1 = (ImageButton) findViewById(R.id.xZoomIn);
		button1.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				signalDetailView.zoomIn();
			}
		});
		// 缩小按钮
		ImageButton button2 = (ImageButton) findViewById(R.id.xZoomOut);
		button2.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				signalDetailView.zoomOut();
			}
		});

		// 暂停按钮
		final ImageButton buttonPlay = (ImageButton) findViewById(R.id.buttonPlay);
		buttonPlay.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				signalDetailView.stoped = !signalDetailView.stoped;
				if (signalDetailView.stoped) {
					buttonPlay.setImageResource(R.drawable.play);
				} else {
					buttonPlay.setImageResource(R.drawable.pause);
				}
			}
		});

		// 左移
		ImageButton buttonLeft = (ImageButton) findViewById(R.id.moveLeft);
		buttonLeft.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				signalDetailView.moveXCursor(-10);
			}
		});

		// 右移
		ImageButton buttonRight = (ImageButton) findViewById(R.id.moveRight);
		buttonRight.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				signalDetailView.moveXCursor(+10);
			}
		});

		// 上移
		ImageButton buttonUp = (ImageButton) findViewById(R.id.moveUp);
		buttonUp.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				signalDetailView.moveYCursor(5);
			}
		});
		// 上移
		ImageButton buttonDown = (ImageButton) findViewById(R.id.moveDown);
		buttonDown.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				signalDetailView.moveYCursor(-5);
			}
		});

		ImageButton buttonYZoomIn = (ImageButton) findViewById(R.id.yZoomIn);
		buttonYZoomIn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				signalDetailView.zoomYIn();
			}
		});

		ImageButton buttonYZoomOut = (ImageButton) findViewById(R.id.yZoomOut);
		buttonYZoomOut.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				signalDetailView.zoomYOut();
			}
		});

		Spinner spinner1 = (Spinner) findViewById(R.id.spinner1);
		// 将可选内容与ArrayAdapter连接起来
		ArrayAdapter adapter2 = ArrayAdapter.createFromResource(this,
				R.array.display_mode, android.R.layout.simple_spinner_item);

		// 设置下拉列表的风格
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// 将adapter2 添加到spinner中
		spinner1.setAdapter(adapter2);

		spinner1.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				switch (arg2) {
				case 0:
					signalDetailView.setDisplayMode(DrawMode.SignalData);
					break;
				case 1:
					signalDetailView.setDisplayMode(DrawMode.SignalAmpl);
					
					break;
				case 2:
					signalDetailView.setDisplayMode(DrawMode.SignalFreq);
					break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});

		// 设置默认值
		spinner1.setVisibility(View.VISIBLE);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        case R.id.action_settings:
        	this.finish();
        break;
        }
        return super.onOptionsItemSelected(item);
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		SignalModuleManager.getInstance().getChannel(0)
				.removeSignalChangedListener(signalChangedListener);

	}

}
