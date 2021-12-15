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
package js.widget;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;

import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import js.graphics.Paint;
import js.widget.WidgetManager;
import static js.base.Tools.*;

public class SwingUtils {

  public static String str(GridBagConstraints gc) {
    StringBuilder sb = new StringBuilder();
    sb.append("GBC[");
    sb.append(" wt:");
    sb.append(gc.weightx);
    sb.append(",");
    sb.append(gc.weighty);
    sb.append(" g:");
    sb.append(gc.gridx);
    sb.append(",");
    sb.append(gc.gridy);
    sb.append(" sz:");
    sb.append(gc.gridwidth);
    sb.append(",");
    sb.append(gc.gridheight);
    sb.append(" a:");
    sb.append(gc.anchor);
    sb.append(" f:");
    sb.append(gc.fill);
    sb.append(" ]");
    return sb.toString();
  }

  public static Border buildStandardBorderWithZeroBottom() {
    return new EmptyBorder(10, 10, 0, 10);
  }

  public static Border buildStandardBorder() {
    return new EmptyBorder(10, 10, 10, 10);
  }

  public static JComponent addStandardBorderForSpacing(JComponent component) {
    component.setBorder(buildStandardBorder());
    return component;
  }

  public static void applyMinDimensions(JComponent container, Font font, float minWidthEm,
      float minHeightEm) {
    if (minWidthEm == 0 && minHeightEm == 0)
      return;
    float pointSizeToEmScaleFactor = 0.60f;
    if (font == null)
      font = Paint.LABEL_FONT;
    float minWidthPixels = font.getSize() * pointSizeToEmScaleFactor * minWidthEm;
    float minHeightPixels = font.getSize() * 1.4f * minHeightEm;
    Dimension d = new Dimension((int) minWidthPixels, (int) minHeightPixels);
    // See https://stackoverflow.com/questions/12048047
    container.setPreferredSize(d);
    container.setMinimumSize(d);
  }

  public static void applyMinDimensions(JComponent container, float minWidthEm, float minHeightEm) {
    applyMinDimensions(container, null, minWidthEm, minHeightEm);
  }

  public static int sizeClassToPixels(int sizeClass) {
    int fontSize;
    switch (sizeClass) {
    case WidgetManager.SIZE_DEFAULT:
      fontSize = 16;
      break;
    case WidgetManager.SIZE_SMALL:
      fontSize = 12;
      break;
    default:
      alert("unsupported widget font size: ", sizeClass);
      fontSize = 16;
      break;
    }
    return fontSize;
  }
}
