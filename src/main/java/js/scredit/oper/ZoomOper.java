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
package js.scredit.oper;

import js.geometry.MyMath;
import js.guiapp.UserOperation;
import static js.base.Tools.*;

/**
 * This affects the current project's settings, not individual scripts; so it's
 * not a command operation
 */
public final class ZoomOper extends UserOperation {

  public static UserOperation buildIn() {
    loadTools();
    return new ZoomOper(1.2f);
  }

  public static UserOperation buildOut() {
    return new ZoomOper(1 / 1.2f);
  }

  public static UserOperation buildReset() {
    return new ZoomOper(0f);
  }

  private ZoomOper(float stepFactor) {
    mStepFactor = stepFactor;
  }

  @Override
  public boolean shouldBeEnabled() {
    float oldZoom = projectState().zoomFactor();
    float newZoom;
    if (mStepFactor == 0)
      newZoom = 1f;
    else {
      float modified = oldZoom * mStepFactor;
      float clamped = MyMath.clamp(modified, 0.05f, 20f);
      newZoom = oldZoom;
      if (modified == clamped)
        newZoom = modified;
    }
    mNewZoomFactor = newZoom;
    return oldZoom != newZoom;
  }

  @Override
  public void start() {
    projectState().zoomFactor(mNewZoomFactor);
  }

  private final float mStepFactor;
  private float mNewZoomFactor;
}
