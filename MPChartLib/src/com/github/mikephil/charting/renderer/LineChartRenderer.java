package com.github.mikephil.charting.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.CircleBuffer;
import com.github.mikephil.charting.buffer.LineBuffer;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.lang.ref.WeakReference;
import java.util.List;

public class LineChartRenderer extends LineRadarRenderer {

    protected LineDataProvider mChart;

    /**
     * paint for the inner circle of the value indicators
     */
    protected Paint mCirclePaintInner;

    protected Paint mLabelBackgroundPaint;

    /**
     * Bitmap object used for drawing the paths (otherwise they are too long if
     * rendered directly on the canvas)
     */
    protected WeakReference<Bitmap> mDrawBitmap;

    /**
     * on this canvas, the paths are rendered, it is initialized with the
     * pathBitmap
     */
    protected Canvas mBitmapCanvas;

    /**
     * the bitmap configuration to be used
     */
    protected Bitmap.Config mBitmapConfig = Bitmap.Config.ARGB_8888;

    protected Path cubicPath = new Path();
    protected Path cubicFillPath = new Path();

    protected Path quadraticPath = new Path();
    protected Path quadraticFillPath = new Path();

    protected LineBuffer[] mLineBuffers;

    protected CircleBuffer[] mCircleBuffers;

    DisplayMetrics displayMetrics;

    public LineChartRenderer(LineDataProvider chart, ChartAnimator animator,
                             ViewPortHandler viewPortHandler, DisplayMetrics displayMetrics) {
        super(animator, viewPortHandler);
        mChart = chart;
        this.displayMetrics = displayMetrics;

        mCirclePaintInner = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaintInner.setStyle(Paint.Style.FILL);
        mCirclePaintInner.setColor(Color.WHITE);

        mLabelBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLabelBackgroundPaint.setStyle(Paint.Style.FILL);
        mLabelBackgroundPaint.setColor(Color.parseColor("#c0ffffff"));
    }

    @Override
    public void initBuffers() {

        LineData lineData = mChart.getLineData();
        mLineBuffers = new LineBuffer[lineData.getDataSetCount()];
        mCircleBuffers = new CircleBuffer[lineData.getDataSetCount()];

        for (int i = 0; i < mLineBuffers.length; i++) {
            ILineDataSet set = lineData.getDataSetByIndex(i);
            mLineBuffers[i] = new LineBuffer(set.getEntryCount() * 4 - 4);
            mCircleBuffers[i] = new CircleBuffer(set.getEntryCount() * 2);
        }
    }

    @Override
    public void drawData(Canvas c) {

        int width = (int) mViewPortHandler.getChartWidth();
        int height = (int) mViewPortHandler.getChartHeight();

        if (mDrawBitmap == null
                || (mDrawBitmap.get().getWidth() != width)
                || (mDrawBitmap.get().getHeight() != height)) {

            if (width > 0 && height > 0) {

                mDrawBitmap = new WeakReference<Bitmap>(Bitmap.createBitmap(width, height, mBitmapConfig));
                mBitmapCanvas = new Canvas(mDrawBitmap.get());
            } else
                return;
        }

        mDrawBitmap.get().eraseColor(Color.TRANSPARENT);

        LineData lineData = mChart.getLineData();

        for (ILineDataSet set : lineData.getDataSets()) {

            if (set.isVisible() && set.getEntryCount() > 0)
                drawDataSet(c, set);
        }

        c.drawBitmap(mDrawBitmap.get(), 0, 0, mRenderPaint);
    }

    protected void drawDataSet(Canvas c, ILineDataSet dataSet) {

        if (dataSet.getEntryCount() < 1)
            return;

        mRenderPaint.setStrokeWidth(dataSet.getLineWidth());
        mRenderPaint.setPathEffect(dataSet.getDashPathEffect());

        switch (dataSet.getApproximation()) {
            case ILineDataSet.APPROXIMATION_NONE:
                drawLinear(c, dataSet);
                break;
            case ILineDataSet.APPROXIMATION_QUADRATIC:
                drawQuadratic(c, dataSet);
                break;
            case ILineDataSet.APPROXIMATION_CUBIC:
                drawCubic(c, dataSet);
                break;
        }

        mRenderPaint.setPathEffect(null);
    }

