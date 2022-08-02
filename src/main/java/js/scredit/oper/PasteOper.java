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

import java.util.List;

import geom.EditorElement;
import js.data.IntArray;
import js.geometry.IPoint;
import js.geometry.Matrix;
import js.scredit.SlotList;
import js.scredit.StateTools;
import js.scredit.gen.Command;
import js.scredit.gen.ScriptEditState;

public class PasteOper extends CommandOper {

  @Override
  public boolean constructCommand(Command.Builder b) {
    ScriptEditState.Builder es = b.newState().toBuilder();
    if (es.clipboard().isEmpty())
      return false;
    StateTools.setDescriptionForSelected(b, "Paste", es.clipboard().size());

    List<EditorElement> elements = es.elements();
    if (false) {
      // Delete any currently selected elements
      IntArray retainList = SlotList.complement(IntArray.with(es.selectedElements()), es.elements().size());
      elements = StateTools.subsetOfElements(es.elements(), retainList.array());
    }

    IPoint trans = es.duplicationOffset();
    trans = trans.sumWith(8, 8);
    Matrix offset = Matrix.getTranslate(trans);
    IntArray.Builder newSel = IntArray.newBuilder();
    for (EditorElement elem : es.clipboard()) {
      elem = elem.applyTransform(offset);
      newSel.add(elements.size());
      elements.add(elem);
    }

    b.newState(es//
        .elements(elements)//
        .selectedElements(newSel.array())//
        .duplicationOffset(trans)//
    );
    return true;
  }

}
