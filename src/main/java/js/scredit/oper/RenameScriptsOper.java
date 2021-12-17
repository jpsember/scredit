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
package js.scredit.oper;

import static js.base.Tools.*;

import java.io.File;
import java.util.Map;

import js.file.DirWalk;
import js.file.Files;
import js.graphics.ImgUtil;
import js.graphics.ScriptUtil;
import js.scredit.Project;
import js.scredit.ScriptWrapper;

/**
 * This affects the current project's settings, not individual scripts; so it's
 * not a command operation
 */
public final class RenameScriptsOper extends EditorOper {

  public RenameScriptsOper() {
  }

  @Override
  public boolean shouldBeEnabled() {
    constructRenameMap();
    return mChanges;
  }

  @Override
  public void start() {
    if (!shouldBeEnabled())
      return;

    Project proj = editor().currentProject();
    File projDirectory = proj.directory();

    editor().closeProject();

    File temporaryProjectDir;
    for (int attempt = 0;; attempt++) {
      temporaryProjectDir = renameBase(projDirectory, "_SKIP_rename_scripts_dir_" + attempt);
      if (!temporaryProjectDir.exists())
        break;
      alert("temporary directory already exists:", temporaryProjectDir);
    }
    checkState(!temporaryProjectDir.exists(), "temp directory already exists:", temporaryProjectDir);
    File temporaryScriptDir = ScriptUtil.scriptDirForProject(temporaryProjectDir);
    Files.S.mkdirs(temporaryScriptDir);

    // Copy and rename scripts from project directory to temporary new directory
    //
    for (int i = 0; i < proj.scriptCount(); i++) {
      ScriptWrapper w = proj.script(i);
      if (w.scriptFile().exists())
        copyAndRename(w.scriptFile(), temporaryScriptDir);
    }

    // Copy and rename images
    //
    {
      DirWalk w = new DirWalk(projDirectory).withExtensions(ImgUtil.IMAGE_EXTENSIONS).withRecurse(false);
      for (File f : w.files())
        copyAndRename(f, temporaryProjectDir);
    }

    // Delete old project directory, and rename temp to it
    //
    Files.S.deleteDirectory(projDirectory);
    Files.S.moveDirectory(temporaryProjectDir, projDirectory);

    editor().openProject(proj.directory());
  }

  private void copyAndRename(File sourceFile, File targetDirectory) {
    String base = Files.basename(sourceFile);
    String newName = mRenameMap.get(base);
    checkState(newName != null);
    File targetFile = new File(targetDirectory, Files.setExtension(newName, Files.getExtension(sourceFile)));
    checkState(!targetFile.exists());
    Files.S.copyFile(sourceFile, targetFile);
  }

  private static File renameBase(File file, String newBaseName) {
    File parent = Files.parent(file);
    String ext = Files.getExtension(file);
    File newName = new File(parent, Files.setExtension(newBaseName, ext));
    return newName;
  }

  private Map<String, String> constructRenameMap() {
    if (mRenameMap != null)
      return mRenameMap;
    Map<String, String> map = hashMap();
    Project proj = editor().currentProject();
    mChanges = false;
    if (proj.definedAndNonEmpty()) {
      int nDigits = (int) Math.ceil(Math.log10(proj.scriptCount()));
      String fmt = "%0" + nDigits + "d";
      for (int i = 0; i < proj.scriptCount(); i++) {
        ScriptWrapper w = proj.script(i);
        String base = Files.basename(w.scriptFile());
        String newName = String.format(fmt, i);
        String prevMapping = map.put(base, newName);
        if (!newName.equals(base))
          mChanges = true;
        if (prevMapping != null)
          throw badArg("duplicate key:", base, w.scriptFile());
      }
    }
    mRenameMap = map;
    return mRenameMap;
  }

  private Map<String, String> mRenameMap;
  private boolean mChanges;
}
