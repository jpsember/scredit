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

import js.graphics.gen.ElementProperties;
import js.scredit.gen.ScriptEditState;
import js.scredit.EditorElement;
import js.scredit.ScrEdit;
import js.scredit.gen.Command.Builder;

public class SetCategoryOper extends CommandOper {

  public static SetCategoryOper buildSetCategoryOper(ScrEdit editor, int category) {
    return new SetCategoryOper(editor, category);
  }

  public SetCategoryOper(ScrEdit editor, int newCategory) {
    setEditor(editor);
    mNewCategory = newCategory;
  }

  @Override
  public boolean constructCommand(Builder b) {
    ScriptEditState.Builder s = b.newState().toBuilder();
    boolean changed = false;
    for (int slot : s.selectedElements()) {
      EditorElement originalObj = s.elements().get(slot);
      ElementProperties oldProp = originalObj.properties();
      ElementProperties newProp = oldProp.toBuilder().category(mNewCategory);
      if (!oldProp.equals(newProp)) {
        EditorElement updatedObj = originalObj.toEditorElement(originalObj.withProperties(newProp));
        changed = true;
        s.elements().set(slot, updatedObj);
      }
    }
    todo(
        "can we have a generic 'check if new state differs from old' to determine quickly if operation should be enabled?");
    if (changed)
      b.newState(s);
    return changed;
  }

  private int mNewCategory;
}
