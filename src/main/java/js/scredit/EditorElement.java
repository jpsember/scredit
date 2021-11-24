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

import js.graphics.ScriptElement;
import js.graphics.gen.ElementProperties;
import js.guiapp.UserEvent;
import js.guiapp.UserOperation;
import js.geometry.IPoint;
import js.geometry.Matrix;

/**
 * A type of ScriptElement with special capabilities supporting editing
 */
public interface EditorElement extends ScriptElement {

  // ------------------------------------------------------------------
  // AbstractData implementation
  // ------------------------------------------------------------------

  // The abstract data classes will attempt to use a class variable to parse 
  // such items, so supply one that can act as a parser
  //
  public static final EditorElement DEFAULT_INSTANCE = EditorElementRegistry.PARSER;

  /**
   * Construct an EditorElement from a ScriptElement
   */
  default EditorElement toEditorElement(ScriptElement obj) {
    throw notSupported();
  }

  /**
   * Determine if boundary of element contains point
   */
  default boolean contains(int paddingPixels, IPoint pt) {
    throw notSupported();
  }

  /**
   * Determine if object can be grabbed at a particular point. Default
   * implementation returns true the object contains() the point.
   */
  default boolean isGrabPoint(int paddingPixels, IPoint pt) {
    return contains(paddingPixels, pt);
  }

  /**
   * Determine if an event is occurring at a part of an object that should
   * trigger an operation. If so, return the operation; else, null
   */
  default UserOperation isEditingSelectedObject(ScrEdit editor, int slot, UserEvent event) {
    return null;
  }

  default EditorElement applyTransform(Matrix m) {
    throw notSupported();
  }

  public enum Render {
    NOMINAL, SELECTED, DISABLED,
  }

  default void render(EditorPanel editorPanel, Render appearance) {
    throw notSupported("render unsupported for element:", getClass().getSimpleName());
  }

  /**
   * Construct a duplicate of this element, but with new properties
   */
  EditorElement withProperties(ElementProperties properties);

}
