package js.widget;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import static js.base.Tools.*;
import js.data.DataUtil;
import js.geometry.IPoint;
import js.geometry.MyMath;
import js.widget.Grid;
import js.widget.GridCell;
import js.widget.NumericStepper;
import js.widget.WidgetManager;
import js.widget.SymbolicNameSet;
import js.widget.Widget;
import static js.widget.SwingUtils.*;

/**
 * Swing implementation of abstract widget manager
 */
public final class SwingWidgetManager extends WidgetManager {

  @Override
  public SwingWidget wrap(Object component) {
    if (component == null || component instanceof JComponent) {
      return new ComponentWidget(this, (JComponent) component);
    }
    if (component instanceof SwingWidget)
      return (SwingWidget) component;
    throw new IllegalArgumentException("cannot create Widget wrapper for: " + component);
  }

  @Override
  public SwingWidget open() {
    Grid grid = new Grid(false);
    if (mPendingTabPanelKey != null) {
      checkState(mPendingContainer == null, "cannot use pending container for tabbed pane");
      grid.setWidget(new OurTabbedPane(this, mPendingTabPanelKey));
    } else {
      if (mPendingColumnWeights == null)
        columns("x");
      grid.setColumnSizes(mPendingColumnWeights);

      JComponent panel;
      if (mPendingContainer != null)
        panel = (JComponent) mPendingContainer;
      else {
        panel = new JPanel();
        applyMinDimensions(panel, mPendingMinWidthEm, mPendingMinHeightEm);
      }
      panel.setLayout(buildLayout());
      addStandardBorderForSpacing(panel);
      grid.setWidget(wrap(panel));
    }
    add(grid.widget());
    mPanelStack.add(grid);
    return grid.widget();
  }

  @Override
  public SwingWidgetManager close() {
    endRow();
    Grid parent = pop(mPanelStack);
    if (verbose()) {
      log("close", compInfo(gridComponent(parent)));
      log("");
    }

    if (!(parent.widget() instanceof OurTabbedPane)) {
      assignViewsToGridLayout(parent);
    }

    if (mPanelStack.isEmpty()) {
      SwingWidget widget = parent.widget();
      mOutermostView = widget.swingComponent();
      ensureListenerStackEmpty();
    }
    return this;
  }

  @Override
  public WidgetManager endRow() {
    Grid parent = last(mPanelStack);
    if (parent.nextCellLocation().x != 0)
      spanx().addHorzSpace();
    return this;
  }

  @Override
  public Widget addText(String key) {
    //unimp("fixedWidthEm not implemented (Issue #726); also, if multiline, use as fixed height");
    OurText t = new OurText(this, key, mLineCount, mEditableFlag, mPendingSize, mPendingMonospaced,
        mPendingMinWidthEm, mPendingMinHeightEm);
    consumeTooltip(t);
    add(t);
    return t;
  }

  @Override
  public Widget addLog(String key) {
    throw notFinished();
  }

  @Override
  public SwingWidgetManager addHeader(String text) {
    spanx();
    JLabel label = new JLabel(text);
    label.setBorder(
        new CompoundBorder(buildStandardBorderWithZeroBottom(), BorderFactory.createEtchedBorder()));
    label.setHorizontalAlignment(SwingConstants.CENTER);
    add(wrap(label));
    return this;
  }

  @Override
  public SwingWidgetManager addHorzSpace() {
    add(wrap(new JPanel()));
    return this;
  }

  @Override
  public SwingWidgetManager addHorzSep() {
    spanx();
    add(wrap(new JSeparator(JSeparator.HORIZONTAL)));
    return this;
  }

  @Override
  public SwingWidgetManager addVertSep() {
    spanx();
    growY();
    add(wrap(new JSeparator(JSeparator.VERTICAL)));
    return this;
  }

  @Override
  public SwingWidgetManager addVertGrow() {
    JComponent panel;
    if (verbose())
      panel = colorPanel();
    else
      panel = new JPanel();
    spanx().growY();
    add(wrap(panel));
    return this;
  }

  @Override
  public SwingWidget add(Widget w) {
    SwingWidget widget = (SwingWidget) w;
    mWidgetList.add(widget);
    JComponent tooltipOwner = widget.componentForTooltip();
    if (tooltipOwner != null)
      consumeTooltip(tooltipOwner);
    addView(widget);
    return widget;
  }

  private void consumeTooltip(SwingWidget widget) {
    consumeTooltip(widget.swingComponent());
  }

