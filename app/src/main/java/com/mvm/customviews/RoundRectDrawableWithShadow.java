/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mvm.customviews;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * A rounded rectangle drawable which also includes a shadow around.
 */
public class RoundRectDrawableWithShadow extends Drawable {
    // used to calculate content padding
    final static double COS_45 = Math.cos(Math.toRadians(45));

    enum TYPE {
        ALL, TOP, BOTTOM,NONE
    }

    TYPE mType = TYPE.ALL;

    final static float SHADOW_MULTIPLIER = 1.5f;

    final int mInsetShadow; // extra shadow to avoid gaps between card and shadow

    /*
    * This helper is set by CardView implementations.
    * <p>
    * Prior to API 17, canvas.drawRoundRect is expensive; which is why we need this interface
    * to draw efficient rounded rectangles before 17.
    * */
    static RoundRectHelper sRoundRectHelper;
    final RectF sCornerRect = new RectF();

    Paint mPaint;

    Paint mCornerShadowPaint;

    Paint mEdgeShadowPaint;

    final RectF mCardBounds;

    float mCornerRadius;


    Path mCornerShadowPath;

    // updated value with inset
    float mMaxShadowSize;

    // actual value set by developer
    float mRawMaxShadowSize;

    // multiplied value to account for shadow offset
    float mShadowSize;

    // actual value set by developer
    float mRawShadowSize;

    private boolean mDirty = true;

    private final int mShadowStartColor;

    private final int mShadowEndColor;

    private boolean mAddPaddingForCorners = true;

    /**
     * If shadow size is set to a value above max shadow, we print a warning
     */
    private boolean mPrintedShadowClipWarning = false;

    public RoundRectDrawableWithShadow(Resources resources, int backgroundColor, float topRadius,
            float bottomRadius, float shadowSize, float maxShadowSize) {
        mShadowStartColor = resources.getColor(
                android.support.v7.cardview.R.color.cardview_shadow_start_color);
        mShadowEndColor = resources.getColor(
                android.support.v7.cardview.R.color.cardview_shadow_end_color);
        mInsetShadow = resources.getDimensionPixelSize(
                android.support.v7.cardview.R.dimen.cardview_compat_inset_shadow);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(backgroundColor);

        setCornerRadius(topRadius, bottomRadius);

        mCornerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mCornerShadowPaint.setStyle(Paint.Style.FILL);

        mEdgeShadowPaint = new Paint(mCornerShadowPaint);
        //mCornerRadius = (int) (radius + .5f);
        mCardBounds = new RectF();

        mEdgeShadowPaint.setAntiAlias(false);
        setShadowSize(shadowSize, maxShadowSize);

        initHelper();
    }

