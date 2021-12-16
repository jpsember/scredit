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

import java.io.File;
import java.util.*;

import js.base.BaseObject;
import js.data.DataUtil;
import js.file.Files;
import js.geometry.MyMath;
import js.json.*;
import js.scredit.gen.ProjectState;
import js.graphics.ImgUtil;
import js.graphics.ScriptUtil;

import static js.base.Tools.*;

import org.apache.commons.io.FileUtils;

public final class Project extends BaseObject {

  public static final Project DEFAULT_INSTANCE = new Project();

  private Project() {
    mDirectory = null;
    mProjectFile = null;
  }

  public Project(File directory) {
    mDirectory = directory;
    mProjectFile = isDefault() ? null : ScriptUtil.projectFileForProject(directory);
  }

  private ProjectState readProjectState(File projectForDefaultStateOrNull) {
    log("readProjectState, directory:", directory());
    log("projectForDefaultStateOrNull:", Files.infoMap(projectForDefaultStateOrNull));

    ProjectState projectState = null;

    {
      File stateFile = ScriptUtil.projectFileForProject(directory());
      if (stateFile.exists()) {
        log("parsing state from existing state file:", stateFile);
        try {
          JSMap m = JSMap.fromFileIfExists(stateFile);
          if (m.containsKey("version")) {
            if (m.getInt("version") != ProjectState.DEFAULT_INSTANCE.version()) {
              badArg("Project version is out of date");
            }
          }
          projectState = Files.parseAbstractDataOpt(ProjectState.DEFAULT_INSTANCE, m);
        } catch (Throwable t) {
          pr("trouble parsing", stateFile, INDENT, t);
        }
      }
    }

    if (projectState == null) {
      log("project state is null");
      if (Files.nonEmpty(projectForDefaultStateOrNull)) {
        File stateFile = ScriptUtil.projectFileForProject(projectForDefaultStateOrNull);
        if (verbose())
          log("looking for template state in:", Files.infoMap(projectForDefaultStateOrNull));
        if (stateFile.exists()) {
          projectState = Files.parseAbstractDataOpt(ProjectState.DEFAULT_INSTANCE, stateFile);
        }
      }
    }

    if (projectState == null) {
      log("project state is still null, setting to default");
      projectState = ProjectState.DEFAULT_INSTANCE;
    }
    log("returning state:", INDENT, projectState);
    return projectState;
  }

  public void open(File projectForDefaultStateOrNull) {
    log("Project.open, dir:", directory(), INDENT, "projForDefState:", projectForDefaultStateOrNull);
    mProjectState = readProjectState(projectForDefaultStateOrNull).toBuilder();
    buildScriptList();

    // Make sure script index is legal
    //
    int scriptIndex = state().currentScriptIndex();
    if (scriptCount() == 0)
      scriptIndex = 0;
    else
      scriptIndex = MyMath.clamp(scriptIndex, 0, scriptCount() - 1);
    log("correcting state script index from", state().currentScriptIndex(), "to", scriptIndex);
    state().currentScriptIndex(scriptIndex);
  }

  public boolean definedAndNonEmpty() {
    return defined() && scriptCount() != 0;
  }

  public boolean defined() {
    return !isDefault();
  }

  public boolean isDefault() {
    return mDirectory == null;
  }

  public File directory() {
    return mDirectory;
  }

  private void ensureDefined() {
    if (isDefault())
      badState("Illegal method for default project");
  }

  private static String[] sFileExtImages = { ImgUtil.JPEG_EXT, "jpeg", ImgUtil.PNG_EXT, ImgUtil.RAX_EXT };
  private static String[] sFileExtAnnotation = { Files.EXT_JSON };

  private void buildScriptList() {
    ProjectState state = state();
    Set<String> fileRootSet = hashSet();
    List<ScriptWrapper> scripts = arrayList();
    mScripts = scripts;

    for (File imageFile : FileUtils.listFiles(directory(), sFileExtImages, false)) {
      File scriptFile = ScriptUtil.scriptPathForImage(imageFile);
      ScriptWrapper script = new ScriptWrapper(scriptFile);
      scripts.add(script);
      fileRootSet.add(Files.removeExtension(imageFile.getName()));
    }

    if (!state.requireImages()) {
      File annotDir = ScriptUtil.scriptDirForProject(directory());
      if (annotDir.exists()) {
        if (verbose())
          log("looking for scripts in:", Files.infoMap(annotDir));
        int logCount = 0;
        for (File scriptFile : FileUtils.listFiles(annotDir, sFileExtAnnotation, false)) {
          String rootName = Files.basename(scriptFile);
          if (logCount++ < 10)
            log("reading script:", rootName);
          if (!fileRootSet.add(rootName))
            continue;
          scripts.add(new ScriptWrapper(scriptFile));
        }
      }
    }

    // Sort the scripts by filename 
    scripts.sort(SCRIPT_NAME_COMPARATOR);
  }

  private static final Comparator<ScriptWrapper> SCRIPT_NAME_COMPARATOR = (a, b) -> {
    String na = Files.basename(a.scriptFile());
    String nb = Files.basename(b.scriptFile());
    return na.compareTo(nb);
  };

  // ------------------------------------------------------------------
  // Project state
  // ------------------------------------------------------------------

  public ProjectState.Builder state() {
    ensureDefined();
    return mProjectState;
  }

  public void flush() {
    String newContent = DataUtil.toString(state());
    if (Files.S.writeIfChanged(mProjectFile, newContent))
      // pr("...Project changes written:", mProjectFile, INDENT, state()) //
      ;
  }

  private ProjectState.Builder mProjectState = ProjectState.newBuilder();

  // ------------------------------------------------------------------
  // Scripts
  // ------------------------------------------------------------------

  public int scriptIndex() {
    int index = state().currentScriptIndex();
    int count = scriptCount();
    if (index >= count) {
      pr("scriptIndex", index, "exceeds count", count, "!!!!");
      index = (count == 0) ? -1 : 0;
    }
    return index;
  }

  public int scriptCount() {
    ensureDefined();
    return mScripts.size();
  }

  public void setScriptIndex(int index) {
    state().currentScriptIndex(index);
  }

  /**
   * Get the current script
   */
  public ScriptWrapper script() {
    int index = state().currentScriptIndex();
    int count = scriptCount();
    if (index < 0 || index >= count)
      return ScriptWrapper.DEFAULT_INSTANCE;
    return mScripts.get(index);
  }

  public ScriptWrapper script(int scriptIndex) {
    return mScripts.get(scriptIndex);
  }

  // ------------------------------------------------------------------

  private final File mDirectory;
  private final File mProjectFile;
  private List<ScriptWrapper> mScripts;

}
