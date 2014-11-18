package com.lon.fiber.core;

public interface IDataBlock {

	float[] getBlock(int timeout);

    int getSampleRate();
}
