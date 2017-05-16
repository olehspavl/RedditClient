package io.rftp.redditclient;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Arrays;
import java.util.Stack;

import io.rftp.redditclient.ApiHandler.Command;

public class MainActivity extends AppCompatActivity {

  private RedditListFragment mBackPressedFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (savedInstanceState == null) {
      mBackPressedFragment = createFragment();
      mBackPressedFragment.setArguments(getIntent().getExtras());
      getSupportFragmentManager().beginTransaction()
          .add(R.id.fragmentContainer, mBackPressedFragment)
          .commit();
    } else {
      mBackPressedFragment = (RedditListFragment)getSupportFragmentManager().getFragments().get(0);
    }
  }

  private RedditListFragment createFragment() {
    return new RedditListFragment();
  }

  @Override
  public void onBackPressed() {
    mBackPressedFragment.onBackPressed();
  }

  public static class RedditListFragment extends Fragment {

    //constants
    private static final String TAG = RedditListFragment.class.getSimpleName();
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 14;
    private static final String STATE_PREV_ID = "prev_id";
    private static final String STATE_NEXT_ID = "next_id";
    private static final String STATE_PAGE_NUMBER = "page_number";
    private static final String STATE_ADAPTER_DATA = "adapter_data";
    //view
    private RecyclerView mRedditRV;
    private ProgressBar mProgressBar;
    //adapter
    private RedditListAdapter mAdapter;

    private PagerController mPagerController;

    private Stack<Command> mHistory;

    //callbacks
    private ApiHandler.OnResponse mOnResponseCallback;
    private RedditListAdapter.OnRedditClickListener mOnPostClicked;
    private View.OnClickListener mPagingButtonsListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      mHistory = new Stack<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
      View root = inflater.inflate(R.layout.fragment_reddit_list, container, false);

      initCallbacks();
      initProgressBar(root);
      setupRedditRecyclerView(root);
      setupPagingView(root);
      setupPagerController(root);

      return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);

      if (savedInstanceState != null) {
        restoreFragmentState(savedInstanceState);
      } else {
        onNextRequired();
      }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      //save pageNum
      outState.putInt(STATE_PAGE_NUMBER, mPagerController.currentPageNum);
      //save Adapter data: data, nextId, prevId
      outState.putString(STATE_ADAPTER_DATA, mAdapter.getData().toString());
      outState.putString(STATE_NEXT_ID, mPagerController.getNextPostId());
      outState.putString(STATE_PREV_ID, mPagerController.getPrevPostId());
      //save navigation stack
      Command[] history = mHistory.toArray(new Command[0]);
      outState.putParcelableArray("history", history);
    }

    public void onBackPressed() {
      if (!mHistory.isEmpty() && mHistory.size() > 1) {
        Command reverseCommand = mHistory.pop();
        ApiHandler.executeCommand(reverseCommand, mOnResponseCallback);
      }
    }

    private void initProgressBar(View root) {
      mProgressBar = (ProgressBar)root.findViewById(R.id.progressBar);
    }

    private void setupRedditRecyclerView(View root) {
      mRedditRV = (RecyclerView)root.findViewById(R.id.redditRV);
      mAdapter = new RedditListAdapter(mOnPostClicked);
      mRedditRV.setAdapter(mAdapter);
    }

    private void setupPagingView(View root) {
      TextView prevTV = (TextView)root.findViewById(R.id.pagingPrevTV);
      TextView nextTV = (TextView)root.findViewById(R.id.pagingNextTV);

      prevTV.setOnClickListener(mPagingButtonsListener);
      nextTV.setOnClickListener(mPagingButtonsListener);
    }

    private void setupPagerController(View root) {
      mPagerController = new PagerController(root);
    }

    private boolean isDataAvailable(JSONArray data) {
      return data != null;
    }

    private void updateViewState(JSONArray data, String prevItemId, String nextItemId,
                                 boolean isDirAscend, boolean isHistoryAscend) {
      mAdapter.resetData(data);
      int infoPageNum = mPagerController.currentPageNum;
      mPagerController.update(nextItemId, prevItemId, isDirAscend);
      mPagerController.setButtonsEnable(true);
      mProgressBar.setVisibility(View.GONE);

      if (isHistoryAscend) {
        Command reverseCommand = isDirAscend ?
            createPrevCommand(false) :
            createNextCommand(false);
        mHistory.add(reverseCommand);
      }
      Log.i(TAG, "Page from " + infoPageNum + " to " + mPagerController.currentPageNum);
    }

    private void onNextRequired() {
      Command nextCommand = createNextCommand(true);
      mPagerController.setButtonsEnable(false);
      mProgressBar.setVisibility(View.VISIBLE);
      ApiHandler.executeCommand(nextCommand, mOnResponseCallback);
    }

    @NonNull
    private Command createNextCommand(boolean isHistoryAscend) {
      String nextToPostId = mPagerController.getNextPostId();
      return new Command(nextToPostId, null, true, isHistoryAscend);
    }

    private void onPrevRequired() {
      Command prevCommand = createPrevCommand(true);
      mPagerController.setButtonsEnable(false);
      mProgressBar.setVisibility(View.VISIBLE);
      ApiHandler.executeCommand(prevCommand, mOnResponseCallback);
    }

    @NonNull
    private Command createPrevCommand(boolean isHistoryAscend) {
      String prevToPostId = mPagerController.getPrevPostId();
      return new Command(null, prevToPostId, false, isHistoryAscend);
    }

    private void restoreFragmentState(Bundle savedInstanceState) {
      String prevId = savedInstanceState.getString(STATE_PREV_ID);
      String nextId = savedInstanceState.getString(STATE_NEXT_ID);
      String data = savedInstanceState.getString(STATE_ADAPTER_DATA);
      int pageNum = savedInstanceState.getInt(STATE_PAGE_NUMBER);

      Command[] history = (Command[]) savedInstanceState.getParcelableArray("history");
      assert history != null;
      assert data != null;

      JSONArray parsedData = null;
      try {
        parsedData = new JSONArray(data);
      } catch (JSONException e) {
        e.printStackTrace();
      }

      if (isDataAvailable(parsedData)) {
        mHistory.addAll(Arrays.asList(history));
        mAdapter.resetData(parsedData);
        mPagerController.restore(nextId, prevId, pageNum);
      }
    }

    private void initCallbacks() {
      mPagingButtonsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (mPagerController.isTransitionAvailable(v)) {

            String msg;
            if (v.getId() == R.id.pagingPrevTV) {
              onPrevRequired();
              msg = "PREV";
            } else {
              onNextRequired();
              msg = "NEXT";
            }
            Log.i(TAG, msg + " button was clicked");
          }

        }
      };
      mOnResponseCallback = new ApiHandler.OnResponse() {
        @Override
        public void onSuccess(JSONArray data, String prevItemId, String nextItemId, boolean isDirAscend, boolean isHistoryAscend) {
          updateViewState(data, prevItemId, nextItemId, isDirAscend, isHistoryAscend);
        }

        @Override
        public void onFailure(String message) {
          Toast.makeText(getActivity(), "Request failed. Reason: " + message, Toast.LENGTH_SHORT).show();
        }
      };

      mOnPostClicked = new RedditListAdapter.OnRedditClickListener() {
        @Override
        public void openUrl(String url) {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setData(Uri.parse(url));
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
        }

        @Override
        public void openImage(final String url) {

          if (needPermissions()) {
            return;
          }

          new AsyncTask<String, Void, Uri>() {

            @Override
            protected Uri doInBackground(String... params) {

              String url = params[0];
              return FileUtils.convertImageUrlToUri(getActivity(), url);
            }

            @Override
            protected void onPostExecute(Uri uri) {
              super.onPostExecute(uri);

              if (uri != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                getActivity().grantUriPermission(getActivity().getApplicationContext().getPackageName(),
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setData(uri);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(intent);
              } else {
                Toast.makeText(getActivity(), "Somehow file wasn't saved", Toast.LENGTH_SHORT).show();
                openUrl(url);
              }
            }
          }.execute(url);

        }
      };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      switch (requestCode) {
        case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE: {
          if (grantResults.length > 0
              && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            //to not create more variables - ask user to click on post again
            Toast.makeText(getActivity(), "Nice! You are the best! And now click on any Post you like to =)", Toast.LENGTH_LONG).show();

          } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showMessage("You need to allow access to this permission to watch image Posts",
                    new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        requestPermissionsAfterSdkM();
                      }
                    });
              } else {
                Toast.makeText(getActivity(), "Have no permissions", Toast.LENGTH_SHORT).show();
              }
            }
          }
        }
      }
    }

    private void showMessage(String message, DialogInterface.OnClickListener okListener) {
      new AlertDialog.Builder(getActivity())
          .setMessage(message)
          .setPositiveButton("OK", okListener)
          .setNegativeButton("Cancel", null)
          .create()
          .show();
    }

    private void requestPermissionsAfterSdkM() {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
            PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
      }
    }

    private boolean isPermissionsGranted() {
      return ContextCompat.checkSelfPermission(getActivity(),
          Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean shouldShowRequestPermissionRationale() {
      return ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
          Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private boolean needPermissions() {
      if (!isPermissionsGranted()) {
        if (shouldShowRequestPermissionRationale()) {
          Toast.makeText(getActivity(), "I want to save image for you!", Toast.LENGTH_SHORT).show();
        }
        requestPermissionsAfterSdkM();
      } else {
        return false;
      }
      return true;
    }
  }
}
