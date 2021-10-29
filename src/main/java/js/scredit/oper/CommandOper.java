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

import js.guiapp.UserEvent;
import js.scredit.gen.Command;
import js.scredit.gen.ScriptEditState;

/**
 * Subclass of UserOperation for operations that construct and perform commands,
 * without user interaction
 */
public abstract class CommandOper extends EditorOper {

  public CommandOper() {
    checkState(!(this instanceof UserEvent.Listener));
  }

  @Override
  public final boolean shouldBeEnabled() {
    boolean result = isCommandValid();
    log("shouldBeEnabled returning", result);
    return result;
  }

  @Override
  public final void start() {
    log("perform");
    if (!isCommandValid())
      return;
    log("performing command:", command());
    editor().perform(command());
  }

  /**
   * Subclasses should NOT override this method, as it should never get called
   */
  @Override
  public final void stop() {
    notSupported();
  }

  /**
   * Subclasses should provide an implementation of this method.
   *
   * Return true if command is valid (i.e. operation should be enabled)
   */
  public abstract boolean constructCommand(Command.Builder b);

  /**
   * Construct a command for the current editor state, and determine if it is
   * valid. Caches the result to avoid unnecessary computation
   */
  private Command.Builder command() {
    ScriptEditState editState = editor().state();
    if (mCommandBuilder == null || !editState.equals(mCachedScriptEditState)) {
      mCachedScriptEditState = editState;
      mCommandBuilder = editor().buildCommand(getClass().getSimpleName());
      mCachedValidFlag = constructCommand(mCommandBuilder);
      log("built fresh command:", mCommandBuilder);
    }
    log("cached command valid:", mCachedValidFlag);
    return mCommandBuilder;
  }

  private boolean isCommandValid() {
    command();
    return mCachedValidFlag;
  }

  private Command.Builder mCommandBuilder;
  private ScriptEditState mCachedScriptEditState;
  private boolean mCachedValidFlag;

}
