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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import js.geometry.FPoint;
import js.geometry.FRect;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.Matrix;
import js.geometry.MyMath;
import js.graphics.Paint;
import js.graphics.RectElement;
import js.graphics.ScriptElement;
import js.graphics.gen.ElementProperties;
import js.guiapp.UserEvent;
import js.guiapp.UserOperation;
import js.scredit.EditorElement;
import js.scredit.EditorPanel;
import js.scredit.ScrEdit;
import js.scredit.oper.AdjustBoxRotationOper;
import js.scredit.oper.RectEditOper;

public class EditableRectElement extends RectElement implements EditorElement {

  public static final EditableRectElement DEFAULT_INSTANCE = new EditableRectElement(
      RectElement.DEFAULT_INSTANCE.properties(), RectElement.DEFAULT_INSTANCE.bounds());

  @Override
  public EditableRectElement toEditorElement(ScriptElement obj) {
    return new EditableRectElement(obj.properties(), obj.bounds());
  }

  @Override
  public EditableRectElement withProperties(ElementProperties properties) {
    return new EditableRectElement(properties, bounds());
  }

  public EditableRectElement withBounds(IRect newBounds) {
    return new EditableRectElement(properties(), newBounds);
  }

  protected EditableRectElement(ElementProperties properties, IRect bounds) {
    super(properties, bounds);
  }

  @Override
  public boolean contains(int paddingPixels, IPoint pt) {
    IRect paddedBounds = bounds().withInset(-paddingPixels);
    return paddedBounds.contains(pt);
  }

  @Override
  public EditableRectElement applyTransform(Matrix m) {
    //todo("!This doesn't take into account effect of scaling or rotating");
    IPoint center = bounds().midPoint();
    IPoint newCenter = m.apply(center);
    IRect newBounds = bounds().translatedBy(IPoint.difference(newCenter, center));
    return withBounds(newBounds);
  }

  private static final Paint PAINT_NOMINAL = Paint.newBuilder().width(4).color(119, 52, 235).build();
  private static final Paint PAINT_DISABLED = Paint.newBuilder().width(3).color(119, 52, 235, 64).build();
  private static final Paint PAINT_SELECTED = Paint.newBuilder().width(8).color(252, 232, 3).build();
  private static final Paint DISC = Paint.newBuilder().width(4).color(255, 255, 0, 128).build();
  private static final Paint DISC_GUIDE = DISC.toBuilder().color(82, 212, 47).width(2).build();
  private static final Paint DISC_GUIDE_BGND = DISC_GUIDE.toBuilder().color(0, 0, 0, 128).width(3.5f).build();

  private static final FPoint[] sDiscPoints = new FPoint[6];
  private static final FPoint[] sViewPoints = new FPoint[sDiscPoints.length];
  private static final int[] sSegmentScript = { 0, 1, 2, 3, 4, 5 };

