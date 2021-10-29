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

import js.base.BaseObject;
import js.data.IntArray;

/**
 * User operation, which may involve mouse or touch device
 * 
 * If operation involves a sequence of user interaction events, it should
 * implement the UserEvent.Listener interface
 */
public abstract class UserOperation extends BaseObject implements Enableable {

  @Override
  public boolean shouldBeEnabled() {
    loadTools();
    return true;
  }

  // ------------------------------------------------------------------
  // Execution 
  // ------------------------------------------------------------------

  /**
   * <pre>
   * 
   * If the operation implements UserEvent.Listener, then the framework will
   * execute it by performing:
   * 
   *    oper.start();
   *    
   *       oper.processUserEvent(...);   // for each user interface event received
   *          :
   *          :
   *          
   *    oper.stop();
   *    
   * Otherwise,
   * 
   *    oper.start();
   * 
   * </pre>
   */
  public void start() {
    log("starting");
  }

  /**
   * Called when operation that implements UserEvent.Listener is stopped
   */
  public void stop() {
    log("stopping");
  }

  // ------------------------------------------------------------------
  // Rendering
  // ------------------------------------------------------------------

  /**
   * Optionally specify only specific slots of objects to be rendered. Default
   * returns null, which renders all of them
   */
  public IntArray displayedSlotsFilter() {
    return null;
  }

  /**
   * Determine if a repaint of various UI elements is required following
   * handling of a user event. Default returns an integer with all bits set
   */
  public int repaintRequiredFlags(UserEvent event) {
    return ~0;
  }

}
