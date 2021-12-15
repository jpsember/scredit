package js.widget;

import js.json.JSMap;
import static js.base.Tools.*;

/**
 * A wrapper class for user interface components, compatible with both Swing and
 * Android platforms
 */
public abstract class Widget {

  public Widget(WidgetManager manager, String key) {
    mManager = manager;
    mKey = key;
  }

  @SuppressWarnings("unchecked")
  public final <T extends WidgetManager> T manager() {
    return (T) mManager;
  }

  private WidgetManager mManager;

  public abstract String getText();

  public abstract void setText(String text);

  public static void setTextIfExists(Widget widget, String text) {
    if (widget != null)
      widget.setText(text);
  }

  public void appendText(String text) {
    throw new UnsupportedOperationException();
  }

  public abstract void setHint(String hint);

  protected final void registerListener(WidgetListener listener) {
    mListener = listener;
  }

  /**
   * Notify WidgetListener, if there is one, of an event involving this widget
   */
  protected final void notifyListener() {
    if (mListener != null)
      manager().notifyWidgetListener(this, mListener);
  }

  protected final void storeValueToStateMap(JSMap stateMap, Object value) {
    // Don't persist value if no key was given
    if (key() == null)
      return;
    if (!mManager.validStateMap())
      return;
    stateMap.putUnsafe(key(), value);
  }

  public void restoreValue(Object value) {
    pr("...no restoreValue override, value:", value, "class:", className());
  }

  public String key() {
    return mKey;
  }

  @Override
  public String toString() {
    return nullTo(key(), "<none>") + ":" + getClass().getSimpleName();
  }

  public void displayKeyboard() {
    throw new UnsupportedOperationException();
  }

  public void hideKeyboard() {
    throw new UnsupportedOperationException();
  }

  public void setInputType(int inputType) {
    throw new UnsupportedOperationException();
  }

  public Object component() {
    throw new UnsupportedOperationException();
  }

  public void setEnabled(boolean enabled) {
    todo("setEnabled not implemented for:" , className());
  }

  public void setVisible(boolean visible) {
    todo("setVisible not implemented for:" , className());
  }

  public void doClick() {
    throw new UnsupportedOperationException();
  }

  public void setChecked(boolean state) {
    throw new UnsupportedOperationException();
  }

  public boolean isChecked() {
    throw new UnsupportedOperationException();
  }

  public void setValue(Number number) {
    throw new UnsupportedOperationException();
  }

  /**
   * Cause the wrapped component to be repainted
   */
  public void repaint() {
    throw new UnsupportedOperationException();
  }

  /**
   * Replace this widget in its view hierarchy with another
   */
  public void replaceWith(Widget other) {
    throw new UnsupportedOperationException();
  }

  private String className() {
    return getClass().getSimpleName();
  }

  private final String mKey;
  protected WidgetListener mListener;
}