  private void consumeTooltip(JComponent component) {
    if (mTooltip != null) {
      // Don't consume the tooltip if the component is a label or panel; we will
      // assume it is targeted at some later component
      if (component instanceof JPanel || component instanceof JLabel)
        return;
      component.setToolTipText(mTooltip);
      mTooltip = null;
    }
  }

  /**
   * Get the outermost view in the hierarchy
   */
  public JComponent container() {
    return checkNotNull(mOutermostView, "layout not completed");
  }

  private JComponent mOutermostView;

  /**
   * Add a component to the current panel. Process pending constraints
   */
  private SwingWidgetManager addView(SwingWidget widget) {
    checkState(mOutermostView == null, "manager layout already completed");

    consumeTooltip(widget);

    if (!mPanelStack.isEmpty())
      auxAddComponent(widget);

    clearPendingComponentFields();
    return this;

  }

  private void auxAddComponent(SwingWidget widget) {
    JComponent component = widget.swingComponent();

    Grid grid = last(mPanelStack);
    if (grid.widget() instanceof OurTabbedPane) {
      OurTabbedPane tabPane = grid.widget();
      tabPane.add(consumePendingTabTitle(component), component);
      return;
    }

    GridCell cell = new GridCell();
    cell.view = widget;
    IPoint nextGridCellLocation = grid.nextCellLocation();
    cell.x = nextGridCellLocation.x;
    cell.y = nextGridCellLocation.y;

    // determine location and size, in cells, of component
    int cols = 1;
    if (mSpanXCount != 0) {
      int remainingCols = grid.numColumns() - cell.x;
      if (mSpanXCount < 0)
        cols = remainingCols;
      else {
        if (mSpanXCount > remainingCols)
          throw new IllegalStateException(
              "requested span of " + mSpanXCount + " yet only " + remainingCols + " remain");
        cols = mSpanXCount;
      }
    }
    cell.width = cols;

    cell.growX = mGrowXFlag;
    cell.growY = mGrowYFlag;

    // If any of the spanned columns have 'grow' flag set, set it for this component
    for (int i = cell.x; i < cell.x + cell.width; i++) {
      int colSize = grid.columnSizes()[i];
      cell.growX = Math.max(cell.growX, colSize);
    }

    // "paint" the cells this view occupies by storing a copy of the entry in each cell
    for (int i = 0; i < cols; i++)
      grid.addCell(cell);

  }

  private int mColorIndex;
  private static Color sColors[] = { Color.BLUE, Color.GREEN, Color.RED, Color.GRAY, Color.MAGENTA,
      Color.pink.darker(), Color.BLUE.darker(), Color.GREEN.darker(), Color.RED.darker(), Color.GRAY.darker(),
      Color.MAGENTA.darker(), };

  private JComponent colorPanel() {
    JPanel panel = new JPanel();
    panel.setBackground(randomColor());
    return panel;
  }

  private Color randomColor() {
    return sColors[MyMath.myMod(mColorIndex++, sColors.length)];
  }

  private static String compInfo(Component c) {
    String s = c.getClass().getSimpleName();
    if (c instanceof JLabel) {
      s = s + " " + quoted(((JLabel) c).getText());
    }
    return s;
  }

  private static String quoted(String s) {
    if (s == null)
      return "<null>";
    return "\"" + s + "\"";
  }

  @Override
  public Widget addButton(String key, String label) {
    OurButton button = new OurButton(this, key, label);
    add(button);
    return button;
  }

  @Override
  public Widget addToggleButton(String key, String label) {
    OurToggleButton button = new OurToggleButton(this, key, label);
    add(button);
    return button;
  }

  @Override
  public SwingWidget addLabel(String key, String text) {
    return add(new OurLabel(this, key, mPendingGravity, mLineCount, text, mPendingSize, mPendingMonospaced,
        mPendingAlignment));
  }

  public Widget addSpinner(String key) {
    OurSpinner spinner = new OurSpinner(this, key, mPendingFloatingPoint, mPendingDefaultValue,
        mPendingMinValue, mPendingMaxValue, mPendingStepSize);
    add(spinner);
    return spinner;
  }

  @Override
  public Widget addSlider(String key) {
    OurSlider slider = new OurSlider(this, key, mPendingFloatingPoint, mPendingDefaultValue, mPendingMinValue,
        mPendingMaxValue, mPendingWithDisplayFlag);
    add(slider);
    return slider;
  }

  @Override
  public Widget addChoiceBox(String key) {
    OurComboBox c = new OurComboBox(this, key, mComboChoices);
    add(c);
    return c;
  }

