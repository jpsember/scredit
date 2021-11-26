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
import java.awt.Cursor;
import java.awt.geom.Path2D;

import js.base.StateMachine;
import js.data.IntArray;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.MyMath;
import js.graphics.Paint;
import js.guiapp.UserEvent;
import js.scredit.EditorElement;
import js.scredit.EditorElement.Render;
import js.scredit.EditorPanel;
import js.scredit.StateTools;
import js.scredit.elem.EditableRectElement;
import js.scredit.gen.Command;

public class RectAddOper extends EditorOper implements UserEvent.Listener {

  protected String getObjectMenuName() {
    return "Box";
  }

  protected EditorElement getObjectDefaultInstance() {
    return EditableRectElement.DEFAULT_INSTANCE;
  }

  protected EditorElement constructNewObject(IRect bounds) {
    return EditableRectElement.DEFAULT_INSTANCE.withBounds(EditableRectElement.ensureValid(bounds));
  }

  @Override
  public void start() {
    loadTools();
    log("start");
    mAddState = null;
    mMouseOffset = IPoint.ZERO;
    mCommand = editor().buildCommand("Add " + getObjectMenuName());
    mSlot = StateTools.addNewElement(mCommand, getObjectDefaultInstance());
    editor().setMouseCursor(Cursor.HAND_CURSOR);
  }

