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
package js.scredit.elem;

import java.awt.Color;

import static js.base.Tools.*;

import js.geometry.*;
import js.graphics.Paint;
import js.graphics.PointElement;
import js.graphics.ScriptElement;
import js.graphics.gen.ElementProperties;
import js.scredit.EditorElement;
import js.scredit.EditorPanel;

public class EditablePointElement extends PointElement implements EditorElement {

  public static final EditablePointElement DEFAULT_INSTANCE = new EditablePointElement(
      PointElement.DEFAULT_INSTANCE.properties(), PointElement.DEFAULT_INSTANCE.location());

  @Override
  public EditablePointElement toEditorElement(ScriptElement obj) {
    return new EditablePointElement(obj.properties(), obj.location());
  }

  private EditablePointElement(ElementProperties properties, IPoint location) {
    super(properties, location);
  }

  @Override
  public boolean contains(int paddingPixels, IPoint pt) {
    IPoint loc = location();
    return loc.x - paddingPixels < pt.x && loc.x + paddingPixels > pt.x && loc.y - paddingPixels < pt.y
        && loc.y + paddingPixels > pt.y;
  }

  @Override
  public EditablePointElement applyTransform(Matrix m) {
    IPoint loc = location();
    IPoint tfm = m.apply(loc);
    return new EditablePointElement(properties(), tfm);
  }

  private static final Color OBJECT_COLOR = new Color(50, 255, 50);
  private static final Color SELECTED_BORDER_COLOR = Color.YELLOW;

  private static final Paint sObjectPaint = Paint.newBuilder().fill().color(OBJECT_COLOR).build();
  private static final Paint sSelectedBorderPaint = Paint.newBuilder().color(SELECTED_BORDER_COLOR).width(2)
      .build();

  private static final FRect SELECTED_RELATIVE_BOUNDS = new FRect(-6, -6, 6 * 2, 6 * 2);

  @Override
  public void render(EditorPanel panel, Render appearance) {
    loadTools();
    panel.pushFocusOn(bounds().toRect());
    if (appearance == Render.SELECTED)
      panel.apply(sSelectedBorderPaint).renderFrame(SELECTED_RELATIVE_BOUNDS);
    panel.apply(sObjectPaint).renderDisc(IPoint.ZERO, 4.0f);
    panel.popFocus();
  }

  public EditablePointElement withLocation(IPoint loc) {
    return new EditablePointElement(properties(), loc);
  }

  @Override
  public EditablePointElement withProperties(ElementProperties properties) {
    return new EditablePointElement(properties, location());
  }

}
