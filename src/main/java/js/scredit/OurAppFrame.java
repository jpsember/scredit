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
package js.scredit;

import java.awt.Color;
import java.awt.Toolkit;

import javax.swing.JFrame;

import static js.base.Tools.*;

import js.base.BaseObject;
import js.geometry.IPoint;
import js.geometry.IRect;

public class OurAppFrame extends BaseObject {

  public OurAppFrame() {
    loadTools();
    JFrame frame = new JFrame();
    // Set a distinctive background color in case no other components are added
    // (e.g. if no project is open)
    frame.getContentPane().setBackground(Color.gray);
    mFrame = frame;
    setBounds(IRect.DEFAULT_INSTANCE);
  }

  public JFrame frame() {
    return mFrame;
  }

  public IRect bounds() {
    return new IRect(frame().getBounds());
  }

  public void setBounds(IRect bounds) {
    if (bounds.minDim() < 40) {
      IPoint screenSize = new IPoint(Toolkit.getDefaultToolkit().getScreenSize());
      IPoint defaultSize = screenSize.scaledBy(.9f);
      bounds = new IRect((screenSize.x - defaultSize.x) / 2, (screenSize.y - defaultSize.y) / 2,
          defaultSize.x, defaultSize.y);
    }
    frame().setBounds(bounds.toRectangle());
  }

  private JFrame mFrame;
}