  @Override
  public void showModalErrorDialog(String message) {
    JOptionPane.showMessageDialog(getApplicationFrame(), message, "Problem", JOptionPane.ERROR_MESSAGE);
  }

  @Override
  public void showModalInfoDialog(String message) {
    JOptionPane.showMessageDialog(getApplicationFrame(), message, "Info", JOptionPane.INFORMATION_MESSAGE);
  }

  private Component getApplicationFrame() {
    todo("getApplicationFrame returning null for now");
    return null;
  }

  private static final class ComponentWidget extends SwingWidget {
    public ComponentWidget(WidgetManager manager, JComponent component) {
      super(manager, null);
      setComponent(component);
    }

    @Override
    public void setVisible(boolean visible) {
      swingComponent().setVisible(visible);
    }
  }

  private static final class OurTabbedPane extends SwingWidget implements ChangeListener {

    public OurTabbedPane(WidgetManager manager, String key) {
      super(manager, key);
      JTabbedPane component = new JTabbedPane();
      component.addChangeListener(this);
      setComponent(component);
      registerListener(manager.consumePendingListener());
    }

    public void add(String tabNameExpr, JComponent component) {
      mTabNames.add(tabNameExpr);
      String lastDisplayName = last(mTabNames.displayNames());
      tabbedPane().add(lastDisplayName, component);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      storeValueToStateMap(manager().stateMap(), mTabNames.getSymbolicName(tabbedPane().getSelectedIndex()));
      notifyListener();
    }

    @Override
    public void restoreValue(Object value) {
      if (value == null)
        value = "";
      String symbolicName = value.toString();
      int index = mTabNames.getSymbolicIndex(symbolicName);
      tabbedPane().setSelectedIndex(index);
    }

    private JTabbedPane tabbedPane() {
      return swingComponent();
    }

    private SymbolicNameSet mTabNames = new SymbolicNameSet();
  }

  private static final class OurButton extends SwingWidget {

    public OurButton(WidgetManager manager, String key, String label) {
      super(manager, key);
      JButton component = new JButton(label);
      component.addActionListener(this);
      setComponent(component);
      registerListener(manager.consumePendingListener());
    }

    @Override
    public void setEnabled(boolean enabled) {
      swingComponent().setEnabled(enabled);
    }
  }

  private static final class OurToggleButton extends SwingWidget {
    public OurToggleButton(WidgetManager manager, String key, String label) {
      super(manager, key);
      JCheckBox component = new JCheckBox(label);
      component.addActionListener(this);
      setComponent(component);
      registerListener(manager.consumePendingListener());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JCheckBox component = swingComponent();
      storeValueToStateMap(manager().stateMap(), component.isSelected());
      super.actionPerformed(e);
    }

    @Override
    public void restoreValue(Object value) {
      Boolean boolValue = (Boolean) value;
      if (boolValue == null)
        boolValue = false;
      setChecked(boolValue);
    }

    @Override
    public boolean isChecked() {
      JCheckBox component = swingComponent();
      return component.isSelected();
    }

    @Override
    public void doClick() {
      JCheckBox component = swingComponent();
      component.doClick();
    }

    @Override
    public void setChecked(boolean state) {
      JCheckBox component = swingComponent();
      component.setSelected(state);
    }
  }

  private static final class OurSpinner extends SwingWidget implements ChangeListener {
    public OurSpinner(WidgetManager manager, String key, boolean floatsFlag, Number defaultValue,
        Number minimum, Number maximum, Number stepSize) {
      super(manager, key);
      // If no default value is defined, take from state map (if there's a key)
      if (defaultValue == null && key != null) {
        defaultValue = (Number) manager.stateMap().optUnsafe(key);
      }
      mStepper = new NumericStepper(floatsFlag, defaultValue, minimum, maximum, stepSize);
      checkState(mStepper.isInt(), "non-integer not supported");
      SpinnerModel model = new SpinnerNumberModel(mStepper.def().intValue(), mStepper.min().intValue(),
          mStepper.max().intValue(), mStepper.step().intValue());
      JSpinner component = new JSpinner(model);
      model.addChangeListener(this);
      setComponent(component);
      registerListener(manager.consumePendingListener());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      JSpinner component = swingComponent();
      storeValueToStateMap(manager().stateMap(), component.getModel().getValue());
      notifyListener();
    }

    @Override
    public void restoreValue(Object value) {
      JSpinner component = swingComponent();
      Number number = (Number) value;
      if (number == null)
        number = mStepper.def();
      component.getModel().setValue(number.intValue());
    }

