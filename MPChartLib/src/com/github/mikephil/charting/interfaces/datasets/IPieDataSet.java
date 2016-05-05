package com.github.mikephil.charting.interfaces.datasets;

import android.graphics.DashPathEffect;

import com.github.mikephil.charting.data.Entry;

/**
 * Created by Philipp Jahoda on 03/11/15.
 */
public interface IPieDataSet extends IDataSet<Entry> {

    /**
     * Returns the space that is set to be between the piechart-slices of this
     * DataSet, in pixels.
     *
     * @return
     */
    float getSliceSpace();

    /**
     * Returns the distance a highlighted piechart slice is "shifted" away from
     * the chart-center in dp.
     *
     * @return
     */
    float getSelectionShift();

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

    int getStrokeColor();

    int getHighlightedColor();
}

