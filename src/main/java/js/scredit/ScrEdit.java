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
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import js.data.AbstractData;
import js.file.Files;
import js.geometry.IPoint;
import js.graphics.ScriptElement;
import js.graphics.gen.Script;
import js.guiapp.*;
import js.json.JSMap;
import js.scredit.gen.Command;
import js.scredit.gen.ProjectState;
import js.scredit.gen.ScreditConfig;
import js.scredit.gen.ScriptEditState;
import js.scredit.oper.*;

public final class ScrEdit extends GUIApp {

  public static final boolean ISSUE_14 = false && alert("ISSUE_14 logging");
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
    ;
  }

  @Override
  public UserOperation getDefaultUserOperation() {
    return new DefaultOper().setEditor(this);
  }

  @Override
  public JSMap getKeyboardShortcutRegistry() {
    return JSMap.fromResource(this.getClass(), "key_shortcut_defaults.json");
  }

  @Override
  public void startedGUI() {
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

  public int paddingPixels() {
    return (int) (20 / zoomFactor());
  }

  /**
   * This is needed for some operations that occur outside of rendering
   * operation
   */
  public float zoomFactor() {
    return projectState().zoomFactor();
  }

  // ------------------------------------------------------------------
  // Menu bar
  // ------------------------------------------------------------------

  @Override
  public void populateMenuBar(OurMenuBar m) {
    addProjectMenu(m);
    if (currentProject().definedAndNonEmpty()) {
      addFileMenu(m);
      addEditMenu(m);
      addViewMenu(m);
      addCategoryMenu(m);
    }
  }

  private void addProjectMenu(OurMenuBar m) {
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
    if (false)
      addItem("", "Rename Scripts", new RenameScriptsOper());
    addItem("project_find_problems", "Find Problems", new FindProblemsOper());
  }

  private void addFileMenu(OurMenuBar m) {
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

  private void addEditMenu(OurMenuBar m) {
    m.addMenu("Edit", null);
    // The labels will be fetched via getLabelText(), so use placeholders ('_')
    addItem("undo", "_", new UndoOper());
    addItem("redo", "_", new RedoOper());

    m.addSeparator();

    addItem("cut", "Cut", new CutOper());
    addItem("copy", "Copy", new CopyOper());
    addItem("paste", "Paste", new PasteOper());
    m.addSeparator();
    addItem("select_none", "Select None", new SelectNoneOper());
    addItem("select_all", "Select All", new SelectAllOper());
    m.addSeparator();
    addItem("box_add", "Add Box", new RectAddOper());
    addItem("mask_add", "Add Mask", new MaskAddOper());
    addItem("pt_add", "Add Point", new PointAddOper());
    addItem("polygon_add", "Add Polygon", PolygonEditOper.buildAddOper(this));
    addItem("rotation_toggle", "Toggle Rotation", new ToggleRotationOper(this));

    {
      EditorOper oper = PolygonEditOper.buildAddCurveOper(this);
      addItem("curve_add", "Add Curve", oper);
      addItem("curve_add2", "Add Curve (2)", oper);
    }

    addItem("yolo_merge", "Yolo Merge", new NonMaxSuppressOper());
  }

  private void addViewMenu(OurMenuBar m) {
    m.addMenu("View");
    addItem("zoom_in", "Zoom In", ZoomOper.buildIn());
    addItem("zoom_out", "Zoom Out", ZoomOper.buildOut());
    addItem("zoom_reset", "Zoom Reset", ZoomOper.buildReset());
  }

  private void addCategoryMenu(OurMenuBar m) {
    m.addMenu("Category");
    for (int i = 0; i < 10; i++)
      m.addItem("category_" + i, "" + i, SetCategoryOper.buildSetCategoryOper(this, i));
  }

  @Override
  public JMenuItem addItem(String hotKeyId, String displayedName, UserOperation operation) {
    // Store reference to ScrEdit within operation, if it is an appropriate subclass
    if (operation instanceof EditorOper) {
      EditorOper ourOperation = (EditorOper) operation;
      ourOperation.setEditor(this);
    }
    return super.addItem(hotKeyId, displayedName, operation);
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
    replaceCurrentScriptWith(ScriptWrapper.DEFAULT_INSTANCE);
    discardMenuBar();
    updateTitle();
  }

  public void openProject(File file) {
    closeProject();

    Project project = new Project(file);

    // If there are recent projects, use their state as the default for this one in case it is a new project

    if (false && alert("logging for issue #12"))
      project.setVerbose();
    project.open(recentProjects().getMostRecentFile());
    mCurrentProject = project;
    recentProjects().setCurrentFile(project.directory());
    AppDefaults.sharedInstance().edit().recentProjects(recentProjects().state());
    rebuildFrameContent();
    mInfoPanel.opening(project);

    replaceCurrentScriptWith(currentProject().script());

    // TODO: restore panel visibilities, etc according to project
    appFrame().setBounds(projectState().appFrame());
    updateTitle();
    discardMenuBar();

    if (ISSUE_14)
      pr("EditorPanel requesting focus");

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

  public void flushScript() {
    mScript.flush();
  }

  private void flushProject() {
    if (!currentProject().defined())
      return;
    // Store the app frame location, in case it has changed
    projectState().appFrame(appFrame().bounds());
    currentProject().flush();
  }

  public void switchToScript(int index) {
    flushScript();
    if (currentProject().scriptIndex() != index) {
      currentProject().setScriptIndex(index);
      replaceCurrentScriptWith(currentProject().script());
    }
  }

  public ProjectState.Builder projectState() {
    return currentProject().state();
  }

  private void replaceCurrentScriptWith(ScriptWrapper newScript) {
    if (newScript == mScript)
      return;

    // Copy the clipboard from the current script, so we can copy or paste with the new script
    ScriptEditState oldState = mState;
    mScript = newScript;

    // Parse the ScriptElement objects, constructing an appropriate
    // EditorElement for each
    List<EditorElement> editorElements = arrayList();
    for (ScriptElement element : newScript.data().items()) {
      // It is possible that elements are null, if they were unable to be parsed
      if (element == null)
        continue;
      EditorElement parser = EditorElementRegistry.sharedInstance().factoryForTag(element.tag(), false);
      if (parser == null) {
        pr("*** No EditorElement parser found for tag:", quote(element.tag()));
        continue;
      }
      EditorElement elem = parser.toEditorElement(element);
      EditorElement validatedElement = elem.validate();
      if (validatedElement == null) {
        pr("*** failed to validate element:", INDENT, elem);
        continue;
      }
      editorElements.add(validatedElement);
    }

    setState(ScriptEditState.newBuilder() //
        .elements(editorElements)//
        .clipboard(oldState.clipboard())//
    );

    // Discard undo manager, since it refers to a different script
    // TODO: have some support for keeping around multiple undo managers, one for each script,
    // or some finite number of them to keep memory usage down
    mUndoManager = null;
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
  // Current script and edit state
  // ------------------------------------------------------------------

  /**
   * Get (immutable) current script state
   */
  public ScriptEditState state() {
    return mState;
  }

  public void setState(ScriptEditState state) {
    mState = state.build();
    if (mScript.defined()) {
      // We have to construct an array of ScriptElements, since we can't
      // just pass an array of EditorElements (even though each element implements ScriptElement)
      List<ScriptElement> elements = new ArrayList<>(mState.elements());
      Script.Builder b = Script.newBuilder();
      b.usage(mScript.data().usage());
      b.items(elements);
      mScript.setData(b.build());
    }
  }

  public ScriptWrapper currentScript() {
    return mScript;
  }

  private ScriptWrapper mScript = ScriptWrapper.DEFAULT_INSTANCE;
  private ScriptEditState mState = ScriptEditState.DEFAULT_INSTANCE;

  // ------------------------------------------------------------------
  // Commands
  // ------------------------------------------------------------------

  /**
   * Construct a command, with the final state initially set to the current
   * state
   */
  public Command.Builder buildCommand(String description) {
    Command.Builder b = Command.newBuilder().description(description);
    b.newState(state());
    return b;
  }

  public void perform(Command command) {
    command = command.build();
    undoManager().record(command);
    setState(command.newState());
  }

  public void perform(CommandOper oper) {
    oper.setEditor(this);
    UserEventManager.sharedInstance().perform(oper);
  }

  /**
   * Delete the last registered command from the undo list, in case the command
   * didn't end up producing anything useful, e.g. an incomplete polygon.
   */
  public void discardLastCommand() {
    undoManager().discardLastCommand();
  }

  public UndoManager undoManager() {
    if (mUndoManager == null)
      mUndoManager = new UndoManager(state());
    return mUndoManager;
  }

  private UndoManager mUndoManager;

  // ------------------------------------------------------------------
  // User interface elements within frame
  // ------------------------------------------------------------------

  public static final int REPAINT_EDITOR = (1 << 0);
  public static final int REPAINT_INFO = (1 << 1);
  public static final int REPAINT_ALL = ~0;

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
      mEditorPanel.repaint();
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

    mEditorPanel = new EditorPanel(this);
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
      subPanel.add(mEditorPanel, BorderLayout.CENTER);
      parentPanel.add(subPanel, BorderLayout.CENTER);
    }
    if (mControlPanel != null)
      parentPanel.add(mControlPanel, BorderLayout.EAST);
    parentPanel.add(mInfoPanel, BorderLayout.SOUTH);
  }

  private void removeUIElements() {
    contentPane().removeAll();
    mEditorPanel = null;
    mControlPanel = null;
  }

  private EditorPanel mEditorPanel;
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
    flushScript();
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
