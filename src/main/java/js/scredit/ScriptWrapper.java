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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import js.base.BaseObject;
import js.data.DataUtil;
import js.file.Files;
import js.geometry.IPoint;
import js.geometry.MyMath;
import js.graphics.ImgEffects;
import js.graphics.ImgUtil;
import js.graphics.MonoImageUtil;
import js.json.JSMap;
import js.graphics.ScriptUtil;
import js.graphics.gen.ImageStats;
import js.graphics.gen.MonoImage;
import js.graphics.gen.Script;

/**
 * An enhanced wrapper for a Script object, that supports additional features
 * for ScrEdit, e.g. working with images
 */
public final class ScriptWrapper extends BaseObject {

  public static final ScriptWrapper DEFAULT_INSTANCE = new ScriptWrapper();

  private ScriptWrapper() {
    mScriptFile = Files.DEFAULT;
  }

  public ScriptWrapper(File scriptPath) {
    mScriptFile = scriptPath;
  }

  public boolean isNone() {
    return this == DEFAULT_INSTANCE;
  }

  public boolean defined() {
    return !isNone();
  }

  /**
   * Parse the Script from the file. Caches the result for subsequent calls
   */
  public Script data() {
    if (isNone())
      return Script.DEFAULT_INSTANCE;
    if (mScriptData == null)
      mScriptData = Files.parseAbstractDataOpt(Script.DEFAULT_INSTANCE, mScriptFile);
    return mScriptData;
  }

  public void setData(Script data) {
    assertNotNone();
    mScriptData = data.build();
  }

  public boolean hasImage() {
    if (isNone())
      return false;
    if (mImageFile == null) {
      List<File> imageCandidates = ScriptUtil.findImagePathsForScript(scriptFile());
      if (imageCandidates.size() > 1)
        throw badState("Multiple image candidates for script:", scriptFile(), INDENT, imageCandidates);
      mImageFile = Files.DEFAULT;
      if (!imageCandidates.isEmpty())
        mImageFile = first(imageCandidates);
    }
    return Files.nonEmpty(mImageFile);
  }

  /**
   * Get script's image; read if necessary
   */
  public BufferedImage image() {
    BufferedImage image = sImageCache.get(imageFile());
    if (image == null)
      image = sImageCache.put(imageFile(), readImage(imageFile()));
    return image;
  }

  /**
   * Read a BufferedImage, with special treatment for .rax files
   */
  private static BufferedImage readImage(File file) {
    if (!Files.getExtension(file).equals(ImgUtil.RAX_EXT))
      return ImgUtil.read(file);

    MonoImage monoImage = ImgUtil.readRax(Files.openInputStream(file));
    ImageStats s = MonoImageUtil.generateRangeStatsOnly(monoImage);

    int range = s.range();
    // Don't attempt normalization if there's a strange distribution
    //int range = max - min;
    if (range > 500) {
      todo("can we use the MonoImageUtil.normalizeImageMagick here?");
      int lowCutoffValue = s.min();
      int highCutoffValue = s.max();
      float scale = ((float) MonoImageUtil.MAX_PIXEL_VALUE) / (highCutoffValue - lowCutoffValue);
      float translate = -lowCutoffValue;
      normalizeToDepth(monoImage.pixels(), translate, scale, 15);
    }
    BufferedImage img = ImgUtil.to8BitRGBBufferedImage(monoImage.size(), monoImage.pixels());
    return ImgEffects.sharpen(img);
  }

  /**
   * Apply linear normalization to image, translating pixels then scaling, and
   * clamping to a particular depth
   */
  private static void normalizeToDepth(short[] pix, float translate, float scale, int depth) {
    short maxPixelValue = (short) ((1 << depth) - 1);
    for (int i = 0; i < pix.length; i++) {
      short inPixel = pix[i];
      int p = (int) ((inPixel + translate) * scale);
      p = MyMath.clamp(p, 0, maxPixelValue);
      pix[i] = (short) p;
    }
  }

  private static ObjectCache<File, BufferedImage> sImageCache = new ObjectCache<>(100);

  public void flush() {
    if (isNone())
      return;
    String content = DataUtil.toString(mScriptData);
    if (ScrEdit.DISABLE_FLUSH_CHANGES)
      return;
    if (Files.S.writeIfChanged(scriptFile(), content)) {
      if (verbose())
        log("flushed changes; new content:", INDENT, mScriptData);
    }
  }

  private File imageFile() {
    checkState(hasImage(), "script has no image");
    return mImageFile;
  }

  public File scriptFile() {
    assertNotNone();
    return mScriptFile;
  }

  /**
   * If script has an image, return its size; otherwise, zero
   */
  public IPoint imageSize() {
    if (!hasImage())
      return IPoint.ZERO;
    return ImgUtil.size(image());
  }

  private void assertNotNone() {
    if (isNone())
      throw notSupported("not supported for default instance");
  }

  // ------------------------------------------------------------------
  // BaseObject interface
  // ------------------------------------------------------------------

  @Override
  protected String supplyName() {
    if (isNone())
      return "_NONE_";
    return Files.basename(scriptFile());
  }

  @Override
  public JSMap toJson() {
    if (isNone())
      return JSMap.DEFAULT_INSTANCE;
    return data().toJson();
  }

  // ------------------------------------------------------------------

  private final File mScriptFile;
  // File containing script's image, or Files.DEFAULT if there is no image
  private File mImageFile;
  private Script mScriptData;
}
