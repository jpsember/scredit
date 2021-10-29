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

import js.geometry.IPoint;
import js.json.JSMap;
import static js.base.Tools.*;

import js.base.BaseObject;

public final class UserEvent extends BaseObject {

  public static interface Listener {
    public void processUserEvent(UserEvent event);
  }

  public static final int CODE_NONE = 0;
  public static final int CODE_DOWN = 1;
  public static final int CODE_DRAG = 2;
  public static final int CODE_UP = 3;
  public static final int CODE_MOVE = 4;
  public static final int CODE_STOP = 5;

  public static final int FLAG_RIGHT = (1 << 0);
  public static final int FLAG_CTRL = (1 << 1);
  public static final int FLAG_SHIFT = (1 << 2);
  public static final int FLAG_ALT = (1 << 3);
  public static final int FLAG_META = (1 << 4);
  public static final int FLAG_MULTITOUCH = (1 << 5);

  public static final UserEvent DEFAULT_INSTANCE = new UserEvent(CODE_NONE, null, IPoint.DEFAULT_INSTANCE, 0);

  public UserEvent(int code, UserEventSource source, IPoint viewLocation, int modifierFlags) {
    mCode = code;
    mSource = source;
    mViewLocation = viewLocation;
    mModifierFlags = modifierFlags;
  }

  public int getCode() {
    return mCode;
  }

  public UserEventSource getSource() {
    return mSource;
  }

  public IPoint getViewLocation() {
    checkState(hasLocation(), "no view location");
    return mViewLocation;
  }

  public IPoint getWorldLocation() {
    if (!hasLocation())
      throw new IllegalStateException();
    return mSource.viewToWorld(mViewLocation);
  }

  public boolean isDownVariant() {
    return mCode == CODE_DOWN;
  }

  public boolean isUpVariant() {
    return mCode == CODE_UP;
  }

  public boolean isDragVariant() {
    return mCode == CODE_DRAG;
  }

  public boolean hasLocation() {
    return mViewLocation != null;
  }

  public boolean isRight() {
    return hasFlag(FLAG_RIGHT);
  }

  public boolean isAlt() {
    return hasFlag(FLAG_ALT);
  }

  public boolean isMeta() {
    return hasFlag(FLAG_META);
  }

  public boolean isCtrl() {
    return hasFlag(FLAG_CTRL);
  }

  public boolean isShift() {
    return hasFlag(FLAG_SHIFT);
  }

  public boolean isMultipleTouch() {
    return hasFlag(FLAG_MULTITOUCH);
  }

  public boolean hasModifierKeys() {
    return hasFlag(FLAG_CTRL | FLAG_SHIFT | FLAG_ALT | FLAG_META);
  }

  private boolean hasFlag(int f) {
    return 0 != (mModifierFlags & f);
  }

  // ------------------------------------------------------------------
  // Convenience methods involving event manager, operations
  // ------------------------------------------------------------------

  /**
   * Convenience method for getManager().setOperation()
   */
  public void setOperation(UserOperation oper) {
    getManager().setOperation(oper);
  }

  /**
   * Convenience method for getManager().getOperation()
   */
  public UserOperation getOperation() {
    return getManager().getOperation();
  }

  /**
   * Convenience method for getManager().clearOperation()
   */
  public void clearOperation() {
    getManager().clearOperation();
  }

  private UserEventManager getManager() {
    return getSource().getManager();
  }

  // ------------------------------------------------------------------
  // BaseObject interface
  // ------------------------------------------------------------------

  @Override
  protected String supplyName() {
    if (mCode < 0 || mCode >= sCodeStrings.length)
      return "#" + mCode;
    else
      return sCodeStrings[mCode];
  }

  private static String sCodeStrings[] = { "NONE", "DOWN", "DRAG", "UP  ", "MOVE", "STOP", };

  @Override
  public JSMap toJson() {
    JSMap m = map();
    m.put("code", name());

    if (mViewLocation != null) {
      m.put("view_loc", mViewLocation.toJson());
      m.put("world_loc", getWorldLocation().toJson());
    }

    if (mModifierFlags != 0) {
      StringBuilder sb = new StringBuilder();
      sb.append(" <");
      if (isAlt())
        sb.append("A");
      if (isCtrl())
        sb.append("C");
      if (isMeta())
        sb.append("M");
      if (isMultipleTouch())
        sb.append("MULTI");
      if (isRight())
        sb.append("R");
      if (isShift())
        sb.append("S");
      sb.append(">");
      m.put("mod_flags", sb.toString());
    }
    return m;
  }

  // ------------------------------------------------------------------

  private final int mCode;
  private final UserEventSource mSource;
  private final IPoint mViewLocation;
  private final int mModifierFlags;

  public boolean withLogging() {
    return mCode != CODE_DRAG && mCode != CODE_MOVE;
  }
}
