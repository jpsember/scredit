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

import java.awt.Color;

import js.data.IntArray;
import js.geometry.FRect;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.graphics.Paint;
import js.guiapp.UserEvent;
import js.guiapp.UserOperation;
import js.scredit.EditorElement;
import js.scredit.EditorPanel;
import js.scredit.ScrEdit;
import js.scredit.SlotList;
import js.scredit.gen.ScriptEditState;

public class SelectElementsWithBoxOper extends EditorOper implements UserEvent.Listener {

  @Override
  public void processUserEvent(UserEvent event) {
    switch (event.getCode()) {
    case UserEvent.CODE_DRAG:
      mDragEvent = event;
      break;
    case UserEvent.CODE_UP:
      finishSelect();
      event.clearOperation();
      break;
    }
  }

  public static UserOperation build(ScrEdit editor, UserEvent event) {
    return new SelectElementsWithBoxOper(editor, event);
  }

  private SelectElementsWithBoxOper(ScrEdit editor, UserEvent event) {
    loadTools();
    setEditor(editor);
    mInitialEvent = event;
  }

  private static final Paint HIGHLIGHT_PAINT = Paint.newBuilder().color(Color.YELLOW.darker()).width(3);

  @Override
  public void paint(EditorPanel p) {
    IRect r = getRect();
    FRect f = p.pushFocusOn(r.toRect());
    p.apply(HIGHLIGHT_PAINT).renderFrame(f);
    p.popFocus();
  }

  private void finishSelect() {
    IRect r = getRect();
    if (r == null)
      return;

    ScriptEditState state = editor().state();
    IntArray.Builder slots = IntArray.newBuilder();
    int slot = INIT_INDEX;
    for (EditorElement element : state.elements()) {
      slot++;
      if (r.contains(element.bounds())) {
        slots.add(slot);
      }
    }
    IntArray built = slots;
    if (mInitialEvent.isShift())
      built = SlotList.union(built, IntArray.with(state.selectedElements()));
    editor().perform(new SetSelectedElementsOper(built));
  }

  private IRect getRect() {
    // If there was no initial drag event, use a small rectangle at the initial event location
    IPoint pt1 = mInitialEvent.getWorldLocation();
    IPoint pt2 = pt1;
    if (mDragEvent != null)
      pt2 = mDragEvent.getWorldLocation();
    return new IRect(pt1, pt2);
  }

  private UserEvent mInitialEvent;
  private UserEvent mDragEvent;
}
