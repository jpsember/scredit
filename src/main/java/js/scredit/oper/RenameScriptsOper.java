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
    if (mRenameMap == null)
      constructRenameMap();
    return mChanges;
  }

  @Override
  public void start() {
    if (!mChanges)
      return;

    pr("before rename, current script:", editor().currentScript().name(), editor().currentScript());

    Project proj = editor().currentProject();

    editor().closeProject();

    File tempProj = renameBase(proj.directory(), "___temp___");
    File tempScript;
    checkState(!tempProj.exists(), "temp directory already exists:", tempProj);
    tempScript = ScriptUtil.scriptDirForProject(tempProj);
    Files.S.mkdirs(tempScript);

    // Copy and rename scripts from project directory to temporary new directory
    //
    for (int i = 0; i < proj.scriptCount(); i++) {
      ScriptWrapper w = proj.script(i);
      rename(w.scriptFile(), tempScript);
    }

    // Copy and rename images
    //
    {
      DirWalk w = new DirWalk(proj.directory()).withExtensions(ImgUtil.IMAGE_EXTENSIONS).withRecurse(false);
      for (File f : w.files()) {
        rename(f, tempProj);
      }
    }

    // Delete old project directory, and rename temp to it
    //
    todo("Not replacing project directory with temp");

    editor().openProject(proj.directory());
  }

  private void rename(File sourceFile, File targetDirectory) {
    if (!sourceFile.exists())
      return;
    String base = Files.basename(sourceFile);
    String newName = mRenameMap.get(base);
    checkState(newName != null);
    File targetFile = new File(targetDirectory, Files.setExtension(newName, Files.getExtension(sourceFile)));
    checkState(!targetFile.exists());
    pr("renaming:", INDENT, sourceFile, CR, targetFile);
    Files.S.copyFile(sourceFile, targetFile);
  }

  private static File renameBase(File file, String newBaseName) {
    File parent = Files.parent(file);
    String ext = Files.getExtension(file);
    File newName = new File(parent, Files.setExtension(newBaseName, ext));
    return newName;
  }

  private void constructRenameMap() {
    Map<String, String> mp = hashMap();
    Project proj = editor().currentProject();
    mChanges = false;
    boolean conflicts = false;
    if (proj.definedAndNonEmpty()) {
      int nDigits = (int) Math.ceil(Math.log10(proj.scriptCount()));
      String fmt = "%0" + nDigits + "d";
      for (int i = 0; i < proj.scriptCount(); i++) {
        ScriptWrapper w = proj.script(i);
        String base = Files.basename(w.scriptFile());
        String newName = String.format(fmt, i);
        String prevMapping = mp.put(base, newName);
        if (!newName.equals(base))
          mChanges = true;
        if (prevMapping != null)
          throw badArg("duplicate key:", base, w.scriptFile());
        pr("storing rename mapping:", INDENT, base, "->", newName);
        if (!conflicts) {
          File renamedFile = renameBase(w.scriptFile(), newName);
          if (renamedFile.exists())
            conflicts = true;
        }
      }
    }
    mRenameMap = mp;
  }

  private Map<String, String> mRenameMap;
  private boolean mChanges;
}
