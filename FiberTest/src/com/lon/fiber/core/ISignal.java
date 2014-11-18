package com.lon.fiber.core;

public interface ISignal {

	public SignalType getSignalType();
	
	/*
	 * ��ȡ��������
	 */
	public float getACAmpl();
	/*
	 * ��ȡֱ������
	 */
	public float getDCAmpl();
	
	public String getSignalInfo();
	
	public void copyTo(ISignal dest);
	
	public float[] getRawData();
	public float[] getSpectrumData();
	
	public SignalAmpl getAmpl();
	
	public String getUnit();
	
}