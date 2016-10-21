package de.ust.skill.ir.restriction;

/**
 * @author Timm Felden
 */
public class IntRangeRestriction extends RangeRestriction {

	public long low, high;

	public IntRangeRestriction(long low, long high, boolean inclusiveLow, boolean inclusiveHigh) {
		if (inclusiveLow)
			this.low = low;
		else
			this.low = low + 1L;

		if (inclusiveHigh)
			this.high = high;
		else
			this.high = high - 1L;

        if (this.low > this.high)
			throw new IllegalStateException("Integer range restriction has no legal values: " + this.low + " -> "
					+ this.high);
	}

	/**
	 * @return lowest legal value. Always inclusive.
	 */
	public long getLow() {
		return low;
	}

	/**
	 * @return highest legal value. Always inclusive.
	 */
	public long getHigh() {
		return high;
	}

    @Override
    public String toString() {
        return "@range(" + low + ", " + high + ")";
    }
}
