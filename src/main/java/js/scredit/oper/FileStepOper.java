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

import static js.base.Tools.*;

import js.geometry.MyMath;
import js.scredit.Project;

/**
 * This affects the current project's settings, not individual scripts; so it's
 * not a command operation
 */
public final class FileStepOper extends EditorOper {

  public FileStepOper(int stepVelocity) {
    mStepVelocity = stepVelocity;
    mStepSpeed = Math.abs(mStepVelocity);
  }

  public FileStepOper withAccel() {
    mAccelFlag = true;
    return this;
  }

  @Override
  public boolean shouldBeEnabled() {
    mNextFileIndex = null;
    Project project = editor().currentProject();
    if (!project.definedAndNonEmpty())
      return false;

    int currScript = project.scriptIndex();
    int count = project.scriptCount();

    while (mNextFileIndex == null) {
      int filteredSpeed = Math.abs(mStepVelocity);
      if (mAccelFlag) {
        todo("put filtered speed, etc in app defaults");
        float stepAccelFactor = 1.2f;
        float stepAccelSpeedMin = 0f;
        float stepAccelSpeedMax = 100f;

        if (filteredSpeed <= 0) {
          long time = System.currentTimeMillis();
          long elapsed = time - mLastOperationTime;
          float speed = mStepSpeed;
          if (elapsed < 300) {
            speed = Math.min(speed * stepAccelFactor, stepAccelSpeedMin);
            if (stepAccelSpeedMax > 0)
              speed = Math.min(speed, stepAccelSpeedMax);
          } else if (elapsed > 600) {
            speed = stepSpeed();
          }
          mStepSpeed = speed;
          filteredSpeed = (int) mStepSpeed;
        }
      }

      int updatedIndex = currScript + filteredSpeed * stepSign();
      updatedIndex = MyMath.clamp(updatedIndex, 0, count - 1);
      if (updatedIndex == currScript)
        break;
      currScript = updatedIndex;
      mNextFileIndex = updatedIndex;
    }
    return mNextFileIndex != null;
  }

  @Override
  public void start() {
    if (mNextFileIndex == null)
      return;
    editor().switchToScript(mNextFileIndex);
    mLastOperationTime = System.currentTimeMillis();
  }

  private int stepSign() {
    return Integer.signum(mStepVelocity);
  }

  private int stepSpeed() {
    return Math.abs(mStepVelocity);
  }

  private final int mStepVelocity;
  private boolean mAccelFlag;
  private Integer mNextFileIndex;
  private long mLastOperationTime;
  private float mStepSpeed;
}