    /**
     * Draws a cubic line.
     *
     * @param c
     * @param dataSet
     */
    protected void drawCubic(Canvas c, ILineDataSet dataSet) {

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        int entryCount = dataSet.getEntryCount();

        Entry entryFrom = dataSet.getEntryForXIndex((mMinX < 0) ? 0 : mMinX, DataSet.Rounding.DOWN);
        Entry entryTo = dataSet.getEntryForXIndex(mMaxX, DataSet.Rounding.UP);

        int diff = (entryFrom == entryTo) ? 1 : 0;
        int minx = Math.max(dataSet.getEntryIndex(entryFrom) - diff, 0);
        int maxx = Math.min(dataSet.getEntryIndex(entryTo) + 1, entryCount);

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        float intensity = dataSet.getCubicIntensity();

        cubicPath.reset();

        int size = (int) Math.ceil((maxx - minx) * phaseX + minx);

        if (size - minx >= 2) {

            float prevDx = 0f;
            float prevDy = 0f;
            float curDx = 0f;
            float curDy = 0f;

            Entry prevPrev = dataSet.getEntryForIndex(minx);
            Entry prev = prevPrev;
            Entry cur = prev;
            Entry next = dataSet.getEntryForIndex(minx + 1);

            // let the spline start
            cubicPath.moveTo(isFromZero(dataSet) ? 0 : cur.getXIndex(), isFromZero(dataSet) ? 0 : cur.getVal() * phaseY);

            prevDx = (cur.getXIndex() - prev.getXIndex()) * intensity;
            prevDy = (cur.getVal() - prev.getVal()) * intensity;

            curDx = (next.getXIndex() - cur.getXIndex()) * intensity;
            curDy = (next.getVal() - cur.getVal()) * intensity;

            // the first cubic
            cubicPath.cubicTo(prev.getXIndex() + prevDx, (prev.getVal() + prevDy) * phaseY,
                    cur.getXIndex() - curDx,
                    (cur.getVal() - curDy) * phaseY, cur.getXIndex(), cur.getVal() * phaseY);

            for (int j = minx + 1, count = Math.min(size, entryCount - 1); j < count; j++) {

                prevPrev = dataSet.getEntryForIndex(j == 1 ? 0 : j - 2);
                prev = dataSet.getEntryForIndex(j - 1);
                cur = dataSet.getEntryForIndex(j);
                next = dataSet.getEntryForIndex(j + 1);

                prevDx = (cur.getXIndex() - prevPrev.getXIndex()) * intensity;
                prevDy = (cur.getVal() - prevPrev.getVal()) * intensity;
                curDx = (next.getXIndex() - prev.getXIndex()) * intensity;
                curDy = (next.getVal() - prev.getVal()) * intensity;

                cubicPath.cubicTo(prev.getXIndex() + prevDx, (prev.getVal() + prevDy) * phaseY,
                        cur.getXIndex() - curDx,
                        (cur.getVal() - curDy) * phaseY, cur.getXIndex(), cur.getVal() * phaseY);
            }

            if (size > entryCount - 1) {

                prevPrev = dataSet.getEntryForIndex((entryCount >= 3) ? entryCount - 3
                        : entryCount - 2);
                prev = dataSet.getEntryForIndex(entryCount - 2);
                cur = dataSet.getEntryForIndex(entryCount - 1);
                next = cur;

                prevDx = (cur.getXIndex() - prevPrev.getXIndex()) * intensity;
                prevDy = (cur.getVal() - prevPrev.getVal()) * intensity;
                curDx = (next.getXIndex() - prev.getXIndex()) * intensity;
                curDy = (next.getVal() - prev.getVal()) * intensity;

                // the last cubic
                cubicPath.cubicTo(prev.getXIndex() + prevDx, (prev.getVal() + prevDy) * phaseY,
                        cur.getXIndex() - curDx,
                        (cur.getVal() - curDy) * phaseY, cur.getXIndex(), cur.getVal() * phaseY);
            }
        }

        // if filled is enabled, close the path
        if (dataSet.isDrawFilledEnabled()) {

            cubicFillPath.reset();
            cubicFillPath.addPath(cubicPath);
            // create a new path, this is bad for performance
            drawCubicFill(mBitmapCanvas, dataSet, cubicFillPath, trans,
                    entryFrom.getXIndex(), entryFrom.getXIndex() + size);
        }

        mRenderPaint.setColor(dataSet.getColor());

        mRenderPaint.setStyle(Paint.Style.STROKE);

        trans.pathValueToPixel(cubicPath);

        mBitmapCanvas.drawPath(cubicPath, mRenderPaint);

        mRenderPaint.setPathEffect(null);
    }

