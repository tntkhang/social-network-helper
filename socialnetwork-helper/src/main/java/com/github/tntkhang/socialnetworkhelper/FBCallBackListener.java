package com.github.tntkhang.socialnetworkhelper;

/**
 * Created by khang on 12/24/2017.
 */

public interface FBCallBackListener {
    void onLoginRevokeSuccess();

    void onLoginRevokeFail();

    void onLoginRevokeCancel();

    void onFBPostSuccess();

    void onFBPostFail();
}
