package io.rftp.redditclient;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Copyright (c) 2016-present, RFTP Technologies Ltd.
 * All rights reserved.
 * <p>
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

class RedditListAdapter extends RecyclerView.Adapter<RedditListAdapter.ViewHolder> {
  private JSONArray data;

  private final OnRedditClickListener onClickListener;

  RedditListAdapter(OnRedditClickListener onClickListener) {
    data = new JSONArray();
    this.onClickListener = onClickListener;
  }

  void resetData(JSONArray data) {
    this.data = data;
    notifyDataSetChanged();
  }

  JSONArray getData() {
    return data;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reddit_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, int position) {
    try {
      //extract
      JSONObject item = data.getJSONObject(position);
      JSONObject itemData = item.getJSONObject("data");
      String title = itemData.getString("title");
      String author = itemData.getString("author");
      int numComments = itemData.getInt("num_comments");
      String thumbnailUrl = itemData.getString("thumbnail");

      long createdUtc = itemData.getLong("created_utc");
      long time = System.currentTimeMillis();

      long ago4 = (time - createdUtc * 1000) / TimeUnit.HOURS.toMillis(1);

      //set
      holder.authorTV.setText(wrapAuthor(author));
      holder.titleTV.setText(title);
      holder.commentsCountTV.setText(wrapCommentsCount(numComments));
      holder.timeAgoTV.setText(wrapTimeAgo(ago4));
      Picasso.with(holder.itemView.getContext()).load(thumbnailUrl)
          .error(R.drawable.no_image).into(holder.thumbnailIV);

      holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          try {
            JSONObject item = data.getJSONObject(holder.getAdapterPosition());
            JSONObject itemData = item.getJSONObject("data");
            String url = itemData.getString("url");

            if (getPostTypeHint(itemData).equals("image")) {
              Toast.makeText(holder.itemView.getContext(), "It is image", Toast.LENGTH_SHORT).show();
              onClickListener.openImage(url);
            } else {
              onClickListener.openUrl(url);
            }

          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      });
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private String getPostTypeHint(JSONObject itemData) {
    return itemData.optString("post_hint", "");
  }

  private String wrapTimeAgo(long timeAgo) {
    int HOURS_IN_DAY = 24;
    String res;
    if (timeAgo <= 1) {
      res =  timeAgo + " hour ago";
    } else if (timeAgo <= HOURS_IN_DAY) {
      res = timeAgo + " hours ago";
    } else if ((timeAgo / HOURS_IN_DAY) >= 1) {
      res = (timeAgo / HOURS_IN_DAY) + " days ago";
    } else {
      res = (timeAgo / HOURS_IN_DAY) + " day ago";
    }
    return res;
  }

  private String wrapAuthor(String author) {
    return "by " + author;
  }

  private String wrapCommentsCount(int numComments) {
    return String.valueOf(numComments) + " comments";
  }

  @Override
  public int getItemCount() {
    return data.length();
  }

  class ViewHolder extends RecyclerView.ViewHolder {

    final TextView titleTV;
    final TextView commentsCountTV;
    final TextView timeAgoTV;
    final TextView authorTV;
    final ImageView thumbnailIV;

    ViewHolder(View itemView) {
      super(itemView);
      titleTV = (TextView)itemView.findViewById(R.id.itemTitleTV);
      commentsCountTV = (TextView)itemView.findViewById(R.id.itemCommentsCountTV);
      timeAgoTV = (TextView)itemView.findViewById(R.id.itemTimeAgoTV);
      authorTV = (TextView)itemView.findViewById(R.id.itemAuthorTV);
      thumbnailIV = (ImageView)itemView.findViewById(R.id.itemThumbnailIV);
    }
  }

  interface OnRedditClickListener {
    void openUrl(String url);
    void openImage(String url);
  }
}
