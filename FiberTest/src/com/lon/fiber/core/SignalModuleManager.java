package com.lon.fiber.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import android.R.integer;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class SignalModuleManager {

	private List<SignalModule> modulesList = new ArrayList<SignalModule>();
	static final String[] portNames1 = new String[] { "/dev/ttySAC3"};
	static final String[] portNames2 = new String[] { "/dev/ttyO2",
			"/dev/ttyO3", "/dev/ttyO4" };
	static final int Baudrate = 250000;

	private static SignalModuleManager singleton = null;

	private SignalModuleManager(UsbManager usbManager) {
		// TODO Auto-generated constructor stub
		// 加入串口设备是否存在的自动 判断

		

		File serial1 = new File(portNames1[0]);
		File serial2 = new File(portNames2[0]);
		if (serial1.exists()) {
			for (int i = 0; i < portNames1.length; i++) {
				SignalModule module = new SignalModule(portNames1[i], Baudrate,
						i);
				module.run();
				modulesList.add(module);
			}
			return ;
		} 
		
		if (serial2.exists()) {
			for (int i = 0; i < portNames2.length; i++) {
				SignalModule module = new SignalModule(portNames2[i], Baudrate,
						i);
				module.run();
				modulesList.add(module);
			}
			return;
		}
		
		List<UsbSerialPort> usbList = new ArrayList<UsbSerialPort>();

		final List<UsbSerialDriver> drivers = UsbSerialProber
				.getDefaultProber().findAllDrivers(usbManager);

		for (final UsbSerialDriver driver : drivers) {
			final List<UsbSerialPort> ports = driver.getPorts();

			for (UsbSerialPort port : ports) {
				UsbDeviceConnection connection = usbManager.openDevice(port
						.getDriver().getDevice());
				
				try {

					port.open(connection);
					port.setParameters(Baudrate, 8, UsbSerialPort.STOPBITS_1,
							UsbSerialPort.PARITY_NONE);

					usbList.add(port);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					try {
						port.close();
					} catch (IOException e2) {
						// Ignore.
					}
					e.printStackTrace();
				}
			}
		}
		if (usbList.size() > 0) {
			for (int i = 0; i < usbList.size(); i++) {
				SignalModule module = new SignalModule(usbList.get(i), i);
				module.run();
				modulesList.add(module);
			}
			return;
		}

	}

	public static SignalModuleManager getInstance(UsbManager usb_manager) {
		if (singleton == null) {
			singleton = new SignalModuleManager(usb_manager);
		}
		return singleton;
	}

	public static SignalModuleManager getInstance() {

		return singleton;
	}

	public SignalChannel getChannel(int num) {

		int modIndex = 0;// num/3;
		int chIndex = 0;// num%3;
		SignalModule module = getModule(modIndex);
		if (module != null) {
			return module.getChannel(chIndex);
		}
		return null;
	}

	public SignalModule getModule(int num) {
		if (num >= 0 && num < modulesList.size()) {
			return modulesList.get(num);
		}
		return null;
	}

	public int getModuleNum() {
		return modulesList.size();
	}

	public void stop() {

		for (SignalModule module : modulesList) {
			module.stop();
		}
	}

}
