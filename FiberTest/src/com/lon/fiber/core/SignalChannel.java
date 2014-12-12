package com.lon.fiber.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.lon.fiber.dsp.DSPUtil;
import com.lon.fiber.dsp.FFTPlan;
import com.lon.fiber.dsp.SignalUtil;

import android.R.bool;
import android.R.integer;
import android.util.Log;
import android.widget.ListView;

///信号通道
public class SignalChannel {

	private SignalModule module;
	private ArrayList<DataBlock> listDataBlocks = new ArrayList<DataBlock>(); // 数据块
	private ArrayList<WorkMode> listWorkModes = new ArrayList<WorkMode>(); // 工作模式列表

	private WorkMode currentWorkMode = null;// 当前工作模式

	private int channelNum = 0;

	private boolean needSpectrum=false;
	
	byte[] OneSampleFrame=new byte[16*1024]; //这个地方需要完善，假定采样率固定为8000,多余的表示模式
	
	int OneSampleLength=0;
	
	int prevSampleNum=-100;
	
	ArrayList<ISignal> listSignals = new ArrayList<ISignal>();

	ISignal currentSignal;

	ReentrantLock lock = new ReentrantLock();
	Condition condition = lock.newCondition();

	Thread threadCheck;
	Thread threadDSP;
	
	int selfTestResult=-1;
	int realWorkMode=-1; // 0-自检模式 1-工作模式
	int needWorkMode=-1;
	
	private List<ISignalChangedListener> signalChangedListeners=new ArrayList<ISignalChangedListener>();
	
	private List<IWorkModeChangedListener> workModeChangedListeners=new ArrayList<IWorkModeChangedListener>();

	/*
	 * @param SignalModule 信号模块
	 * 
	 * @param num 信号通道的编号 从0开始
	 */
	public SignalChannel(SignalModule module, int channelNum) {
		// TODO Auto-generated constructor stub
		this.module = module;
		this.channelNum = channelNum;
		listSignals.add(new SignalSingle()); // 单频信号
		listSignals.add(new SignalFSK());// 移频和UM71信号
		listSignals.add(new SignalUnknown());// 未知信号
		listSignals.add(new SignalNULL());// 小信号
		currentWorkMode = new WorkMode((byte)0,6250,255,4096,0,5,0,"V","","");
		
		for (DataBlock block : listDataBlocks) {
			block.setSampleRate(6250);
		}
	}

	public int getChannelNum() {
		return channelNum;
	}

	public WorkMode getCurrentMode() {
		return currentWorkMode;
	}

	public List<WorkMode> getModeList() {
		return listWorkModes;
	}

	public ISignal getCurrentSignal() {
		return currentSignal;
	}

