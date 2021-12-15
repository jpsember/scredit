package js.widget;

import static js.base.Tools.*;

import java.util.List;

public final class CellWeightList {

  public void set(int index, int weight) {
    growTo(1 + index);
    mWeights.set(index, weight);
  }

  public int get(int index) {
    if (mWeights.size() <= index)
      return 0;
    return mWeights.get(index);
  }

  private void growTo(int capacity) {
    while (mWeights.size() < capacity)
      mWeights.add(0);
  }

  private List<Integer> mWeights = arrayList();
}
