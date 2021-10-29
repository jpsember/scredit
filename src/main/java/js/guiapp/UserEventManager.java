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

/**
 * Manages user events, passing them to appropriate listeners, and keeping track
 * of a current operation
 */
public final class UserEventManager extends BaseObject implements UserEvent.Listener {

  public UserEventManager(UserOperation defaultOper) {
    checkArgument(defaultOper != null, "no default operation");
    mDefaultOperation = defaultOper;
    setOperation(defaultOper);
    //alertVerbose();
  }

  public UserOperation getOperation() {
    return mOper;
  }

  /**
   * Set current operation. Stops existing, if one exists; then starts new. If
   * new is null, uses default operation.
   */
  void setOperation(UserOperation oper) {
    oper = nullTo(oper, mDefaultOperation);
    if (mOper != null) {
      if (mOper instanceof UserEvent.Listener)
        mOper.stop();
      mOper = null;
    }

    if (mOper != oper)
      log("setOperation:", oper);
    mOper = oper;

    if (oper instanceof UserEvent.Listener) {
      oper.start();
    } else {
      oper.start();
      clearOperation();
    }
  }

  void clearOperation() {
    log("clearOperation");
    setOperation(null);
  }

  @Override
  public void processUserEvent(UserEvent event) {
    if (event.withLogging())
      log("processUserEvent:", event);
    if (getOperation() instanceof UserEvent.Listener)
      ((UserEvent.Listener) getOperation()).processUserEvent(event);
    if (mListener != null)
      mListener.processUserEvent(event);
  }

  /**
   * Specify optional listener, to be informed of an event AFTER it has been
   * processed by the current operation (note, only one listener is supported)
   */
  public void setListener(UserEvent.Listener listener) {
    mListener = listener;
  }

  /**
   * Start an operation, if enabled
   */
  public void perform(UserOperation operation) {
    log("perform operation:", operation);
    if (operation.shouldBeEnabled()) {
      setOperation(operation);
      if (mListener != null)
        mListener.processUserEvent(UserEvent.DEFAULT_INSTANCE);
    }
  }

  private final UserOperation mDefaultOperation;

  private UserEvent.Listener mListener;
  private UserOperation mOper;

}
