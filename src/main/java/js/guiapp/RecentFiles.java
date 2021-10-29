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
package js.guiapp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import js.base.BaseObject;
import js.file.Files;
import js.json.*;
import js.scredit.gen.RecentFilesList;
import js.guiapp.Enableable;
import js.guiapp.UserEventManager;
import js.guiapp.UserOperation;

import static js.base.Tools.*;

/**
 * A queue of recently-used files
 */
public class RecentFiles extends BaseObject {

  public void setDirectoryMode() {
    checkState(mDirMode == null);
    mDirMode = true;
  }

  /**
   * Get current file, if one exists
   */
  public File getCurrentFile() {
    if (!mState.active())
      return null;
    return mState.files().get(0);
  }

  /**
   * Specify current file; if null or empty, moves it to the front of the list
   * (and bumps one if the list is too full)
   */
  public void setCurrentFile(File file) {
    mState.active(false);
    if (Files.nonEmpty(file)) {
      Files.assertAbsolute(file);
      mState.active(true);
      List<File> fileList = getFileList();
      int j = fileList.indexOf(file);
      if (j >= 0)
        fileList.remove(j);
      fileList.add(0, file);
      while (fileList.size() > MAXIMUM_RECENT_FILES)
        fileList.remove(fileList.size() - 1);
    }
    log("set current file", INDENT, this);
  }

  private List<File> getFileList() {
    return mState.files();
  }

  private int firstInactiveIndex() {
    return mState.active() ? 1 : 0;
  }

  /**
   * Get the most recently used file, or null if the list is empty
   */
  public File getMostRecentFile() {
    File file = null;
    if (!getFileList().isEmpty())
      file = getFileList().get(0);
    return file;
  }

  @Override
  public JSMap toJson() {
    return mState.toJson();
  }

  public void restore(RecentFilesList recentFilesList) {
    mState = recentFilesList.toBuilder();

    RecentFilesList.Builder validate = RecentFilesList.newBuilder();

    for (File f : mState.files()) {
      boolean add = (directoryMode() ? f.isDirectory() : f.isFile());
      if (!add) {
        log("*** Recent project file doesn't exist or is incorrect type; removing from list:", INDENT, f);
        continue;
      }
      validate.files().add(f);
    }

    validate.active(mState.active() && !validate.files().isEmpty());
    mState = validate;

    log("restored", INDENT, mState);
  }

  public RecentFilesList state() {
    return mState;
  }

  public OurMenuBar.Menu constructMenu(String title, UserEventManager eventManager,
      UserOperation userOperation) {
    return new OurMenu(title, eventManager, userOperation);
  }

  /**
   * JMenu subclass for displaying RecentFiles sets
   */
  private class OurMenu extends OurMenuBar.Menu implements MenuListener, ActionListener, Enableable {

    public OurMenu(String title, UserEventManager eventManager, UserOperation userOperation) {
      super(title);
      setEnabelableDelegate(this);
      mEventManager = eventManager;
      mUserOperation = userOperation;
      addMenuListener(this);
      rebuild();
    }

    @Override
    public boolean shouldBeEnabled() {
      return getFileList().size() > firstInactiveIndex();
    }

    @Override
    public void actionPerformed(ActionEvent arg) {
      OurMenuItem item = (OurMenuItem) arg.getSource();
      if (!item.file().exists()) {
        pr("*** file doesn't exist:", item.file(), INDENT, ST);
        return;
      }
      setCurrentFile(item.file());
      mEventManager.perform(mUserOperation);
    }

    @Override
    public void menuCanceled(MenuEvent arg0) {
    }

    @Override
    public void menuDeselected(MenuEvent arg0) {
    }

    @Override
    public void menuSelected(MenuEvent arg0) {
      // For recent projects, this call is unnecessary, as the entire menu bar is rebuilt.
      // But we may want a 'recent scripts' menu, where it will be useful
      rebuild();
    }

    private void rebuild() {
      removeAll();
      List<File> fileList = getFileList();
      for (int index = firstInactiveIndex(); index < fileList.size(); index++) {
        File recentFile = fileList.get(index);
        JMenuItem item = new OurMenuItem(recentFile, recentFile.getName());
        add(item);
        item.addActionListener(this);
      }
    }

    private final UserOperation mUserOperation;
    private final UserEventManager mEventManager;

  }

  /**
   * JMenuItem subclass representing RecentFiles items
   */
  private static class OurMenuItem extends JMenuItem {
    public OurMenuItem(File file, String label) {
      super(label);
      mFile = file;
    }

    public File file() {
      return mFile;
    }

    private File mFile;
  }

  private boolean directoryMode() {
    if (mDirMode == null)
      mDirMode = false;
    return mDirMode;
  }

  private static final int MAXIMUM_RECENT_FILES = 8;

  private Boolean mDirMode;
  private RecentFilesList.Builder mState;

}
