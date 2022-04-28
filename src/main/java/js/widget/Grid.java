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
package js.widget;

import java.util.List;

import static js.base.Tools.*;
import js.geometry.IPoint;

/**
 * Data for a View that contains a grid of child views
 */
public final class Grid {

  public Grid(boolean free) {
    mFree = free;
  }

  public <T extends Widget> T widget() {
    return (T) mWidget;
  }

  public void setWidget(Widget widget) {
    mWidget = widget;
  }

  public boolean free() {
    return mFree;
  }

  public int numColumns() {
    return mColumnSizes.length;
  }

  public void setColumnSizes(int[] columnSizes) {
    mColumnSizes = columnSizes;
  }

  public int[] columnSizes() {
    return mColumnSizes;
  }

  public IPoint nextCellLocation() {
    if (mCachedNextCellLocation == null) {
      int x = 0;
      int y = 0;
      if (!mCells.isEmpty()) {
        GridCell lastCell = last(mCells);
        x = lastCell.x + lastCell.width;
        y = lastCell.y;
        checkState(x <= numColumns());
        if (x == numColumns()) {
          x = 0;
          y += 1;
        }
      }
      mCachedNextCellLocation = new IPoint(x, y);
    }
    return mCachedNextCellLocation;
  }

  public int numRows() {
    IPoint nextLoc = nextCellLocation();
    int y = nextLoc.y;
    if (nextLoc.x > 0)
      y++;
    return y;
  }

  private int checkValidColumn(int x) {
    if (x < 0 || x >= numColumns())
      throw new IllegalArgumentException("not a valid column: " + x);
    return x;
  }

  private int checkValidRow(int y) {
    if (y < 0 || y >= numRows())
      throw new IllegalArgumentException("not a valid row: " + y);
    return y;
  }

  public GridCell cellAt(int x, int y) {
    return mCells.get(checkValidRow(y) * numColumns() + checkValidColumn(x));
  }

  public void addCell(GridCell cell) {
    mCells.add(cell);
    mCachedNextCellLocation = null;
  }

  /**
   * Get list of cells... must be considered READ ONLY
   */
  public List<GridCell> cells() {
    return mCells;
  }

  public void propagateGrowFlags() {
   
    CellWeightList colGrowFlags = new CellWeightList();
    CellWeightList rowGrowFlags = new CellWeightList();

    for (GridCell cell : cells()) {
      if (cell.isEmpty())
        continue;

      // If view occupies multiple cells horizontally, don't propagate its grow flag
      if (cell.growX > 0 && cell.width == 1) {
        if (colGrowFlags.get(cell.x) < cell.growX) {
          colGrowFlags.set(cell.x, cell.growX);
        }
      }
      // If view occupies multiple cells vertically, don't propagate its grow flag
      // (at present, we don't support views stretching across multiple rows)
      if (cell.growY > 0 /* && cell.height == 1 */) {
        if (rowGrowFlags.get(cell.y) < cell.growY) {
          rowGrowFlags.set(cell.y, cell.growY);
        }
      }
    }

    // Now propagate grow flags from bit sets back to individual cells
    for (GridCell cell : cells()) {
      if (cell.isEmpty())
        continue;
      for (int x = cell.x; x < cell.x + cell.width; x++) {
        cell.growX = Math.max(cell.growX, colGrowFlags.get(x));
      }
      cell.growY = rowGrowFlags.get(cell.y);
    }
  }

  private final boolean mFree;
  private final List<GridCell> mCells = arrayList();
  private int[] mColumnSizes;
  private IPoint mCachedNextCellLocation;
  private Widget mWidget;

}
