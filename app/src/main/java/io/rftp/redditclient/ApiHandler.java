package io.rftp.redditclient;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Copyright (c) 2016-present, RFTP Technologies Ltd.
 * All rights reserved.
 * <p>
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

class ApiHandler {

  private static final String TAG = ApiHandler.class.getSimpleName();
  private static final int QUERY_COUNT = 1;
  private static final int QUERY_LIMIT = 10;

  private static long queryCount = 0;

  static void executeCommand(final Command command, final OnResponse callback) {

    if (command.isDirAscend) {
      queryCount += 10;
    } else {
      queryCount -= 10;
    }

    RedditService service = getRedditService();
    final Call<ResponseBody> call =
        service.getRedditsList(QUERY_LIMIT, command.nextToPostId, command.prevToPostId, (int)queryCount + QUERY_COUNT);

    String msg = call.request().toString();
    Log.i(TAG, "Request is " + msg);

    call.enqueue(new Callback<ResponseBody>() {
      @Override
      public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        try {
          //get data
          String responseString = response.body().string();
          JSONObject responseJSON = new JSONObject(responseString);

          JSONObject object = responseJSON.getJSONObject("data");
          String nextItemId = object.getString("after");
          String prevItemId = object.getString("before");
          JSONArray data = object.getJSONArray("children");

          Log.i(TAG, "Response: min - " + prevItemId + " max - " + nextItemId);

          boolean isDirAscend = command.isDirAscend;
          boolean isHistoryAscend = command.isHistoryAscend;

          //updateViewState
          callback.onSuccess(data, prevItemId, nextItemId, isDirAscend, isHistoryAscend);

        } catch (IOException | JSONException e) {
          callback.onFailure(e.getMessage());
          e.printStackTrace();
        }
      }

      @Override
      public void onFailure(Call<ResponseBody> call, Throwable t) {
        callback.onFailure(t.getMessage());
        t.printStackTrace();
      }
    });
  }

  interface OnResponse {
    void onSuccess(JSONArray data, String prevItemId, String nextItemId,
                   boolean isDirAscend, boolean isHistoryAscend);
    void onFailure(String message);
  }

  static class Command implements Parcelable {
    String nextToPostId;
    String prevToPostId;
    boolean isDirAscend;
    boolean isHistoryAscend;

    Command(String nextToPostId, String prevToPostId,
            boolean isDirAscend, boolean isHistoryAscend) {
      this.nextToPostId = nextToPostId;
      this.prevToPostId = prevToPostId;
      this.isDirAscend = isDirAscend;
      this.isHistoryAscend = isHistoryAscend;
    }

    protected Command(Parcel in) {
      nextToPostId = in.readString();
      prevToPostId = in.readString();
      isDirAscend = in.readByte() != 0;
      isHistoryAscend = in.readByte() != 0;
    }

    public static final Creator<Command> CREATOR = new Creator<Command>() {
      @Override
      public Command createFromParcel(Parcel in) {
        return new Command(in);
      }

      @Override
      public Command[] newArray(int size) {
        return new Command[size];
      }
    };

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(nextToPostId);
      dest.writeString(prevToPostId);
      dest.writeByte((byte) (isDirAscend ? 1 : 0));
      dest.writeByte((byte) (isHistoryAscend ? 1 : 0));
    }
  }

  private static RedditService getRedditService() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://www.reddit.com/")
        .build();
    return retrofit.create(RedditService.class);
  }

  interface RedditService {
    @GET("r/all/top.json")
    Call<ResponseBody> getRedditsList(@Query("limit") int limit,
                                      @Query("after") String after,
                                      @Query("before") String before,
                                      @Query("count") int count);
  }
}
