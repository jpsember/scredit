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
package js.guiapp;

import static js.base.Tools.*;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import js.base.BaseObject;

/**
 * Supports periodically calling tasks on Swing event thread
 */
public class SwingTaskManager extends BaseObject {

  public SwingTaskManager addTask(Runnable task) {
    assertMutable();
    mTaskList.add(task);
    return this;
  }

  public void start() {
    assertMutable();
    mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
      public Thread newThread(Runnable r) {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
      }
    });
    mScheduledThreadPoolExecutor.scheduleWithFixedDelay(
        () -> SwingUtilities.invokeLater(() -> backgroundTask()), 1000, 3000, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    if (!started())
      return;
    mScheduledThreadPoolExecutor.shutdown();
    mScheduledThreadPoolExecutor = null;
  }

  private void backgroundTask() {
    log("executing background task", System.currentTimeMillis());
    for (Runnable r : mTaskList) {
      try {
        r.run();
      } catch (Throwable t) {
        pr("*** Caught exception in periodic background task:", INDENT, t);
      }
    }
  }

  private void assertMutable() {
    checkState(!started(), "already started");
  }

  private boolean started() {
    return mScheduledThreadPoolExecutor != null;
  }

  private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;
  private List<Runnable> mTaskList = arrayList();
}
