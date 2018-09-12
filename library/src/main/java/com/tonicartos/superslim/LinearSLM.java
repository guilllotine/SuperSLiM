package com.tonicartos.superslim;

import android.view.View;

public class LinearSLM extends SectionLayoutManager {

    public static int ID = LayoutManager.SECTION_MANAGER_LINEAR;

    public LinearSLM(LayoutManager layoutManager) {
        super(layoutManager);
    }

    @Override
    public int computeHeaderOffset(int firstVisiblePosition, SectionData sd, LayoutState state) {
        /*
         * Work from an assumed overlap and add heights from the start until the overlap is zero or
         * less, or the current position (or max items) is reached.
         */

        int areaAbove = 0;
        for (int position = sd.firstPosition + 1;
                areaAbove < sd.getHeaderSize() && position < firstVisiblePosition;
                position++) {
            // Look to see if the header overlaps with the displayed area of the mSection.
            LayoutState.View child = state.getView(position);
            measureChild(child, sd);

            areaAbove += mLayoutManager.isVerticalOrientation()
                    ? mLayoutManager.getDecoratedMeasuredHeight(child.view)
                    : mLayoutManager.getDecoratedMeasuredWidth(child.view);
            state.cacheView(position, child.view);
        }

        if (areaAbove == sd.getHeaderSize()) {
            return 0;
        } else if (areaAbove > sd.getHeaderSize()) {
            return 1;
        } else {
            return -areaAbove;
        }
    }

    @Override
    public int fillToEnd(int leadingEdge, int markerLine, int anchorPosition, SectionData sd,
            LayoutState state) {
        final int itemCount = state.getRecyclerState().getItemCount();

        for (int i = anchorPosition; i < itemCount; i++) {
            if (markerLine >= leadingEdge) {
                break;
            }

            LayoutState.View next = state.getView(i);
            LayoutManager.LayoutParams params = next.getLayoutParams();
            if (params.getTestedFirstPosition() != sd.firstPosition) {
                state.cacheView(i, next.view);
                break;
            }

            measureChild(next, sd);
            markerLine = layoutChild(next, markerLine, LayoutManager.Direction.END, sd, state);
            addView(next, i, LayoutManager.Direction.END, state);
        }

        return markerLine;
    }

    @Override
    public int fillToStart(int leadingEdge, int markerLine, int anchorPosition, SectionData sd,
            LayoutState state) {
        // Check to see if we have to adjust for minimum section height. We don't if there is an
        // attached non-header view in this section.
        boolean applyMinHeight = false;
        for (int i = 0; i < state.getRecyclerState().getItemCount(); i++) {
            View check = mLayoutManager.getChildAt(0);
            if (check == null) {
                applyMinHeight = false;
                break;
            }

            LayoutManager.LayoutParams checkParams =
                    (LayoutManager.LayoutParams) check.getLayoutParams();
            if (checkParams.getTestedFirstPosition() != sd.firstPosition) {
                applyMinHeight = true;
                break;
            }

            if (!checkParams.isHeader) {
                applyMinHeight = false;
                break;
            }
        }

        // Work out offset to marker line by measuring items from the end. If section height is less
        // than min height, then adjust marker line and then lay out items.
        int measuredPositionsMarker = -1;
        int sectionSize = 0;
        int minSizeOffset = 0;
        boolean isVertical = mLayoutManager.isVerticalOrientation();
        if (applyMinHeight) {
            for (int i = anchorPosition; i >= 0; i--) {
                LayoutState.View measure = state.getView(i);
                state.cacheView(i, measure.view);
                LayoutManager.LayoutParams params = measure.getLayoutParams();
                if (params.getTestedFirstPosition() != sd.firstPosition) {
                    break;
                }

                if (params.isHeader) {
                    continue;
                }

                measureChild(measure, sd);
                if (isVertical) {
                    sectionSize += mLayoutManager.getDecoratedMeasuredHeight(measure.view);
                } else {
                    sectionSize += mLayoutManager.getDecoratedMeasuredWidth(measure.view);
                }
                measuredPositionsMarker = i;
                if (sectionSize >= sd.getMinimumSize()) {
                    break;
                }
            }

            if (sectionSize < sd.getMinimumSize()) {
                minSizeOffset = sectionSize - sd.getMinimumSize();
                markerLine += minSizeOffset;
            }
        }

        for (int i = anchorPosition; i >= 0; i--) {
            if (markerLine - minSizeOffset <= leadingEdge) {
                break;
            }

            LayoutState.View next = state.getView(i);
            LayoutManager.LayoutParams params = next.getLayoutParams();
            if (params.isHeader) {
                state.cacheView(i, next.view);
                break;
            }
            if (params.getTestedFirstPosition() != sd.firstPosition) {
                state.cacheView(i, next.view);
                break;
            }

            if (!applyMinHeight || i < measuredPositionsMarker) {
                measureChild(next, sd);
            } else {
                state.decacheView(i);
            }
            markerLine = layoutChild(next, markerLine, LayoutManager.Direction.START, sd, state);
            addView(next, i, LayoutManager.Direction.START, state);
        }

        return markerLine;
    }

