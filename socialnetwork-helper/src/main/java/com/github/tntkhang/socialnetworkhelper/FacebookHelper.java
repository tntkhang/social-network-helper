package com.github.tntkhang.socialnetworkhelper;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Created by khang on 12/3/2017.
 */

public class FacebookHelper {

    private static FacebookHelper instance;
    private Activity activity;
    private static final String PERMISSION = "publish_actions";
    private PendingAction pendingAction;
    private FBCallBackListener mFBCallBackListener;
    private Bitmap mBmpPhoto;
    private String mStatus;

    private Boolean isRevokeAuthen = true;

    private FacebookHelper(Activity activity) {
        this.activity = activity;
        FacebookSdk.sdkInitialize(activity);
        CallbackManager callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, mFbLoginCallBack);
    }

    public static FacebookHelper initHelper(Activity activity) {
        if (instance == null) {
            instance = new FacebookHelper(activity);
            return instance;
        }
        return instance;
    }

    public static FacebookHelper getInstance() {
        if (instance == null) {
            throw new IllegalArgumentException("You must call initHelper(activity) before call getInstance for using");
        }
        return instance;
    }

    public void postStatus(String status) {
        performPublish(PendingAction.FB_POST_STATUS, status);
    }

    public void postImage(String status, Bitmap photo) {
        mBmpPhoto = photo;
        performPublish(PendingAction.FB_POST_PHOTO, status);
    }

    public void setFBCallBackListener(FBCallBackListener listener) {
        this.mFBCallBackListener = listener;
    }

    public void setRevokeAuthen(boolean isRevokeAuthen) {
        this.isRevokeAuthen = isRevokeAuthen;
    }
    private void performPublish(PendingAction action, String status) {
        pendingAction = action;
        mStatus = status;
        handlePendingAction();
    }

    private void handlePendingAction() {
//        PendingAction previouslyPendingAction = pendingAction;
        switch (pendingAction) {
            case NONE:
                break;
            case FB_POST_PHOTO:
                actionPostPhoto(mStatus, mBmpPhoto);
                break;
            case FB_POST_STATUS:
                actionPostStatus(mStatus);
                break;
        }
    }

    private void actionPostStatus(String message) {
        if (hasPublishPermission()) {
            Bundle params = new Bundle();
            params.putString("message", message);
            // make the API call
            new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "/me/feed",
                    params,
                    HttpMethod.POST,
                    mFbPostCallBack
            ).executeAsync();
        } else if (isRevokeAuthen) {
            pendingAction = PendingAction.FB_POST_STATUS;
            LoginManager.getInstance().logInWithPublishPermissions(activity, Arrays.asList(PERMISSION));
        }
    }

    private void actionPostPhoto(String message, Bitmap postBitmap) {
        if (hasPublishPermission()) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            postBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            final JSONObject graphObject = new JSONObject();

            GraphRequest request = GraphRequest.newPostRequest(AccessToken.getCurrentAccessToken(), "me/photos", graphObject, mFbPostCallBack);
            Bundle params = new Bundle();
            params.putString("caption", message);
            params.putByteArray("picture", byteArray);

            request.setParameters(params);
            request.executeAsync();
        } else if (isRevokeAuthen) {
            pendingAction = PendingAction.FB_POST_PHOTO;
            LoginManager.getInstance().logInWithPublishPermissions(activity, Arrays.asList(PERMISSION));
        }
    }

    private boolean hasPublishPermission() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null && accessToken.getPermissions().contains("publish_actions");
    }


    /*Init CallBack*/

    private FacebookCallback mFbLoginCallBack = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            handlePendingAction();
            if (mFBCallBackListener != null) {
                mFBCallBackListener.onLoginRevokeSuccess();
            }
            Log.i("tntkhang", "mFbLoginCallBack onSuccess ");
        }

        @Override
        public void onCancel() {
            if (pendingAction != PendingAction.NONE) {
                pendingAction = PendingAction.NONE;
            }
            if (mFBCallBackListener != null) {
                mFBCallBackListener.onLoginRevokeCancel();
            }
            Log.i("tntkhang", "mFbLoginCallBack onCancel");
        }

        @Override
        public void onError(FacebookException exception) {
            if (pendingAction != PendingAction.NONE
                    && exception instanceof FacebookAuthorizationException) {
                pendingAction = PendingAction.NONE;
            }
            if (mFBCallBackListener != null) {
                mFBCallBackListener.onLoginRevokeFail();
            }
            Log.i("tntkhang", "mFbLoginCallBack onError " + exception.getMessage());
        }
    };
    private GraphRequest.Callback mFbPostCallBack = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse response) {
            if (response.getError() == null) {
                if (mFBCallBackListener != null) {
                    mFBCallBackListener.onFBPostSuccess();
                }
                Log.i("tntkhang", "mFbPostCallBack onSuccess ");
            } else {
                if (mFBCallBackListener != null) {
                    mFBCallBackListener.onFBPostFail();
                }
                Log.i("tntkhang", "mTWPostCallBack failure " + response.getError().getErrorMessage());
            }
        }
    };
}
