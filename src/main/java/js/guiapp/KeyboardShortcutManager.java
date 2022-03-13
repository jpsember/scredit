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

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.KeyStroke;

import static js.base.Tools.*;

import js.base.BaseObject;
import js.data.DataUtil;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.RegExp;
import js.scredit.gen.HotKey;

/**
 * Manages keyboard shortcuts within a user-editable file
 * 
 * <pre>
 * 
 * NOTE!
 *
 * 1) Don't attempt to assign symbols that are accessed via shift keys.
 *    For example, '>' which is normally typed by pressing SHIFT '.' should
 *    not be assigned to a hot key by referring to its code as "greater".
 *    Instead, use the code "period" with modifier "s".
 *
 * 2) It seems that using the "s" modifier in conjunction with other modifiers
 *    is not reliable.  For example, if there are distinct operations
 *
 *        "A" : { "code" : "comma", "flags" : "m"}
 *    and
 *        "B" : { "code" : "comma", "flags" : "ms" }
 *
 *    Then pressing META+SHIFT+comma will cause both operations to be executed!
 *
 * </pre>
 */
public final class KeyboardShortcutManager extends BaseObject {

  public KeyboardShortcutManager() {
    parseRegistry();
    detectProblems();
  }

  /**
   * Determine which HotKey, if any, is assigned to an operation
   */
  public HotKey opt(String operationName) {
    return mOperationHotKeyMap.get(operationName);
  }

  public HotKey assignHotKeyToOperation(String operationName, HotKey hotKey) {
    log("assign hot key to oper:", operationName, hotKey);
    if (!mUniqueOperationNames.add(operationName))
      badState("Multiple operations exist with name:", operationName);
    return auxAssignHotKeyToOperation(operationName, hotKey, true);
  }

  /**
   * Clear the list of assigned operations. This is only used to detect if
   * multiple operations are assigned to a particular shortcut (and throw an
   * exception if so)
   */
  public void clearAssignedOperationList() {
    mUniqueOperationNames.clear();
  }

  /**
   * Compile a KeyStroke from a HotKey
   */
  public static KeyStroke compileKeystroke(HotKey hotKey) {
    int modifiers = 0;
    String s = hotKey.modifiers();
    if (!sLegalModifiers.matcher(s).matches())
      throw new IllegalArgumentException("bad modifiers: " + s);
    int[] km = modifierKeyMasks();
    if (has(s, 'c'))
      modifiers |= km[0];
    if (has(s, 'a'))
      modifiers |= km[1];
    if (has(s, 'm'))
      modifiers |= km[2];
    if (has(s, 's'))
      modifiers |= km[3];
    return KeyStroke.getKeyStroke(hotKey.code(), modifiers);
  }

  private static boolean has(String s, char c) {
    return s.indexOf(c) >= 0;
  }

  private static int[] modifierKeyMasks() {
    if (sModifierKeyMasks == null) {
      int[] m = new int[4];

      m[0] = KeyEvent.CTRL_MASK;
      m[1] = KeyEvent.ALT_MASK;
      m[2] = KeyEvent.META_MASK;
      m[3] = KeyEvent.SHIFT_DOWN_MASK;

      int j = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
      for (int k = 1; k < m.length; k++) {
        if (m[k] == j) {
          int tmp = m[0];
          m[0] = j;
          m[k] = tmp;
          break;
        }
      }
      sModifierKeyMasks = m;
    }
    return sModifierKeyMasks;
  }

  private static int[] sModifierKeyMasks;
  private static Pattern sLegalModifiers = RegExp.pattern("[acms]*");

  private static HotKey parseHotKey(JSMap m, int code) {
    HotKey.Builder b = HotKey.newBuilder();
    b.modifiers(m.opt("flags", ""));
    b.code(code);
    return b.build();
  }

  private HotKey auxAssignHotKeyToOperation(String operationName, HotKey hotKey, boolean checkForConflict) {
    checkArgument(!nullOrEmpty(operationName));
    if (verbose())
      log("add:", operationName, DataUtil.toString(hotKey));
    mOperationHotKeyMap.put(operationName, hotKey);

    if (checkForConflict) {
      List<String> operNames = mConflictMap.get(hotKey);
      if (operNames == null) {
        operNames = arrayList();
        mConflictMap.put(hotKey, operNames);
      } else {
        mConflictsFound = true;
      }
      operNames.add(operationName);
    }
    return hotKey;
  }

  private void parseRegistry() {
    JSMap json = JSMap.fromResource(this.getClass(), "key_shortcut_defaults.json");
    log("parsing hot keys from map:", INDENT, json);
    for (String key : json.keySet()) {
      JSMap m = json.getMap(key);
      mOperationNames.add(key);
      // If the map has no key assignment, don't assign one
      int code = parseCode(m);
      if (code != 0) {
        HotKey hk = parseHotKey(m, code);
        auxAssignHotKeyToOperation(key, hk, false);
      }
    }
    mRegistry = json.lock();
  }

  private void detectProblems() {
    JSMap report = map();
    {
      if (mOperationNames.size() > mRegistry.size()) {
        Set<String> missingKeys = treeSet();
        missingKeys.addAll(mOperationNames);
        missingKeys.removeAll(mRegistry.keySet());
        report.put("unregistered", JSList.withStringRepresentationsOf(missingKeys));
      }
    }

    if (mConflictsFound) {
      JSList conf = list();
      report.put("conflicts", conf);
      for (List<String> confList : mConflictMap.values()) {
        if (confList.size() > 1)
          conf.add(JSList.withStringRepresentationsOf(confList));
      }
    }

    if (report.nonEmpty()) {
      pr("*** Hot Key problems:");
      pr(report);
    }
  }

  private static int parseCode(JSMap m) {
    Object value = m.optUnsafe("code");
    if (value == null)
      value = 0;
    if (value instanceof Number)
      return ((Number) value).intValue();
    String str = (String) value;

    int result = -1;
    KeyStroke ks = KeyStroke.getKeyStroke(str.toUpperCase());
    if (ks != null)
      return ks.getKeyCode();
    if (str.length() == 1) {
      result = KeyEvent.getExtendedKeyCodeForChar(str.charAt(0));
      if (result != KeyEvent.VK_UNDEFINED)
        return result;
    }
    throw badArg("failed parsing KeyEvent code:", quote(value));
  }

  private JSMap mRegistry;
  private Set<String> mOperationNames = hashSet();
  private Map<String, HotKey> mOperationHotKeyMap = hashMap();

  // Map of HotKey to list of names of operations requesting to use that HotKey
  //
  private Map<HotKey, List<String>> mConflictMap = hashMap();
  private Set<String> mUniqueOperationNames = hashSet();
  private boolean mConflictsFound;

}
