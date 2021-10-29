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

import js.data.IntArray;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.graphics.RectElement;
import js.guiapp.UserEvent;
import js.scredit.EditorElement;
import js.scredit.ScrEdit;
import js.scredit.StateTools;
import js.scredit.elem.EditableRectElement;
import js.scredit.gen.Command;

public class RectEditOper extends EditorOper implements UserEvent.Listener {

  public RectEditOper(ScrEdit editor, UserEvent event, int slot, int handle) {
    setEditor(editor);
    mHandle = handle;
    mSlot = slot;
    mMouseDownLoc = event.getWorldLocation();
  }

  private static int[] sCursors = { Cursor.NW_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR, Cursor.SE_RESIZE_CURSOR,
      Cursor.SW_RESIZE_CURSOR, Cursor.N_RESIZE_CURSOR, Cursor.E_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR,
      Cursor.W_RESIZE_CURSOR, };

  @Override
  public void start() {
    log("start");
    editor().setMouseCursor(sCursors[mHandle]);
    mMouseOffset = IPoint.ZERO;
    mCommand = editor().buildCommand("Adjust Box");
    EditorElement elem = mCommand.newState().elements().get(mSlot);
    mOriginalElem = (EditableRectElement) elem;
    IPoint vertexLoc = originalBounds().corner(cornerIndex(mHandle));
    mMouseOffset = IPoint.difference(vertexLoc, mMouseDownLoc);
  }

  @Override
  public void processUserEvent(UserEvent event) {
    if (event.withLogging())
      log("processUserEvent", event);

    switch (event.getCode()) {

    case UserEvent.CODE_DRAG: {
      RectElement p = setElementPosition(mHandle, applyMouseOffset(event.getWorldLocation()));
      StateTools.replaceAndSelectItem(mCommand, mSlot, constructNewObject(p.bounds()));
      editor().perform(mCommand);
    }
      break;

    case UserEvent.CODE_UP:
      editor().perform(mCommand);
      event.clearOperation();
      break;
    }
  }

  @Override
  public IntArray displayedSlotsFilter() {
    return IntArray.with(mSlot);
  }

  private EditorElement constructNewObject(IRect bounds) {
    return mOriginalElem.withBounds(EditableRectElement.ensureValid(bounds));
  }

  private IPoint applyMouseOffset(IPoint mouseLocation) {
    return IPoint.sum(mouseLocation, mMouseOffset);
  }

  /**
   * Construct modified object for an element's new position
   */
  private RectElement setElementPosition(int elem, IPoint worldLocation) {

    int elemCorner = cornerIndex(elem);
    // Calculate the amount we want to add to the original box's element to get the new position
    IPoint movingCorner = originalBounds().corner(elemCorner);
    IPoint elementTranslateVector = IPoint.difference(worldLocation, movingCorner);

    switch (elem) {
    default:
      throw new IllegalArgumentException();
    case 0:
    case 1:
    case 2:
    case 3:
      movingCorner = movingCorner.sumWith(elementTranslateVector);
      break;
    case 4:
    case 6:
      movingCorner = movingCorner.sumWith(0, elementTranslateVector.y);
      break;
    case 5:
    case 7:
      movingCorner = movingCorner.sumWith(elementTranslateVector.x, 0);
      break;
    }
    IPoint fixedCorner = originalBounds().corner(cornerIndex((elem + 2) & 0x7));

    return mOriginalElem.withBounds(new IRect(movingCorner, fixedCorner));
  }

  private static int cornerIndex(int cornerOrEdgeIndex) {
    checkArgument(cornerOrEdgeIndex >= 0 && cornerOrEdgeIndex < 8);
    return cornerOrEdgeIndex & 0x3;
  }

  private IRect originalBounds() {
    return mOriginalElem.bounds();
  }

  // Which part of the rectangle is being edited (e.g. side, corner); analogous to polygon's vertex
  private int mHandle;
  private int mSlot;
  private IPoint mMouseDownLoc;
  private IPoint mMouseOffset;
  private Command.Builder mCommand;
  private EditableRectElement mOriginalElem;
}
