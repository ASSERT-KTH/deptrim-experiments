/*
 * The MIT License
 *
 * Copyright (c) 2012, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.model.lazy;

import java.util.AbstractList;
import java.util.Arrays;

/**
 * {@code ArrayList&lt;Integer>} that uses {@code int} for storage.
 *
 * Plus a number of binary-search related methods that assume the array is sorted in the ascending order.
 *
 * @author Kohsuke Kawaguchi
 */
class SortedIntList extends AbstractList<Integer> {
    private int[] data;
    private int size;

    SortedIntList(int capacity) {
        this.data = new int[capacity];
        this.size = 0;
    }

    /**
     * Internal copy constructor.
     */
    SortedIntList(SortedIntList that) {
        this.data = new int[that.size + 8];
        System.arraycopy(that.data, 0, this.data, 0,
                         that.size);
        this.size = that.size;
    }

    /**
     * Binary search to find the position of the given string.
     *
     * @return
     *      -(insertionPoint+1) if the exact string isn't found.
     *      That is, -1 means the probe would be inserted at the very beginning.
     */
    public int find(int probe) {
        return Arrays.binarySearch(data, 0, size, probe);
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof Integer && contains(((Integer) o).intValue());
    }

    public boolean contains(int i) {
        return find(i) >= 0;
    }

    @Override
    public Integer get(int index) {
        if (size <= index)    throw new IndexOutOfBoundsException();
        return data[index];
    }

    @Override
    public int size() {
        return size;
    }

    public int max() {
        return size > 0 ? data[size - 1] : 0;
    }

    @Override
    public boolean add(Integer i) {
        return add(i.intValue());
    }

    public boolean add(int i) {
        ensureCapacity(size + 1);
        data[size++] = i;
        return true;
    }

    private void ensureCapacity(int i) {
        if (data.length < i) {
            int[] r = new int[Math.max(data.length * 2, i)];
            System.arraycopy(data, 0, r, 0, size);
            data = r;
        }
    }

    /**
     * Finds the index of the entry lower than v.
     */
    public int lower(int v) {
        return Boundary.LOWER.apply(find(v));
    }

    /**
     * Finds the index of the entry greater than v.
     */
    public int higher(int v) {
        return Boundary.HIGHER.apply(find(v));
    }

    /**
     * Finds the index of the entry lower or equal to v.
     */
    public int floor(int v) {
        return Boundary.FLOOR.apply(find(v));
    }

    /**
     * Finds the index of the entry greater or equal to v.
     */
    public int ceil(int v) {
        return Boundary.CEIL.apply(find(v));
    }

    public boolean isInRange(int idx) {
        return 0 <= idx && idx < size;
    }

    public void sort() {
        Arrays.sort(data, 0, size);
    }

    public void copyInto(int[] dest) {
        System.arraycopy(data, 0, dest, 0, size);
    }

    public void removeValue(int n) {
        int idx = find(n);
        if (idx < 0)  return;
        System.arraycopy(data, idx + 1, data, idx, size - (idx + 1));
        size--;
    }
}