    @Override
    public int finishFillToEnd(int leadingEdge, View anchor, SectionData sd, LayoutState state) {
        final int anchorPosition = mLayoutManager.getPosition(anchor);
        int markerLine;
        if (mLayoutManager.isVerticalOrientation()) {
            markerLine = mLayoutManager.getDecoratedBottom(anchor);
        } else {
            markerLine = mLayoutManager.getDecoratedRight(anchor);
        }

        return fillToEnd(leadingEdge, markerLine, anchorPosition + 1, sd, state);
    }

    @Override
    public int finishFillToStart(int leadingEdge, View anchor, SectionData sd, LayoutState state) {
        final int anchorPosition = mLayoutManager.getPosition(anchor);
        int markerLine;
        if (mLayoutManager.isVerticalOrientation()) {
            markerLine = mLayoutManager.getDecoratedTop(anchor);
        } else {
            markerLine = mLayoutManager.getDecoratedLeft(anchor);
        }
        return fillToStart(leadingEdge, markerLine, anchorPosition - 1, sd, state);
    }

    private int layoutChild(LayoutState.View child, int markerLine,
            LayoutManager.Direction direction, SectionData sd, LayoutState state) {
        boolean isVertical = mLayoutManager.isVerticalOrientation();
        final int height = mLayoutManager.getDecoratedMeasuredHeight(child.view);
        final int width = mLayoutManager.getDecoratedMeasuredWidth(child.view);

        int left = isVertical ? state.isLTR ? sd.contentStart : sd.contentEnd : 0;
        int right = isVertical ? left + width : 0;
        int top = isVertical ? 0 : state.isLTR ? sd.contentStart : sd.contentEnd;
        int bottom = isVertical ? 0 : top + height;

        if (direction == LayoutManager.Direction.END) {
            if (isVertical) {
                top = markerLine;
                bottom = top + height;
            } else {
                left = markerLine;
                right = left + width;
            }
        } else {
            if (isVertical) {
                bottom = markerLine;
                top = bottom - height;
            } else {
                right = markerLine;
                left = right - width;
            }
        }
        mLayoutManager.layoutDecorated(child.view, left, top, right, bottom);

        if (direction == LayoutManager.Direction.END) {
            if (isVertical) {
                markerLine = mLayoutManager.getDecoratedBottom(child.view);
            } else {
                markerLine = mLayoutManager.getDecoratedRight(child.view);
            }
        } else {
            if (isVertical) {
                markerLine = mLayoutManager.getDecoratedTop(child.view);
            } else {
                markerLine = mLayoutManager.getDecoratedLeft(child.view);
            }
        }

        return markerLine;
    }

    private void measureChild(LayoutState.View child, SectionData sd) {
        mLayoutManager.measureChildWithMargins(child.view, sd.getTotalMarginWidth(), 0);
    }
}
