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

import java.util.List;

import js.base.BaseObject;
import js.data.IntArray;
import js.json.JSList;
import js.json.JSMap;
import js.scredit.gen.Command;
import js.scredit.gen.ScriptEditState;

/**
 * Class to encapsulate the state and behaviour of the undo/redo functionality.
 * Conceptually, maintains two stacks of commands: an undo stack, and a redo
 * stack.
 *
 * (The stacks are actually implemented internally using a single array.)
 * 
 * It also handles merging certain operations into a single 'undoable' unit,
 * e.g., a sequence of move object operations that are produced as a sequence of
 * many very small displacements
 */
public class UndoManager extends BaseObject {

  public UndoManager(ScriptEditState initialState) {
    mCommandHistory.add(Command.newBuilder().newState(initialState).build());
    mCommandHistoryCursor = 1;
  }

  private static final int MAX_COMMAND_HISTORY_SIZE = 50;

  /**
   * Register a command being performed
   *
   * Throws out any existing redo commands, since their validity depended upon
   * the preceding commands, which are now changing.
   *
   * Merges the command, if possible, with the preceding command so that undo
   * 'makes sense'; e.g., combine multiple little 'move' adjustments into a
   * single move command.
   *
   * Trim the undo stack to a reasonable size.
   *
   * Doesn't actually perform the command
   */
  public void record(Command command) {
    if (command.skipUndo())
      return;
    command = command.build();

    // Throw out any older 'redoable' commands that will now be stale
    {
      boolean popped = false;
      while (mCommandHistory.size() > mCommandHistoryCursor) {
        pop(mCommandHistory);
        popped = true;
      }
      if (popped)
        log("after popping older 'redoables':", INDENT, this);
    }

    if (!mergeWithPreviousCommand(command)) {
      mCommandHistory.add(command);
      mCommandHistoryCursor++;
    }

    if (mCommandHistoryCursor > MAX_COMMAND_HISTORY_SIZE) {
      int del = mCommandHistoryCursor - MAX_COMMAND_HISTORY_SIZE;
      mCommandHistoryCursor -= del;
      mCommandHistory.subList(0, del).clear();
    }
    log("record:", INDENT, command, CR, this);
  }

  /**
   * Clear the command history; essentially place object into just-constructed
   * state
   */
  public void reset() {
    mCommandHistoryCursor = 0;
    mCommandHistory.clear();
  }

  /**
   * Get the command to be undone by 'undo', or null if there is none.
   * 
   * To perform an undo, we need to have TWO commands on the stack: the first
   * containing the state to restore, and the second containing a description of
   * the command that is being undone to get to that state
   */
  public Command getUndo() {
    if (mCommandHistoryCursor < 2)
      return null;
    return mCommandHistory.get(mCommandHistoryCursor - 1);
  }

  public Command getUndoPredecessor() {
    checkState(getUndo() != null);
    return mCommandHistory.get(mCommandHistoryCursor - 2);
  }

  /**
   * Get the command to be reddone by 'redo', or null if there is none
   */
  public Command getRedo() {
    if (mCommandHistoryCursor >= mCommandHistory.size())
      return null;
    return mCommandHistory.get(mCommandHistoryCursor);
  }

  /**
   * Perform an undo, and return the restored script state
   */
  public ScriptEditState performUndo() {
    log("performUndo", INDENT, this);
    Command command = getUndoPredecessor();
    mCommandHistoryCursor--;
    return command.newState();
  }

  /**
   * Perform a redo, and return the restored script state
   */
  public ScriptEditState performRedo() {
    log("performUndo", INDENT, this);
    Command command = getRedo();
    mCommandHistoryCursor++;
    return command.newState();
  }

  /**
   * If previous command is very similar, merge new command into it
   */
  private boolean mergeWithPreviousCommand(Command command) {
    if (mCommandHistoryCursor == 0)
      return false;

    if (command.mergeDisabled())
      return false;

    Command prev = mCommandHistory.get(mCommandHistoryCursor - 1);

    // Descriptions must match
    if (!prev.description().equals(command.description()))
      return false;

    IntArray prevSelected = IntArray.with(prev.newState().selectedElements());
    IntArray newSelected = IntArray.with(command.newState().selectedElements());

    if (!prevSelected.equals(newSelected))
      return false;

    // Replace previous command's final state with this one's

    Command modPrev = prev.toBuilder().newState(command.newState()).build();
    mCommandHistory.set(mCommandHistoryCursor - 1, modPrev);
    return true;
  }

  @Override
  public JSMap toJson() {
    JSMap m = super.toJson();
    m.put("cursor", mCommandHistoryCursor);
    JSList list = list();
    m.put("history", list);
    for (int i = 0; i < mCommandHistory.size(); i++) {
      Command c = mCommandHistory.get(i);
      String desc = c.description();
      if (i == mCommandHistoryCursor)
        desc = "*" + desc;
      list.add(desc);
    }
    return m;
  }

  private List<Command> mCommandHistory = arrayList();
  private int mCommandHistoryCursor;

}
