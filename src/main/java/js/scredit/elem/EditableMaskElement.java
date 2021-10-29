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

import static js.base.Tools.*;

import js.geometry.IRect;
import js.graphics.MaskElement;
import js.graphics.Paint;
import js.graphics.ScriptElement;
import js.graphics.gen.ElementProperties;

public class EditableMaskElement extends EditableRectElement {

  public static final EditableMaskElement DEFAULT_INSTANCE = new EditableMaskElement(
      MaskElement.DEFAULT_INSTANCE.properties(), MaskElement.DEFAULT_INSTANCE.bounds());

  @Override
  public String tag() {
    loadTools();
    return MaskElement.TAG;
  }

  @Override
  public EditableMaskElement toEditorElement(ScriptElement obj) {
    return new EditableMaskElement(obj.properties(), obj.bounds());
  }

  @Override
  public EditableMaskElement withBounds(IRect newBounds) {
    return new EditableMaskElement(properties(), newBounds);
  }

  private EditableMaskElement(ElementProperties properties, IRect bounds) {
    super(properties, bounds);
  }

  private static final Paint PAINT_NOMINAL = Paint.newBuilder().fill().color(141, 42, 184, 128) //
      .build();
  private static final Paint PAINT_DISABLED = Paint.newBuilder().fill().color(141, 42, 184, 64).build();

  @Override
  protected Paint getNominalPaint() {
    return PAINT_NOMINAL;
  }

  @Override
  protected Paint getDisabledPaint() {
    return PAINT_DISABLED;
  }
}