	public int getSelfTestResult()
	{
		return selfTestResult;
	}
	int prevNUM=0;
	public void putFrame(byte[] frame) {

		lock.lock();
		try {
			condition.signal();
		} finally {
			lock.unlock();
		}

		int cmd = frame[3];
		switch (cmd) {
		case 0: // 所有工作模式
		{
			// 移除所有工作模式
			synchronized (listWorkModes) {
				listWorkModes.clear();

				byte num = frame[9]; // 工作模式个数
				for (int i = 0; i < num; i++) {
					int index = 10 + i * 77; //加入模块的ID号
					ByteBuffer byteBuffer = ByteBuffer.wrap(frame, index, 77);
					byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
					
					byte mode = byteBuffer.get();
					byte[] modID=new byte[10];
					//模块ID
					byteBuffer.get(modID);
					StringBuilder sbBuilder=new StringBuilder();
					for(int j=0;j<10;j++)
					{
						String string=Integer.toHexString(modID[j]&0xff);
						if(string.length()<2)
						{
							sbBuilder.append("0");
							sbBuilder.append(string);
						}
						else
						{
							sbBuilder.append(string);
						}
						
					}
					
					//模式描述
					byte[] desc=new byte[40];
					byteBuffer.get(desc);
					int byteCount=0;
					for(int j=0;j<40;j++)
					{
						if(desc[j]==0) break;
						byteCount++;
					}
					String descriptor=new String(desc, 0, byteCount, Charset.forName("GBK"));
					int sampleRate = byteBuffer.getInt();
					int transformNum = byteBuffer.getShort();
					int adMax = byteBuffer.getShort();
					int adMin = byteBuffer.getShort();
					float upper = byteBuffer.getFloat();
					float lower = byteBuffer.getFloat();

					int sLen = 0;
					for (int j = 0; j < 8; j++) {
						if (frame[index + 69 + j] == 0)
							break;
						sLen++;
					}
					String unit = new String(frame, index + 69, sLen);
					String strModID=sbBuilder.toString().toUpperCase();
					WorkMode workMode = new WorkMode(mode, sampleRate,
							transformNum, adMax, adMin, upper, lower, unit,descriptor,strModID);
					listWorkModes.add(workMode);
					Log.e("模块ID", strModID);
				}

			}

		}
			break;
		case 1: // 工作模式改变
		{
			synchronized (listWorkModes) {
				Log.e("data", "模式改变");
				int mode = frame[9] & 0xff;
				if (mode == 0xff) {
					currentWorkMode = null;
					
				} else {

					for (int i = 0; i < listWorkModes.size(); i++) {
						WorkMode wkMode = listWorkModes.get(i);
						if ((wkMode.getMode() & 0xff) == mode) {
							currentWorkMode = wkMode;
							break;
						}
					}
				}
				WorkModeChanged(currentWorkMode); //触发模式改变事件
				int sampleRate = 0;
				if (currentWorkMode != null) {
					sampleRate = currentWorkMode.getSampleRate();
				}
				for (DataBlock block : listDataBlocks) {
					block.setSampleRate(sampleRate);
				}
			}
		}
			break;
		case 2: // 当前工作模式
		{

		}
			break;
		case 0x17: //自检结果
		{
			this.realWorkMode=0;
			this.selfTestResult=(int)(frame[4]&0x0ff);
			break;
		}
		case 0x15: // 采集数据
		{
			this.realWorkMode=1;
			int sampleNum = (frame[4] & 0x0ff)+ ((frame[5] & 0x0ff) << 8);
			int dataLength = (frame[2] & 0x0ff);
			
			
			
			if (currentWorkMode != null) {
				
				//currentWorkMode.getModeInfo(OneSampleFrame, 8000*2); //获取模式信息
				parseOneSample(sampleNum,frame,6,dataLength-5);
				
				float[] realData = currentWorkMode.getRealData(frame, 6,
						dataLength - 5);
				
				synchronized (listDataBlocks) {

					for (DataBlock block : listDataBlocks) {
					
						
						block.putSampleData(realData, sampleNum);
					}
				}
			}
		}
			break;
		}

	}
	
	private void parseOneSample(int sampleNum,byte[] buffer,int offset,int length)
	{
		if(prevSampleNum+1!=sampleNum)
		{
			Log.e("MISS:"+(module.getModuleNum()*3+ channelNum), String.valueOf(sampleNum));
			
			OneSampleLength=0; //重置
		}
		prevSampleNum=sampleNum;
		for(int i=0;i<length;i++)
		{
			OneSampleFrame[OneSampleLength]=buffer[i+offset];
			OneSampleLength++;
			if(OneSampleLength>=8000*2) //找到一帧
			{
				FileManager.getInstance().putSampleData(module.getModuleNum()*3+channelNum, OneSampleFrame);
				OneSampleLength=0;
			}
		}
	}
	
	public void sendFrame(byte[] frame) {
		this.module.sendFrame(frame);
	}

	public void run() {
		if (threadCheck == null || threadCheck.isInterrupted()) {
			threadCheck = new Thread(new SelfCheck());
			threadCheck.start();
		}
		if (threadDSP == null || threadDSP.isInterrupted()) {
			threadDSP = new Thread(new SignalDSP());
			threadDSP.start();
		}
	}

	public void stop() {
		if (threadCheck != null && !threadCheck.isInterrupted()) {
			threadCheck.interrupt();
		}
		if (threadDSP != null && !threadDSP.isInterrupted()) {
			threadDSP.interrupt();
		}
	}

	public IDataBlock addDataBlock(int timeSegmnet, int timeLength) {

		if (timeSegmnet > timeLength) {
			timeSegmnet = timeLength;
		}
		DataBlock block = null;
		synchronized (listDataBlocks) {
			int sampleRate = 0;
			if (currentWorkMode != null) {
				sampleRate = currentWorkMode.getSampleRate();
			}
			block = new DataBlock(timeSegmnet, timeLength, 6250);

			listDataBlocks.add(block);
		}

		return block;
	}