    protected void drawCubicFill(Canvas c, ILineDataSet dataSet, Path spline, Transformer trans,
                                 int from, int to) {

        if (to - from <= 1)
            return;

        float fillMin = dataSet.getFillFormatter()
                .getFillLinePosition(dataSet, mChart);

        spline.lineTo(to - 1, fillMin);
        spline.lineTo(from, fillMin);
        spline.close();

        trans.pathValueToPixel(spline);

        final Drawable drawable = dataSet.getFillDrawable();
        if (drawable != null) {

            drawFilledPath(c, spline, drawable);
        } else {

            drawFilledPath(c, spline, dataSet.getFillColor(), dataSet.getFillAlpha());
        }
    }

    /**
     * Draws a normal line.
     *
     * @param c
     * @param dataSet
     */
    protected void drawLinear(Canvas c, ILineDataSet dataSet) {

        int entryCount = dataSet.getEntryCount();

        int dataSetIndex = mChart.getLineData().getIndexOfDataSet(dataSet);

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        mRenderPaint.setStyle(Paint.Style.STROKE);

        Canvas canvas = null;

        // if the data-set is dashed, draw on bitmap-canvas
        if (dataSet.isDashedLineEnabled()) {
            canvas = mBitmapCanvas;
        } else {
            canvas = c;
        }

        Entry entryFrom = dataSet.getEntryForXIndex((mMinX < 0) ? 0 : mMinX, DataSet.Rounding.DOWN);
        Entry entryTo = dataSet.getEntryForXIndex(mMaxX, DataSet.Rounding.UP);

        int minx = Math.max(dataSet.getEntryIndex(entryFrom), 0);
        int maxx = Math.min(dataSet.getEntryIndex(entryTo) + 1, entryCount);

        int range = (maxx - minx) * 4 - 4;

        LineBuffer buffer = mLineBuffers[dataSetIndex];
        buffer.setPhases(phaseX, phaseY);
        buffer.limitFrom(minx);
        buffer.limitTo(maxx);
        buffer.feed(dataSet);

        trans.pointValuesToPixel(buffer.buffer);

        // more than 1 color
        if (dataSet.getColors().size() > 1) {

            for (int j = 0; j < range; j += 4) {

                if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j]))
                    break;

