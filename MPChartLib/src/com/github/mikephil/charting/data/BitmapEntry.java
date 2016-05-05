package com.github.mikephil.charting.data;

import android.os.Parcel;

/**
 * Created by mykhailo on 5/3/16.
 */
public class BitmapEntry extends Entry {

    private final int bitmapResId;
    private final int position;

    public BitmapEntry(float val, int xIndex, int bitmapResId, int position) {
        super(val, xIndex);
        this.bitmapResId = bitmapResId;
        this.position = position;
    }

    public int getBitmapResId() {
        return bitmapResId;
    }

    public int getPosition() {
        return position;
    }
}
