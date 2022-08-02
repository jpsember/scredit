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

import java.util.List;

import js.base.BasePrinter;
import js.graphics.PolygonElement;
import js.graphics.ScriptUtil;
import js.graphics.gen.Script;
import js.json.JSMap;
import js.scredit.Project;
import js.scredit.ScriptWrapper;
import static geom.GeomTools.*;

public final class FindProblemsOper extends UserOperation {

  @Override
  public void start() {

    Project proj = editor().currentProject();
    if (proj.isDefault())
      return;

    JSMap summary = map();
    int firstProblem = -1;
    for (int i = 0; i < proj.scriptCount(); i++) {
      ScriptWrapper w = proj.script(i);
      mProbMap = null;

      if (!w.hasImage()) {
        addProb("no image");
      }

      Script sc = w.data();
      List<PolygonElement> polys = ScriptUtil.polygonElements(sc);
      if (polys.size() != 1) {
        addProb("unexpected polygon count:", polys.size());
      }
      if (sc.items().size() != polys.size())
        addProb("extraneous items");
      if (mProbMap != null) {
        summary.put(w.name(), mProbMap);
        if (firstProblem < 0)
          firstProblem = i;
      }
    }
    pr("Project:", proj.directory());
    if (firstProblem >= 0) {
      pr("Problems:", INDENT, summary);
      editor().switchToScript(firstProblem);
    } else
      pr("...no problems");
  }

  private JSMap probMap() {
    if (mProbMap == null)
      mProbMap = map();
    return mProbMap;
  }

  private void addProb(Object... messages) {
    String msg = BasePrinter.toString(messages);
    probMap().putNumbered(msg);
  }

  private JSMap mProbMap;

}
