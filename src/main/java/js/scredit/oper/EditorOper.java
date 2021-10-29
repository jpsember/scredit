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

import js.guiapp.UserOperation;
import js.scredit.EditorPanel;
import js.scredit.ScrEdit;
import js.scredit.gen.ProjectState;

/**
 * A subclass of UserOperation that maintains a reference to its ScrEdit
 * instance
 */
public abstract class EditorOper extends UserOperation {

  public final EditorOper setEditor(ScrEdit editor) {
    mEditor = editor;
    return this;
  }

  public final ProjectState.Builder projectState() {
    return editor().projectState();
  }

  public final ScrEdit editor() {
    if (mEditor == null)
      badState("No editor defined", getClass().getName());
    return mEditor;
  }

  /**
   * Display any optional rendering associated with this operation
   */
  public void paint(EditorPanel panel) {
  }

  private ScrEdit mEditor;

}
