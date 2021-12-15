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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;

import js.widget.Widget;
import js.widget.WidgetManager;

/**
 * Subclass of Widget for Swing platforms
 */
public class SwingWidget extends Widget implements ActionListener {

  public SwingWidget(WidgetManager manager, String key) {
    super(manager, key);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    notifyListener();
  }

  public void setComponent(JComponent component) {
    mWrappedComponent = component;
  }

  @Override
  public final Object component() {
    return mWrappedComponent;
  }

  @SuppressWarnings("unchecked")
  public final <T extends JComponent> T swingComponent() {
    return (T) component();
  }

  /**
   * Get component to attach tooltip to (if there is one). Default
   * implementation returns swingComponent()
   */
  public JComponent componentForTooltip() {
    return swingComponent();
  }

  public void setText(String text) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getText() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setHint(String hint) {
    throw new UnsupportedOperationException();
  }

  private JComponent mWrappedComponent;

}
