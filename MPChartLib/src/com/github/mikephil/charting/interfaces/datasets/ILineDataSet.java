package com.github.mikephil.charting.interfaces.datasets;

import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.FillFormatter;

/**
 * Created by Philpp Jahoda on 21/10/15.
 */
public interface ILineDataSet extends ILineRadarDataSet<Entry> {

    int STYLE_ALL = 1;
    int STYLE_FIRST_END = 2;
    int STYLE_FROM_ZERO = 4;

    int APPROXIMATION_NONE = 1;
    int APPROXIMATION_QUADRATIC = 2;
    int APPROXIMATION_CUBIC = 3;

    /**
     * Returns the intensity of the cubic lines (the effect intensity).
     * Max = 1f = very cubic, Min = 0.05f = low cubic effect, Default: 0.2f
     *
     * @return
     */
    float getCubicIntensity();

    /**
     *
     * @return
     */
    int getApproximation();

    /**
     * Returns the size of the drawn circles.
     */
    float getCircleRadius();

    /**
     * Returns the color at the given index of the DataSet's circle-color array.
     * Performs a IndexOutOfBounds check by modulus.
     *
     * @param index
     * @return
     */
    int getCircleColor(int index);

    /**
     * Returns true if drawing circles for this DataSet is enabled, false if not
     *
     * @return
     */
    boolean isDrawCirclesEnabled();

    int drawStyle();

    /**
     * Returns the color of the inner circle (the circle-hole).
     *
     * @return
     */
    int getCircleHoleColor();

    /**
     * Returns true if drawing the circle-holes is enabled, false if not.
     *
     * @return
     */
    boolean isDrawCircleHoleEnabled();

    /**
     * Returns the DashPathEffect that is used for drawing the lines.
     *
     * @return
     */
    DashPathEffect getDashPathEffect();

    /**
     * Returns true if the dashed-line effect is enabled, false if not.
     * If the DashPathEffect object is null, also return false here.
     *
     * @return
     */
    boolean isDashedLineEnabled();

    /**
     * Returns the FillFormatter that is set for this DataSet.
     *
     * @return
     */
    FillFormatter getFillFormatter();
}