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

import java.util.Map;

import js.graphics.ScriptUtil;
import js.graphics.gen.ElementProperties;
import js.json.JSMap;
import js.scredit.elem.EditableMaskElement;
import js.scredit.elem.EditablePointElement;
import js.scredit.elem.EditableRectElement;

/**
 * A mapping of EditableElement tags to their default instances
 * 
 * Analogous to ScriptElementRegistry, but for the scredit versions of these
 * objects
 */
final class EditorElementRegistry {

  public static EditorElementRegistry sharedInstance() {
    if (sSharedInstance == null)
      sSharedInstance = new EditorElementRegistry();
    return sSharedInstance;
  }

  public EditorElement factoryForTag(String tag, boolean mustExist) {
    EditorElement factory = mElementFactoryMap.get(tag);
    if (factory == null && mustExist)
      throw notSupported("No factory found for tag:", tag);
    return factory;
  }

  private EditorElementRegistry() {
    registerObjectTypes();
  }

  private void registerObjectTypes() {
    registerFactory(EditablePointElement.DEFAULT_INSTANCE);
    registerFactory(EditableRectElement.DEFAULT_INSTANCE);
    registerFactory(EditableMaskElement.DEFAULT_INSTANCE);
  }

  private void registerFactory(EditorElement factory) {
    String name = factory.tag();
    mElementFactoryMap.put(name, factory);
  }

  private final Map<String, EditorElement> mElementFactoryMap = concurrentHashMap();
  private static EditorElementRegistry sSharedInstance;

  /**
   * An object that implements EditorElement to serve as a parser that produces
   * concrete instances (analogous to the ScriptElementRegistry's version)
   */
  public static final EditorElement PARSER = new EditorElement() {

    @Override
    public EditorElement parse(Object object) {
      todo("we can use default methods that throw notSupported()");
      JSMap m = (JSMap) object;
      String tag = m.get(ScriptUtil.TAG_KEY);
      EditorElement parser = sharedInstance().factoryForTag(tag, true);
      if (parser == null)
        return null;
      return (EditorElement) parser.parse(m);
    }

    @Override
    public EditorElement withProperties(ElementProperties properties) {
      throw notSupported();
    }

    public ElementProperties properties() {
      throw notSupported();
    }

  };

}
