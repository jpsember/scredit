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
import java.util.ArrayList;
import java.util.List;

import js.geometry.FPoint;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.Matrix;
import js.geometry.MyMath;
import js.geometry.Polygon;
import js.graphics.Paint;
import js.graphics.PolygonElement;
import js.graphics.ScriptElement;
import js.graphics.gen.ElementProperties;
import js.guiapp.UserEvent;
import js.guiapp.UserOperation;
import js.scredit.EditorElement;
import js.scredit.EditorPanel;
import js.scredit.ScrEdit;
import js.scredit.oper.PolygonEditOper;

public final class EditablePolygonElement extends PolygonElement implements EditorElement {

  public static final EditablePolygonElement DEFAULT_INSTANCE = new EditablePolygonElement(
      PolygonElement.DEFAULT_INSTANCE.properties(), PolygonElement.DEFAULT_INSTANCE.polygon(), false);

  @Override
  public EditablePolygonElement toEditorElement(ScriptElement obj) {
    PolygonElement elem = (PolygonElement) obj;
    return new EditablePolygonElement(elem.properties(), elem.polygon(), false);
  }

  public EditablePolygonElement(ElementProperties properties, Polygon polygon, boolean curveMode) {
    super(properties, polygon);
    mCurveMode = curveMode;
  }

  @Override
  public boolean contains(int paddingPixels, IPoint pt) {
    IRect paddedBounds = bounds().withInset(-paddingPixels);
    return paddedBounds.contains(pt);
  }

  @Override
  public EditablePolygonElement applyTransform(Matrix m) {
    return withPolygon(polygon().applyTransform(m));
  }

  public EditablePolygonElement withPolygon(Polygon polygon) {
    return new EditablePolygonElement(properties(), polygon, curveMode());
  }

  private ArrayList<IPoint> getVertices(Polygon p) {
    ArrayList<IPoint> lst = arrayList();
    for (IPoint vert : p.vertices())
      lst.add(vert);
    return lst;
  }

  /**
   * Add a point at a particular location, shifting following points to make
   * room; return new polygon
   */
  public EditablePolygonElement withAddPoint(int ptIndex, IPoint point) {
    List<IPoint> v = getVertices(polygon());
    v.add(ptIndex, point);
    return withPolygon(polygon().withVertices(v));
  }

  public EditablePolygonElement withDeletedPoint(int ptIndex) {
    List<IPoint> v = getVertices(polygon());
    v.remove(ptIndex);
    return withPolygon(polygon().withVertices(v));
  }

  public EditablePolygonElement withSetPoint(int ptIndex, IPoint point) {
    checkState(ptIndex >= 0 && ptIndex < polygon().numVertices(), "attempt to store point", ptIndex,
        "for size", polygon().numVertices());
    List<IPoint> v = getVertices(polygon());
    v.set(ptIndex, point);
    return withPolygon(polygon().withVertices(v));
  }

  /**
   * For displaying polygon being edited, a vertex to be added and the position
   * it is to be inserted at
   */
  public EditablePolygonElement withInsertVertex(int position, IPoint vertexOrNull) {
    EditablePolygonElement p = new EditablePolygonElement(properties(), polygon(), curveMode());
    p.mInsertVertex = vertexOrNull;
    p.mInsertPosition = position;
    return p;
  }

  public boolean curveMode() {
    return mCurveMode;
  }

  @Override
  public UserOperation isEditingSelectedObject(ScrEdit editor, int slot, UserEvent event) {

    UserOperation ret = null;

    IPoint pt = event.getWorldLocation();
    float tolerance = editor.paddingPixels();
    float toleranceSq = tolerance * tolerance;
    int edElement = -1;
    float closestDistSq = 0;
    for (int v = 0; v < polygon().numVertices(); v++) {
      IPoint vertex = polygon().vertex(v);
      float distSq = MyMath.squaredDistanceBetween(vertex, pt);
      if (distSq >= toleranceSq)
        continue;
      if (edElement < 0 || distSq < closestDistSq) {
        edElement = v;
        closestDistSq = distSq;
      }
    }

    if (edElement >= 0)
      ret = PolygonEditOper.buildEditExistingOper(editor, event, slot, edElement);
    return ret;
  }

