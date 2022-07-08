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

import java.util.Comparator;
import java.util.List;

import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.MyMath;
import js.graphics.RectElement;
import js.graphics.ScriptElement;
import js.graphics.ScriptUtil;
import js.scredit.EditorElement;
import js.scredit.gen.Command;
import js.scredit.gen.ScriptEditState;

public class NonMaxSuppressOper extends CommandOper {

  @Override
  public boolean constructCommand(Command.Builder b) {
    ScriptEditState.Builder s = b.newState().toBuilder();

    List<EditorElement> hl = s.elements(); //StateTools.subsetOfElements(s.elements(), s.selectedElements());
    if (hl.size() < 2)
      return false;

    for (EditorElement elem : hl)
      if (!elem.is(RectElement.DEFAULT_INSTANCE))
        return false;

    List<EditorElement> result = (List<EditorElement>) performNonMaximumSuppression(hl, 0.4f, true);
    b.newState(s//
        .elements(result)//
        .selectedElements(null)//
    );
    return true;

  }

  public static final Comparator<ScriptElement> COMPARATOR_LEFT_TO_RIGHT = (a, b) -> {
    IPoint ac = a.bounds().center();
    IPoint bc = b.bounds().center();
    int rc = Integer.compare(ac.x, bc.x);
    if (rc == 0)
      rc = Integer.compare(ac.y, bc.y);
    if (rc == 0)
      rc = Integer.compare(a.bounds().width, b.bounds().width);
    if (rc == 0)
      rc = Integer.compare(a.bounds().height, b.bounds().height);
    if (rc == 0)
      rc = Integer.compare(ScriptUtil.categoryOrZero(a), ScriptUtil.categoryOrZero(b));
    return rc;
  };

  public static final Comparator<ScriptElement> COMPARATOR_CONFIDENCE = (a, b) -> {
    int rc = 0;
    if (ScriptUtil.hasConfidence(a) && ScriptUtil.hasConfidence(b))
      rc = -Integer.compare(ScriptUtil.confidence(a), ScriptUtil.confidence(b));
    if (rc == 0)
      rc = COMPARATOR_LEFT_TO_RIGHT.compare(a, b);
    return rc;
  };

  public static List<? extends ScriptElement> performNonMaximumSuppression(
      List<? extends ScriptElement> boxes, float maxIOverU) {
    return performNonMaximumSuppression(boxes, maxIOverU, false);
  }

  public static List<? extends ScriptElement> performNonMaximumSuppression(
      List<? extends ScriptElement> boxes, float maxIOverU, boolean db) {

    List<ScriptElement> sorted = arrayList();
    sorted.addAll(boxes);
    sorted.sort(COMPARATOR_CONFIDENCE);
    if (db)
      pr("performNonMaxSuppression; sorted elements:", INDENT, sorted);

    List<ScriptElement> result = arrayList();
    omit: for (ScriptElement b : sorted) {
      IRect br = b.bounds();
      if (db)
        pr("testing box:", b);

      for (ScriptElement a : result) {
        IRect ar = a.bounds();
        float iOverU = MyMath.intersectionOverUnion(//
            ar.x, ar.y, ar.width, ar.height, //
            br.x, br.y, br.width, br.height);

        // If logging, only show results if the two boxes have the same category
        if (db && ScriptUtil.categoryOrZero(b) == ScriptUtil.categoryOrZero(a))
          pr("existing:", INDENT, a, CR, "i/u:", iOverU);

        if (iOverU >= maxIOverU) {
          if (db)
            pr("...omitting");
          continue omit;
        }
      }
      if (db)
        pr("...adding to result");
      result.add(b);
    }
    result.sort(COMPARATOR_LEFT_TO_RIGHT);
    return result;
  }

}
