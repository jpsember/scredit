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

import java.awt.image.BufferedImage;

import com.apple.eawt.*;

import static js.base.Tools.*;

import js.file.Files;
import js.graphics.ImgUtil;

public final class MacUtils {

  /**
   * Connect 'quit program' verification code to the system-generated
   * application menu
   */
  public static void setQuitHandler(GUIApp guiApp) {
    loadTools();
    Application app = Application.getApplication();
    app.disableSuddenTermination();
    app.setQuitHandler((quitEvent, quitResponse) -> {
      if (guiApp.requestProgramExit())
        quitResponse.performQuit();
      else
        quitResponse.cancelQuit();
    });
  }

  public static void setDockIcon() {
    BufferedImage iconImage = ImgUtil.read(Files.openResource(MacUtils.class, "dock_icon.png"));
    Application.getApplication().setDockIconImage(iconImage);
  }

  public static void useScreenMenuBar() {
    System.setProperty("apple.laf.useScreenMenuBar", "true");
  }
}