	public void removeDataBlock(IDataBlock block) {

		if (DataBlock.class.isInstance(block)) {
			DataBlock iBlock = DataBlock.class.cast(block);
			synchronized (listDataBlocks) {
				listDataBlocks.remove(iBlock);
			}
		}
	}

	private void queryAllWorkMode() {
		byte[] frame = new byte[2];
		frame[0] = (byte) 0x80;
		frame[1] = (byte) channelNum;
		module.sendFrame(frame);

	}

	//设置自检模式
	public void setSelfTest()
	{
		
		byte[] frame = new byte[1];
		frame[0] = (byte) 0x11;
		module.sendFrame(frame);
	}
	
	//设置工作模式
	public void setWorkMode()
	{
		
		byte[] frame = new byte[1];
		frame[0] = (byte) 0x12;
		module.sendFrame(frame);
	}
	
	public void setMode(int mode)
	{
		this.realWorkMode=-1;
		this.needWorkMode=mode;
		switch(mode)
		{
		case 0:
			setSelfTest();
			break;
		case 1:
			setWorkMode();
			break;
		}
	}
	
	public void setWorkMode(byte mode) {
		byte[] frame = new byte[3];

		frame[0] = (byte)0x81;
		frame[1] = (byte) channelNum;
		frame[2] = mode;

		sendFrame(frame);
	}

	private void WorkModeChanged(WorkMode mode)
	{
		synchronized (workModeChangedListeners) {
			for(IWorkModeChangedListener listener:workModeChangedListeners)
			{
				listener.onWorkModeChanged(mode);
			}
		}
	}
	private void setSignal(ISignal signal) {
		if(signal==null)
		{
			currentSignal=null; 
			synchronized (signalChangedListeners) {
				for(ISignalChangedListener listener:signalChangedListeners)
				{
					listener.onSignalChanged(null);
				}
			}
			return;
		}
		SignalType signalType = signal.getSignalType();
		for (ISignal s : listSignals) {
			if (s.getSignalType() == signalType) {
				signal.copyTo(s);

				currentSignal = s;
				synchronized (signalChangedListeners) {
					for(ISignalChangedListener listener:signalChangedListeners)
					{
						listener.onSignalChanged(currentSignal);
					}
				}
				break;
			}
		}
	}
	
	public void addSignalChangedListener(ISignalChangedListener listener) {
		synchronized (signalChangedListeners) {
			signalChangedListeners.add(listener);
		}
		
	}
	
	public void removeSignalChangedListener(ISignalChangedListener listener) {
		synchronized (signalChangedListeners) {
			signalChangedListeners.remove(listener);
		}
	}
	
	public void addWorkModeChangedListener(IWorkModeChangedListener listener)
	{
		synchronized (workModeChangedListeners) {
			workModeChangedListeners.add(listener);
		}
	}
	
	public void remodeWorkModeChangedListner(IWorkModeChangedListener listener)
	{
		synchronized (workModeChangedListeners) {
			workModeChangedListeners.remove(listener);
		}
	}
	
	

