package js.scredit.oper;

import static js.base.Tools.*;

import js.geometry.IPoint;
import js.geometry.MyMath;
import js.geometry.Polygon;
import js.guiapp.UserEvent;
import js.json.JSMap;
import js.scredit.EditorElement;
import js.scredit.MergePoly;
import js.scredit.ScrEdit;
import js.scredit.StateTools;
import js.scredit.gen.Command;

import js.scredit.elem.EditablePolygonElement;

import java.awt.Cursor;
import java.util.List;

public class PolygonEditOper extends EditorOper implements UserEvent.Listener {

  public static PolygonEditOper buildAddOper(ScrEdit editor) {
    return new PolygonEditOper(editor, false);
  }

  public static PolygonEditOper buildAddCurveOper(ScrEdit editor) {
    return new PolygonEditOper(editor, true);
  }

  public static PolygonEditOper buildEditExistingOper(ScrEdit editor, UserEvent event, int slot,
      int vertexIndex) {
    return new PolygonEditOper(editor, event.getWorldLocation(), slot, vertexIndex);
  }

  /**
   * Constructor for adding a new polygon (or curve)
   */
  private PolygonEditOper(ScrEdit editor, boolean curveMode) {
    setEditor(editor);
    mAddMode = true;
    mCurveMode = curveMode;
    //warnVerbose();
  }

  /**
   * Constructor for editing existing polygon, starting at a vertex
   */
  private PolygonEditOper(ScrEdit editor, IPoint mouseDownLoc, int slot, int vertexIndex) {
    setEditor(editor);
    setState(STATE_ADJUST);
    mVertexIndex = vertexIndex;
    mSlot = slot;
    mMouseDownLoc = mouseDownLoc;
  }

  @Override
  public void start() {
    if (mAddMode) {
      mCommand = editor().buildCommand("Add Polygon");
      mVertexIndex = 0;
      setState(mCurveMode ? STATE_STARTING_CURVE : STATE_UP);
      //  mCommand.finalState().clearSelection();
      //  mSlot = mCommand.initialState().objects().size();

      //int flavor = 0; // TODO: support different flavors
      boolean open = mCurveMode;
      if (false && alert("always open; need to debug editing open non-curve poly"))
        open = true;

      Polygon template = Polygon.DEFAULT_INSTANCE.withOpen(open);

      EditablePolygonElement polygon = new EditablePolygonElement(null, template, mCurveMode);

      mSlot = StateTools.addNewElement(mCommand, polygon);
      editor().setMouseCursor(Cursor.HAND_CURSOR);

      //  StateTools.addNewElement(mCommand,  polygon.withProperties(polygon.properties().
      //  mCommand.newState().elements().addSelected(polygon);
      mMouseOffset = IPoint.ZERO;
    } else {
      mCommand = editor().buildCommand("Adjust Polygon");
      EditorElement elem = mCommand.newState().elements().get(mSlot);
      mOriginalElem = (EditablePolygonElement) elem;
      IPoint vertexLoc = mOriginalElem.polygon().vertex(mVertexIndex);
      mMouseOffset = IPoint.difference(vertexLoc, mMouseDownLoc);
    }
  }

  @Override
  public void processUserEvent(UserEvent event) {
    if (!mCurveMode)
      processPolygonUserEvent(event);
    else
      processCurveUserEvent(event);
  }

  private int setState(int newState) {
    if (mState != newState) {
      log("state changing from", stateName(mState), "to", stateName(newState));
      mState = newState;
    }
    return mState;
  }

