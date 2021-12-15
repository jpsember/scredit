package js.widget;

import java.util.List;
import java.util.Random;

import javax.swing.SwingConstants;

import js.base.BaseObject;
import js.data.DataUtil;
import js.json.JSMap;
import js.parsing.RegExp;

import static js.base.Tools.*;

public abstract class WidgetManager extends BaseObject {

  public static final int SIZE_DEFAULT = 0;
  public static final int SIZE_TINY = 1;
  public static final int SIZE_SMALL = 2;
  public static final int SIZE_LARGE = 3;
  public static final int SIZE_HUGE = 4;
  public static final int SIZE_MEDIUM = 5;

  public static final int ALIGNMENT_DEFAULT = -1;
  public static final int ALIGNMENT_LEFT = SwingConstants.LEFT;
  public static final int ALIGNMENT_CENTER = SwingConstants.CENTER;
  public static final int ALIGNMENT_RIGHT = SwingConstants.RIGHT;

  public WidgetManager() {
    withStateMap(map());
  }

  protected void withStateMap(JSMap map) {
    mStateMap = map;
  }

  /**
   * Determine if app is in landscape (vs portrait) mode. Default implementation
   * always returns true
   */
  public boolean landscapeMode() {
    return true;
  }

  protected abstract int stackSize();

  public static String viewInfo(Object c) {
    String s = c.getClass().getSimpleName();
    if (s.isEmpty())
      s = c.getClass().getName();
    return s;
  }

  // ---------------------------------------------------------------------
  // Widget state
  // ---------------------------------------------------------------------

  public final JSMap stateMap() {
    return mStateMap;
  }

  public final boolean validStateMap() {
    return !mStateMap.isEmpty();
  }

  public final WidgetManager setStateMap(JSMap map) {
    checkNotNull(map);
    mStateMap = map;
    return this;
  }

  public final boolean prepared() {
    return mPrepared;
  }

  public final WidgetManager setPrepared(boolean state) {
    mPrepared = state;
    if (!state)
      mTimeSincePrepared = System.currentTimeMillis();
    return this;
  }

  public final WidgetManager restoreWidgetValues() {
    for (Widget w : mWidgetList) {
      if (w.key() == null)
        continue;
      Object value = mStateMap.optUnsafe(w.key());
      w.restoreValue(value);
    }
    return this;
  }

  // ---------------------------------------------------------------------
  // Composing
  // ---------------------------------------------------------------------

  /**
   * <pre>
   *
   * Set the number of columns, and which ones can grow, for the next view in
   * the hierarchy. The columns expression is a string of column expressions,
   * which may be one of:
   * 
   *     "."   a column with weight zero
   *     "x"   a column with weight 100
   *     "\d+" column with integer weight
   * 
   * Spaces are ignored, except to separate integer weights from each other.
   * </pre>
   */
  public final WidgetManager columns(String columnsExpr) {
    checkState(mPendingColumnWeights == null, "previous column weights were never used");

    List<Integer> columnSizes = arrayList();
    for (String word : split(columnsExpr, ' ')) {
      if (RegExp.patternMatchesString("\\d+", word)) {
        columnSizes.add(Integer.parseInt(word));
      } else {
        for (int i = 0; i < word.length(); i++) {
          char c = columnsExpr.charAt(i);
          int size;
          if (c == '.') {
            size = 0;
          } else if (c == 'x') {
            size = 100;
          } else {
            throw new IllegalArgumentException(columnsExpr);
          }
          columnSizes.add(size);
        }
      }
    }
    mPendingColumnWeights = DataUtil.intArray(columnSizes);
    return this;
  }

  /**
   * Make next component added occupy remaining columns in its row
   */
  public final WidgetManager spanx() {
    mSpanXCount = -1;
    return this;
  }

  /**
   * Make next component added occupy some number of columns in its row
   */
  public final WidgetManager spanx(int count) {
    checkArgument(count > 0);
    mSpanXCount = count;
    return this;
  }

  /**
   * Skip a single cell
   */
  public final WidgetManager skip() {
    add(wrap(null));
    return this;
  }

  /**
   * Skip one or more cells
   */
  public final WidgetManager skip(int count) {
    spanx(count);
    add(wrap(null));
    return this;
  }

  /**
   * Set pending component, and the column it occupies, as 'growable'. This can
   * also be accomplished by using an 'x' when declaring the columns.
   * <p>
   * Calls growX(100)...
   */
  public final WidgetManager growX() {
    return growX(100);
  }

