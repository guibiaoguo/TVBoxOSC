package com.github.tvbox.osc.player.controller;

import static xyz.doikki.videoplayer.util.PlayerUtils.stringForTime;

import android.content.Context;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.owen.tvrecyclerview.widget.TvRecyclerView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.util.PlayerUtils;

/**
 * 直播控制器
 */

public class LiveController extends BaseController {
    protected ProgressBar mLoading;
    private int minFlingDistance = 100;             //最小识别距离
    private int minFlingVelocity = 10;              //最小识别速度

    public LiveController(@NonNull @NotNull Context context) {
        super(context);
        mHandlerCallback = new HandlerCallback() {
            @Override
            public void callback(Message msg) {
                switch (msg.what) {
                    case 1000: { // seek 刷新
                        mProgressRoot.setVisibility(VISIBLE);
                        break;
                    }
                    case 1001: { // seek 关闭
                        mProgressRoot.setVisibility(GONE);
                        break;
                    }
                    case 1002: { // 显示底部菜单
                        mBottomRoot.setVisibility(VISIBLE);
                        break;
                    }
                    case 1003: { // 隐藏底部菜单
                        mBottomRoot.setVisibility(GONE);
                        break;
                    }
                }
            }
        };
    }

    SeekBar mSeekBar;
    TextView mCurrentTime;
    TextView mTotalTime;
    boolean mIsDragging;
    LinearLayout mProgressRoot;
    TextView mProgressText;
    ImageView mProgressIcon;
    LinearLayout mBottomRoot;
    LinearLayout mParseRoot;
    TvRecyclerView mGridView;
    TextView mPlayTitle;
    TextView mNextBtn;
    TextView mPreBtn;
    TextView mPlayerScaleBtn;
    TextView mPlayerSpeedBtn;
    TextView mPlayerBtn;
    TextView mPlayerIJKBtn;
    TextView mPlayerRetry;
    TextView mPlayerTimeStartBtn;
    TextView mPlayerTimeSkipBtn;
    TextView mPlayerTimeStepBtn;

