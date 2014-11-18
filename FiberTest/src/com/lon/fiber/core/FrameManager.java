package com.lon.fiber.core;


import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;
import android_serialport_api.SerialPort;

///串口数据解析
public class FrameManager {

	static final int MaxFrameLength = 16 * 1024; // 最大的数据链路层的帧大小
	static final byte ProtocolVersion = 1; // 协议版本
	final int[] HeaderTag = new int[] { 0x10, 0x02, }; // 帧头

	SerialPort serialPort;

	LinkedList<FrameFilter> listFilter = new LinkedList<FrameFilter>();

	Thread readThread; // 读串口数据

	public FrameManager(SerialPort serialPort) {
		// TODO Auto-generated constructor stub
		this.serialPort = serialPort;
		readThread = new Thread(new SerialRead(serialPort));
		readThread.setPriority(Thread.MAX_PRIORITY);
		readThread.start();

	}

	/*
	 * 添加数据帧过滤
	 */
	public IFrameFilter createFilter() {
		FrameFilter filter = new FrameFilter();

		synchronized (listFilter) {
			listFilter.add(filter);
		}
		return filter;
	}

	public void removeFilter(IFrameFilter filter) {
		if (FrameFilter.class.isInstance(filter)) {
			FrameFilter frameFilter = FrameFilter.class.cast(filter);
			synchronized (listFilter) {
				listFilter.remove(frameFilter);
			}
		}
	}

	class FrameFilter implements IFrameFilter {

		
		Queue<byte[]> frameQueue=new LinkedList<byte[]>();
		ReentrantLock lock = new ReentrantLock();
		Condition condition = lock.newCondition();
		int[] cmdId;

		public FrameFilter(int[] cmdId) {
			// TODO Auto-generated constructor stub
			this.cmdId = cmdId;
		}

		public FrameFilter() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public byte[] getFrame(int timeout) {
			// TODO Auto-generated method stub
			byte[] frame = null;
			lock.lock();
			try {

				if (frameQueue.size() > 0) {
					frame = frameQueue.poll();
				}
				if (frame == null && timeout != 0) {
					try {
						if (timeout > 0) {

							condition.await(timeout, TimeUnit.MILLISECONDS);

						} else {
							condition.await();
						}
					} catch (InterruptedException e) {
						// TODO: handle exception
					}
					if (frameQueue.size() > 0) {
						frame = frameQueue.poll();
					}
				}
				
			} finally {
				lock.unlock();
			}
			
			return frame;
		}

		protected void putFrame(byte[] frame, int length) {

			// 先判断是否匹配
			int cmd = frame[HeaderTag.length + 1];

			if (cmdId != null) // 必须匹配命令
			{
				boolean match = false;
				for (int i = 0; i < cmdId.length; i++) {
					if (cmdId[i] == cmd) {
						match = true;
						break;
					}
				}
				if (match == false)
					return;
			}
			byte[] data = new byte[length];
			System.arraycopy(frame, 0, data, 0, length);
			
			lock.lock();
			try {
				if (frameQueue.size() > 100) {
					
					frameQueue.poll();
				}
				frameQueue.offer(data);
				condition.signal();

			} finally {
				lock.unlock();
			}
		}

	}

	class SerialRead implements Runnable {

		byte[] rcvBuffer = new byte[MaxFrameLength]; // 接收缓存
		byte[] frameBuffer = new byte[MaxFrameLength]; // 数据帧

		int frameLength = 0;
		int dataLength = 0;

		boolean metDLE = false;

		SerialPort serialPort;

		public SerialRead(SerialPort serialPort) {
			// TODO Auto-generated constructor stub
			this.serialPort = serialPort;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {

				while (true) {
					int length = serialPort.read(rcvBuffer);
					
					ParseFrame(rcvBuffer, length);
				}

			} catch (Exception e) {
				// TODO: handle exception
			}

		}

		

		private void ParseFrame(byte[] rdBuffer, int length) {
			for (int i = 0; i < length; i++) {

				switch (rdBuffer[i]) {
				case (byte) 0x10:
					metDLE = !metDLE;
					if (metDLE) {
						frameBuffer[frameLength] = rdBuffer[i];
						frameLength++;
					}
					break;
				case (byte) 0x02: {
					if (metDLE) {
						frameBuffer[0] = 0x10;
						frameBuffer[1] = 0x02;
						frameLength = 2;
					} else if (frameLength >= 2) {
						frameBuffer[frameLength] = rdBuffer[i];
						frameLength++;
					}
					metDLE = false;
				}
					break;
				case (byte) 0x03: {
					if (metDLE) {
						frameBuffer[frameLength] = 0x03;
						frameLength++;
						if(frameLength>5)
						{
							
							for (FrameFilter filter : listFilter) {
								filter.putFrame(frameBuffer, frameLength);
							}
						}
						
						frameLength = 0;
						
					} else if (frameLength >= 2) {
						frameBuffer[frameLength] = rdBuffer[i];
						frameLength++;
					}
					metDLE = false;
				}
					break;
				default: {
					if (frameLength >= 2) {
						frameBuffer[frameLength] = rdBuffer[i];
						frameLength++;
					}
					metDLE = false;
				}
					break;
				}

				if (frameLength >= frameBuffer.length) // 数据溢出
				{
					metDLE = false;
					frameLength = 0;
				}

			}
		}

	}

}