  private void processPolygonUserEvent(UserEvent event) {
    if (event.getCode() != UserEvent.CODE_MOVE)
      log("processPolyUserEvent", INDENT, this, CR, event);
    switch (event.getCode()) {

    case UserEvent.CODE_DOWN: {
      if (event.isRight()) {
        editor().perform(mCommand);
        event.clearOperation();
        break;
      }

      EditablePolygonElement p = activePolygon();
      IPoint pos = applyMouseOffset(event.getWorldLocation());
      log("adding point at index", mVertexIndex, pos);
      p = p.withAddPoint(mVertexIndex, pos);
      writeActivePolygon(p);
      setState(STATE_ADJUST);
    }
      break;

    case UserEvent.CODE_DRAG: {
      if (mState != STATE_ADJUST)
        break;

      EditablePolygonElement p = activePolygon();

      IPoint pt = applyMouseOffset(event.getWorldLocation());
      p = p.withSetPoint(mVertexIndex, pt);
      writeActivePolygon(p);
    }
      break;

    case UserEvent.CODE_UP: {
      if (mState != STATE_ADJUST)
        break;
      checkState(activePolygon().polygon().numVertices() > 0, "edit object has no vertices");

      if (event.isRight()) {
        // Delete the vertex 
        EditablePolygonElement p = activePolygon().withDeletedPoint(mVertexIndex);
        writeActivePolygon(p);
        event.clearOperation();
        break;
      } else {
        mVertexIndex = (mVertexIndex + 1);
        if (activePolygon().polygon().isClosed())
          mVertexIndex = mVertexIndex % activePolygon().polygon().numVertices();
        log("vertex index incr'd to", mVertexIndex);
      }
      setState(STATE_UP);
    }
      break;

    case UserEvent.CODE_MOVE: {
      EditablePolygonElement p = activePolygon();
      p = p.withInsertVertex(mVertexIndex, applyMouseOffset(event.getWorldLocation()));
      writeActivePolygon(p);
      editor().perform(mCommand);
    }
      break;
    }
  }

  private void processCurveUserEvent(UserEvent event) {
    log("processCurveUserEvent", INDENT, this, CR, event);

    switch (event.getCode()) {

    case UserEvent.CODE_DOWN: {
      if (mState == STATE_STARTING_CURVE && !event.isRight()) {
        setState(STATE_ADJUST);
      } else {
        editor().perform(mCommand);
        event.clearOperation();
      }
    }
      break;

    case UserEvent.CODE_MOVE: {
      if (mState != STATE_ADJUST)
        break;

      EditablePolygonElement p = activePolygon();

      IPoint pt = applyMouseOffset(event.getWorldLocation());

      if (p.polygon().numVertices() == 0) {
        p = p.withAddPoint(0, pt);
      } else {
        IPoint prevPt = p.polygon().lastVertex();
        if (MyMath.distanceBetween(prevPt, pt) > MIN_CURVE_MOVEMENT_DISTANCE)
          p = p.withAddPoint(p.polygon().numVertices(), pt);
      }
      writeActivePolygon(p);
    }
      break;
    }
  }

  private IPoint applyMouseOffset(IPoint mouseLocation) {
    return IPoint.sum(mouseLocation, mMouseOffset);
  }

  /**
   * Get the polygon being edited
   */
  private EditablePolygonElement activePolygon() {
    return (EditablePolygonElement) mCommand.newState().elements().get(mSlot);
  }

  /**
   * Store new version of polygon being edited
   */
  private void writeActivePolygon(EditablePolygonElement p) {
    StateTools.replaceAndSelectItem(mCommand, mSlot, p);
    editor().perform(mCommand);
  }

  @Override
  public void stop() {
    EditablePolygonElement p = activePolygon();
    p = p.withInsertVertex(-1, null);
    writeActivePolygon(p);

    if (mCurveMode) {
      if (p.polygon().isWellDefined()) {
        Polygon poly = p.polygon();

        if (MyMath.distanceBetween(poly.vertex(0), poly.lastVertex()) < 10)
          poly = poly.withOpen(false);

        poly = poly.simplify(0.5f);
        p = p.withPolygon(poly);
        writeActivePolygon(p);
      }
    }

    if (!p.polygon().isWellDefined()) {
      log("...removing incomplete edit polygon");
      // mCommand.finalState().objects().remove(mSlot);
      editor().perform(mCommand);
      todo("discard this last performed command");
      return;
    }

    attemptMerge(p);
  }