    @Override
    protected int getLayoutId() {
        return R.layout.player_live_control_view;
    }
    @Override
    protected void initView() {
        super.initView();
//        mLoading = findViewById(R.id.loading);
        mCurrentTime = findViewById(R.id.curr_time);
        mTotalTime = findViewById(R.id.total_time);
//        mPlayTitle = findViewById(R.id.tv_info_name);
        mSeekBar = findViewById(R.id.seekBar);
        mProgressRoot = findViewById(R.id.tv_progress_container);
        mProgressIcon = findViewById(R.id.tv_progress_icon);
        mProgressText = findViewById(R.id.tv_progress_text);
        mBottomRoot = findViewById(R.id.bottom_container);
//        mParseRoot = findViewById(R.id.parse_root);
//        mGridView = findViewById(R.id.mGridView);
        mPlayerRetry = findViewById(R.id.play_retry);
        mNextBtn = findViewById(R.id.play_next);
        mPreBtn = findViewById(R.id.play_pre);
        mPlayerScaleBtn = findViewById(R.id.play_scale);
        mPlayerSpeedBtn = findViewById(R.id.play_speed);
        mPlayerBtn = findViewById(R.id.play_player);
        mPlayerIJKBtn = findViewById(R.id.play_ijk);
        mPlayerTimeStartBtn = findViewById(R.id.play_time_start);
        mPlayerTimeSkipBtn = findViewById(R.id.play_time_end);
        mPlayerTimeStepBtn = findViewById(R.id.play_time_step);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }

                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * progress) / seekBar.getMax();
                if (mCurrentTime != null)
                    mCurrentTime.setText(stringForTime((int) newPosition));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDragging = true;
                mControlWrapper.stopProgress();
                mControlWrapper.stopFadeOut();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * seekBar.getProgress()) / seekBar.getMax();
                mControlWrapper.seekTo((int) newPosition);
                mIsDragging = false;
                mControlWrapper.startProgress();
                mControlWrapper.startFadeOut();
            }
        });
    }

    private boolean skipEnd = true;

    @Override
    protected void setProgress(int duration, int position) {
        if (mIsDragging) {
            return;
        }
        super.setProgress(duration, position);
        if (skipEnd && position != 0 && duration != 0) {
            int et = 0;
            if (et > 0 && position + (et * 1000) >= duration) {
                skipEnd = false;
//                listener.playNext(true);
            }
        }
        listener.setVideoSize(getPlayVideoSize());
        listener.setVideoUrl();
        mCurrentTime.setText(PlayerUtils.stringForTime(position));
        mTotalTime.setText(PlayerUtils.stringForTime(duration));
        if (duration > 0) {
            mSeekBar.setEnabled(true);
            int pos = (int) (position * 1.0 / duration * mSeekBar.getMax());
            mSeekBar.setProgress(pos);
        } else {
            mSeekBar.setEnabled(false);
        }
        int percent = mControlWrapper.getBufferedPercentage();
        if (percent >= 95) {
            mSeekBar.setSecondaryProgress(mSeekBar.getMax());
        } else {
            mSeekBar.setSecondaryProgress(percent * 10);
        }
    }

    @Override
    protected void updateSeekUI(int curr, int seekTo, int duration) {
        super.updateSeekUI(curr, seekTo, duration);
        if (seekTo > curr) {
            mProgressIcon.setImageResource(R.drawable.icon_pre);
        } else {
            mProgressIcon.setImageResource(R.drawable.icon_back);
        }
        mProgressText.setText(PlayerUtils.stringForTime(seekTo) + " / " + PlayerUtils.stringForTime(duration));
        mHandler.sendEmptyMessage(1000);
        mHandler.removeMessages(1001);
        mHandler.sendEmptyMessageDelayed(1001, 1000);
//        showBottom();
    }

    public interface LiveControlListener {
        boolean singleTap();

        void longPress();

        void playStateChanged(int playState);

        void changeSource(int direction);

        void setVideoSize(String videoSize);

        void setVideoUrl();
    }

    private LiveController.LiveControlListener listener = null;

    public void setListener(LiveController.LiveControlListener listener) {
        this.listener = listener;
    }

    boolean isBottomVisible() {
        return mBottomRoot.getVisibility() == VISIBLE;
    }

    void showBottom() {
        mHandler.removeMessages(1003);
        mHandler.sendEmptyMessage(1002);
    }

    void hideBottom() {
        mHandler.removeMessages(1002);
        mHandler.sendEmptyMessage(1003);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (!isBottomVisible()) {
            showBottom();
        } else {
            hideBottom();
        }
        if (listener.singleTap())
            return true;
        return super.onSingleTapConfirmed(e);
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (super.onKeyEvent(event)) {
            return true;
        }
        if (isBottomVisible()) {
            return super.dispatchKeyEvent(event);
        }
        boolean isInPlayback = isInPlaybackState();
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (isInPlayback && mControlWrapper.getDuration() > 0) {
                    tvSlideStart(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                    return true;
                } else {
                    listener.changeSource(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (isInPlayback) {
                    togglePlay();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!isBottomVisible()) {
                    showBottom();
                }
            }
        } else if (action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (isInPlayback && mControlWrapper.getDuration() > 0) {
                    tvSlideStop();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }
    @Override
    public void onLongPress(MotionEvent e) {
        listener.longPress();
        super.onLongPress(e);
    }

    private boolean simSlideStart = false;
    private int simSeekPosition = 0;
    private long simSlideOffset = 0;

    public void tvSlideStop() {
        if (!simSlideStart)
            return;
        mControlWrapper.seekTo(simSeekPosition);
        if (!mControlWrapper.isPlaying())
            mControlWrapper.start();
        simSlideStart = false;
        simSeekPosition = 0;
        simSlideOffset = 0;
    }

    public void tvSlideStart(int dir) {
        int duration = (int) mControlWrapper.getDuration();
        if (duration <= 0)
            return;
        if (!simSlideStart) {
            simSlideStart = true;
        }
        // 每次10秒
        simSlideOffset += (10000.0f * dir);
        int currentPosition = (int) mControlWrapper.getCurrentPosition();
        int position = (int) (simSlideOffset + currentPosition);
        if (position > duration) position = duration;
        if (position < 0) position = 0;
        updateSeekUI(currentPosition, position, duration);
        simSeekPosition = position;
//        listener.changeSource(1);
    }

    @Override
    protected void onPlayStateChanged(int playState) {
        super.onPlayStateChanged(playState);
        switch (playState) {
            case VideoView.STATE_PLAYING:
                startProgress();
                break;
        }
        listener.playStateChanged(playState);
    }

//    @Override
//    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        if (e1.getX() - e2.getX() > minFlingDistance && Math.abs(velocityX) > minFlingVelocity) {
//            listener.changeSource(-1);          //左滑
//        } else if (e2.getX() - e1.getX() > minFlingDistance && Math.abs(velocityX) > minFlingVelocity) {
//            listener.changeSource(1);           //右滑
//        } else if (e1.getY() - e2.getY() > minFlingDistance && Math.abs(velocityY) > minFlingVelocity) {
//        } else if (e2.getY() - e1.getY() > minFlingDistance && Math.abs(velocityY) > minFlingVelocity) {
//        }
//        return false;
//    }

    @Override
    public boolean onBackPressed() {
        if (super.onBackPressed()) {
            return true;
        }
        if (isBottomVisible()) {
            hideBottom();
            return true;
        }
        return false;
    }

    //设置倍数播放
    public void setPlaySpeed(float speed) {
        mControlWrapper.setSpeed(speed);
    }

    public String getPlayVideoSize() {
        if (mControlWrapper.getVideoSize().length >=2)
            return mControlWrapper.getVideoSize()[0] + " x " + mControlWrapper.getVideoSize()[1];
        return "0 x 0";
    }
}
