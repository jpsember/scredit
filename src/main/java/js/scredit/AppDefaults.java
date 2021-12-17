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

import js.scredit.gen.ScreditDefaults;
import static js.base.Tools.*;

import js.base.BaseObject;
import js.file.Files;
import js.graphics.ScriptUtil;

public final class AppDefaults extends BaseObject {

  // ------------------------------------------------------------------
  // Shared instance
  // ------------------------------------------------------------------

  public static AppDefaults sharedInstance() {
    return sSharedInstance;
  }

  private static final AppDefaults sSharedInstance;

  static {
    sSharedInstance = new AppDefaults();
  }

  // ------------------------------------------------------------------

  private AppDefaults() {
    loadTools();
    alertVerbose();
  }

  public ScreditDefaults read() {
    // We may be reading this before we've started up the app, i.e. outside of the Swing thread
    if (mDefaults == null) {
      mDefaults = Files.parseAbstractDataOpt(ScreditDefaults.DEFAULT_INSTANCE, file()).toBuilder();
      if (verbose())
        log("Read ScreditDefaults from:", file(), INDENT, mDefaults);
    }
    return mDefaults;
  }

  public ScreditDefaults.Builder edit() {
    read();
    return mDefaults;
  }

  public void flush() {
    log("flushDefaults");
    boolean written = Files.S.writeIfChanged(file(), mDefaults.build().toString());
    if (written)
      log("...changes saved to", file(), INDENT, mDefaults);
  }

  private File file() {
    return new File(ScriptUtil.scriptProjectsDirectory(), "scredit_defaults.json");
  }

  private ScreditDefaults.Builder mDefaults;
}
