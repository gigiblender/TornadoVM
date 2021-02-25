package uk.ac.manchester.tornado.drivers.spirv;

import uk.ac.manchester.tornado.drivers.opencl.*;

import java.util.ArrayList;
import java.util.List;

public class SPIRVOpenCLPlatform implements SPIRVPlatform {

    private TornadoPlatform oclPlatform;
    private OCLExecutionEnvironment context;
    private List<SPIRVDevice> spirvDevices;

    public SPIRVOpenCLPlatform(int platformIndex, TornadoPlatform oclPlatform) {
        this.oclPlatform = oclPlatform;
        context = this.oclPlatform.createContext();

        spirvDevices = new ArrayList<>();

        for (int i = 0; i < context.getNumDevices(); i++) {
            SPIRVDevice spirvDevice = new SPIRVOCLDevice(platformIndex, i, context.devices().get(i));
            spirvDevices.add(spirvDevice);
        }

    }

    public TornadoPlatform getPlatform() {
        return this.oclPlatform;
    }

    @Override
    public int getNumDevices() {
        return context.getNumDevices();
    }

    @Override
    public SPIRVDevice getDevice(int deviceIndex) {
        return spirvDevices.get(deviceIndex);
    }

    @Override
    public SPIRVOCLContext createContext() {
        if (context == null) {
            context = oclPlatform.createContext();
        }
        return new SPIRVOCLContext(this, spirvDevices, context);
    }

}
