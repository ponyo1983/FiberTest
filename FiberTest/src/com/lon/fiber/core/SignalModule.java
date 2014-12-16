package com.lon.fiber.core;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import android.util.Log;
import android_serialport_api.SerialPort;

///采集模块
public class SignalModule {

	private SerialPort serialPort;
	private FrameManager frameManager;

	private Thread threadRcv; // 查询参数的读取
	private ChannelCollection channels; // 信号通道

	int moduleNum = 0;

	public SignalModule(UsbSerialPort usbPort, int moduleNum) {
		this.serialPort = new SerialPort(usbPort);

		this.moduleNum = moduleNum;
		frameManager = new FrameManager(serialPort);

		channels = new ChannelCollection(this, 1);
	}

	public SignalModule(String portName, int baudrate, int moduleNum) {
		// TODO Auto-generated constructor stub
		try {
			this.serialPort = new SerialPort(new File(portName), baudrate, 0);

			this.moduleNum = moduleNum;
			frameManager = new FrameManager(serialPort);

			channels = new ChannelCollection(this, 3);

		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public int getModuleNum() {
		return moduleNum;
	}

	public SignalChannel getChannel(int index) {

		return channels.getChannel(index);
	}

	class FrameRcv implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			IFrameFilter filter = frameManager.createFilter();
			try {
				while (Thread.currentThread().isInterrupted() == false) {
					byte[] frame = filter.getFrame(-1);

					if (frame == null)
						continue;

					channels.processFrame(frame);
				}

			} finally {
				frameManager.removeFilter(filter);
			}

		}

	}

	public void run() {
		if (threadRcv == null || threadRcv.isInterrupted()) {
			threadRcv = new Thread(new FrameRcv());
			threadRcv.start();
		}
		// 开启通道
		channels.run();
	}

	public void stop() {
		if (threadRcv != null && threadRcv.isInterrupted() == false) {
			threadRcv.interrupt();
		}
		// 停止通道
		channels.stop();
	}

	public void sendFrame(byte[] frame) {
		if (serialPort == null)
			return;

		int length = frame.length;
		int realLength = length + 2 + 1 + 2 + 2; // 帧头(2)+数据长度(1)+校验(2)+帧尾(2)
		int infoLength = (length + 2);
		if (infoLength == 0x10) {
			realLength++;
		}
		for (int i = 0; i < length; i++) {
			if (frame[i] == (byte) 0x10) {
				realLength++;
			}
		}
		int index = 0;
		byte[] totalFrame = new byte[realLength];
		// 帧头
		totalFrame[index++] = (byte) 0x10;
		totalFrame[index++] = (byte) 0x02;
		// 长度
		totalFrame[index++] = (byte) infoLength;
		if (totalFrame[index - 1] == (byte) 0x10) {
			totalFrame[index++] = (byte) 0x10;
		}
		for (int i = 0; i < length; i++) {
			totalFrame[index++] = frame[i];
			if (frame[i] == (byte) 0x10) {
				totalFrame[index++] = frame[i];
			}
		}
		// 校验码
		totalFrame[index++] = (byte) 0;
		totalFrame[index++] = (byte) 0;
		// 帧头
		totalFrame[index++] = (byte) 0x10;
		totalFrame[index++] = (byte) 0x03;

		serialPort.write(totalFrame);
	}
}

class ChannelCollection {

	ArrayList<SignalChannel> listChannels = new ArrayList<SignalChannel>();

	public ChannelCollection(SignalModule module, int channelNum) {
		// TODO Auto-generated constructor stub
		for (int i = 0; i < channelNum; i++) {
			listChannels.add(new SignalChannel(module, i));
		}
	}

	public SignalChannel getChannel(int index) {
		if (index >= 0 && index < listChannels.size()) {
			return listChannels.get(index);
		}
		return null;
	}

	public void processFrame(byte[] frame) {
		byte cmd = frame[3];

		switch (cmd) {
		case 0:
		case 1:
		case 2:
		case 0x17:
		case 0x15: {

			listChannels.get(0).putFrame(frame);
		}
			break;
		}

	}

	public void run() {
		for (SignalChannel channel : listChannels) {
			channel.run();
		}
	}

	public void stop() {
		for (SignalChannel channel : listChannels) {
			channel.stop();
		}
	}

}