    private void initHelper() {

        sRoundRectHelper = new RoundRectHelper() {
            @Override
            public void drawRoundRect(Canvas canvas, RectF bounds, float cornerRadius,
                    Paint paint,TYPE type) {
                final float twoRadius = cornerRadius * 2;
                final float innerWidth = bounds.width() - twoRadius - 1;
                final float innerHeight = bounds.height() - twoRadius - 1;

                float topRadius,bottomRadius;
                topRadius=bottomRadius= cornerRadius;

                // increment it to account for half pixels.
                if (cornerRadius >= 1f) {
                    cornerRadius += .5f;
                    sCornerRect.set(-cornerRadius, -cornerRadius, cornerRadius, cornerRadius);
                    int saved = canvas.save();
                    canvas.translate(bounds.left + cornerRadius, bounds.top + cornerRadius);
                    if (type == TYPE.TOP) {
                        canvas.drawArc(sCornerRect, 180, 90, true, paint);
                        canvas.translate(innerWidth, 0);
                        //canvas.rotate(90);
                        canvas.drawArc(sCornerRect, 270, 180, true, paint);
                        bottomRadius = 0;
                    }else if (type == TYPE.BOTTOM) {
                        canvas.translate(0, innerHeight);
                        //canvas.rotate(90);
                        canvas.drawArc(sCornerRect, 360, 270, true, paint);
                        canvas.translate(innerWidth, 0);
                        //canvas.rotate(90);
                        canvas.drawArc(sCornerRect, 270, 360, true, paint);
                        topRadius = 0;
                    }else if(type==TYPE.ALL){
                        canvas.drawArc(sCornerRect, 180, 90, true, paint);
                        canvas.translate(innerWidth, 0);
                        canvas.rotate(90);
                        canvas.drawArc(sCornerRect, 180, 90, true, paint);
                        canvas.translate(innerHeight, 0);
                        canvas.rotate(90);
                        canvas.drawArc(sCornerRect, 180, 90, true, paint);
                        canvas.translate(innerWidth, 0);
                        canvas.rotate(90);
                        canvas.drawArc(sCornerRect, 180, 90, true, paint);
                    }else{
                        topRadius = bottomRadius=0;
                    }
                    canvas.restoreToCount(saved);
                    //draw top and bottom pieces
                    if (type == TYPE.TOP || type == TYPE.ALL) {
                        canvas.drawRect(bounds.left + cornerRadius - 1f, bounds.top,
                                bounds.right - cornerRadius + 1f, bounds.top + cornerRadius, paint);
                    }
                    if (type == TYPE.BOTTOM || type == TYPE.ALL) {
                        canvas.drawRect(bounds.left + cornerRadius - 1f,
                                bounds.bottom - cornerRadius + 1f, bounds.right - cornerRadius + 1f,
                                bounds.bottom, paint);
                    }
                }
                ////                center
                canvas.drawRect(bounds.left, bounds.top + Math.max(0, topRadius - 1f),
                        bounds.right, bounds.bottom - bottomRadius + 1f, paint);
            }
        };
    }

    public RoundRectDrawableWithShadow(Resources resources, int backgroundColor, float radius,
            float shadowSize, float maxShadowSize) {
        this(resources, backgroundColor, radius, radius, shadowSize, maxShadowSize);
    }

    /**
     * Casts the value to an even integer.
     */
    private int toEven(float value) {
        int i = (int) (value + .5f);
        if (i % 2 == 1) {
            return i - 1;
        }
        return i;
    }

    public void setAddPaddingForCorners(boolean addPaddingForCorners) {
        mAddPaddingForCorners = addPaddingForCorners;
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        if (mCornerShadowPaint != null) mCornerShadowPaint.setAlpha(alpha);
        //mCornerShadowPaint.setAlpha(alpha);
        mEdgeShadowPaint.setAlpha(alpha);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mDirty = true;
    }

    void setShadowSize(float shadowSize, float maxShadowSize) {
        if (shadowSize < 0 || maxShadowSize < 0) {
            throw new IllegalArgumentException("invalid shadow size");
        }
        shadowSize = toEven(shadowSize);
        maxShadowSize = toEven(maxShadowSize);
        if (shadowSize > maxShadowSize) {
            shadowSize = maxShadowSize;
            if (!mPrintedShadowClipWarning) {
                mPrintedShadowClipWarning = true;
            }
        }
        if (mRawShadowSize == shadowSize && mRawMaxShadowSize == maxShadowSize) {
            return;
        }
        mRawShadowSize = shadowSize;
        mRawMaxShadowSize = maxShadowSize;
        mShadowSize = (int) (shadowSize * SHADOW_MULTIPLIER + mInsetShadow + .5f);
        mMaxShadowSize = maxShadowSize + mInsetShadow;
        mDirty = true;
        invalidateSelf();
    }

