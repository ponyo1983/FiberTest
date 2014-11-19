package com.lon.fiber.gui;

import java.io.File;

import com.lon.fiber.core.CalManager;
import com.lon.fiber.core.FileManager;
import com.lon.fiber.core.SignalModule;
import com.lon.fiber.core.SignalModuleManager;
import com.lon.fiber.R;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;

public class MainActivity extends Activity {

	private GridView gridView;
	// 图片的文字标题
	private String[] titles = new String[] { "1:测试", "2:设置", "3:关于", };
	// 图片ID数组
	private int[] images = new int[] { R.drawable.pic1, R.drawable.pic3,
			R.drawable.pic2, };
	SignalModule module;

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		//super.onBackPressed();
		
//		AlertDialog.Builder builder=new AlertDialog.Builder(this);
//		
//		builder.setTitle("移动监测");
//		
//		builder.setMessage("是否确认退出程序?");
//		
//		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//			
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
//		
//		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//			
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				// TODO Auto-generated method stub
//				
//				SignalModuleManager.getInstance().stop();
//				finish();
//				
//			}
//		});
//		
//		
//		
//		builder.show();
//		
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setTitle("光纤测试");
		gridView = (GridView) findViewById(R.id.gridview);
		PictureAdapter adapter = new PictureAdapter(titles, images, this);
		gridView.setAdapter(adapter);

		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				// TODO Auto-generated method stub

				switch (position) {
				case 0:
					startActivity(new Intent(MainActivity.this,
							SignalListActivity.class));
					break;
				case 1:
					startActivity(new Intent(MainActivity.this,
							WorkModeActivity.class));
					break;
				case 2: {

					AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
					builder.setTitle("光纤测试");
					builder.setMessage("光纤测试版本号:1.0.0");
					
					builder.setPositiveButton("确定", null);
					
					builder.show();
				}
					break;
				}

			}
		});
		//CalManager.getInstance().ReadCalData(new File("/cal.txt"));
		//FileManager.getInstance().start();
		UsbManager mUsbManager;
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		
		SignalModuleManager.getInstance(mUsbManager);
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}

}