                // make sure the lines don't do shitty things outside
                // bounds
                if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])
                        || (!mViewPortHandler.isInBoundsTop(buffer.buffer[j + 1]) && !mViewPortHandler
                        .isInBoundsBottom(buffer.buffer[j + 3]))
                        || (!mViewPortHandler.isInBoundsTop(buffer.buffer[j + 1]) && !mViewPortHandler
                        .isInBoundsBottom(buffer.buffer[j + 3])))
                    continue;

                // get the color that is set for this line-segment
                mRenderPaint.setColor(dataSet.getColor(j / 4 + minx));

                canvas.drawLine(buffer.buffer[j], buffer.buffer[j + 1],
                        buffer.buffer[j + 2], buffer.buffer[j + 3], mRenderPaint);
            }

        } else { // only one color per dataset

            mRenderPaint.setColor(dataSet.getColor());

            // c.drawLines(buffer.buffer, mRenderPaint);
            canvas.drawLines(buffer.buffer, 0, range,
                    mRenderPaint);
        }

        mRenderPaint.setPathEffect(null);

        // if drawing filled is enabled
        if (dataSet.isDrawFilledEnabled() && entryCount > 0) {
            drawLinearFill(c, dataSet, minx, maxx, trans);
        }
    }

    protected void drawLinearFill(Canvas c, ILineDataSet dataSet, int minx,
                                  int maxx,
                                  Transformer trans) {

        Path filled = generateFilledPath(
                dataSet, minx, maxx);

        trans.pathValueToPixel(filled);

        final Drawable drawable = dataSet.getFillDrawable();
        if (drawable != null) {

            drawFilledPath(c, filled, drawable);
        } else {

            drawFilledPath(c, filled, dataSet.getFillColor(), dataSet.getFillAlpha());
        }
    }

    /**
     * Generates the path that is used for filled drawing.
     *
     * @param dataSet
     * @return
     */
    private Path generateFilledPath(ILineDataSet dataSet, int from, int to) {

        float fillMin = dataSet.getFillFormatter().getFillLinePosition(dataSet, mChart);
        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        Path filled = new Path();
        Entry entry = dataSet.getEntryForIndex(from);

        filled.moveTo(isFromZero(dataSet) ? 0 : entry.getXIndex(), isFromZero(dataSet) ? 0 : fillMin);
        filled.lineTo(entry.getXIndex(), entry.getVal() * phaseY);

        // create a new path
        for (int x = from + 1, count = (int) Math.ceil((to - from) * phaseX + from); x < count; x++) {

            Entry e = dataSet.getEntryForIndex(x);
            filled.lineTo(e.getXIndex(), e.getVal() * phaseY);
        }

        // close up
        filled.lineTo(
                dataSet.getEntryForIndex(
                        Math.max(
                                Math.min((int) Math.ceil((to - from) * phaseX + from) - 1,
                                        dataSet.getEntryCount() - 1), 0)).getXIndex(), fillMin);

        filled.close();

        return filled;
    }

    /**
     * Draws a normal line.
     *
     * @param c
     * @param dataSet
     */
    protected void drawQuadratic(Canvas c, ILineDataSet dataSet) {
        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        int entryCount = dataSet.getEntryCount();

        Entry entryFrom = dataSet.getEntryForXIndex((mMinX < 0) ? 0 : mMinX, DataSet.Rounding.DOWN);
        Entry entryTo = dataSet.getEntryForXIndex(mMaxX, DataSet.Rounding.UP);

        int diff = (entryFrom == entryTo) ? 1 : 0;
        int minx = Math.max(dataSet.getEntryIndex(entryFrom) - diff, 0);
        int maxx = Math.min(dataSet.getEntryIndex(entryTo) + 1, entryCount);

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        float intensity = dataSet.getCubicIntensity();

        quadraticPath.reset();

        int size = (int) Math.ceil((maxx - minx) * phaseX + minx);

        if (size - minx >= 2) {
            float midX = 0f;
            float midY = 0f;

            Entry prev = dataSet.getEntryForIndex(minx);
            Entry cur = dataSet.getEntryForIndex(minx);

            // let the spline start
            quadraticPath.moveTo(cur.getXIndex(), isFromZero(dataSet) ? 0f : cur.getVal() * phaseY);

            for (int j = minx + 1, count = Math.min(size, entryCount); j < count; j++) {
                prev = dataSet.getEntryForIndex(j - 1);
                cur = dataSet.getEntryForIndex(j);

                midX = ((prev.getXIndex() + cur.getXIndex()) / 2f);
                midY = ((prev.getVal() + cur.getVal()) / 2f);

                quadraticPath.quadTo((prev.getXIndex() + midX) / 2, prev.getVal(), midX, midY);
                quadraticPath.quadTo((midX + cur.getXIndex()) / 2, cur.getVal(), cur.getXIndex(), cur.getVal());
            }
        }

        // if drawing filled is enabled
        if (dataSet.isDrawFilledEnabled() && entryCount > 0) {
            //TODO drawQuadraticFill
        }

        mRenderPaint.setColor(dataSet.getColor());

        mRenderPaint.setStyle(Paint.Style.STROKE);

        trans.pathValueToPixel(quadraticPath);

        mBitmapCanvas.drawPath(quadraticPath, mRenderPaint);

        mRenderPaint.setPathEffect(null);
    }

    @Override
    public void drawValues(Canvas c) {
        drawValues(c, null);
    }

    public void drawValues(Canvas c, Highlight[] highlights) {

        if (mChart.getLineData().getYValCount() < mChart.getMaxVisibleCount()
                * mViewPortHandler.getScaleX()) {

            List<ILineDataSet> dataSets = mChart.getLineData().getDataSets();

            for (int i = 0; i < dataSets.size(); i++) {

                ILineDataSet dataSet = dataSets.get(i);

                if (!dataSet.isDrawValuesEnabled() || dataSet.getEntryCount() == 0)
                    continue;

                // apply the text-styling defined by the DataSet
                applyValueTextStyle(dataSet);

                Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

                // make sure the values do not interfear with the circles
                int valOffset = (int) (dataSet.getCircleRadius() * 1.75f);

                if (!dataSet.isDrawCirclesEnabled())
                    valOffset = valOffset / 2;

                int entryCount = dataSet.getEntryCount();

                Entry entryFrom = dataSet.getEntryForXIndex((mMinX < 0) ? 0 : mMinX, DataSet.Rounding.DOWN);
                Entry entryTo = dataSet.getEntryForXIndex(mMaxX, DataSet.Rounding.UP);

                int minx = Math.max(dataSet.getEntryIndex(entryFrom), 0);
                int maxx = Math.min(dataSet.getEntryIndex(entryTo) + 1, entryCount);

                float[] positions = trans.generateTransformedValuesLine(
                        dataSet, mAnimator.getPhaseX(), mAnimator.getPhaseY(), minx, maxx);

                for (int j = 0; j < positions.length; j += 2) {

                    float x = positions[j];
                    float y = positions[j + 1];

                    if (!mViewPortHandler.isInBoundsRight(x))
                        break;

                    if (!mViewPortHandler.isInBoundsLeft(x) || !mViewPortHandler.isInBoundsY(y))
                        continue;

                    if (highlights == null) {
                        if ((isFirstEnd(dataSet) && (!(j == 0 || j == (positions.length - 2)))))
                            continue;
                    } else {
                        if (!hasIndexInHighlighted(highlights, j / 2))
                            continue;
                    }

                    Entry entry = dataSet.getEntryForIndex(j / 2 + minx);

                    float value = entry.getVal();

                    if (isFirstEnd(dataSet)) {
                        float valueX = 0f;
                        float valueY = 0f;

                        String text = dataSet.getValueFormatter().getFormattedValue(value, entry, i, mViewPortHandler);

                        float labelXOffset = mValuePaint.measureText(text) / 2f;

                        if (j == 0) {
                            valueX = x + valOffset + labelXOffset + dpToPx(3);
                            valueY = y + dataSet.getValueTextSize() / 3f;
                        } else if (j == (positions.length - 2)) {
                            valueX = x - valOffset - labelXOffset - dpToPx(3);
                            valueY = y + dataSet.getValueTextSize() / 3f;
                        }

                        if (highlights != null) {
                            if (j == 0 || j == (positions.length - 2)) {
                                x = valueX;
                            }
                            c.drawRoundRect(new RectF(x - labelXOffset - dpToPx(2), mViewPortHandler.contentTop() + dpToPx(38f) - dataSet.getValueTextSize(),
                                    x + labelXOffset + dpToPx(2), mViewPortHandler.contentTop() + dpToPx(38f) + dpToPx(3)), dpToPx(2), dpToPx(2), mLabelBackgroundPaint);

                            drawValue(c, dataSet.getValueFormatter(), value, entry, i, x,
                                    mViewPortHandler.contentTop() + dpToPx(38f), dataSet.getValueTextColor(j / 2));
                        } else if ((j == 0 || j == (positions.length - 2)) && !(isFromZero(dataSet))) {
                            c.drawRoundRect(new RectF(valueX - labelXOffset - dpToPx(2), valueY - dataSet.getValueTextSize(),
                                    valueX + labelXOffset + dpToPx(2), valueY + dpToPx(3)), dpToPx(2), dpToPx(2), mLabelBackgroundPaint);

                            drawValue(c, dataSet.getValueFormatter(), value, entry, i, valueX, valueY, dataSet.getValueTextColor(j / 2));
                        }
                    } else {
                        drawValue(c, dataSet.getValueFormatter(), value, entry, i, x, y - valOffset, dataSet.getValueTextColor(j / 2));
                    }
                }
            }
        }
    }

    @Override
    public void drawExtras(Canvas c) {
        drawCircles(c);
    }

    protected void drawCircles(Canvas c) {
        drawCircles(c, null);
    }

    protected void drawCircles(Canvas c, Highlight[] highlights) {

        mRenderPaint.setStyle(Paint.Style.FILL);

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        List<ILineDataSet> dataSets = mChart.getLineData().getDataSets();

        for (int i = 0; i < dataSets.size(); i++) {

            ILineDataSet dataSet = dataSets.get(i);

            if (!dataSet.isVisible() || !dataSet.isDrawCirclesEnabled() ||
                    dataSet.getEntryCount() == 0)
                continue;

            mCirclePaintInner.setColor(dataSet.getCircleHoleColor());

            Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

            int entryCount = dataSet.getEntryCount();

            Entry entryFrom = dataSet.getEntryForXIndex((mMinX < 0) ? 0 : mMinX, DataSet.Rounding.DOWN);
            Entry entryTo = dataSet.getEntryForXIndex(mMaxX, DataSet.Rounding.UP);

            int minx = Math.max(dataSet.getEntryIndex(entryFrom), 0);
            int maxx = Math.min(dataSet.getEntryIndex(entryTo) + 1, entryCount);

            CircleBuffer buffer = mCircleBuffers[i];
            buffer.setPhases(phaseX, phaseY);
            buffer.limitFrom(minx);
            buffer.limitTo(maxx);
            buffer.feed(dataSet);

            trans.pointValuesToPixel(buffer.buffer);

            float halfsize = dataSet.getCircleRadius() / 2f;

            for (int j = 0, count = (int) Math.ceil((maxx - minx) * phaseX + minx) * 2; j < count; j += 2) {

                float x = buffer.buffer[j];
                float y = buffer.buffer[j + 1];

                if (!mViewPortHandler.isInBoundsRight(x))
                    break;

                // make sure the circles don't do shitty things outside
                // bounds
                if (!mViewPortHandler.isInBoundsLeft(x) || !mViewPortHandler.isInBoundsY(y))
                    continue;

                if ((isFirstEnd(dataSet) && (!(j == 0 || j == (count - 2)))) && !hasIndexInHighlighted(highlights, j / 2))
                    continue;

                int circleColor = dataSet.getCircleColor(j / 2 + minx);

                mRenderPaint.setColor(circleColor);
                mRenderPaint.setStyle(Paint.Style.STROKE);

                float[] pixelVals = {0f, 0f};
                trans.pointValuesToPixel(pixelVals);
                float valY = isFromZero(dataSet) && j == 0 ? pixelVals[1] : y;

                c.drawCircle(x, valY, dataSet.getCircleRadius() - dpToPx(0.5f),
                        mRenderPaint);

                if (dataSet.isDrawCircleHoleEnabled()
                        && circleColor != mCirclePaintInner.getColor())
                    c.drawCircle(x, valY,
                            halfsize,
                            mCirclePaintInner);
            }
        }
    }

    private boolean hasIndexInHighlighted(Highlight[] highlights, int index) {
        if (highlights == null) return false;

        for (Highlight highlight : highlights) {
            if (highlight.getXIndex() == index) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void drawHighlighted(Canvas c, Highlight[] indices) {
        for (Highlight highlight : indices) {
            ILineDataSet set = mChart.getLineData().getDataSetByIndex(highlight.getDataSetIndex());
            highlight.setXIndex(set.getNearestXIndex(highlight.getXIndex()));
        }

        for (int i = 0; i < indices.length; i++) {

            ILineDataSet set = mChart.getLineData().getDataSetByIndex(indices[i]
                    .getDataSetIndex());

            if (set == null || !set.isHighlightEnabled())
                continue;

            int xIndex = indices[i].getXIndex(); // get the
            // x-position

            if (xIndex > mChart.getXChartMax() * mAnimator.getPhaseX())
                continue;

            final float yVal = set.getYValForXIndex(xIndex);
            if (yVal == Float.NaN)
                continue;

            float y = yVal * mAnimator.getPhaseY(); // get
            // the
            // y-position

            float[] pts = new float[]{
                    xIndex, y
            };

            mChart.getTransformer(set.getAxisDependency()).pointValuesToPixel(pts);

            // draw the lines
            drawHighlightLines(c, pts, set);
        }

        drawCircles(c, indices);
        drawValues(c, indices);
    }

    /**
     * Sets the Bitmap.Config to be used by this renderer.
     * Default: Bitmap.Config.ARGB_8888
     * Use Bitmap.Config.ARGB_4444 to consume less memory.
     *
     * @param config
     */
    public void setBitmapConfig(Bitmap.Config config) {
        mBitmapConfig = config;
        releaseBitmap();
    }

    /**
     * Returns the Bitmap.Config that is used by this renderer.
     *
     * @return
     */
    public Bitmap.Config getBitmapConfig() {
        return mBitmapConfig;
    }

    /**
     * Releases the drawing bitmap. This should be called when {@link LineChart#onDetachedFromWindow()}.
     */
    public void releaseBitmap() {
        if (mDrawBitmap != null) {
            mDrawBitmap.get().recycle();
            mDrawBitmap.clear();
            mDrawBitmap = null;
        }
    }

    public float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }

    boolean isFirstEnd(ILineDataSet dataSet) {
        return (dataSet.drawStyle() & ILineDataSet.STYLE_FIRST_END) != 0;
    }

    boolean isFromZero(ILineDataSet dataSet) {
        return (dataSet.drawStyle() & ILineDataSet.STYLE_FROM_ZERO) != 0;
    }
}
