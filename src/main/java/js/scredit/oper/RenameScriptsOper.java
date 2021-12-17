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
import java.util.List;
import java.util.Map;

import js.file.Files;
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

    mRenamedFiles = arrayList();

    // Rename all scripts and images to new names including prefix
    //
    for (int i = 0; i < proj.scriptCount(); i++) {
      ScriptWrapper w = proj.script(i);
      File sc = w.scriptFile();
      File resultFile = rename(sc);
      if (w.hasImage()) {
        rename(w.imageFile());
      }
      w.setName(resultFile);
    }

    // Remove prefixes from renamed files
    //
    for (File sourceFile : mRenamedFiles) {
      File targetFile = new File(Files.parent(sourceFile), chompPrefix(sourceFile.getName(), WORK_PREFIX));
      checkState(!targetFile.exists());
      pr("renaming:", INDENT, sourceFile, CR, targetFile);
      Files.S.moveFile(sourceFile, targetFile);
    }

    editor().openProject(proj.directory());
  }

  private static final String WORK_PREFIX = "________";

  private File rename(File sourceFile) {
    if (!sourceFile.exists())
      return sourceFile;
    String base = Files.basename(sourceFile);
    String newName = mRenameMap.get(base);
    checkState(newName != null);
    File targetFileFinal = new File(Files.parent(sourceFile),
        Files.setExtension(newName, Files.getExtension(sourceFile)));
    File targetFile = new File(Files.parent(sourceFile),
        Files.setExtension(WORK_PREFIX + newName, Files.getExtension(sourceFile)));
    checkState(!targetFile.exists());
    pr("renaming:", INDENT, sourceFile, CR, targetFile);
    Files.S.moveFile(sourceFile, targetFile);
    mRenamedFiles.add(targetFile);
    return targetFileFinal;
  }

  private List<File> mRenamedFiles;

  private void constructRenameMap() {
    Map<String, String> mp = hashMap();
    Project proj = editor().currentProject();
    mChanges = false;
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
      }
    }
    mRenameMap = mp;
  }

  private Map<String, String> mRenameMap;
  private boolean mChanges;
}
