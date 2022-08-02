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
package js.scredit;

import static js.base.Tools.*;

import java.util.List;

import geom.EditorElement;
import js.data.IntArray;
import js.scredit.gen.Command;
import js.scredit.gen.ScriptEditState;

/**
 * Utilities for manipulating Commands and ScriptEditStates
 */
public final class StateTools {

  /**
   * Set a command's description to an expression involving zero or more
   * elements
   */
  public static void setDescriptionForSelected(Command.Builder b, String actionPrefix, int elementCount) {
    if (elementCount > 1)
      b.description(actionPrefix + " " + elementCount + " Items");
    else
      b.description(actionPrefix + " Item");
  }

  /**
   * Replace an element, make it the single selected one, and return its slot
   */
  public static int replaceAndSelectItem(Command.Builder b, int slot, EditorElement element) {
    if (slot < 0 || slot >= b.newState().elements().size())
      badArg("slot", slot, "doesn't exist:", INDENT, b);
    return addOrReplaceHelper(b, element, slot);
  }

  public static void remove(Command.Builder b, int slot) {
    if (slot < 0 || slot >= b.newState().elements().size())
      badArg("slot", slot, "doesn't exist:", INDENT, b);
    ScriptEditState.Builder s = b.newState().toBuilder();
    s.elements().remove(slot);
    s.selectedElements(null);
    b.newState(s);
  }

  /**
   * Construct a subset of a list of elements
   */
  public static List<EditorElement> subsetOfElements(List<EditorElement> elements, IntArray slotsToInclude) {
    return subsetOfElements(elements, slotsToInclude.array());
  }

  /**
   * Construct a subset of a list of elements
   */
  public static List<EditorElement> subsetOfElements(List<EditorElement> elements, int[] slotsToInclude) {
    List<EditorElement> out = arrayList();
    for (int slot : slotsToInclude)
      out.add(elements.get(slot));
    return out;
  }

  /**
   * Add an element, make it the only selected one, and return its slot
   */
  public static int addNewElement(Command.Builder b, EditorElement element) {
    return addOrReplaceHelper(b, element, b.newState().elements().size());
  }

  /**
   * Add an element, make it the only selected one, and return its slot
   */
  private static int addOrReplaceHelper(Command.Builder b, EditorElement element, int slot) {
    ScriptEditState.Builder s = b.newState().toBuilder();
    if (slot == s.elements().size())
      s.elements().add(element);
    else
      s.elements().set(slot, element);
    s.selectedElements(IntArray.with(slot).array());
    b.newState(s);
    return slot;
  }

}