  @Override
  public void render(EditorPanel panel, Render appearance) {
    FRect bounds = panel.pushFocusOn(bounds().toRect());

    // Draw thick colored frame, with a half pixel overlap with the black single-pixel frames
    // we will draw on top.
    // The half pixels are to fill in any cracks caused by scaling.
    FRect r = bounds.withInset(-0.5f);

    Paint paint;
    switch (appearance) {
    default:
      throw notSupported();
    case DISABLED:
      paint = getDisabledPaint();
      break;
    case NOMINAL:
      paint = getNominalPaint();
      break;
    case SELECTED:
      paint = PAINT_SELECTED;
      break;
    }
    panel.apply(paint).renderFrame(r);

    if (appearance == Render.SELECTED) {
      panel.apply(BLACK_LINE);
      // Draw a single pixel black frame (which, if the colored frame was drawn, will lie within it)
      panel.renderFrame(bounds);
      // Draw single pixel black frame around the colored frame to distinguish frame from background
      FRect r1 = bounds.withInset(-(1 + PAINT_SELECTED.width()));
      panel.renderFrame(r1);

      //if (properties().rotation() != null)
      {
        FPoint center = bounds.midPoint();
        float radius = MyMath.magnitudeOfRay(bounds.width, bounds.height) / 2;
        radius = Math.max(radius + 25, 100);
        Graphics2D g = panel.graphics();
        panel.apply(DISC);
        g.draw(new Ellipse2D.Double(center.x - radius, center.y - radius, 2 * radius, 2 * radius));

        // If there is already a rotation, draw guide 

        if (properties().rotation() != null) {
          // Construct list of vertices in disc space
          //
          int j = 0;
          for (int i = -1; i <= 1; i++) {
            float y = (-i * radius * .2f);
            float x = MyMath.sqrtf(radius * radius - y * y);
            sDiscPoints[j++] = new FPoint(-x, y);
            sDiscPoints[j++] = new FPoint(x, y);
          }

          // Construct list of these points transformed to view space
          //
          Matrix tfm = Matrix.preMultiply(Matrix.getRotate(properties().rotation() * MyMath.M_DEG),
              Matrix.getTranslate(center));
          j = -1;
          for (FPoint pt : sDiscPoints) {
            j++;
            sViewPoints[j] = tfm.apply(pt);
          }

          for (int pass = 0; pass < 2; pass++) {
            panel.apply(pass == 0 ? DISC_GUIDE_BGND : DISC_GUIDE);
            int[] script = sSegmentScript;
            for (int i = 0; i < script.length; i += 2)
              panel.renderLine(sViewPoints[script[i]], sViewPoints[script[i + 1]]);
          }
        }
      }
    }

    todo("!We should have a generic category renderer for rects AND polygons AND...");

    // Render category, if appropriate

    if (properties().category() != null) {
      String categoryString = "" + properties().category();
      panel.apply(appearance == Render.SELECTED ? CATEGORY_TEXT_SELECTED : CATEGORY_TEXT);
      final float TITLE_OFFSET = 16;
      panel.renderText(categoryString, bounds.midX(), bounds.y - TITLE_OFFSET);
    }

    panel.popFocus();
  }

  protected Paint getNominalPaint() {
    return PAINT_NOMINAL;
  }

  protected Paint getDisabledPaint() {
    return PAINT_DISABLED;
  }

  @Override
  public UserOperation isEditingSelectedObject(ScrEdit editor, int slot, UserEvent event) {

    IPoint pt = event.getWorldLocation();

    int edElement = -1;
    int paddingPixels = editor.paddingPixels();
    IRect r = bounds().withInset((int) (-paddingPixels * .75f));
    for (int c = 0; c < 4; c++) {
      IPoint corn = r.corner(c);
      float dist = MyMath.distanceBetween(corn, pt);
      if (dist < paddingPixels) {
        return new RectEditOper(editor, event, slot, c);
      }
    }
    if (edElement < 0)
      for (int edge = 0; edge < 4; edge++) {
        IPoint ep0 = r.corner(edge);
        IPoint ep1 = r.corner((edge + 1) & 3);
        float dist = MyMath.ptDistanceToSegment(pt.toFPoint(), ep0.toFPoint(), ep1.toFPoint(), null);
        if (dist < paddingPixels) {
          edElement = edge + 4;
          return new RectEditOper(editor, event, slot, edge + 4);
        }
      }

    if (event.isDownVariant() && !event.hasModifierKeys()) {

      // We need to reproduce the same distance calcs that we used when rendering
      float zoom = editor.zoomFactor();
      float zm = 1f / zoom;

      FPoint midpoint = bounds().midPoint().toFPoint();

      float radius = MyMath.magnitudeOfRay(bounds().width, bounds().height) / 2;
      radius = Math.max(radius + 25 * zm, 100 * zm);
      float dist = MyMath.distanceBetween(midpoint, pt.toFPoint());
      final float GRAB_TOLERANCE = 35;

      if (Math.abs(dist - radius) < GRAB_TOLERANCE * zm) {
        return new AdjustBoxRotationOper(editor, event, slot);
      }
    }

    return null;
  }

  /**
   * Return a modified rectangle if necessary, so dimensions are positive
   */
  public static IRect ensureValid(IRect r) {
    if (r.isDegenerate())
      r = new IRect(r.x, r.y, Math.max(1, r.width), Math.max(1, r.height));
    return r;
  }

  private static final Paint BLACK_LINE = Paint.newBuilder().width(1).color(Color.BLACK).build();
  private static final Paint CATEGORY_TEXT = PAINT_NOMINAL.toBuilder().color(200, 100, 255).bigFont(1.6f)
      .build();
  private static final Paint CATEGORY_TEXT_SELECTED = CATEGORY_TEXT.toBuilder().bigFont(2.2f)
      .color(Color.YELLOW).build();

}