    @Override
    public boolean getPadding(Rect padding) {
        float rad;
        int vTopOffset, vBottomOffset;

        if (mType == TYPE.TOP) {
            rad = mCornerRadius;
            vTopOffset = (int) Math.ceil(calculateVerticalPadding(mRawMaxShadowSize, rad,
                    mAddPaddingForCorners));
            vBottomOffset = 0;
        } else if (mType == TYPE.BOTTOM) {
            rad = mCornerRadius;
            vTopOffset = 0;
            vBottomOffset = (int) Math.ceil(calculateVerticalPadding(mRawMaxShadowSize, rad,
                    mAddPaddingForCorners));

        } else {
            rad = mCornerRadius;
            vTopOffset = vBottomOffset = (int) Math.ceil(calculateVerticalPadding(mRawMaxShadowSize,
                    rad, mAddPaddingForCorners));
        }
        int hOffset = (int) Math.ceil(calculateHorizontalPadding(mRawMaxShadowSize, rad,
                mAddPaddingForCorners));

        padding.set(hOffset, vTopOffset, hOffset, vBottomOffset);
        return true;
    }

    static float calculateVerticalPadding(float maxShadowSize, float cornerRadius,
            boolean addPaddingForCorners) {
        if (addPaddingForCorners) {
            return (float) (maxShadowSize * SHADOW_MULTIPLIER + (1 - COS_45) * cornerRadius);
        } else {
            return maxShadowSize * SHADOW_MULTIPLIER;
        }
    }

