package rtss.data.curves;

/**
 * Describes the trend of the curve segment
 */
public enum CurveSegmentTrend
{
    UP, // going up

    DOWN,  // going down

    MIN, // minimum segment

    MIN1, // left part of 2-segment minimum

    MIN2,  // right part of 2-segment minimum

    NEUTRAL // sideways or unknown
}
