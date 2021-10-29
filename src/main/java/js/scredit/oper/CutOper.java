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

import js.data.IntArray;
import js.scredit.SlotList;
import js.scredit.StateTools;
import js.scredit.gen.Command;
import js.scredit.gen.ScriptEditState;

public class CutOper extends CommandOper {

  @Override
  public boolean constructCommand(Command.Builder b) {
    ScriptEditState.Builder s = b.newState().toBuilder();
    s.clipboard(StateTools.subsetOfElements(s.elements(), s.selectedElements()));
    if (s.clipboard().isEmpty())
      return false;
    IntArray nonSelectedSlots = SlotList.complement(IntArray.with(s.selectedElements()), s.elements().size());
    s.elements(StateTools.subsetOfElements(s.elements(), nonSelectedSlots));
    s.selectedElements(null);
    b.newState(s);
    return true;
  }

}
