/*
 * This file is part of Tornado: A heterogeneous programming framework:
 * https://github.com/beehive-lab/tornadovm
 *
 * Copyright (c) 2013-2020, APT Group, Department of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GNU Classpath is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * GNU Classpath is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GNU Classpath; see the file COPYING. If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module. An independent module is a module which is not derived from
 * or based on this library. If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 *
 */
package uk.ac.manchester.tornado.api.collections.types;

import java.nio.FloatBuffer;

import uk.ac.manchester.tornado.api.data.nativetypes.FloatArray;

public class VectorFloat3 implements PrimitiveStorage<FloatBuffer> {

    private static final int ELEMENT_SIZE = 3;
    /**
     * backing array.
     */
    protected final FloatArray storage;
    /**
     * number of elements in the storage.
     */
    private final int numElements;

    /**
     * Creates a vector using the provided backing array.
     *
     * @param numElements
     *     Number of elements
     * @param array
     *     array to be copied
     */
    protected VectorFloat3(int numElements, FloatArray array) {
        this.numElements = numElements;
        this.storage = array;
    }

    /**
     * Creates a vector using the provided backing array.
     */
    public VectorFloat3(FloatArray array) {
        this(array.getSize() / ELEMENT_SIZE, array);
    }

    /**
     * Creates an empty vector with.
     *
     * @param numElements
     *     Number of elements
     */
    public VectorFloat3(int numElements) {
        this(numElements, new FloatArray(numElements * ELEMENT_SIZE));
    }

    public FloatArray getArray() {
        return storage;
    }

    private int toIndex(int index) {
        return (index * ELEMENT_SIZE);
    }

    /**
     * Returns the float at the given index of this vector.
     *
     * @param index
     *     Position
     * @return {@link Float3}
     */
    public Float3 get(int index) {
        return new Float3(storage.get(toIndex(index)), storage.get(toIndex(index + 1)), storage.get(toIndex(index + 2)));
    }

    /**
     * Sets the float at the given index of this vector.
     *
     * @param index
     *     Position
     * @param value
     *     Value to be set
     */
    public void set(int index, Float3 value) {
        float x = value.getX();
        float y = value.getY();
        float z = value.getZ();
        storage.set(toIndex(index), x);
        storage.set(toIndex(index + 1), y);
        storage.set(toIndex(index + 2), z);
    }

    /**
     * Sets the elements of this vector to that of the provided vector.
     *
     * @param values
     *     set an input array into the internal array
     */
    public void set(VectorFloat3 values) {
        for (int i = 0; i < numElements; i++) {
            set(i, values.get(i));
        }
    }

    /**
     * Sets the elements of this vector to that of the provided array.
     *
     * @param values
     *     set an input array into the internal array
     */
    public void set(FloatArray values) {
        VectorFloat3 vector = new VectorFloat3(values);
        for (int i = 0; i < numElements; i++) {
            set(i, vector.get(i));
        }
    }

    public void fill(float value) {
        for (int i = 0; i < storage.getSize(); i++) {
            storage.set(i, value);
        }
    }

    /**
     * Duplicates this vector.
     *
     * @return A new vector
     */
    public VectorFloat3 duplicate() {
        VectorFloat3 vector = new VectorFloat3(numElements);
        vector.set(this);
        return vector;
    }

    public String toString() {
        if (this.numElements > ELEMENT_SIZE) {
            return String.format("VectorFloat3 <%d>", this.numElements);
        }
        StringBuilder tempString = new StringBuilder();
        for (int i = 0; i < numElements; i++) {
            tempString.append(" ").append(this.get(i).toString());
        }
        return tempString.toString();
    }

    public Float3 sum() {
        Float3 result = new Float3();
        for (int i = 0; i < numElements; i++) {
            result = Float3.add(result, get(i));
        }
        return result;
    }

    public Float3 min() {
        Float3 result = new Float3();
        for (int i = 0; i < numElements; i++) {
            result = Float3.min(result, get(i));
        }
        return result;
    }

    public Float3 max() {
        Float3 result = new Float3();
        for (int i = 0; i < numElements; i++) {
            result = Float3.max(result, get(i));
        }
        return result;
    }

    @Override
    public void loadFromBuffer(FloatBuffer buffer) {
        asBuffer().put(buffer);
    }

    @Override
    public FloatBuffer asBuffer() {
        return storage.getSegment().asByteBuffer().asFloatBuffer();
    }

    @Override
    public int size() {
        return storage.getSize();
    }

    public int getLength() {
        return numElements;
    }

}