  /**
   * Set pending component, and the column it occupies, as 'growable'. This can
   * also be accomplished by using an 'x' when declaring the columns.
   * <p>
   * Calls growY(100)...
   */
  public final WidgetManager growY() {
    return growY(100);
  }

  /**
   * Set pending component's horizontal weight to a value > 0 (if it is already
   * less than this value)
   */
  public final WidgetManager growX(int weight) {
    mGrowXFlag = Math.max(mGrowXFlag, weight);
    return this;
  }

  /**
   * Set pending component's vertical weight to a value > 0 (if it is already
   * less than this value)
   */
  public final WidgetManager growY(int weight) {
    mGrowYFlag = Math.max(mGrowYFlag, weight);
    return this;
  }

  public final WidgetManager withTabs(String selectTabKey) {
    mPendingTabPanelKey = selectTabKey;
    return this;
  }

  public final WidgetManager tabTitle(String title) {
    mPendingTabTitle = title;
    return this;
  }

  /**
   * Specify the container to use for the next open() call, instead of
   * generating one
   */
  public final WidgetManager setPendingContainer(Object component) {
    mPendingContainer = component;
    return this;
  }

  /**
   * Wrap a system-specific element within a Widget
   *
   * @param component
   *          component, or null to represent a gap in the layout
   */
  public abstract Widget wrap(Object component);

  /**
   * Add widget to view hierarchy
   */
  public abstract Widget add(Widget widget);

  /**
   * Create a child view and push onto stack
   */
  public abstract Widget open();

  /**
   * Create a view, push onto stack; but don't add the view to the current
   * hierarchy
   */
  public abstract Widget openFree();

  /**
   * Pop view from the stack
   */
  public abstract WidgetManager close();

  public final WidgetManager tooltip(String tip) {
    if (mTooltip != null)
      alert("Tooltip was ignored:", mTooltip);
    mTooltip = tip;
    return this;
  }

  private WidgetManager setPendingSize(int value) {
    mPendingSize = value;
    return this;
  }

  private WidgetManager setPendingAlignment(int value) {
    mPendingAlignment = value;
    return this;
  }

  public final WidgetManager small() {
    return setPendingSize(SIZE_SMALL);
  }

  public final WidgetManager large() {
    return setPendingSize(SIZE_LARGE);
  }

  public final WidgetManager medium() {
    return setPendingSize(SIZE_MEDIUM);
  }

  public final WidgetManager tiny() {
    return setPendingSize(SIZE_TINY);
  }

  public final WidgetManager huge() {
    return setPendingSize(SIZE_HUGE);
  }

  public final WidgetManager left() {
    return setPendingAlignment(ALIGNMENT_LEFT);
  }

  public final WidgetManager right() {
    return setPendingAlignment(ALIGNMENT_RIGHT);
  }

  public final WidgetManager center() {
    return setPendingAlignment(ALIGNMENT_CENTER);
  }

  /**
   * Have next widget use a monospaced font
   */
  public final WidgetManager monospaced() {
    mPendingMonospaced = true;
    return this;
  }

  public final WidgetManager minWidth(float ems) {
    mPendingMinWidthEm = ems;
    return this;
  }

  public final WidgetManager minHeight(float ems) {
    mPendingMinHeightEm = ems;
    return this;
  }

  public final WidgetManager fixedWidth(float ems) {
    mPendingFixedWidthEm = ems;
    return this;
  }

  public final WidgetManager fixedHeight(float ems) {
    mPendingFixedHeightEm = ems;
    return this;
  }

  public final WidgetManager gravity(int gravity) {
    mPendingGravity = gravity;
    return this;
  }

  public final WidgetManager editable() {
    mEditableFlag = true;
    return this;
  }

  public final WidgetManager lineCount(int numLines) {
    mLineCount = numLines;
    checkArgument(numLines > 0);
    return this;
  }

  public final WidgetManager scrollable() {
    mScrollableFlag = true;
    return this;
  }

  public abstract Widget addLog(String key);

  /**
   * Add a colored panel, for test purposes
   */
  public abstract Widget addPanel();

  /**
   * Add a dummy item to prevent an editable text field from gaining focus
   * automatically (and showing the keyboard).
   * <p>
   * It will add a column-spanning row of zero height.
   */
  public WidgetManager suppressTextFocus() {
    return this;
  }