    private NumericStepper mStepper;
  }

  private static final class OurSlider extends SwingWidget implements ChangeListener {
    public OurSlider(WidgetManager manager, String key, boolean floatsFlag, Number defaultValue,
        Number minimum, Number maximum, boolean includesDisplay) {
      super(manager, key);
      // If no default value is defined, take from state map (if there's a key)
      if (defaultValue == null && key != null) {
        defaultValue = (Number) manager.stateMap().optUnsafe(key);
      }
      mStepper = new NumericStepper(floatsFlag, defaultValue, minimum, maximum, null);
      JComponent component;
      JSlider slider = new JSlider(mStepper.internalMin(), mStepper.internalMax(), mStepper.internalVal());
      slider.addChangeListener(this);
      mSlider = slider;
      if (includesDisplay) {
        int maxValueStringLength = mStepper.maxDigits();
        mDisplay = new JTextField(maxValueStringLength);
        mDisplay.setEditable(false);
        mDisplay.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel container = new JPanel();
        // Make the container's layout a BorderLayout, with the slider grabbing the available space
        container.setLayout(new BorderLayout());
        container.add(slider, BorderLayout.CENTER);
        container.add(mDisplay, BorderLayout.EAST);
        component = container;
        updateDisplayValue();
      } else {
        component = slider;
      }
      setComponent(component);
      registerListener(manager.consumePendingListener());
    }

    @Override
    public JComponent componentForTooltip() {
      return getSlider();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      int numValue = getSlider().getModel().getValue();
      storeValueToStateMap(manager().stateMap(), mStepper.fromInternalUnits(numValue));
      updateDisplayValue();
      notifyListener();
    }

    @Override
    public void restoreValue(Object value) {
      Number number = (Number) value;
      getSlider().getModel().setValue(mStepper.toInternalUnits(number));
    }

    private void updateDisplayValue() {
      if (mDisplay == null)
        return;
      int numValue = getSlider().getModel().getValue();
      String value = mStepper.fromInternalUnits(numValue).toString();
      mDisplay.setText(value);
    }

    public JSlider getSlider() {
      return mSlider;
    }

    @Override
    public void setValue(Number number) {
      int intValue = number.intValue();
      storeValueToStateMap(manager().stateMap(), intValue);
      getSlider().getModel().setValue(number.intValue());
      updateDisplayValue();
      notifyListener();
      todo("are we supposed to store to the state as well?");
    }

    private NumericStepper mStepper;
    private JTextField mDisplay;
    private JSlider mSlider;
  }

  private static final class OurLabel extends SwingWidget {

    public OurLabel(WidgetManager manager, String key, int gravity, int lineCount, String text, int fontSize,
        boolean monospaced, int alignment) {
      super(manager, key);
      JLabel label = new JLabel(text);
      if (fontSize == SIZE_DEFAULT)
        fontSize = SIZE_SMALL;
      Font font = getFont(monospaced, fontSize);
      label.setFont(font);
      if (alignment == ALIGNMENT_DEFAULT)
        alignment = ALIGNMENT_RIGHT;
      label.setHorizontalAlignment(alignment);
      setComponent(label);
    }

    private JLabel textComponent() {
      return swingComponent();
    }

    @Override
    public void setText(String text) {
      textComponent().setText(text);
    }

    @Override
    public String getText() {
      return textComponent().getText();
    }
  }

  private static final class OurText extends SwingWidget implements DocumentListener {
    public OurText(WidgetManager manager, String key, int lineCount, boolean editable, int fontSize,
        boolean monospaced, float minWidthEm, float minHeightEm) {
      super(manager, key);
      JComponent container;
      JTextComponent textComponent;
      if (lineCount > 1) {
        JTextArea textArea = new JTextArea(lineCount, 40);
        textArea.setLineWrap(true);
        textComponent = textArea;
        container = new JScrollPane(textArea);
      } else {
        JTextField textField = new JTextField();
        textComponent = textField;
        container = textComponent;
      }
      textComponent.setEditable(editable);
      if (editable) {
        registerListener(manager.consumePendingListener());
        textComponent.getDocument().addDocumentListener(this);
      }
      Font font = getFont(monospaced, fontSize);
      textComponent.setFont(font);

      applyMinDimensions(container, font, minWidthEm, minHeightEm);
      mTextComponent = textComponent;
      setComponent(container);
    }

    @Override
    public void restoreValue(Object value) {
      String textValue = nullToEmpty((String) value);
      textComponent().setText(textValue);
    }

