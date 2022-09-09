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

import geom.GeomApp;
import geom.Project;
import geom.ProjectState;
import geom.ScriptManager;
import geom.ScriptWrapper;
import js.data.AbstractData;
import js.file.Files;
import js.geometry.IPoint;
import js.guiapp.*;
import js.json.JSMap;
import js.scredit.gen.ScreditConfig;
import js.scredit.oper.FileJumpOper;
import js.scredit.oper.FileStepOper;
import js.scredit.oper.FileStepUsedOper;
import js.scredit.oper.FindProblemsOper;
import js.scredit.oper.OpenNextProjectOper;
import js.scredit.oper.ProjectCloseOper;
import js.scredit.oper.ProjectOpenOper;
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
  public void processOptionalArgs() {
    if (cmdLineArgs().hasNextArg()) {
      mStartProjectFile = new File(cmdLineArgs().nextArg());
      log(DASHES, "set start project:", INDENT, mStartProjectFile, VERT_SP);
    }
  }

  @Override
  public List<Object> getOptionalArgDescriptions() {
    return arrayList("[<project directory>]");
  }

  private File mStartProjectFile = Files.DEFAULT;

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
  public void populateMenuBar(MenuBarWrapper m) {
    addProjectMenu(m);
    if (currentProject().definedAndNonEmpty()) {
      addFileMenu(m);
      addEditMenu(m);
      addViewMenu(m);
      addCategoryMenu(m);
    }
  }

  private void addProjectMenu(MenuBarWrapper m) {
    todo("Issue #1: move to geom project");
    m.addMenu("Project");
    addItem("project_open", "Open", new ProjectOpenOper());
    addItem("project_close", "Close", new ProjectCloseOper());
    m.addSubMenu(
        recentProjects().constructMenu("Open Recent", UserEventManager.sharedInstance(), new UserOperation() {
          @Override
          public void start() {
            openProject(recentProjects().getCurrentFile());
          }
        }));
    addItem("project_open_next", "Open Next", new OpenNextProjectOper());
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
  // Current project
  // ------------------------------------------------------------------

  public Project currentProject() {
    return mCurrentProject;
  }

  public void closeProject() {
    if (currentProject().isDefault())
      return;
    flushProject();
    mCurrentProject = Project.DEFAULT_INSTANCE;
    removeUIElements();
    recentProjects().setCurrentFile(null);
    scriptManager().replaceCurrentScriptWith(ScriptWrapper.DEFAULT_INSTANCE);
    discardMenuBar();
    updateTitle();
  }

  public void openProject(File file) {
    closeProject();

    Project project = new Project(file);

    // If there are recent projects, use their state as the default for this one in case it is a new project

    project.open(recentProjects().getMostRecentFile());
    mCurrentProject = project;
    recentProjects().setCurrentFile(project.directory());
    AppDefaults.sharedInstance().edit().recentProjects(recentProjects().state());
    rebuildFrameContent();
    mInfoPanel.opening(project);

    scriptManager().replaceCurrentScriptWith(currentProject().script());

    // TODO: restore panel visibilities, etc according to project
    appFrame().setBounds(projectState().appFrame());
    updateTitle();
    discardMenuBar();

    // Make sure the UI is updated to represent this project's state,
    // and to make sure the keyboard shortcuts work (something to do with focus?)
    //
    performRepaint(REPAINT_ALL);
  }

  private void openAppropriateProject() {
    AppDefaults def = AppDefaults.sharedInstance();
    recentProjects().restore(def.read().recentProjects());
    def.edit().recentProjects(recentProjects().state());

    File desiredProjFile = mStartProjectFile;
    if (Files.empty(desiredProjFile))
      desiredProjFile = recentProjects().getMostRecentFile();
    if (Files.empty(desiredProjFile))
      desiredProjFile = Files.currentDirectory();

    if (!desiredProjFile.isDirectory()) {
      pr("*** No such project directory:", desiredProjFile);
      desiredProjFile = Files.currentDirectory();
    }
    desiredProjFile = Files.absolute(desiredProjFile);
    openProject(desiredProjFile);
  }

  private void flushProject() {
    if (!currentProject().defined())
      return;
    // Store the app frame location, in case it has changed
    projectState().appFrame(appFrame().bounds());
    currentProject().flush();
  }

  public void switchToScript(int index) {
    scriptManager().flushScript();
    if (currentProject().scriptIndex() != index) {
      currentProject().setScriptIndex(index);
      scriptManager().replaceCurrentScriptWith(currentProject().script());
    }
  }

  public ProjectState.Builder projectState() {
    return currentProject().state();
  }

  public RecentFiles recentProjects() {
    if (mRecentProjects == null) {
      mRecentProjects = new RecentFiles();
      mRecentProjects.setDirectoryMode();
    }
    return mRecentProjects;
  }

  private RecentFiles mRecentProjects;
  private Project mCurrentProject = Project.DEFAULT_INSTANCE;

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
    if (0 != (repaintFlags & REPAINT_INFO))
      mInfoPanel.refresh();
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
    mInfoPanel = new InfoPanel(this);
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
    parentPanel.add(mInfoPanel, BorderLayout.SOUTH);
  }

  private void removeUIElements() {
    contentPane().removeAll();
    todo("who owns the EditorPanel?  Do we need to get rid of it here?");
    mControlPanel = null;
  }

  private InfoPanel mInfoPanel;
  private JPanel mControlPanel;

  // ------------------------------------------------------------------
  // Periodic background tasks (e.g. flushing changes to script)
  // ------------------------------------------------------------------

  @Override
  public void swingBackgroundTask() {
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
    if (!currentProject().defined())
      return "No project selected; open one from the Project menu";
    if (!currentProject().definedAndNonEmpty())
      return "This project is empty! Open another from the Project menu";
    return null;
  }

}
