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

import js.base.BaseObject;
import js.base.Pair;
import js.geometry.IPoint;
import js.geometry.MyMath;
import js.geometry.Polygon;
import js.json.JSMap;

import static js.base.Tools.*;

import java.util.List;

public class MergePoly extends BaseObject {

  public MergePoly(Polygon a, Polygon b) {
    checkArgument(a.isWellDefined());
    checkArgument(a.isOpen(), "must be open");
    mA = a;
    checkArgument(b.isWellDefined(), "must be well defined");
    mB = b;
  }

  public static final float SCORE_POOR = 1000000;

  public Float score() {
    if (mScore == null) {
      mScore = SCORE_POOR;
      if (mFailReason == null)
        calculateScore();
    }
    return mScore;
  }

  public String failureReason() {
    return mFailReason;
  }

  public Polygon mergedResult() {
    if (mMerged != null)
      return mMerged;
    if (score() == null) {
      mMerged = Polygon.DEFAULT_INSTANCE;
      return mMerged;
    }

    List<IPoint> v = arrayList();
    for (IPoint a : mA.vertices()) {
      v.add(a);
    }
    int b = mBj + 1;
    while (true) {
      b = MyMath.myMod(b, mB.numVertices());
      if (b == mBi)
        break;
      v.add(mB.vertex(b));
      b = b + 1;
    }

    mMerged = new Polygon(mB, v);
    return mMerged;
  }

  private void calculateScore() {
    chooseTargetVertices();
    if (!chooseClosestVerticesToTarget()) {
      setFailReason("B is too far from target vertices of A");
      return;
    }

    float avgDist = avgDistFromPolyline(mBi, mBj);
    if (avgDist > mMaxAverageDistance) {
      setFailReason("A is on average too far from B");
      return;
    }

    mScore = avgDist;
    mFailReason = "";
  }

  private void setFailReason(String cause) {
    if (mFailReason == null)
      mFailReason = cause;
  }

  private void chooseTargetVertices() {
    IPoint a0 = mA.vertex(0);
    IPoint a1 = mA.vertex(1);
    IPoint an = mA.vertexMod(-1);
    IPoint ao = mA.vertexMod(-2);

    float k = 2.5f;
    k = 1.0f;
    IPoint diff0 = IPoint.difference(a0, a1).scaledBy(k);
    IPoint diffn = IPoint.difference(an, ao).scaledBy(k);

    t0 = IPoint.sum(a0, diff0);
    tn = IPoint.sum(an, diffn);
  }

  private Pair<Integer, Float> closestVertex(IPoint target, IPoint[] vertices) {
    Pair<Integer, Float> best = null;
    int i = INIT_INDEX;
    for (IPoint v : vertices) {
      i++;
      float dist = MyMath.distanceBetween(target, v);
      if (best == null || best.second > dist)
        best = new Pair<>(i, dist);
    }
    return best;
  }

  private boolean chooseClosestVerticesToTarget() {
    Pair<Integer, Float> b = closestVertex(t0, mB.vertices());
    mBiDist = b;
    if (b.second > mMaxDistFromTargetVertices) {
      return false;
    }
    mBi = b.first;
    b = closestVertex(tn, mB.vertices());
    mBjDist = b;
    if (b.second > mMaxDistFromTargetVertices)
      return false;
    mBj = b.first;
    return true;
  }

  private float avgDistFromPolyline(int bi, int bj) {
    float distSum = 0;
    int i = bi;
    int samples = 0;
    while (true) {
      float dist = mA.boundaryDistanceFrom(mB.vertex(i));
      samples++;
      distSum += dist * dist;
      if (i == bj)
        break;

      i = (i + 1) % mB.numVertices();
    }
    return (distSum / samples);
  }

  @Override
  public JSMap toJson() {
    JSMap m = super.toJson();
    if (mA != null) {
      m.put("A", mA.toJson());
      m.put("B", mB.toJson());
    }
    if (t0 != null) {
      m.put("t0", t0.toJson());
      m.put("tn", tn.toJson());
    }
    if (mScore != null)
      m.put("score", mScore);

    if (mBiDist != null) {
      m.put("bi", map().put("index", mBiDist.first).put("distance", mBiDist.second));
    }
    if (mBjDist != null) {
      m.put("bj", map().put("index", mBjDist.first).put("distance", mBjDist.second));
    }

    if (mFailReason != null)
      m.put("fail_reason", mFailReason);
    return m;
  }

  // The maximum distance that polygon B can be from the target vertices
  private float mMaxDistFromTargetVertices = 80;

  // The maximum average squared distance of polyline B from A 
  private float mMaxAverageDistance = 300 * 300;

  private Polygon mMerged;
  private final Polygon mA, mB;
  private IPoint t0, tn;
  private int mBi;
  private int mBj;
  private String mFailReason;
  private Float mScore;
  private Pair<Integer, Float> mBiDist;
  private Pair<Integer, Float> mBjDist;

}
