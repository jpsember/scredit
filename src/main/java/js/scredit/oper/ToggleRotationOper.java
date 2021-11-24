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

import js.graphics.RectElement;
import js.graphics.gen.ElementProperties;
import js.scredit.gen.ScriptEditState;
import js.scredit.EditorElement;
import js.scredit.ScrEdit;
import js.scredit.gen.Command.Builder;

public class ToggleRotationOper extends CommandOper {

  public ToggleRotationOper(ScrEdit editor) {
    loadTools();
    setEditor(editor);
  }

  @Override
  public boolean constructCommand(Builder b) {
    ScriptEditState oldState = b.newState();
    ScriptEditState.Builder editState = oldState.toBuilder();

    boolean changed = false;
    for (int slot : oldState.selectedElements()) {
      EditorElement originalObj = oldState.elements().get(slot);
      if (originalObj.is(RectElement.DEFAULT_INSTANCE)) {
        ElementProperties oldProp = originalObj.properties();
        ElementProperties newProp = oldProp.toBuilder().rotation(oldProp.rotation() != null ? null : 0);
        changed = true;
        editState.elements().set(slot, originalObj.withProperties(newProp));
      }
    }
    ScriptEditState newState = editState.build();
    b.newState(newState);
    return changed;
  }

}