  public final Widget addLabel() {
    return addLabel(null, "");
  }

  public final Widget addLabel(String text) {
    return addLabel(null, text);
  }

  public abstract Widget addLabel(String key, String text);

  /**
   * Add a text field whose content is not persisted to the state map
   */
  public final Widget addText() {
    return addText(null);
  }

  public abstract Widget addText(String key);

  public abstract WidgetManager addHeader(String text);

  /**
   * Add a horizontal space to occupy cell(s) in place of other widgets
   */
  public abstract WidgetManager addHorzSpace();

  /**
   * If current row is only partially complete, add space to its end
   */
  public abstract WidgetManager endRow();

  /**
   * Add a horizontal separator that visually separates components above from
   * below
   */
  public abstract WidgetManager addHorzSep();

  /**
   * Add a vertical separator that visually separates components left from right
   */
  public abstract WidgetManager addVertSep();

  /**
   * Add a row that can stretch vertically to occupy the available space
   */
  public abstract WidgetManager addVertGrow();

  public abstract void showModalErrorDialog(String message);

  public abstract void showModalInfoDialog(String message);

  public final WidgetManager pushListener(WidgetListener listener) {
    checkState(mPendingListener == null, "already a pending listener");
    mListenerStack.add(listener);
    return this;
  }

  public final WidgetManager popListener() {
    checkState(mPendingListener == null, "already a pending listener");
    checkState(!mListenerStack.isEmpty(), "listener stack underflow");
    pop(mListenerStack);
    return this;
  }

  protected final void ensureListenerStackEmpty() {
    checkState(mListenerStack.isEmpty(), "listener stack wasn't empty");
  }

  public final WidgetManager listener(WidgetListener listener) {
    checkState(mPendingListener == null, "already a pending listener");
    mPendingListener = listener;
    return this;
  }

  public final Widget addButton(String label) {
    return addButton(null, label);
  }

  public abstract Widget addButton(String key, String label);

  public abstract Widget addToggleButton(String key, String label);

  public final WidgetManager floats() {
    mPendingFloatingPoint = true;
    return this;
  }

  public final WidgetManager min(double value) {
    floats();
    mPendingMinValue = value;
    return this;
  }

  public final WidgetManager min(int value) {
    mPendingMinValue = value;
    return this;
  }

  public final WidgetManager max(double value) {
    floats();
    mPendingMaxValue = value;
    return this;
  }

  public final WidgetManager max(int value) {
    mPendingMaxValue = value;
    return this;
  }

  public final WidgetManager defaultVal(double value) {
    floats();
    mPendingDefaultValue = value;
    return this;
  }

  public final WidgetManager defaultVal(int value) {
    mPendingDefaultValue = value;
    return this;
  }

  /**
   * Include auxilliary widget with this one; e.g., numeric display with slider,
   * or time stamp with log
   */
  public final WidgetManager withDisplay() {
    mPendingWithDisplayFlag = true;
    return this;
  }

  public final WidgetManager stepSize(double value) {
    floats();
    mPendingStepSize = value;
    return this;
  }

  public final WidgetManager stepSize(int value) {
    mPendingStepSize = value;
    return this;
  }

  public abstract Widget addSlider(String key);

  public Widget addSpinner(String key) {
    throw new UnsupportedOperationException();
  }

  /**
   * Append some choices for the next ComboBox
   */
  public final WidgetManager choices(String... choices) {
    for (String choiceExpr : choices) {
      if (mComboChoices == null)
        mComboChoices = new SymbolicNameSet();
      mComboChoices.add(choiceExpr);
    }
    return this;
  }

  public abstract Widget addChoiceBox(String key);

  /**
   * If there's a pending WidgetListener, return it (and clear it)
   */
  public final WidgetListener consumePendingListener() {
    WidgetListener listener = mPendingListener;
    mPendingListener = null;
    if (listener == null && !mListenerStack.isEmpty()) {
      listener = last(mListenerStack);
    }
    return listener;
  }

  protected final String consumePendingTabTitle(Object component) {
    String tabNameExpression = "?NAME?";
    if (mPendingTabTitle == null)
      alert("no tab name specified for:", component);
    else {
      tabNameExpression = mPendingTabTitle;
      mPendingTabTitle = null;
    }
    return tabNameExpression;
  }

