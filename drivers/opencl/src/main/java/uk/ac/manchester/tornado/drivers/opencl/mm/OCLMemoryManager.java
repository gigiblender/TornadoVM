/*
 * This file is part of Tornado: A heterogeneous programming framework: 
 * https://github.com/beehive-lab/tornado
 *
 * Copyright (c) 2013-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Authors: James Clarkson
 *
 */
package uk.ac.manchester.tornado.drivers.opencl.mm;

import static uk.ac.manchester.tornado.common.exceptions.TornadoInternalError.guarantee;
import static uk.ac.manchester.tornado.drivers.opencl.OpenCL.OCL_CALL_STACK_LIMIT;

import java.lang.reflect.Method;

import uk.ac.manchester.tornado.api.Parallel;
import uk.ac.manchester.tornado.api.meta.ScheduleMetaData;
import uk.ac.manchester.tornado.api.meta.TaskMetaData;
import uk.ac.manchester.tornado.common.RuntimeUtilities;
import uk.ac.manchester.tornado.common.Tornado;
import uk.ac.manchester.tornado.common.TornadoLogger;
import uk.ac.manchester.tornado.common.TornadoMemoryProvider;
import uk.ac.manchester.tornado.common.exceptions.TornadoOutOfMemoryException;
import uk.ac.manchester.tornado.drivers.opencl.OCLDeviceContext;
import uk.ac.manchester.tornado.drivers.opencl.enums.OCLMemFlags;
import uk.ac.manchester.tornado.drivers.opencl.graal.OCLInstalledCode;
import uk.ac.manchester.tornado.drivers.opencl.graal.backend.OCLBackend;
import uk.ac.manchester.tornado.meta.domain.DomainTree;
import uk.ac.manchester.tornado.meta.domain.IntDomain;

public class OCLMemoryManager extends TornadoLogger implements TornadoMemoryProvider {

    private final ScheduleMetaData scheduleMeta;
    private final long callStackLimit;
    private long callStackPosition;
    private long deviceBufferAddress;
    private final OCLDeviceContext deviceContext;

    private long buffer;
    private long heapLimit;

    private long heapPosition;

    private boolean initialised;

    private OCLInstalledCode initFP64Code;
    private OCLInstalledCode initFP32Code;
    private OCLInstalledCode initU32Code;
    private OCLCallStack initCallStack;
    private DomainTree initThreads;
    private TaskMetaData initMeta;

    public OCLMemoryManager(final OCLDeviceContext device) {

        deviceContext = device;
        callStackLimit = OCL_CALL_STACK_LIMIT;
        initialised = false;
//        System.out.printf("device id %s\n", device.getId());
        scheduleMeta = new ScheduleMetaData("mm-" + device.getDeviceId());

        reset();
    }

    @Override
    public long getCallStackAllocated() {
        return callStackPosition;
    }

    @Override
    public long getCallStackRemaining() {
        return callStackLimit - callStackPosition;
    }

    @Override
    public long getCallStackSize() {
        return callStackLimit;
    }

    @Override
    public long getHeapAllocated() {
        return heapPosition - callStackLimit;
    }

    @Override
    public long getHeapRemaining() {
        return heapLimit - heapPosition;
    }

    private void initFP64(double[] data, int count) {
        for (@Parallel int i = 0; i < count; i++) {
            data[i] = 0;
        }
    }

    private void initFP32(float[] data, int count) {
        for (@Parallel int i = 0; i < count; i++) {
            data[i] = -1f;
        }
    }

    private void initU32(int[] data, int count) {
        for (@Parallel int i = 0; i < count; i++) {
            data[i] = 0;
        }
    }

    private Method getMethod(final String name, Class<?> type1) {
        Method method = null;
        try {
            method = this.getClass().getDeclaredMethod(name, type1, int.class);
            method.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            Tornado.fatal("unable to find " + name + " method: " + e.getMessage());
        }
        return method;
    }

    private void createMemoryInitializers(final OCLBackend backend) {
//        initThreads = new DomainTree(1);
//        initMeta = new OCLMeta(2);
//        initMeta.addProvider(TornadoDevice.class, backend.getDeviceContext().asMapping());

//    	initFP64Code = OCLCompiler.compileCodeForDevice(
//				TornadoRuntime.resolveMethod(getMethod("initFP64",double[].class)), null, initMeta, (OCLProviders) backend.getProviders(), backend);
//        initFP32Code = OCLCompiler.compileCodeForDevice(
//                getTornadoRuntime().resolveMethod(getMethod("initFP32", float[].class)), null, initMeta, (OCLProviders) backend.getProviders(), backend);
//        initU32Code = OCLCompiler.compileCodeForDevice(
//                getTornadoRuntime().resolveMethod(getMethod("initU32", int[].class)), null, initMeta, (OCLProviders) backend.getProviders(), backend);
        initCallStack = createCallStack(4);

    }

