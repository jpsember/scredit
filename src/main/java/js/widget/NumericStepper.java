package js.widget;

import static js.base.Tools.*;

import js.geometry.MyMath;

/**
 * Common functionality for manipulating min/max/default/step values that may be
 * integers or floats
 */
public final class NumericStepper {

  public NumericStepper(boolean floatsFlag, Number def, Number min, Number max,
      Number step) {
    if (step == null)
      step = 1;
    if (min == null)
      min = 0;
    if (def == null)
      def = min;

    mIntegerFlag = !floatsFlag;

    if (!floatsFlag && !(isInt(def) && isInt(min) && isInt(max) && isInt(step))) {
      throw new IllegalArgumentException(
          "non-integer values but 'floatsFlag' is false");
    }

    mMinValue = min;
    mMaxValue = max;
    checkArgument(mMinValue.floatValue() < mMaxValue.floatValue());

    mDefaultValue = clampValueIntoRange(def);
    if (step == null)
      step = 1;
    mStep = step;

    determineFormatString();
  }

  /**
   * Determine if a Number contains a value that can be represented as an
   * Integer without loss of precision
   */
  private static boolean isInt(Number number) {
    return number.intValue() == number.floatValue();
  }

  private static int numberOfDigits(int value) {
    int absValue = Math.abs(value);

    // Avoid nasty math functions (log10) by doing simple loop:
    int nDigits = 1;
    int power = 10;
    while (absValue >= power) {
      nDigits++;
      power *= 10;
    }

    // If the value was negative, add an extra digit for a minus sign
    if (value < 0)
      nDigits++;

    return nDigits;
  }

  public Number fromInternalUnits(int internalValue) {
    Number result;
    if (mIntegerFlag)
      result = internalValue + mMinValue.intValue();
    else
      result = (internalValue
          * (mMaxValue.floatValue() - mMinValue.floatValue()) / 100)
          + mMinValue.floatValue();
    return result;
  }

  public int toInternalUnits(Number ourValue) {
    int result;
    if (ourValue == null)
      ourValue = def();
    if (mIntegerFlag)
      result = ourValue.intValue() - mMinValue.intValue();
    else
      result = (int) ((100 * (ourValue.floatValue() - mMinValue.floatValue())) / (mMaxValue
          .floatValue() - mMinValue.floatValue()));
    return result;
  }

  public Number clampValueIntoRange(Number value) {
    if (value == null)
      value = mDefaultValue;
    if (mIntegerFlag)
      value = MyMath.clamp(value.intValue(), mMinValue.intValue(),
          mMaxValue.intValue());
    else
      value = MyMath.clamp(value.floatValue(), mMinValue.floatValue(),
          mMaxValue.floatValue());
    return value;
  }

  public Number def() {
    return mDefaultValue;
  }

  private Number mDefaultValue, mMinValue, mMaxValue, mStep;
  private boolean mIntegerFlag;

  public Number max() {
    return mMaxValue;
  }

  public int maxDigits() {
    return mMaxDisplayedCharacters;
  }

  public Number min() {
    return mMinValue;
  }

  public Number step() {
    return mStep;
  }

  public boolean isInt() {
    return mIntegerFlag;
  }

  public int internalVal() {
    return toInternalUnits(def());
  }

  public int internalMin() {
    return toInternalUnits(min());
  }

  public int internalMax() {
    return toInternalUnits(max());
  }

  public String formatNumber(Number displayValue) {
    return String.format(mFormatString, displayValue);
  }

  private void determineFormatString() {
    int maxIntegerDigits = Math.max(numberOfDigits(mMinValue.intValue()),
        numberOfDigits(mMaxValue.intValue()));
    if (isInt()) {
      mFormatString = "%" + maxIntegerDigits + "d";
      mMaxDisplayedCharacters = maxIntegerDigits;
    } else {
      int fracDigits = Math.max(0, 4 - maxIntegerDigits);
      mFormatString = "%" + (maxIntegerDigits + fracDigits) + "." + fracDigits
          + "f";
      int maxDigits = maxIntegerDigits;
      if (fracDigits > 0)
        maxDigits += 1 + fracDigits;
      mMaxDisplayedCharacters = maxDigits;
    }
  }

  private String mFormatString;
  private int mMaxDisplayedCharacters;
}
