package js.widget;

import static js.base.Tools.*;

/**
 * Data associated with a view occupying one or more cells within its parent
 * view
 */
public final class GridCell {

  public Object view;
  public int x;
  public int y;
  public int width;
  public int growX, growY;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("cell ");
    sb.append(WidgetManager.viewInfo(view));
    tab(sb, 16);
    sb.append(x);
    sb.append(",");
    sb.append(y);
    sb.append(" w:" + width);
    sb.append(" grow: ");
    sb.append(growX > 0 ? "X" : "-");
    sb.append(",");
    sb.append(growY > 0 ? "Y" : "-");
    return sb.toString();
  }

  public boolean isEmpty() {
    return view == null;
  }
}