  private static final float MIN_CURVE_MOVEMENT_DISTANCE = 2.5f;

  private void attemptMerge(EditablePolygonElement p) {
    if (!mCurveMode)
      return;
    Polygon a = p.polygon();
    if (!a.isOpen())
      return;

    // Find another polygon to attempt a merge with.  If there isn't one, attempt merge with ourself.
    List<EditorElement> objects = mCommand.newState().elements();

    EditablePolygonElement mergeCandidate = null;
    int polyCount = 0;
    int bIndex = -1;

    for (int i = 0; i < objects.size(); i++) {
      if (i == mSlot)
        continue;
      EditorElement obj = objects.get(i);
      if (obj.tag() != EditablePolygonElement.TAG)
        continue;
      polyCount++;
      mergeCandidate = (EditablePolygonElement) obj;
      bIndex = i;
    }
    if (polyCount > 1) {
      pr("multiple polycounts, aborting");
      return;
    }

    EditablePolygonElement closedPolygon = null;

    if (mergeCandidate == null) {
      // If distance between endpoints is near zero, close it
      float dist = MyMath.distanceBetween(p.polygon().vertex(0), p.polygon().lastVertex());
      float curveSize = p.bounds().maxDim() / 3f;
      if (dist > curveSize)
        return;
      closedPolygon = p.withPolygon(p.polygon().withOpen(false));
    } else {
      log("examining merge for slot", bIndex, mSlot);
      EditablePolygonElement pb = mergeCandidate;
      Polygon b = pb.polygon().withOpen(false);
      MergePoly mg = new MergePoly(a, b);
      MergePoly best = mg;
      MergePoly mg2 = new MergePoly(a, b.reverse());
      if (mg2.score() < mg.score())
        best = mg2;
      if (!best.failureReason().isEmpty()) {
        log("failure:", best);
        return;
      }
      log("...replacing pair of polys with merged result");
      closedPolygon = p.withPolygon(best.mergedResult());
    }

    // Discard the 'add polygon' operation, since we're about to merge it; we will
    // want the result of the merge to be the one added to the undo history
    editor().discard();

    StateTools.remove(mCommand, mSlot);
    if (bIndex >= 0) {
      StateTools.remove(mCommand, bIndex);
      mCommand.description("Merge polygons");
    } else {
      mCommand.description("Close curve polygon");
    }
    StateTools.addNewElement(mCommand, closedPolygon);
    //    mCommand.finalState().objects().add(closedPolygon);
    mCommand.mergeDisabled(true);
    editor().perform(mCommand);
  }

  // ------------------------------------------------------------------
  // States
  // ------------------------------------------------------------------

  private static List<String> sStateNames = split("up adjust starting", ' ');

  private static String stateName(int state) {
    return sStateNames.get(state);
  }

  private static final int STATE_UP = 0;
  private static final int STATE_ADJUST = 1;
  private static final int STATE_STARTING_CURVE = 2;

  // ------------------------------------------------------------------

  @Override
  public JSMap toJson() {
    JSMap m = super.toJson();
    m.put("add?", mAddMode);
    m.put("curve?", mCurveMode);
    m.put("slot", mSlot);
    m.put("vertex", mVertexIndex);
    m.put("state", stateName(mState));
    if (mMouseOffset != null)
      m.put("mouse_offset", mMouseOffset.toJson());
    return m;
  }

  private EditablePolygonElement mOriginalElem;

  private boolean mAddMode;
  private boolean mCurveMode;
  // amount to add to mouse location to avoid 'jumping' when dragging vertex when starting editing existing
  private IPoint mMouseOffset;
  private IPoint mMouseDownLoc;
  // index of polygon being edited
  private int mSlot;
  private Command.Builder mCommand;

  // Vertex being manipulated
  private int mVertexIndex;
  private int mState;

}
