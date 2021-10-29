/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package js.scredit;

import static js.base.Tools.*;

import js.data.IntArray;

/**
 * Utilities to manipulate ordered sets of integers
 */
public final class SlotList {

  public static IntArray intersection(IntArray a, IntArray b) {
    IntArray.Builder outputList = IntArray.newBuilder();
    int cursor1 = 0;
    int cursor2 = 0;
    while (cursor1 < a.size() && cursor2 < b.size()) {
      int s1 = a.get(cursor1);
      int s2 = b.get(cursor2);
      if (s1 == s2)
        outputList.add(s1);
      if (s1 <= s2)
        cursor1++;
      if (s2 <= s1)
        cursor2++;
    }
    return outputList.build();
  }

  /**
   * Construct the slotlist {this \ b}
   */
  public static IntArray minus(IntArray a, IntArray b) {
    IntArray.Builder outputList = IntArray.newBuilder();
    int cursor1 = 0;
    int cursor2 = 0;
    while (cursor1 < a.size()) {
      int s1 = a.get(cursor1);
      if (cursor2 < b.size()) {
        int s2 = b.get(cursor2);
        if (s1 == s2) {
          cursor1++;
          cursor2++;
          continue;
        }
        if (s2 < s1) {
          cursor2++;
          continue;
        }
      }
      outputList.add(s1);
      cursor1++;
    }
    return outputList.build();
  }

  public static IntArray union(IntArray a, IntArray b) {
    IntArray.Builder outputList = IntArray.newBuilder();
    int cursor1 = 0;
    int cursor2 = 0;
    while (true) {
      int s1 = Integer.MAX_VALUE;
      int s2 = Integer.MAX_VALUE;
      if (cursor1 < a.size())
        s1 = a.get(cursor1);
      if (cursor2 < b.size())
        s2 = b.get(cursor2);
      int nextSlot = Math.min(s1, s2);
      if (nextSlot == Integer.MAX_VALUE)
        break;
      outputList.add(nextSlot);
      if (s1 == nextSlot)
        cursor1++;
      if (s2 == nextSlot)
        cursor2++;
    }
    return outputList.build();
  }

  public static IntArray complement(IntArray array, int domainSize) {
    IntArray.Builder outputList = IntArray.newBuilder();
    int omitIndex = 0;
    for (int slot = 0; slot < domainSize; slot++) {
      if (omitIndex < array.size() && slot == array.get(omitIndex)) {
        omitIndex++;
        continue;
      }
      outputList.add(slot);
    }
    checkState(omitIndex == array.size());
    return outputList.build();
  }

}
