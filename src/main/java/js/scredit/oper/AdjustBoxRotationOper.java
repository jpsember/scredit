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

import js.data.IntArray;
import js.geometry.FPoint;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.MyMath;
import js.graphics.gen.ElementProperties;
import js.guiapp.UserEvent;
import js.scredit.EditorElement;
import js.scredit.ScrEdit;
import js.scredit.StateTools;
import js.scredit.elem.EditableRectElement;
import js.scredit.gen.Command;

public class AdjustBoxRotationOper extends EditorOper implements UserEvent.Listener {

  public AdjustBoxRotationOper(ScrEdit editor, UserEvent event, int slot) {
    loadTools();
    setEditor(editor);
    mSlot = slot;
    mMouseDownLoc = event.getWorldLocation();
  }

  @Override
  public void start() {
    log("start");

    mCommand = editor().buildCommand("Adjust Box Rotation");
    EditorElement elem = mCommand.newState().elements().get(mSlot);
    mOriginalElem = (EditableRectElement) elem;
    mOriginalAngle = MyMath.polarAngle(FPoint.difference(mMouseDownLoc.toFPoint(), origin()));
    FPoint origRadialPt = MyMath.pointOnCircle(origin(), mOriginalAngle, radius());
    mGrabOffset = FPoint.difference(origRadialPt, mMouseDownLoc.toFPoint());
  }

  private FPoint origin() {
    return mOriginalElem.bounds().midPoint().toFPoint();
  }

  private float radius() {
    IRect r = mOriginalElem.bounds();
    return MyMath.magnitudeOfRay(r.width / 2, r.height / 2);
  }

  @Override
  public void processUserEvent(UserEvent event) {
    if (event.withLogging())
      log("processUserEvent", event);

    switch (event.getCode()) {

    case UserEvent.CODE_DRAG: {
      FPoint mouseLoc = event.getWorldLocation().toFPoint();
      mouseLoc = FPoint.sum(mouseLoc, mGrabOffset);
      float newAngle = MyMath.polarAngle(FPoint.difference(mouseLoc, origin()));
      float angleDiff = (newAngle - mOriginalAngle);
      ElementProperties.Builder properties = mOriginalElem.properties().toBuilder();
      float ang = properties.rotation() * MyMath.M_DEG + angleDiff;
      ang = MyMath.clamp(ang, -85 * MyMath.M_DEG, 85 * MyMath.M_DEG);
      properties.rotation(Math.round(ang / MyMath.M_DEG));
      StateTools.replaceAndSelectItem(mCommand, mSlot, mOriginalElem.withProperties(properties));
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

  private int mSlot;
  private IPoint mMouseDownLoc;
  private Command.Builder mCommand;
  private EditableRectElement mOriginalElem;
  private FPoint mGrabOffset;
  private float mOriginalAngle;
}
