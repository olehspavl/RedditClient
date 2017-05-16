package io.rftp.redditclient;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Copyright (c) 2016-present, RFTP Technologies Ltd.
 * All rights reserved.
 * <p>
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

class PagerController {
  int currentPageNum = -1;
  private String nextPostId;
  private String prevPostId;
  private final ViewGroup indicatorsParent;
  private final TextView prevTV;
  private final TextView nextTV;

  PagerController(View parent) {
    this.indicatorsParent = (ViewGroup)parent.findViewById(R.id.pageIndicatorsLL);
    this.prevTV = (TextView)parent.findViewById(R.id.pagingPrevTV);
    this.nextTV = (TextView)parent.findViewById(R.id.pagingNextTV);
  }

  String getNextPostId() {
    return nextPostId;
  }

  String getPrevPostId() {
    return prevPostId;
  }

  boolean isTransitionAvailable(View pagingBtn) {
    switch (pagingBtn.getId()) {
      case R.id.pagingPrevTV:
        return currentPageNum > 0;
      case R.id.pagingNextTV:
        return currentPageNum < 4;
      default:
        return false;
    }
  }

  void restore(String nextPostId, String prevPostId, int currentPageNum) {
    this.nextPostId = nextPostId;
    this.prevPostId = prevPostId;
    this.currentPageNum = currentPageNum;
    changeFocusedIndicator();
  }

  void update(String nextPostId, String prevPostId, boolean isAscend) {
    this.nextPostId = nextPostId;
    this.prevPostId = prevPostId;
    if (isAscend) {
      incrementPageNum();
    } else {
      decrementPageNum();
    }
  }

  private void incrementPageNum() {
    currentPageNum++;
    changeFocusedIndicator();
  }

  private void decrementPageNum() {
    currentPageNum--;
    changeFocusedIndicator();
  }

  private void changeFocusedIndicator() {
    for (int idx = 0; idx < indicatorsParent.getChildCount(); ++idx) {
      int color;
      if (idx == currentPageNum) {
        color = ContextCompat.getColor(indicatorsParent.getContext(), R.color.colorAccent);
      } else {
        color = ContextCompat.getColor(indicatorsParent.getContext(), R.color.colorPrimary);
      }
      View child = indicatorsParent.getChildAt(idx);
      child.setBackgroundColor(color);
    }
  }

  void setButtonsEnable(boolean isEnable) {
    prevTV.setEnabled(isEnable);
    nextTV.setEnabled(isEnable);
  }
}
