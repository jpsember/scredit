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

import java.awt.Cursor;

import js.guiapp.UserEvent;
import js.guiapp.UserOperation;
import js.scredit.EditorElement;
import js.scredit.ScrEdit;
import js.scredit.SlotList;
import js.scredit.gen.ScriptEditState;

import js.data.IntArray;

public final class DefaultOper extends EditorOper implements UserEvent.Listener {

  public DefaultOper() {
    loadTools();
    //alertVerbose();
  }

  @Override
  public void start() {
    editor().setMouseCursor(Cursor.DEFAULT_CURSOR);
  }

  @Override
  public int repaintRequiredFlags(UserEvent event) {
    // If it's only a mouse move, don't update the main view
    if (event.getCode() == UserEvent.CODE_MOVE)
      return ScrEdit.REPAINT_INFO;
    return super.repaintRequiredFlags(event);
  }

  @Override
  public void processUserEvent(UserEvent event) {

    // Ignore events if we never received an initiating 'down' event.
    // This can happen if we cancelled a previous operation while the mouse
    // was still down.

    if (mInitialDownEvent == null && event.getCode() != UserEvent.CODE_DOWN) {
      if (event.withLogging())
        log("...ignoring event; we never received an initial DOWN");
      return;
    }

    switch (event.getCode()) {
    case UserEvent.CODE_DOWN:
      log("DOWN");
      mInitialDownEvent = event;
      mIsDrag = false;
      constructPickSet(event);
      break;

    case UserEvent.CODE_DRAG:
      log("DRAG");
      if (!mIsDrag) {
        mIsDrag = true;
        doStartDrag(event);
      }
      doContinueDrag(event);
      break;

    case UserEvent.CODE_UP:
      log("UP");
      if (!mIsDrag)
        doClick(event);
      else
        doFinishDrag();
      break;
    }
  }

  private void constructPickSet(UserEvent event) {
    ScriptEditState state = editor().state();
    IntArray.Builder b1 = IntArray.newBuilder();
    IntArray.Builder b2 = IntArray.newBuilder();

    int paddingPixels = editor().paddingPixels();

    int slot = -1;
    for (EditorElement element : state.elements()) {
      slot++;
      if (!element.contains(paddingPixels, event.getWorldLocation()))
        continue;
      b1.add(slot);
      if (IntArray.with(state.selectedElements()).contains(slot))
        b2.add(slot);
    }
    mPickSet = b1.build();
    mPickSetSelected = b2.build();
  }

  private void doFinishDrag() {
  }

  /**
   *
   * If click, no shift key:
   *
   * [] If pick set is empty, unselect all objects (i.e., any selected objects
   * not intersecting the click location); otherwise, cycle through the pick
   * set, so exactly one element is selected (and thus editable).
   *
   * If click, with shift key:
   *
   * [] if pick set is nonempty, toggle the selected state of its frontmost item
   *
   */
  private void doClick(UserEvent event) {
    if (!event.isShift()) {
      if (!pickSet().isEmpty()) {
        walkThroughPickSet();
      } else {
        editor().perform(new SetSelectedElementsOper(IntArray.DEFAULT_INSTANCE));
      }
    } else {
      if (!pickSet().isEmpty()) {
        int index = last(pickSet());
        IntArray single = IntArray.with(index);
        IntArray current = IntArray.with(editor().state().selectedElements());
        if (current.contains(index))
          current = SlotList.minus(current, single);
        else
          current = SlotList.union(current, single);
        editor().perform(new SetSelectedElementsOper(current));
      }
    }
  }

  /**
   * Find item immediately following the last currently selected item in the
   * pick set, and select that item. If no following item exists, select the
   * first item.
   */
  private void walkThroughPickSet() {
    checkState(!pickSet().isEmpty());
    // Look through pick set to find item following last selected item
    int outputSlot = -1;
    // Walk from highest to lowest, since frontmost are highest
    IntArray current = IntArray.with(editor().state().selectedElements());
    for (int cursor = pickSet().size() - 1; cursor >= 0; cursor--) {
      int slot = pickSet().get(cursor);
      if (current.contains(slot)) {
        outputSlot = -1;
      } else {
        if (outputSlot < 0)
          outputSlot = slot;
      }
    }
    if (outputSlot < 0)
      outputSlot = last(pickSet());
    editor().perform(new SetSelectedElementsOper(IntArray.with(outputSlot)));
  }

  private void doContinueDrag(UserEvent event) {
  }

  private static int last(IntArray intArray) {
    return intArray.get(intArray.size() - 1);
  }

  private void doStartDrag(UserEvent event) {
    /**
     *
     * If drag, no shift key:
     *
     * [] If an object is editable, see if press starts an editing operation
     * with it;
     *
     * [] else, if pick set contains any selected objects, start a move
     * operation with the selection;
     *
     * [] else, if pick set contains any objects, select and move just the
     * topmost;
     *
     * [] else, unselect all items, start a drag rectangle operation, and select
     * the items contained within the rectangle.
     *
     * If drag, with shift key:
     *
     * [] Leave any existing selected set intact, and start a drag rectangle
     * operation, and add the enclosed items to the selected set.
     *
     * If drag, right mouse button:
     *
     * [] start move focus operation
     */
    ScrEdit ed = editor();

    if (!event.isShift()) {
      UserOperation oper = findOperationForEditableObject();
      if (oper != null) {
        event.setOperation(oper);
        return;
      }
      if (!pickSetSelected().isEmpty()) {
        oper = new MoveElementsOper(editor(), mInitialDownEvent);
        event.setOperation(oper);
      } else if (!pickSet().isEmpty()) {

        // We will either start moving the item at the front of the pick stack,
        // or if we can construct an editable operation for it once it's been selected,
        // we'll start that operation (so user doesn't need to first select the item)

        int slot = last(pickSet());
        EditorElement obj = ed.state().elements().get(slot);
        oper = obj.isEditingSelectedObject(ed, slot, mInitialDownEvent);
        if (oper == null) {
          IntArray selItem = IntArray.with(slot);
          editor().perform(new SetSelectedElementsOper(selItem));
          oper = new MoveElementsOper(editor(), mInitialDownEvent);
        }
        event.setOperation(oper);
      } else {
        oper = SelectElemenentsWithBoxOper.build(editor(), mInitialDownEvent);
        event.setOperation(oper);
      }
    } else {
      event.setOperation(SelectElemenentsWithBoxOper.build(editor(), mInitialDownEvent));
    }
  }

  /**
   * Determine if there's an editable object which can construct an edit
   * operation for a particular location. If so, return that operation; else,
   * null
   */
  private UserOperation findOperationForEditableObject() {
    if (editor().state().selectedElements().length != 1)
      return null;
    int editableSlot = editor().state().selectedElements()[0];

    if (editor().state().elements().size() <= editableSlot) {
      die("findOperationForEditableObject selected elements disagrees with elements:", INDENT,
          editor().state());
    }

    EditorElement obj = (EditorElement) editor().state().elements().get(editableSlot);
    return obj.isEditingSelectedObject(editor(), editableSlot, mInitialDownEvent);
  }

  private IntArray pickSet() {
    checkState(mPickSet != null);
    return mPickSet;
  }

  private IntArray pickSetSelected() {
    checkState(mPickSetSelected != null);
    return mPickSetSelected;
  }

  private UserEvent mInitialDownEvent;
  private boolean mIsDrag;
  private IntArray mPickSet;
  private IntArray mPickSetSelected;
}
