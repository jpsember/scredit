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

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import js.scredit.gen.HotKey;

import static js.base.Tools.*;

public final class OurMenuBar {

  public OurMenuBar(UserEventManager eventManager, KeyboardShortcutManager hotKeyManager) {
    mMenuBar = new JMenuBar();
    mEventManager = eventManager;
    mHotKeyManager = hotKeyManager;
  }

  public JMenuBar jmenuBar() {
    return mMenuBar;
  }

  /**
   * Add a new menu, and make it the active one (for subsequent editing)
   */
  public Menu addMenu(String title, Enableable handlerOrNull) {
    mSeparatorPending = false;
    mActiveMenu = new Menu(title);
    mActiveMenu.mEnableableDelegate = handlerOrNull;
    mMenuBar.add(mActiveMenu);

    mActiveMenu.addMenuListener(new MenuListener() {
      @Override
      public void menuCanceled(MenuEvent ev) {
        JMenu m = (JMenu) ev.getSource();
        enableItems(m, false);
      }

      @Override
      public void menuDeselected(MenuEvent ev) {
        JMenu m = (JMenu) ev.getSource();
        enableItems(m, false);
      }

      @Override
      public void menuSelected(MenuEvent ev) {
        JMenu m = (JMenu) ev.getSource();
        enableItems(m, true);
      }
    });
    return mActiveMenu;
  }

  public void addMenu(String name) {
    addMenu(name, null);
  }

  /**
   * Add a menu item to the active menu
   */
  public JMenuItem addItem(String hotKeyId, String displayedName, UserOperation operation) {
    MenuItem menuItem = new MenuItem(displayedName, operation, mActiveMenu, mEventManager);

    KeyboardShortcutManager mgr = mHotKeyManager;
    HotKey hotKey = mgr.opt(hotKeyId);
    if (hotKey != null) {
      mgr.assignHotKeyToOperation(hotKeyId, hotKey);
      menuItem.setAccelerator(KeyboardShortcutManager.compileKeystroke(hotKey));
    } else {
      alert("no hot key found with id:", hotKeyId);
    }

    if (mSeparatorPending) {
      mSeparatorPending = false;
      mActiveMenu.addSeparator();
    }
    mActiveMenu.add(menuItem);

    return menuItem;
  }

  public void addSubMenu(JMenu menu) {
    mActiveMenu.add(menu);
  }

  /**
   * Insert a separator line before adding next item to the current menu
   */
  public void addSeparator() {
    if (mActiveMenu.getItemCount() > 0)
      mSeparatorPending = true;
  }

  private static void enableItems(JMenu m, boolean showingMenu) {
    for (int i = 0; i < m.getItemCount(); i++) {
      JMenuItem item = m.getItem(i);

      // separators are null
      if (item == null)
        continue;

      // We added our own subclass of JMenu, which implements Enableable
      Enableable enableable = (Enableable) item;
      String label = enableable.getLabelText();
      if (!nullOrEmpty(label))
        item.setText(label);

      // If the menu isn't showing, ALWAYS enable the items.
      // If user selects them via shortcut key, we'll perform an additional
      // call to shouldBeEnabled() before acting on them.
      item.setEnabled(!showingMenu || enableable.shouldBeEnabled());
    }
  }

  private final UserEventManager mEventManager;
  private final KeyboardShortcutManager mHotKeyManager;
  private final JMenuBar mMenuBar;

  private Menu mActiveMenu;
  private boolean mSeparatorPending;

  // ------------------------------------------------------------------
  // Custom swing components for use by OurMenuBar
  // ------------------------------------------------------------------

  public static class Menu extends JMenu {

    public Menu(String name) {
      super(name);
    }

    public void setEnabelableDelegate(Enableable delegate) {
      checkState(mEnableableDelegate == null, "already set");
      mEnableableDelegate = delegate;
    }

    public Enableable mEnableableDelegate;
  }

  private static class MenuItem extends JMenuItem implements Enableable {

    public MenuItem(String name, UserOperation operation, Menu parentMenu, UserEventManager eventManager) {
      super(name);
      mEventManager = eventManager;
      mParentMenu = parentMenu;
      mOperation = operation;

      addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
          if (shouldBeEnabled()) {
            mEventManager.perform(mOperation);
          }
        }
      });
    }

    @Override
    public String getLabelText() {
      return mOperation.getLabelText();
    }

    @Override
    public boolean shouldBeEnabled() {
      Enableable enableable = nullTo(mParentMenu.mEnableableDelegate, Enableable.DEFAULT_INSTANCE);
      return enableable.shouldBeEnabled() && mOperation.shouldBeEnabled();
    }

    private final UserOperation mOperation;
    private final Menu mParentMenu;
    private final UserEventManager mEventManager;
  }

}