	/*
	 * 自身参数检测
	 */
	class SelfCheck implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			boolean alive = false;
			while (!Thread.currentThread().isInterrupted()) {

				lock.lock();
				try {
					alive = condition.await(2000, TimeUnit.MILLISECONDS);

				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
				if (!alive) // 没有接收到数据
				{
					realWorkMode=-1;
				} 
				if(needWorkMode!=realWorkMode)
				{
					switch(needWorkMode)
					{
					case 0:
						SignalChannel.this.setSelfTest();
						break;
					case 1:
						SignalChannel.this.setWorkMode();
						break;
					}
				}
				
			}

		}

	}

	
	private float getRealAmpl(float ampl)
	{
		if(currentWorkMode!=null)
		{
			float realAmpl=CalManager.getInstance().getRealAmpl(currentWorkMode.moduleID, channelNum, ampl);
			return realAmpl;
		}
		return ampl;
	}
	
	/*
	 * 采集信号的数字处理
	 */
	class SignalDSP implements Runnable {

		FFTPlan fftPlan;
		SignalUtil amplTool = new SignalUtil();
		DSPUtil util = new DSPUtil();
		float[] data1 = null;
		float[] data2 = null;
		float[] dataSpectrum=null;
		float[] peakVal = new float[5];
		int[] peakIndex = new int[5];

		float[] amplDense=new float[25];
		
		float[] dcacAmpl=new float[2];
		float[] amplTemp=new float[16*1024];
		@Override
		public void run() {
			// TODO Auto-generated method stub

			IDataBlock dataBlock = addDataBlock(1000, 1000);
			
			
			try {
				while (!Thread.currentThread().isInterrupted()) {
					float[] sampleData = dataBlock.getBlock(-1);
					SignalModuleManager manager=SignalModuleManager.getInstance();
					synchronized (manager) {
						
						
						long millisTime=System.currentTimeMillis(); //获取系统时间
						
						if (sampleData == null)
							continue;
						
						
						
						if (currentWorkMode == null)
							continue; // 不太可能
						
						int sampleRate = currentWorkMode.getSampleRate();
						if (sampleData.length != sampleRate)
							continue;
						
						
					
						//计算交直流幅度
						amplTool.calDCACAmpl(sampleData, 0, sampleRate, dcacAmpl);
						
						dcacAmpl[0]=getRealAmpl(dcacAmpl[0]);
						dcacAmpl[1]=getRealAmpl(dcacAmpl[1]);
						
						SignalAmpl signalAmplA=new SignalAmpl();
						//最多每秒计算25次,因为测量的最小的信号的频率是25Hz
						int amplCnt=sampleRate/25;
						boolean largeChanged=false;
						for(int i=0;i<25;i++)
						{
							amplDense[i]=amplTool.calAmpl(sampleData, i*amplCnt,
									amplCnt);
							amplDense[i]=getRealAmpl(amplDense[i]);
							//signalAmplA.addAmpl(amplDense[i], millisTime+i*40);
							if(i>0) //计算这1秒内的最大的变化的斜率
							{
								if(Math.abs(amplDense[i]-amplDense[i-1])>0.1f)
								{
									largeChanged=true;
								}
							}
						}
						if(largeChanged)
						{
							for(int i=0;i<25;i++)
							{
								signalAmplA.addAmpl(amplDense[i], millisTime+i*40);
							}
						}
						else
						{
							signalAmplA.addAmpl(amplDense[24], millisTime+24*40); //加入最后一个点
						}
						
						//每秒钟计算

						float signalAmpl=amplDense[24];
						
						//看看幅度是不是很小
						
						if (fftPlan != null && fftPlan.getFFTNum() != sampleRate) {
							fftPlan = null;
							System.gc();
						}
						if (fftPlan == null) {
							fftPlan = new FFTPlan(sampleRate);
						}
						if (data1 != null && data1.length < sampleData.length * 2) {
							data1 = null;
							System.gc();
						}
						if (data2 != null && data2.length < sampleData.length * 2) {
							data2 = null;
							System.gc();
						}
						if(dataSpectrum!=null && dataSpectrum.length<sampleData.length/2)
						{
							dataSpectrum=null;
						}
						if (data1 == null) {
							data1 = new float[sampleData.length * 2];
						}
						if (data2 == null) {
							data2 = new float[sampleData.length * 2];
						}
						if(dataSpectrum==null)
						{
							dataSpectrum=new float[sampleData.length/2]; //实数的频谱是对称的
						}
						System.arraycopy(sampleData, 0, data1, 0, sampleData.length);
						fftPlan.realForward(data1, 0);
						
						//计算频谱
						if (needSpectrum) {
							int spectrumLen = sampleData.length / 2;
							for (int i = 0; i < spectrumLen; i++) {
								dataSpectrum[i] = (float) Math.sqrt(data1[i * 2]
										* data1[i * 2] + data1[i * 2 + 1]
										* data1[i * 2 + 1]);
							}
						}
						
							SignalUnknown signal = new SignalUnknown(signalAmplA,currentWorkMode.getUnit());
							signal.setDCAmpl(dcacAmpl[0]);
							signal.setACAmpl(dcacAmpl[1]);
							signal.putRawData(sampleData);
							signal.putSpectrumData(dataSpectrum);
							SignalChannel.this.setSignal(signal);
							//Log.e("Unknown-Time", String.valueOf(System.currentTimeMillis()-millisTime));
						
						
						
					}
					
					

				}
			}  finally {
				removeDataBlock(dataBlock);
			}

		}

	}

}
