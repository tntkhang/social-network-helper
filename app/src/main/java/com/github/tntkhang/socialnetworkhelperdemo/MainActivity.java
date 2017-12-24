package com.github.tntkhang.socialnetworkhelperdemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.github.tntkhang.socialnetworkhelper.FBCallBackListener;
import com.github.tntkhang.socialnetworkhelper.FacebookHelper;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.models.Media;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.MediaService;
import com.twitter.sdk.android.core.services.StatusesService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fabric.sdk.android.Fabric;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;

public class MainActivity extends Activity {
    @BindView(R.id.checkBox)
    CheckBox mChkboxContainImage;
    @BindView(R.id.btnPostFb)
    Button btnPostFb;
    @BindView(R.id.btnPostTw)
    Button btnPostTw;
    @BindView(R.id.edtInputText)
    EditText edtInputText;

    public static final String KEY_IS_MY_SET_MENU = "KEY_IS_MY_SET_MENU";
    public static final String KEY_MOVIE_SET_ID = "KEY_MOVIE_SET_ID";
    public static final String KEY_POST_TYPE = "KEY_POST_TYPE";
    public static final String KEY_FEEDBACK_STRING = "KEY_FEEDBACK_STRING";

    private static final String PERMISSION = "publish_actions";
    private static final int REQUEST_CODE_TAKE_IMAGE = 123;
    private static final int REQUEST_CODE_HASH_TAG = 124;
    public static final String BITMAP_IMAGE = "BITMAP_IMAGE";

    private Bitmap mPostBitmap;
    private String mPostBMPPath;