    static float calculateHorizontalPadding(float maxShadowSize, float cornerRadius,
            boolean addPaddingForCorners) {
        if (addPaddingForCorners) {
            return (float) (maxShadowSize + (1 - COS_45) * cornerRadius);
        } else {
            return maxShadowSize;
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        if (mCornerShadowPaint != null) mCornerShadowPaint.setColorFilter(cf);
        //mCornerShadowPaint.setColorFilter(cf);
        mEdgeShadowPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setCornerRadius(float topRadius, float bottomRadius) {
        topRadius = (int) (topRadius + .5f);
        bottomRadius = (int) (bottomRadius + .5f);

        if (topRadius == bottomRadius && mCornerRadius == topRadius && mCornerRadius>0) {
            return;
        }
        if (topRadius == bottomRadius && topRadius > 0) {
            mCornerRadius = topRadius;
            mType = TYPE.ALL;
        } else if (topRadius > 0) {
            mCornerRadius = topRadius;
            mType = TYPE.TOP;
        } else if(bottomRadius>0){
            mCornerRadius = bottomRadius;
            mType = TYPE.BOTTOM;
        }else{
            mCornerRadius = 0;
            mType = TYPE.NONE;
        }

        mDirty = true;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mDirty) {
            buildComponents(getBounds());
            mDirty = false;
        }
        canvas.translate(0, mRawShadowSize / 2);
        drawShadow(canvas);
        canvas.translate(0, -mRawShadowSize / 2);
        sRoundRectHelper.drawRoundRect(canvas, mCardBounds, mCornerRadius, mPaint,mType);
    }

    private void drawShadow(Canvas canvas) {
        final float edgeShadowTop = -mCornerRadius - mShadowSize;
        final float inset = mCornerRadius + mInsetShadow + mRawShadowSize / 2;
        final boolean drawHorizontalEdges = mCardBounds.width() - 2 * inset > 0;
        final boolean drawVerticalEdges = mCardBounds.height() - 2 * inset > 0;

        int saved = canvas.save();
        if(mType==TYPE.TOP){
            // LT
            canvas.translate(mCardBounds.left + inset, mCardBounds.top + inset);
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            if (drawHorizontalEdges) {
                canvas.drawRect(0, edgeShadowTop, mCardBounds.width() - 2 * inset, -mCornerRadius,
                        mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);

            // RT
            saved = canvas.save();
            canvas.translate(mCardBounds.right - inset, mCardBounds.top + inset);
            canvas.rotate(90f);
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            if (drawVerticalEdges) {
                canvas.drawRect(0, edgeShadowTop, mCardBounds.height(), -mCornerRadius,
                        mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);

            saved = canvas.save();
            canvas.translate(mCardBounds.left + inset, mCardBounds.bottom);
            canvas.rotate(270f);
            if (drawVerticalEdges) {
                canvas.drawRect(- mInsetShadow, edgeShadowTop, mCardBounds.height() - inset, -mCornerRadius,
                        mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);
        }else if(mType==TYPE.BOTTOM){
            // LB
            saved = canvas.save();
            canvas.translate(mCardBounds.left + inset, mCardBounds.bottom - inset);
            canvas.rotate(270f);
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            if (drawVerticalEdges) {
                canvas.drawRect(0, edgeShadowTop, mCardBounds.bottom, -mCornerRadius,
                        mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);

            // RB
            saved = canvas.save();
            canvas.translate(mCardBounds.right - inset, mCardBounds.bottom - inset);
            canvas.rotate(180f);
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            if (drawHorizontalEdges) {
                canvas.drawRect(0, edgeShadowTop, mCardBounds.width() - 2 * inset,
                        /*-mCornerRadius + mCornerRadius +*/ mInsetShadow-(mCornerRadius/2), mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);


            saved = canvas.save();
            canvas.translate(mCardBounds.right - inset, mCardBounds.top);
            canvas.rotate(90f);
            if (drawVerticalEdges) {
                canvas.drawRect(-2*mCornerRadius-mInsetShadow, edgeShadowTop, mCardBounds.height() - inset, -mCornerRadius,
                        mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);

        }else if(mType==TYPE.ALL){
            // LT
            canvas.translate(mCardBounds.left + inset, mCardBounds.top + inset);
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            if (drawHorizontalEdges) {
                canvas.drawRect(0, edgeShadowTop, mCardBounds.width() - 2 * inset, -mCornerRadius,
                        mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);

            // RB
            saved = canvas.save();
            canvas.translate(mCardBounds.right - inset, mCardBounds.bottom - inset);
            canvas.rotate(180f);
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            if (drawHorizontalEdges) {
                canvas.drawRect(0, edgeShadowTop, mCardBounds.width() - 2 * inset,
                        -mCornerRadius + mShadowSize, mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);

            // LB
            saved = canvas.save();
            canvas.translate(mCardBounds.left + inset, mCardBounds.bottom - inset);
            canvas.rotate(270f);
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            if (drawVerticalEdges) {
                canvas.drawRect(0, edgeShadowTop, mCardBounds.height() - 2 * inset, -mCornerRadius,
                        mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);

            // RT
            saved = canvas.save();
            canvas.translate(mCardBounds.right - inset, mCardBounds.top + inset);
            canvas.rotate(90f);
            canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            if (drawVerticalEdges) {
                canvas.drawRect(0, edgeShadowTop, mCardBounds.height() - 2 * inset, -mCornerRadius,
                        mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);
        }else{
            saved = canvas.save();
            canvas.translate(mCardBounds.left + inset, mCardBounds.bottom);
            canvas.rotate(270f);
            //canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
            if (drawVerticalEdges) {
                canvas.drawRect(0, edgeShadowTop, mCardBounds.bottom+inset, -mCornerRadius,
                        mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);

            saved = canvas.save();
            canvas.translate(mCardBounds.right - inset, mCardBounds.top-inset);
            canvas.rotate(90f);
            if (drawVerticalEdges) {
                canvas.drawRect(0, edgeShadowTop, mCardBounds.bottom+inset, -mCornerRadius,
                        mEdgeShadowPaint);
            }
            canvas.restoreToCount(saved);
        }
    }

    private void buildShadowCorners() {
        float hRad = mCornerRadius;
        float vTRad = 0, vBRad = 0;

        switch (mType) {
            case ALL:
                vTRad = vBRad = mCornerRadius;
                break;
            case TOP:
                vTRad = mCornerRadius;
                vBRad = 0;
                break;
            case BOTTOM:
                vTRad = 0;
                vBRad = mCornerRadius;
                break;
        }

        RectF innerBounds = new RectF(-hRad, -vTRad, hRad, vBRad);
        RectF outerBounds = new RectF(innerBounds);
        outerBounds.inset(-mShadowSize, -mShadowSize);

        if (mCornerShadowPath == null) {
            mCornerShadowPath = new Path();
        } else {
            mCornerShadowPath.reset();
        }
        mCornerShadowPath.setFillType(Path.FillType.EVEN_ODD);
        mCornerShadowPath.moveTo(-hRad, 0);
        mCornerShadowPath.rLineTo(-mShadowSize, 0);
        // outer arc
        mCornerShadowPath.arcTo(outerBounds, 180f, 90f, false);
        // inner arc
        mCornerShadowPath.arcTo(innerBounds, 270f, -90f, false);
        mCornerShadowPath.close();

        float startRatio = mCornerRadius / (mCornerRadius + mShadowSize);
        mCornerShadowPaint.setShader(new RadialGradient(0, 0, mCornerRadius + mShadowSize,
                new int[]{mShadowStartColor, mShadowStartColor, mShadowEndColor},
                new float[]{0f, startRatio, 1f}, Shader.TileMode.CLAMP));
        // we offset the content shadowSize/2 pixels up to make it more realistic.
        // this is why edge shadow shader has some extra space
        // When drawing bottom edge shadow, we use that extra space.
        mEdgeShadowPaint.setShader(new LinearGradient(0, -mCornerRadius + mShadowSize, 0,
                -mCornerRadius - mShadowSize,
                new int[]{mShadowStartColor, mShadowStartColor, mShadowEndColor},
                new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP));

        mEdgeShadowPaint.setAntiAlias(false);
    }

    private void buildComponents(Rect bounds) {
        // Card is offset SHADOW_MULTIPLIER * maxShadowSize to account for the shadow shift.
        // We could have different top-bottom offsets to avoid extra gap above but in that case
        // center aligning Views inside the CardView would be problematic.
        float vTopOff = 0, vBottomOff = 0;
        final float verticalOffset = mRawMaxShadowSize * SHADOW_MULTIPLIER;
        switch (mType) {
            case ALL:
                vTopOff = vBottomOff = verticalOffset;
                break;
            case TOP:
                vTopOff = verticalOffset;
                vBottomOff = 0;
                break;
            case BOTTOM:
                vTopOff = 0;
                vBottomOff = verticalOffset;
                break;
        }

        mCardBounds.set(bounds.left + mRawMaxShadowSize, bounds.top + vTopOff,
                bounds.right - mRawMaxShadowSize, bounds.bottom - vBottomOff);
        buildShadowCorners();
    }

    float getCornerRadius() {
        return mCornerRadius;
    }

    void getMaxShadowAndCornerPadding(Rect into) {
        getPadding(into);
    }

    void setShadowSize(float size) {
        setShadowSize(size, mRawMaxShadowSize);
    }

    void setMaxShadowSize(float size) {
        setShadowSize(mRawShadowSize, size);
    }

    float getShadowSize() {
        return mRawShadowSize;
    }

    float getMaxShadowSize() {
        return mRawMaxShadowSize;
    }

    float getMinWidth() {
        final float content = 2 * Math.max(mRawMaxShadowSize,
                mCornerRadius + mInsetShadow + mRawMaxShadowSize / 2);
        return content + (mRawMaxShadowSize + mInsetShadow) * 2;
    }

    float getMinHeight() {
        final float content = 2 * Math.max(mRawMaxShadowSize,
                mCornerRadius + mInsetShadow + mRawMaxShadowSize * SHADOW_MULTIPLIER / 2);
        return content + (mRawMaxShadowSize * SHADOW_MULTIPLIER + mInsetShadow) * 2;
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        invalidateSelf();
    }

    static interface RoundRectHelper {
        void drawRoundRect(Canvas canvas, RectF bounds, float cornerRadius, Paint paint, TYPE type);
    }
}
