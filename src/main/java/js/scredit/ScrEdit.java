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

import static geom.GeomTools.*;
import static js.base.Tools.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.List;

import javax.swing.*;

import geom.AppDefaults;
import geom.GeomApp;
import geom.ScriptManager;
import js.data.AbstractData;
import js.geometry.IPoint;
import js.guiapp.*;
import js.json.JSMap;
import js.scredit.gen.ScreditConfig;
import js.scredit.oper.FileJumpOper;
import js.scredit.oper.FileStepOper;
import js.scredit.oper.FileStepUsedOper;
import js.scredit.oper.FindProblemsOper;
import geom.oper.*;

public final class ScrEdit extends GeomApp {

  public static final boolean DISABLE_FLUSH_CHANGES = false && alert("Saving script changes is DISABLED");

  public static void main(String[] args) {
    new ScrEdit().startApplication(args);
  }

  // ------------------------------------------------------------------
  // Construction
  // ------------------------------------------------------------------

  private ScrEdit() {
    guiAppConfig() //
        .appName("scredit") //
        .keyboardShortcutRegistry(JSMap.fromResource(this.getClass(), "key_shortcut_defaults.json")) //;
    ;
  }

  @Override
  public void startedGUI() {
    ScriptManager.setSingleton(new ScriptManager());
    openAppropriateProject();
  }

  @Override
  public AbstractData defaultArgs() {
    return ScreditConfig.DEFAULT_INSTANCE;
  }


  @Override
  public List<Object> getOptionalArgDescriptions() {
    return arrayList("[<project directory>]");
  }

  @Override
  public float zoomFactor() {
    return projectState().zoomFactor();
  }

  @Override
  public void setZoomFactor(float zoom) {
    projectState().zoomFactor(zoom);
  }

  // ------------------------------------------------------------------
  // Menu bar
  // ------------------------------------------------------------------

  @Override
  public boolean usesProjects() {
    return true;
  }

  @Override
  public void populateMenuBar(MenuBarWrapper m) {
    addProjectMenu(m);
    if (currentProject().definedAndNonEmpty()) {
      addFileMenu(m);
      addEditMenu(m);
      addViewMenu(m);
      addCategoryMenu(m);
    }
  }

  @Override
  public void addProjectMenu(MenuBarWrapper m) {
    super.addProjectMenu(m);
    m.addSeparator();
    addItem("project_find_problems", "Find Problems", new FindProblemsOper());
  }

  private void addFileMenu(MenuBarWrapper m) {
    m.addMenu("File", null);
    UserOperation prevOper = new FileStepOper(-1);
    UserOperation nextOper = new FileStepOper(1);
    UserOperation prevUsedOper = new FileStepUsedOper(-1);
    UserOperation nextUsedOper = new FileStepUsedOper(1);
    addItem("script_step_bwd", "Prev", prevOper);
    addItem("script_step_fwd", "Next", nextOper);
    addItem("script_step_bwd2", "Prev_", prevOper);
    addItem("script_step_fwd2", "Next_", nextOper);
    addItem("script_page_bwd", "Page Bwd", new FileStepOper(-1).withAccel());
    addItem("script_page_fwd", "Page Fwd", new FileStepOper(1).withAccel());
    addItem("script_used_prev", "Prev Used", prevUsedOper);
    addItem("script_used_next", "Next Used", nextUsedOper);
    addItem("script_jump_first", "First", new FileJumpOper(-1));
    addItem("script_jump_last", "Last", new FileJumpOper(1));
  }

  @Override
  public void addEditMenu(MenuBarWrapper m) {
    super.addEditMenu(m);

    m.addSeparator();
    addItem("mask_add", "Add Mask", new MaskAddOper());
    addItem("rotation_toggle", "Toggle Rotation", new ToggleRotationOper());

    {
      UserOperation oper = PolygonEditOper.buildAddCurveOper();
      addItem("curve_add", "Add Curve", oper);
      addItem("curve_add2", "Add Curve (2)", oper);
    }

    addItem("yolo_merge", "Yolo Merge", new NonMaxSuppressOper());
  }

  private void addCategoryMenu(MenuBarWrapper m) {
    m.addMenu("Category");
    for (int i = 0; i < 10; i++)
      m.addItem("category_" + i, "" + i, SetCategoryOper.buildSetCategoryOper(i));
  }

  // ------------------------------------------------------------------
  // User interface elements within frame
  // ------------------------------------------------------------------

  @Override
  public void userEventManagerListener(UserEvent event) {
    // Avoid repainting if default operation and just a mouse move
    // (though later we may want to render the mouse's position in an info box)
    int repaintFlags = UserEventManager.sharedInstance().getOperation().repaintRequiredFlags(event);
    if (repaintFlags != 0)
      performRepaint(repaintFlags);
  }

  @Override
  public void repaintPanels(int repaintFlags) {
    if (0 != (repaintFlags & REPAINT_EDITOR))
      getEditorPanel().repaint();

    if (infoPanel() != null)
      if (0 != (repaintFlags & REPAINT_INFO))
        infoPanel().refresh();
  }

  @Override
  public String getTitleText() {
    if (currentProject().defined()) {
      File dir = currentProject().directory();
      return dir.getName();
    }
    return null;
  }

  @Override
  public void populateFrame(JPanel parentPanel) {
    if (currentProject().isDefault())
      return;

    todo("who creates the editor panel?");
    constructEditorPanel();
    constructInfoPanel();
    if (false) {
      mControlPanel = new JPanel() {
        @Override
        public Dimension getPreferredSize() {
          todo("!this is a placeholder until there's an actual control panel");
          return new IPoint(120, 40).toDimension();
        }
      };
      mControlPanel.setBackground(Color.blue);
    }

    // Allow the control panel to occupy the full vertical height by putting it in its own panel
    {
      JPanel subPanel = new JPanel(new BorderLayout());
      subPanel.add(getEditorPanel(), BorderLayout.CENTER);
      parentPanel.add(subPanel, BorderLayout.CENTER);
    }
    if (mControlPanel != null)
      parentPanel.add(mControlPanel, BorderLayout.EAST);
    if (infoPanel() != null)
      parentPanel.add(infoPanel(), BorderLayout.SOUTH);
  }

  private JPanel mControlPanel;

  // ------------------------------------------------------------------
  // Periodic background tasks (e.g. flushing changes to script)
  // ------------------------------------------------------------------

  @Override
  public void swingBackgroundTask() {
    todo("Move this to geom project");
    if (!currentProject().defined())
      return;

    mTaskTicker++;
    scriptManager().flushScript();
    if ((mTaskTicker & 0x3) == 0) {
      if (currentProject().defined())
        flushProject();
      else
        alert("wtf? current project was not defined");
      AppDefaults.sharedInstance().flush();
    }
  }

  private int mTaskTicker;

  @Override
  public String getAlertText() {
    todo("Move this to geom project");
     if (!currentProject().defined())
      return "No project selected; open one from the Project menu";
    if (!currentProject().definedAndNonEmpty())
      return "This project is empty! Open another from the Project menu";
    return null;
  }

}
