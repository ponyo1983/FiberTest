package com.lon.fiber.core;

public class SignalAmplPoint {

	float ampl;
	long millisTime;
	public SignalAmplPoint(float ampl,long millisTime)
	{
		this.ampl=ampl;
		this.millisTime=millisTime;
	}
	
	public float getAmpl()
	{
		return ampl;
	}
	
	public long getTime()
	{
		return millisTime;
	}
}
