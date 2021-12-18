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

import java.awt.FileDialog;
import java.io.File;

import javax.swing.JFrame;

import js.file.Files;

import static js.base.Tools.*;

public final class SwingUtils {

  public static File displayOpenDirectoryFileRequester(File startDirOrNull, String prompt) {
    FileDialog fileChooser = new FileDialog((JFrame) null, prompt, FileDialog.LOAD);
    // See: https://coderanch.com/t/645553/java/JFileChooser-Mac-OSX
    System.setProperty("apple.awt.fileDialogForDirectories", "true");
    if (Files.nonEmpty(startDirOrNull))
      fileChooser.setDirectory(startDirOrNull.getPath());
    fileChooser.setVisible(true);
    String filename = fileChooser.getFile();
    String directory = fileChooser.getDirectory();
    fileChooser.setVisible(false);
    if (filename == null)
      return null;
    return new File(directory, filename);
  }

  public static void setEventDispatchThread() {
    checkState(sSwingThread == null, "already set");
    sSwingThread = Thread.currentThread();
  }

  public static Thread getEventDispatchThread() {
    if (sSwingThread == null)
      badState("swing event dispatch thread not defined");
    return sSwingThread;
  }

  @Deprecated // This seems to not work reliably
  public static void assertSwingThread() {
    if (Thread.currentThread() != getEventDispatchThread()) {
      // Issue #56: display a full(er) stack trace to try to track down the cause
      IllegalStateException t = new IllegalStateException("Current thread is not the Swing event thread");
      t.printStackTrace();
      throw t;
    }
  }

  private static Thread sSwingThread;

}