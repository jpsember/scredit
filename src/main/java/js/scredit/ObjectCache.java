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

import java.util.List;
import java.util.Map;

import js.base.BaseObject;
import js.geometry.MyMath;

/**
 * In-memory cache; thread safe
 */
public class ObjectCache<KEY, VALUE> extends BaseObject {

  public VALUE get(KEY scriptFile) {
    return mCache.get(scriptFile);
  }

  public VALUE put(KEY key, VALUE image) {
    if (image == null)
      badArg("No object for key:", key);
    trim();
    log("Storing", key);
    mCache.put(key, image);
    return image;
  }

  private void trim() {
    int size = mCache.size();
    log("trim; size:", size, "capacity:", mCapacity);
    if (size < mCapacity)
      return;
    auxTrim();
  }

  private synchronized void auxTrim() {
    List<KEY> keys = arrayList();
    keys.addAll(mCache.keySet());
    MyMath.permute(keys, null);
    int end = keys.size();
    for (int i = mCapacity / 2; i < end; i++) {
      KEY key = keys.get(i);
      log("removing", key);
      mCache.remove(key);
    }
  }

  public ObjectCache(int capacity) {
    mCapacity = capacity;
  }

  private Map<KEY, VALUE> mCache = concurrentHashMap();
  private int mCapacity = 100;
}