    public JTextComponent textComponent() {
      return mTextComponent;
    }

    @Override
    public void setText(String text) {
      textComponent().setText(text);
    }

    @Override
    public String getText() {
      return textComponent().getText();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      commonUpdateHandler();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      commonUpdateHandler();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      commonUpdateHandler();
    }

    private void commonUpdateHandler() {
      if (key() != null) {
        storeValueToStateMap(manager().stateMap(), textComponent().getText());
      }
      notifyListener();
    }

    private JTextComponent mTextComponent;
  }

  private static final class OurComboBox extends SwingWidget {
    public OurComboBox(WidgetManager manager, String key, SymbolicNameSet choices) {
      super(manager, key);
      mChoices = checkNotNull(choices);
      JComboBox<String> component = new JComboBox<>(DataUtil.toStringArray(mChoices.displayNames()));
      component.addActionListener(this);
      setComponent(component);
      registerListener(manager.consumePendingListener());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JComboBox<String> component = swingComponent();
      String selectedItem = (String) component.getSelectedItem();
      storeValueToStateMap(manager().stateMap(), displayToInternal(selectedItem));
      super.actionPerformed(e);
    }

    @Override
    public void restoreValue(Object value) {
      JComboBox<String> component = swingComponent();
      int index = mChoices.getSymbolicIndex((String) value);
      component.setSelectedIndex(index);
    }

    private String displayToInternal(String s) {
      return mChoices.displayToSymbolic(s);
    }

    private SymbolicNameSet mChoices;
  }

  private List<Grid> mPanelStack = arrayList();

  // ------------------------------------------------------------------
  // Layout manager
  // ------------------------------------------------------------------

  private LayoutManager buildLayout() {
    return new GridBagLayout();
  }

  @Override
  protected int stackSize() {
    return mPanelStack.size();
  }

  @Override
  public SwingWidget addPanel() {
    return add(wrap(colorPanel()));
  }

  private static <T extends JComponent> T gridComponent(Grid grid) {
    SwingWidget widget = grid.widget();
    return widget.swingComponent();
  }

  private void assignViewsToGridLayout(Grid grid) {
    grid.propagateGrowFlags();
    SwingWidget containerWidget = grid.widget();
    JComponent container = containerWidget.swingComponent();

    int gridWidth = grid.numColumns();
    int gridHeight = grid.numRows();
    for (int gridY = 0; gridY < gridHeight; gridY++) {
      for (int gridX = 0; gridX < gridWidth; gridX++) {
        GridCell cell = grid.cellAt(gridX, gridY);
        if (cell.isEmpty())
          continue;

        // If cell's coordinates don't match our iteration coordinates, we've
        // already added this cell
        if (cell.x != gridX || cell.y != gridY)
          continue;

        GridBagConstraints gc = new GridBagConstraints();

        float weightX = cell.growX;
        gc.weightx = weightX;
        gc.gridx = cell.x;
        gc.gridwidth = cell.width;
        gc.weighty = cell.growY;
        gc.gridy = cell.y;
        gc.gridheight = 1;

        SwingWidget widget = (SwingWidget) cell.view;
        JComponent component = widget.swingComponent();
        // Padding widgets have no views
        if (component == null)
          continue;

        // Not using gc.anchor
        gc.fill = GridBagConstraints.BOTH;

        // Not using gravity
        container.add(widget.swingComponent(), gc);
      }
    }
  }

  @Override
  public Widget openFree() {
    throw die("not supported");
  }

  public static Font getFont(boolean monospaced, int widgetFontSize) {
    //unimp("use SwingUtils for this");
    int fontSize;
    switch (widgetFontSize) {
    case WidgetManager.SIZE_DEFAULT:
      fontSize = 16;
      break;
    case WidgetManager.SIZE_MEDIUM:
      fontSize = 16;
      break;
    case WidgetManager.SIZE_SMALL:
      fontSize = 12;
      break;
    default:
      alert("unsupported widget font size:", widgetFontSize);
      fontSize = 16;
      break;
    }

    Integer mapKey = fontSize + (monospaced ? 0 : 1000);

    Font f = mFontMap.get(mapKey);
    if (f == null) {
      if (monospaced)
        f = new Font("Monaco", Font.PLAIN, fontSize);
      else
        f = new Font("Lucida Grande", Font.PLAIN, fontSize);
      mFontMap.put(mapKey, f);
    }
    return f;
  }

  private static Map<Integer, Font> mFontMap = hashMap();

}