    private PendingAction pendingAction = PendingAction.NONE;
    private TwitterAuthClient mTWLoginClient;
    private CallbackManager mFBCallbackManager;
    private FacebookCallback mFbLoginCallBack = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            handlePendingAction();
            Log.i("tntkhang", "mFbLoginCallBack onSuccess ");
        }

        @Override
        public void onCancel() {
            if (pendingAction != PendingAction.NONE) {
                pendingAction = PendingAction.NONE;
            }
            Log.i("tntkhang", "mFbLoginCallBack onCancel");
        }

        @Override
        public void onError(FacebookException exception) {
            if (pendingAction != PendingAction.NONE
                    && exception instanceof FacebookAuthorizationException) {
                pendingAction = PendingAction.NONE;
            }
            Log.i("tntkhang", "mFbLoginCallBack onError " + exception.getMessage());
        }
    };
    private Callback mTWLoginCallBack = new Callback<TwitterSession>() {
        @Override
        public void success(Result<TwitterSession> result) {
            handlePendingAction();
            Log.i("tntkhang", "mTWPostCallBack onSuccess ");
        }

        @Override
        public void failure(TwitterException exception) {
            if (pendingAction != PendingAction.NONE) {
                pendingAction = PendingAction.NONE;
            }
            Log.i("tntkhang", "mTWPostCallBack failure " + exception.getMessage());
        }
    };
    private GraphRequest.Callback mFbPostCallBack = new GraphRequest.Callback() {

        @Override
        public void onCompleted(GraphResponse response) {
            if (response.getError() == null) {
                Log.i("tntkhang", "mFbPostCallBack onSuccess ");
            } else {
                Log.i("tntkhang", "mTWPostCallBack failure " + response.getError().getErrorMessage());
            }
        }
    };
    private Callback mTWPostCallBack = new Callback<TwitterSession>() {
        @Override
        public void success(Result<TwitterSession> result) {
            Log.i("tntkhang", "mTWPostCallBack onSuccess ");
        }

        @Override
        public void failure(TwitterException exception) {
            if (pendingAction != PendingAction.NONE) {
                pendingAction = PendingAction.NONE;
            }
            Log.i("tntkhang", "mTWPostCallBack failure " + exception.getMessage());
        }
    };

    private enum PendingAction {
        NONE,
        FB_POST_PHOTO,
        FB_POST_STATUS,
        TW_POST_PHOTO,
        TW_POST_STATUS
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        FacebookSdk.sdkInitialize(this);

        TwitterAuthConfig authConfig = new TwitterAuthConfig(getString(R.string.twitter_consumer_key), getString(R.string.twitter_consumer_secret));
        Fabric.with(this, new Twitter(authConfig));

        mFBCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mFBCallbackManager, mFbLoginCallBack);
        mTWLoginClient = new TwitterAuthClient();

        FacebookHelper.initHelper(this);

        FacebookHelper.getInstance().setFBCallBackListener(new FBCallBackListener() {
            @Override
            public void onLoginRevokeSuccess() {
                Log.i("tntkhang", "FacebookHelper.getInstance() Sucess");
            }

            @Override
            public void onLoginRevokeFail() {
                Log.e("tntkhang", "FacebookHelper.getInstance() fail");
            }

            @Override
            public void onLoginRevokeCancel() {

            }

            @Override
            public void onFBPostSuccess() {
                Log.i("tntkhang", "FacebookHelper.getInstance() onFBPostSuccess ");
            }

            @Override
            public void onFBPostFail() {
                Log.e("tntkhang", "FacebookHelper.getInstance() onFBPostFail ");
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Delete temp file
//            File tempFile = new File(mPostBMPPath);
//            if (tempFile.exists()) {
//                if (tempFile.delete()) {
//                    // Notify Gallery App Update
////                    ImageUtils.refreshGallery(this, mPostBMPPath);
//                }
//            }
        super.onDestroy();
    }

    @OnClick(R.id.btnPostFb)
    public void onBtnPostFbClick() {
        FacebookHelper.getInstance().postStatus("Hello from Library");
    }
    @OnClick(R.id.btnPostTw)
    public void onBtnPostTwClick() {
        onTWClickPost();
    }

    private File fileFromBitmap(Bitmap bm) {
        try {
            //create a file to write bitmap data
            File f = new File(getCacheDir(), "tempfile.png");
            f.createNewFile();

            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapdata = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();

            return f;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mFBCallbackManager.onActivityResult(requestCode, resultCode, data);
        mTWLoginClient.onActivityResult(requestCode, resultCode, data);

    }


    private void onTWClickPost() {
        if (mPostBitmap == null) {
            onTWClickPostStatus();
        } else {
            onTWClickPostPhoto();
        }
    }

    private void onTWClickPostStatus() {
        performPublish(PendingAction.TW_POST_STATUS);
    }

    private void onTWClickPostPhoto() {
        performPublish(PendingAction.TW_POST_PHOTO);
    }

    private void postTWStatus(String message) {
        TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
        if (session != null) {
            StatusesService service = TwitterCore.getInstance().getApiClient().getStatusesService();
            service.update(message, null, null, null, null, null, null, null, null).enqueue(mTWPostCallBack);
        } else {
            pendingAction = PendingAction.TW_POST_STATUS;
            mTWLoginClient.authorize(this, mTWLoginCallBack);
        }
    }

    private void postTWPhoto(final String message) {
        File file = fileFromBitmap(mPostBitmap);
        TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
        if (session != null) {
            TwitterApiClient twitterApiClient = TwitterCore.getInstance().getApiClient(session);
            MediaService ms = twitterApiClient.getMediaService();
            MediaType type = MediaType.parse("image/*");
            RequestBody body = RequestBody.create(type, file);
            Call<Media> mediaCall = ms.upload(body, null, null);
            mediaCall.enqueue(new Callback<Media>() {
                @Override
                public void failure(TwitterException exception) {
                    Log.d("failure", "mediaUpload");
                }

                @Override
                public void success(Result<Media> result) {
                    if (!TextUtils.isEmpty(message)) {
                        // EditText is not empty
                        // Check if char count is exceeding 140 limit
                        if (message.length() > 140) {
                            Log.e("tntkhang", "You have exceeded the 140 character limit");
                            return;
                        }
                        StatusesService statusesService = TwitterCore.getInstance().getApiClient().getStatusesService();
                        Call<Tweet> tweetCall = statusesService.update(message, null, false,
                                null, null, null,
                                true, false,
                                result.data.mediaIdString
                        );
                        tweetCall.enqueue(mTWPostCallBack);
                    }

                }
            });
        } else {
            pendingAction = PendingAction.TW_POST_PHOTO;
            mTWLoginClient.authorize(this, mTWLoginCallBack);
        }

    }


    private boolean hasPublishPermission() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null && accessToken.getPermissions().contains("publish_actions");
    }

    private void performPublish(PendingAction action) {
        pendingAction = action;
        handlePendingAction();
    }

    private void handlePendingAction() {
        String message = edtInputText.getText().toString().trim();
        PendingAction previouslyPendingAction = pendingAction;
        switch (previouslyPendingAction) {
            case NONE:
                break;
            case TW_POST_PHOTO:
                postTWPhoto(message);
                break;
            case TW_POST_STATUS:
                postTWStatus(message);
                break;
        }
    }
}