    public final void reset() {
        callStackPosition = 0;
        heapPosition = callStackLimit;
        Tornado.info("Reset heap @ 0x%x (%s) on %s", deviceBufferAddress,
                RuntimeUtilities.humanReadableByteCount(heapLimit, true),
                deviceContext.getDevice().getName());
    }

    @Override
    public long getHeapSize() {
        return heapLimit - callStackLimit;
    }

    private static long align(final long address, final long alignment) {
        return (address % alignment == 0) ? address : address
                + (alignment - address % alignment);
    }

    public long tryAllocate(final Class<?> type, final long bytes, final int headerSize, int alignment)
            throws TornadoOutOfMemoryException {
        final long alignedDataStart = align(heapPosition + headerSize, alignment);
        final long headerStart = alignedDataStart - headerSize;
        if (headerStart + bytes < heapLimit) {
            heapPosition = headerStart + bytes;

//            final long byteCount = bytes - headerSize;
//            if(type != null && type.isArray() && RuntimeUtilities.isPrimitiveArray(type)){
//            	if(type == double[].class ){
//            		initialiseMemory(initFP64Code,offset + headerSize, (int) (byteCount / 8));
//            	} else if(type == float[].class){
//            		initialiseMemory(initFP32Code,offset + headerSize, (int) (byteCount / 4));
//            	} else {
//            		TornadoInternalError.guarantee(byteCount % 4 == 0, "array is not divisible by 4");
//            		initialiseMemory(initU32Code,offset + headerSize, (int) (byteCount / 4));
//            	}
//            } else {
//            	TornadoInternalError.guarantee(byteCount % 4 == 0, "array is not divisible by 4");
//            	initialiseMemory(initU32Code,offset + headerSize, (int) (byteCount/4));
//            }
        } else {
            throw new TornadoOutOfMemoryException("Out of memory on device: "
                    + deviceContext.getDevice().getName());
        }

        return headerStart;
    }

    private void initialiseMemory(OCLInstalledCode code, long offset, int count) {
        if (count <= 0) {
            return;
        }

        initCallStack.reset();

        initCallStack.putLong(offset);
        initCallStack.putInt(count);

        final TaskMetaData meta = new TaskMetaData(scheduleMeta, "init-call-stack", 0);
        initThreads.set(0, new IntDomain(0, 1, count));
        meta.setDomain(initThreads);

        code.execute(initCallStack, meta);
    }

    public OCLCallStack createCallStack(final int maxArgs) {

        OCLCallStack callStack = new OCLCallStack(callStackPosition, maxArgs,
                deviceContext);

        if (callStackPosition + callStack.getSize() < callStackLimit) {
            callStackPosition = align(callStackPosition + callStack.getSize(),
                    32);
        } else {
            callStack = null;
            fatal("Out of call-stack memory");
            System.exit(-1);
        }

        return callStack;
    }

    public long getBytesRemaining() {
        return heapLimit - heapPosition;
    }

    /**
     * *
     * Returns sub-buffer that can be use to access a region managed by the
     * memory manager.
     *
     * @param offset offset within the memory managers heap
     * @param length size in bytes of the sub-buffer
     *
     * @return
     */
    public OCLByteBuffer getSubBuffer(final int offset, final int length) {
        return new OCLByteBuffer(deviceContext, offset, length);
    }

    public void allocateRegion(long numBytes) {

        /*
         * Allocate space on the device
         */
        heapLimit = numBytes;
        buffer = deviceContext.getPlatformContext().createBuffer(
                OCLMemFlags.CL_MEM_READ_WRITE, numBytes);
    }

    public void init(OCLBackend backend, long address) {
        deviceBufferAddress = address;
        initialised = true;
        info("Located heap @ 0x%x (%s) on %s", deviceBufferAddress,
                RuntimeUtilities.humanReadableByteCount(heapLimit, false),
                deviceContext.getDevice().getName());

        scheduleMeta.setDevice(backend.getDeviceContext().asMapping());
//        createMemoryInitializers(backend);
    }

    public long toAbsoluteAddress() {
        return deviceBufferAddress;
    }

    public long toAbsoluteDeviceAddress(final long address) {
        long result = address;

        guarantee(address + deviceBufferAddress >= 0, "absolute address may have wrapped arround: %d + %d = %d", address, deviceBufferAddress, address + deviceBufferAddress);
        result += deviceBufferAddress;

        return result;
    }

    public long toBuffer() {
        return buffer;
    }

    public long toRelativeAddress() {
        return 0;
    }

    public long toRelativeDeviceAddress(final long address) {
        long result = address;
//        guarantee(address - deviceBufferAddress < 0, "relative address may have wrapped arround: %d + %d = %d", address,deviceBufferAddress,address+deviceBufferAddress);
        if (!(Long.compareUnsigned(address, deviceBufferAddress) < 0 || Long
                .compareUnsigned(address, (deviceBufferAddress + heapLimit)) > 0)) {
            result -= deviceBufferAddress;
        }
        return result;
    }

    public boolean isInitialised() {
        return initialised;
    }
}
