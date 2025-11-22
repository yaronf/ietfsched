/*
 * Copyright 2011 Google Inc.
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

package org.ietf.ietfsched.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * Custom layout that arranges children in a grid-like manner, optimizing for even horizontal and
 * vertical whitespace.
 */
public class DashboardLayout extends ViewGroup {
    private static final String TAG = "DashboardLayout";
    private static final int UNEVEN_GRID_PENALTY_MULTIPLIER = 10;

    private int mMaxChildWidth = 0;
    private int mMaxChildHeight = 0;

    public DashboardLayout(Context context) {
        super(context, null);
    }

    public DashboardLayout(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public DashboardLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMaxChildWidth = 0;
        mMaxChildHeight = 0;

        // Measure once to find the maximum child size.

        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST);

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

            mMaxChildWidth = Math.max(mMaxChildWidth, child.getMeasuredWidth());
            mMaxChildHeight = Math.max(mMaxChildHeight, child.getMeasuredHeight());
            Log.d(TAG, "Child " + i + " measured: " + child.getMeasuredWidth() + "x" + child.getMeasuredHeight());
        }
        
        Log.d(TAG, "Max child size: " + mMaxChildWidth + "x" + mMaxChildHeight + ", available space: " + MeasureSpec.getSize(widthMeasureSpec) + "x" + MeasureSpec.getSize(heightMeasureSpec));

        // Measure again for each child to be exactly the same size.

        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                mMaxChildWidth, MeasureSpec.EXACTLY);
        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                mMaxChildHeight, MeasureSpec.EXACTLY);

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }

        setMeasuredDimension(
                resolveSize(mMaxChildWidth, widthMeasureSpec),
                resolveSize(mMaxChildHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;
        Log.d(TAG, "onLayout called: changed=" + changed + ", width=" + width + ", height=" + height);

        final int count = getChildCount();

        // Calculate the number of visible children.
        int visibleCount = 0;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            ++visibleCount;
        }

        if (visibleCount == 0) {
            return;
        }

        // Calculate what number of rows and columns will optimize for even horizontal and
        // vertical whitespace between items. Start with a 1 x N grid, then try 2 x N, and so on.
        int bestSpaceDifference = Integer.MAX_VALUE;
        int spaceDifference;

        // Horizontal and vertical space between items
        int hSpace = 0;
        int vSpace = 0;

        int cols = 1;
        int rows;
        int bestCols = -1; // Track best cols separately, -1 means none found yet

        while (true) {
            rows = (visibleCount - 1) / cols + 1;

            hSpace = ((width - mMaxChildWidth * cols) / (cols + 1));
            vSpace = ((height - mMaxChildHeight * rows) / (rows + 1));

            spaceDifference = Math.abs(vSpace - hSpace);
            if (rows * cols != visibleCount) {
                spaceDifference *= UNEVEN_GRID_PENALTY_MULTIPLIER;
            }
            // Heavily penalize layouts that don't fit (negative spacing)
            boolean fits = (hSpace >= 0 && vSpace >= 0);
            if (!fits) {
                spaceDifference = Integer.MAX_VALUE;
            }

            // Prefer more square-like grids (rows ≈ cols) by giving a bonus
            // Calculate how "square" this grid is (closer to 1.0 is better)
            float aspectRatio = (float) Math.max(cols, rows) / Math.min(cols, rows);
            // Bonus: reduce spaceDifference for more square grids (divide by aspect ratio)
            // This means a 2x3 grid (aspectRatio=1.5) gets a smaller penalty than 1x5 (aspectRatio=5.0)
            int adjustedSpaceDifference = (int) (spaceDifference / aspectRatio);
            
            // Add penalty for single-column layouts when there are more than 3 items
            // This encourages multi-column layouts for better visual balance
            if (cols == 1 && visibleCount > 3) {
                adjustedSpaceDifference += 1000; // Large penalty to discourage single column
            }
            
            Log.d(TAG, "Trying cols=" + cols + ", rows=" + rows + ", hSpace=" + hSpace + ", vSpace=" + vSpace + ", spaceDifference=" + spaceDifference + ", adjustedSpaceDifference=" + adjustedSpaceDifference + ", aspectRatio=" + aspectRatio + ", bestSpaceDifference=" + bestSpaceDifference + ", fits=" + fits);

            if (fits && (bestCols == -1 || adjustedSpaceDifference < bestSpaceDifference)) {
                // Found a better whitespace squareness/ratio that actually fits
                bestSpaceDifference = adjustedSpaceDifference;
                bestCols = cols; // Remember this as the best so far

                // If we found a better whitespace squareness and there's only 1 row, this is
                // the best we can do.
                if (rows == 1) {
                    Log.d(TAG, "Breaking because rows==1");
                    break;
                }
            }

            ++cols;
            // Safety: don't try more columns than we have items
            if (cols > visibleCount) {
                Log.d(TAG, "Breaking because cols > visibleCount");
                break;
            }
            
            // Also break if we've tried enough columns (e.g., more than sqrt of visibleCount * 2)
            if (cols > Math.sqrt(visibleCount) * 2 + 1) {
                Log.d(TAG, "Breaking because cols too high");
                break;
            }
        }
        
        // Use bestCols if we found one that fits, otherwise fallback to 1
        if (bestCols == -1) {
            Log.w(TAG, "No fitting layout found, using cols=1 as fallback");
            bestCols = 1;
        }
        cols = bestCols;

        // Recalculate spacing for final cols/rows to ensure we have correct values
        rows = (visibleCount - 1) / cols + 1;
        hSpace = ((width - mMaxChildWidth * cols) / (cols + 1));
        vSpace = ((height - mMaxChildHeight * rows) / (rows + 1));

        Log.d(TAG, "Grid calculation: cols=" + cols + ", rows=" + rows + ", visibleCount=" + visibleCount + ", hSpace=" + hSpace + ", vSpace=" + vSpace + ", mMaxChildWidth=" + mMaxChildWidth + ", mMaxChildHeight=" + mMaxChildHeight + ", width=" + width + ", height=" + height);

        // Lay out children based on calculated best-fit number of rows and cols.

        // If we chose a layout that has negative horizontal or vertical space, force it to zero.
        hSpace = Math.max(0, hSpace);
        vSpace = Math.max(0, vSpace);

        // Re-use width/height variables to be child width/height.
        // Ensure we don't divide by zero
        if (cols == 0) cols = 1;
        if (rows == 0) rows = 1;
        width = (width - hSpace * (cols + 1)) / cols;
        height = (height - vSpace * (rows + 1)) / rows;

        int left, top;
        int col, row;
        int visibleIndex = 0;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            row = visibleIndex / cols;
            col = visibleIndex % cols;

            left = hSpace * (col + 1) + width * col;
            top = vSpace * (row + 1) + height * row;

            child.layout(left, top,
                    (hSpace == 0 && col == cols - 1) ? r : (left + width),
                    (vSpace == 0 && row == rows - 1) ? b : (top + height));
            ++visibleIndex;
        }
    }
}