  protected void clearPendingComponentFields() {
    mPendingContainer = null;
    mPendingColumnWeights = null;
    mSpanXCount = 0;
    mGrowXFlag = mGrowYFlag = 0;
    mPendingSize = SIZE_DEFAULT;
    mPendingAlignment = ALIGNMENT_DEFAULT;
    mPendingGravity = 0;
    mPendingMinWidthEm = 0;
    mPendingMinHeightEm = 0;
    mPendingFixedWidthEm = 0;
    mPendingFixedHeightEm = 0;
    mPendingMonospaced = false;
    mEditableFlag = false;
    mScrollableFlag = false;
    mLineCount = 0;
    mComboChoices = null;
    mPendingMinValue = null;
    mPendingMaxValue = null;
    mPendingDefaultValue = null;
    mPendingStepSize = null;
    mPendingTabPanelKey = null;
    mPendingWithDisplayFlag = false;
    mPendingFloatingPoint = false;
  }

  // ------------------------------------------------------------------
  // Layout logic
  // ------------------------------------------------------------------

  protected int[] mPendingColumnWeights;
  protected boolean mSuppressFocusFlag;

  /**
   * Call widget listener, setting up event source beforehand
   */
  public void notifyWidgetListener(Widget widget, WidgetListener listener) {
    if (!prepared()) {
      long currentTime = System.currentTimeMillis();
      if (mTimeSincePrepared == 0)
        mTimeSincePrepared = System.currentTimeMillis();
      if (currentTime - mTimeSincePrepared > 5000) {
        pr("*** Widget listeners are not being called... did you forget setPrepared(true)?");
      }
      return;
    }
    Widget previousListener = mListenerWidget;
    try {
      mLastWidgetEventTime = System.currentTimeMillis();
      mListenerWidget = widget;
      listener.event();
    } finally {
      mListenerWidget = previousListener;
    }
  }

  /**
   * Get widget associated with listener event
   */
  public Widget eventSource() {
    checkNotNull(mListenerWidget, "no event source found");
    return mListenerWidget;
  }

  public long timeSinceLastEvent() {
    long currTime = System.currentTimeMillis();
    if (mLastWidgetEventTime == 0)
      mLastWidgetEventTime = currTime;
    return currTime - mLastWidgetEventTime;
  }

  public static String randomText(int maxLength, boolean withLinefeeds) {
    StringBuilder sb = new StringBuilder();
    int len = (int) Math.abs(random().nextGaussian() * maxLength);
    while (sb.length() < len) {
      int wordSize = random().nextInt(8) + 2;
      if (withLinefeeds && random().nextInt(4) == 0)
        sb.append('\n');
      else
        sb.append(' ');
      final String sample = "orhxxidfusuytelrcfdlordburswfxzjfjllppdsywgsw"
          + "kvukrammvxvsjzqwplxcpkoekiznlgsgjfonlugreiqvtvpjgrqotzu";
      int cursor = random().nextInt(sample.length() - wordSize);
      sb.append(sample.substring(cursor, cursor + wordSize));
    }
    return sb.toString().trim();
  }

  public static Random random() {
    if (mRandom == null)
      mRandom = new Random();
    return mRandom;
  }

  private static Random mRandom;
  private JSMap mStateMap;
  private WidgetListener mPendingListener;
  private Widget mListenerWidget;
  private boolean mPrepared;
  private long mTimeSincePrepared;

  protected Object mPendingContainer;
  protected String mPendingTabTitle;
  protected int mSpanXCount;
  protected int mGrowXFlag, mGrowYFlag;
  protected int mPendingSize;
  protected boolean mEditableFlag;
  protected boolean mScrollableFlag;
  protected int mPendingGravity;
  protected float mPendingMinWidthEm;
  protected float mPendingMinHeightEm;
  protected float mPendingFixedWidthEm;
  protected float mPendingFixedHeightEm;
  protected boolean mPendingMonospaced;
  protected int mPendingAlignment;
  protected int mLineCount;
  protected String mPendingTabPanelKey;
  protected SymbolicNameSet mComboChoices;
  protected List<Widget> mWidgetList = arrayList();
  protected String mTooltip;
  protected Number mPendingMinValue, mPendingMaxValue, mPendingDefaultValue, mPendingStepSize;
  protected boolean mPendingFloatingPoint;
  protected boolean mPendingWithDisplayFlag;
  private long mLastWidgetEventTime;
  private List<WidgetListener> mListenerStack = arrayList();
}
