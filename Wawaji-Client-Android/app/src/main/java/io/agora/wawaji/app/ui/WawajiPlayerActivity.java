package io.agora.wawaji.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import io.agora.common.Constant;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.wawaji.app.R;
import io.agora.wawaji.app.model.AGEventHandler;
import io.agora.wawaji.app.model.ConstantApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WawajiPlayerActivity extends BaseActivity implements AGEventHandler {

    private final static Logger log = LoggerFactory.getLogger(WawajiPlayerActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wawaji_player);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    private boolean isBroadcaster(int cRole) {
        return cRole == Constants.CLIENT_ROLE_BROADCASTER;
    }

    private boolean isBroadcaster() {
        return isBroadcaster(config().mClientRole);
    }

    @Override
    protected void initUIandEvent() {
        event().addEventHandler(this);

        Intent i = getIntent();
        int cRole = i.getIntExtra(ConstantApp.ACTION_KEY_CROLE, 0);

        if (cRole == 0) {
            throw new RuntimeException("Should not reach here");
        }

        String roomName = i.getStringExtra(ConstantApp.ACTION_KEY_ROOM_NAME);

        doConfigEngine(cRole);

        if (isBroadcaster(cRole)) {
            worker().prepareWawaji();
            worker().ctrlWawaji(Constant.Wawaji_Ctrl_START);
        }

        worker().joinChannel(roomName, config().mUid);

        TextView textRoomName = (TextView) findViewById(R.id.room_name);
        textRoomName.setText(roomName);
    }

    private void doConfigEngine(int cRole) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int prefIndex = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, ConstantApp.DEFAULT_PROFILE_IDX);
        if (prefIndex > ConstantApp.VIDEO_PROFILES.length - 1) {
            prefIndex = ConstantApp.DEFAULT_PROFILE_IDX;
        }
        int vProfile = ConstantApp.VIDEO_PROFILES[prefIndex];

        worker().configEngine(cRole, vProfile);
    }

    @Override
    protected void deInitUIandEvent() {
        doLeaveChannel();
        event().removeEventHandler(this);
    }

    private void doLeaveChannel() {
        worker().leaveChannel(config().mChannel);
        if (isBroadcaster()) {
        }
    }

    public void onLeaveGameClicked(View view) {
        finish();
    }

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        doRenderRemoteUi(uid);
    }

    private void doRenderRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
                surfaceV.setZOrderOnTop(true);
                surfaceV.setZOrderMediaOverlay(true);
                if (config().mUid == uid) {
                    return;
                } else {
                    rtcEngine().setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                }

                FrameLayout container = (FrameLayout) findViewById(R.id.gaming_video);
                if (container.getChildCount() >= 2) {
                    return;
                }
                container.addView(surfaceV);

                config().mWawajiUid = uid;
            }
        });
    }

    @Override
    public void onJoinChannelSuccess(final String channel, final int uid, final int elapsed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                final boolean isBroadcaster = isBroadcaster();
                log.debug("onJoinChannelSuccess " + channel + " " + (uid & 0xFFFFFFFFL) + " " + elapsed + " " + isBroadcaster);

                worker().getEngineConfig().mUid = uid;
            }
        });
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        log.debug("onUserOffline " + (uid & 0xFFFFFFFFL) + " " + reason);
    }

    @Override
    public void onExtraInfo(int msg, Object... data) {
        if (isFinishing()) {
            return;
        }

        int intv;
        boolean boolv;

        switch (msg) {
            case Constant.Wawaji_Msg_TIMEOUT:
                intv = (Integer) data[0];
                break;
            case Constant.Wawaji_Msg_RESULT:
                boolv = (Boolean) data[0];
                if (boolv) {
                    showLongToast("Congrats, lucky day");
                } else {
                    showShortToast("Sorry oops");
                }
                finish();
                break;
            case Constant.Wawaji_Msg_FORCED_LOGOUT:
                showShortToast("Forced logout by others " + data[0]);
                finish();
                break;
        }
    }

    private void requestRemoteStreamType(int uid) {
        log.debug("requestRemoteStreamType " + (uid & 0xFFFFFFFFL));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        }, 500);
    }

    public void onCatcherBtnClicked(View view) {
        if (!isBroadcaster()) {
            showShortToast(getString(R.string.label_not_a_player));
            return;
        }

        worker().ctrlWawaji(Constant.Wawaji_Ctrl_CATCH);
    }

    public void onRightBtnClicked(View view) {
        if (!isBroadcaster()) {
            showShortToast(getString(R.string.label_not_a_player));
            return;
        }

        worker().ctrlWawaji(Constant.Wawaji_Ctrl_RIGHT);
    }

    public void onDownBtnClicked(View view) {
        if (!isBroadcaster()) {
            showShortToast(getString(R.string.label_not_a_player));
            return;
        }

        worker().ctrlWawaji(Constant.Wawaji_Ctrl_DOWN);
    }

    public void onUpBtnClicked(View view) {
        if (!isBroadcaster()) {
            showShortToast(getString(R.string.label_not_a_player));
            return;
        }

        worker().ctrlWawaji(Constant.Wawaji_Ctrl_UP);
    }

    public void onLeftBtnClicked(View view) {
        if (!isBroadcaster()) {
            showShortToast(getString(R.string.label_not_a_player));
            return;
        }

        worker().ctrlWawaji(Constant.Wawaji_Ctrl_LEFT);
    }

    public void onSwitchCameraClicked(View view) {
        if (!isBroadcaster()) {
            showShortToast(getString(R.string.label_not_a_player));
            return;
        }

        worker().ctrlWawaji(Constant.Wawaji_Ctrl_SWITCH_CAM);
    }
}