  @Override
  public void render(EditorPanel panel, Render appearance) {

    // We want the line width to be constant, independent of the zoom factor
    float scale = 1.0f / panel.zoomFactor();

    Paint paint;
    switch (appearance) {
    default:
      throw notSupported();
    case DISABLED:
      paint = PAINT_DISABLED;
      break;
    case NOMINAL:
      // todo("set color based on flavor(?)");
      paint = PAINT_NOMINAL;
      break;
    case SELECTED:
      paint = PAINT_SELECTED;
      break;
    }

    panel.apply(paint.toBuilder().width(paint.width() * scale));

    // Determine vertices, if any, involved in vertex being inserted

    IPoint pt1 = null;
    IPoint pt2 = null;
    int nPoints = polygon().numVertices();
    if (mInsertVertex != null && nPoints > 0) {
      pt1 = polygon().vertexMod(mInsertPosition - 1);
      if (mInsertPosition < nPoints) {
        pt2 = polygon().vertexMod(mInsertPosition);
      }
    }

    IPoint start = null;
    IPoint last = null;
    for (IPoint pt : polygon().vertices()) {
      if (start == null)
        start = pt;
      if (last != null) {
        if (last == pt1 && pt == pt2) {
          panel.apply(Paint.newBuilder().color(Color.RED).width(0.8f * scale));
          panel.renderLine(last, pt);
        } else
          renderLine(panel, scale, last, pt, appearance == Render.SELECTED);
      }
      last = pt;
    }

    if (polygon().isClosed() && start != null && last != null) {
      renderLine(panel, scale, last, start, appearance == Render.SELECTED);
    }
    if (mInsertVertex != null) {
      if (pt1 != null)
        renderLine(panel, scale, pt1, mInsertVertex, false);
      if (pt2 != null)
        renderLine(panel, scale, mInsertVertex, pt2, false);
      panel.renderDisc(mInsertVertex, VERTEX_RADIUS * scale);
    }

    // TODO: render title?

    if (appearance == Render.SELECTED && !curveMode())
      for (IPoint pt : polygon().vertices())
        panel.renderDisc(pt, VERTEX_RADIUS * scale);
  }

  private void renderLine(EditorPanel panel, float scale, IPoint i1, IPoint i2, boolean withArrows) {
    FPoint p1 = i1.toFPoint();
    FPoint p2 = i2.toFPoint();
    panel.renderLine(p1, p2);
    if (curveMode() || !withArrows)
      return;

    if (MyMath.distanceBetween(p1, p2) < scale * REQUIRED_LENGTH_FOR_ARROWS)
      return;

    FPoint arrowLoc = FPoint.midPoint(p1, p2);
    float angle = MyMath.polarAngle(p1, p2);
    FPoint pa = MyMath.pointOnCircle(arrowLoc, angle - MyMath.M_DEG * (180 - ARROW_ANGLE),
        scale * ARROW_HEAD_LENGTH);
    FPoint pb = MyMath.pointOnCircle(arrowLoc, angle + MyMath.M_DEG * (180 - ARROW_ANGLE),
        scale * ARROW_HEAD_LENGTH);
    panel.renderLine(pa, arrowLoc);
    panel.renderLine(arrowLoc, pb);
  }

  // ------------------------------------------------------------------
  // Constants for rendering
  // ------------------------------------------------------------------

  private static final float VERTEX_RADIUS = 2.5f;
  private static final float REQUIRED_LENGTH_FOR_ARROWS = 20;
  private static final float ARROW_HEAD_LENGTH = 5;
  private static final float ARROW_ANGLE = 30;

  private static final Paint PAINT_NOMINAL = Paint.newBuilder().width(4).color(119, 52, 235).build();
  private static final Paint PAINT_DISABLED = Paint.newBuilder().width(3).color(119, 52, 235, 64).build();
  private static final Paint PAINT_SELECTED = Paint.newBuilder().width(4).color(255, 0, 0).build();

  private final boolean mCurveMode;
  private IPoint mInsertVertex;
  private int mInsertPosition;

}
