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
