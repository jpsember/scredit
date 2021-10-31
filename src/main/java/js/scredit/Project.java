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

  public static final Project DEFAULT_INSTANCE = new Project(Files.DEFAULT);

  public Project(File directory) {
    mDirectory = directory;
    mProjectFile = isDefault() ? null : ScriptUtil.projectFileForProject(directory);
  }

  /**
   * Specify a ProjectState to use in case the project has none
   */
  public void setDefaultState(ProjectState defaultState) {
    mDefaultState = defaultState.build();
  }

  public static ProjectState readProjectState(File projectDirectoryOrNull) {
    return readProjectState(projectDirectoryOrNull, null);
  }

  private static ProjectState readProjectState(File projectDirectoryOrNull, ProjectState defaultState) {
    defaultState = nullTo(defaultState, ProjectState.DEFAULT_INSTANCE);
    ProjectState projectState = defaultState;
    if (!Files.empty(projectDirectoryOrNull)) {
      File stateFile = ScriptUtil.projectFileForProject(projectDirectoryOrNull);
      if (stateFile.exists()) {
        try {
          JSMap m = JSMap.fromFileIfExists(stateFile);
          ProjectState.Builder b = defaultState.parse(m).toBuilder();
          if (b.version() != defaultState.version()) {
            if (b.version() != 0)
              badArg("Project version is out of date");
            b.version(defaultState.version());
          }
          projectState = b.build();
        } catch (Throwable t) {
          pr("trouble parsing", stateFile, INDENT, t);
        }
      }
    }
    return projectState;
  }

  public void open() {
    mProjectState = readProjectState(directory(), mDefaultState).toBuilder();
    buildScriptList();

    // Make sure script index is legal
    //
    int scriptIndex = state().currentScriptIndex();
    if (scriptCount() == 0)
      scriptIndex = 0;
    else
      scriptIndex = MyMath.clamp(scriptIndex, 0, scriptCount());
    state().currentScriptIndex(scriptIndex);
  }

  public boolean definedAndNonEmpty() {
    return defined() && scriptCount() != 0;
  }

  public boolean defined() {
    return !isDefault();
  }

  public boolean isDefault() {
    return this == DEFAULT_INSTANCE;
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
        for (File scriptFile : FileUtils.listFiles(annotDir, sFileExtAnnotation, false)) {
          String rootName = Files.basename(scriptFile);
          log("reading script:", rootName);
          if (!fileRootSet.add(rootName))
            continue;
          scripts.add(new ScriptWrapper(scriptFile));
        }
      }
    }
  }

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
      pr("current ExtScript has index", index, "which exceeds size", count, "!!!!");
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
  private ProjectState mDefaultState;

}
