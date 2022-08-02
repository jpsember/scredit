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

import geom.ScriptWrapper;
import js.geometry.MyMath;
import js.guiapp.UserOperation;
import js.scredit.Project;
import static js.scredit.ScrEditTools.*;

/**
 * This affects the current project's settings, not individual scripts; so it's
 * not a command operation
 */
public final class FileStepUsedOper extends UserOperation {

  public FileStepUsedOper(int signedStepAmount) {
    loadTools();
    mStepVelocity = signedStepAmount;
  }

  @Override
  public boolean shouldBeEnabled() {
    mNextFileIndex = null;
    Project project = seditor().currentProject();
    if (!project.definedAndNonEmpty())
      return false;

    int currScript = project.scriptIndex();
    int count = project.scriptCount();

    while (mNextFileIndex == null) {
      int filteredSpeed = Math.abs(mStepVelocity);
      int updatedIndex = currScript + filteredSpeed * stepSign();
      updatedIndex = MyMath.clamp(updatedIndex, 0, count - 1);
      if (updatedIndex == currScript)
        break;
      currScript = updatedIndex;
      // If script is useful, stop
      ScriptWrapper w = project.script(currScript);
      if (w.data().items().isEmpty())
        continue;
      mNextFileIndex = updatedIndex;
    }
    return mNextFileIndex != null;
  }

  @Override
  public void start() {
    if (mNextFileIndex == null)
      return;
    seditor().switchToScript(mNextFileIndex);
  }

  private int stepSign() {
    return Integer.signum(mStepVelocity);
  }

  private final int mStepVelocity;
  private Integer mNextFileIndex;
}