  @Override
  public void processUserEvent(UserEvent event) {
    if (event.withLogging())
      log("processUserEvent", event);
    IPoint pos = event.getWorldLocation();
    mAddMousePos = pos;
    switch (addState().currentState()) {
    case STATE_ORIGIN:
      if (event.getCode() == UserEvent.CODE_MOVE) {
        mOrigin = pos;
      } else if (event.getCode() == UserEvent.CODE_UP) {
        IRect guide = guideRect(true);
        IPoint activeCorner = guide.corner(cornerIndex());
        IPoint offset = IPoint.difference(activeCorner, guide.center());
        mMouseOffset = mMouseOffset.sumWith(offset);
        mOrigin = pos;
        IRect bounds = boundingRect(mOrigin, IPoint.sum(mOrigin, mMouseOffset));
        writeActiveRect(constructNewObject(bounds));
        editor().perform(mCommand);
        addState().set(STATE_CORNER);
        editor().setMouseCursor(Cursor.CROSSHAIR_CURSOR);
      }
      break;
    case STATE_CORNER:
      if (event.getCode() == UserEvent.CODE_MOVE) {
        IPoint extremalPoint = pos.sumWith(mMouseOffset);
        IRect bounds = boundingRect(mOrigin, extremalPoint);
        // Update the active corner to the one closest to the extremal point
        int extremeCorner = -1;
        float minDistance = -1;
        for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
          IPoint cornerPos = bounds.corner(cornerIndex);
          float distance = MyMath.distanceBetween(cornerPos, extremalPoint);
          if (cornerIndex == 0 || distance < minDistance) {
            extremeCorner = cornerIndex;
            minDistance = distance;
          }
        }
        projectState().addBoxCornerIndex(extremeCorner);
        EditorElement p = constructNewObject(bounds);
        writeActiveRect(p);
        editor().perform(mCommand);
      } else if (event.getCode() == UserEvent.CODE_UP) {
        addState().set(STATE_DONE);
        editor().perform(mCommand);
        event.clearOperation();
      }
      break;
    }
  }

  @Override
  public IntArray displayedSlotsFilter() {
    if (addState().is(STATE_ORIGIN))
      return IntArray.DEFAULT_INSTANCE;
    return IntArray.with(mSlot);
  }

  private void writeActiveRect(EditorElement rect) {
    StateTools.replaceAndSelectItem(mCommand, mSlot, rect);
    todo("Store last completed rect in project state");
    mLastCompletedRect = rect.bounds();
  }

  private int cornerIndex() {
    return projectState().addBoxCornerIndex();
  }

  /**
   * Determine the location of the 'guide' rectangle, which is the last
   * completed rect centered at the last recorded mouse position (in 'quick'
   * mode).
   * 
   * Optionally just returns the last completed rectangle, without the mouse
   * offset
   */
  private IRect guideRect(boolean applyMouseOffset) {
    IRect lastRect = mLastCompletedRect;
    // If there is no last completed rect, choose a reasonable one
    if (lastRect == null)
      lastRect = new IRect(0, 0, 50, 50);
    if (applyMouseOffset) {
      IPoint mousePos = mAddMousePos;
      lastRect = new IRect(mousePos.x - lastRect.width / 2, mousePos.y - lastRect.height / 2, lastRect.width,
          lastRect.height);
    }
    return lastRect;
  }

  private static final int GUIDE_INSET = 5;
  private static final int GUIDE_LENGTH = 10;
  private static final int GUIDE_WIDTH = 2;

  private static int[] sGuidePathPoints = { -GUIDE_INSET, -GUIDE_INSET, -GUIDE_INSET,
      -GUIDE_INSET + GUIDE_LENGTH, -GUIDE_INSET - GUIDE_WIDTH, -GUIDE_INSET + GUIDE_LENGTH - GUIDE_WIDTH,
      -GUIDE_INSET - GUIDE_WIDTH, -GUIDE_INSET - GUIDE_WIDTH, -GUIDE_INSET + GUIDE_LENGTH,
      -GUIDE_INSET - GUIDE_WIDTH, -GUIDE_INSET - GUIDE_WIDTH + GUIDE_LENGTH, -GUIDE_INSET - GUIDE_WIDTH,
      -GUIDE_INSET + GUIDE_LENGTH, -GUIDE_INSET, };

  private static final Paint sGuidePaint = Paint.newBuilder().fill().color(Color.yellow).build();

  /**
   * Construct a path representing the guide, which appears at the appropriate
   * corner
   */
  private static Path2D constructGuidePath(IRect rect, int cornerIndex, float zoomFactor) {
    IPoint origin = rect.corner(cornerIndex);
    float scaleFactor = 3f / zoomFactor;
    Path2D path = new Path2D.Float();
    for (int pathCursor = 0; pathCursor < sGuidePathPoints.length; pathCursor += 2) {
      float x = scaleFactor * sGuidePathPoints[pathCursor];
      float y = scaleFactor * sGuidePathPoints[pathCursor + 1];
      if (cornerIndex == 1 || cornerIndex == 2)
        x = -x;
      if (cornerIndex >= 2)
        y = -y;
      x += origin.x;
      y += origin.y;
      if (pathCursor == 0)
        path.moveTo(x, y);
      else
        path.lineTo(x, y);
    }
    path.closePath();
    return path;
  }

  @Override
  public void paint(EditorPanel p) {
    if (mAddMousePos == null)
      return;
    boolean choosingOrigin = addState().is(STATE_ORIGIN);
    IRect guide = guideRect(choosingOrigin);

    p.apply(sGuidePaint);
    p.render(constructGuidePath(guide, cornerIndex(), editor().zoomFactor()));

    // Use the normal box rendering method to draw the guide rectangle;
    // it is less confusing for the user
    EditorElement temporaryBox = constructNewObject(guide);
    temporaryBox.render(p, Render.SELECTED);
  }

  private StateMachine addState() {
    if (mAddState == null) {
      mAddState = new StateMachine("RectEditOper.add")//
          .add(STATE_ORIGIN)//
          .toAdd(STATE_CORNER)//
          .toAdd(STATE_DONE)//
          .build();
    }
    return mAddState;
  }

  private static IRect boundingRect(IPoint center, IPoint extremalPoint) {
    int w2 = Math.abs(extremalPoint.x - center.x);
    int h2 = Math.abs(extremalPoint.y - center.y);
    return EditableRectElement.ensureValid(new IRect(center.x - w2, center.y - h2, w2 * 2, h2 * 2));
  }

  private StateMachine mAddState;
  private IPoint mMouseOffset;
  private Command.Builder mCommand;
  private int mSlot;
  private IPoint mOrigin;
  private IPoint mAddMousePos;
  private IRect mLastCompletedRect;

  private static final String STATE_ORIGIN = "origin";
  private static final String STATE_CORNER = "corner";
  private static final String STATE_DONE = "done";
}
