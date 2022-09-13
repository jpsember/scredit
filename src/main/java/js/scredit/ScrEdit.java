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

import static js.base.Tools.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;

import javax.swing.*;

import geom.GeomApp;
import js.data.AbstractData;
import js.geometry.IPoint;
import js.guiapp.*;
import js.json.JSMap;
import js.scredit.gen.ScreditConfig;
import geom.oper.*;

public final class ScrEdit extends GeomApp {

  public static void main(String[] args) {
    new ScrEdit().startApplication(args);
  }

  @Override
  public final boolean hasImageSupport() {
    return true;
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
  public AbstractData defaultArgs() {
    return ScreditConfig.DEFAULT_INSTANCE;
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
  public void populateMenuBarForProject(MenuBarWrapper m) {
    super.populateMenuBarForProject(m);
    addCategoryMenu(m);
  }

  @Override
  public void addProjectMenu(MenuBarWrapper m) {
    super.addProjectMenu(m);
    m.addSeparator();
    addItem("project_find_problems", "Find Problems", new FindProblemsOper());
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

}
