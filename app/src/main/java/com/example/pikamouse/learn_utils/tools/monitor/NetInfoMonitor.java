package com.example.pikamouse.learn_utils.tools.monitor;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.view.WindowManager;

import com.example.pikamouse.learn_utils.tools.util.DisplayUtil;
import com.example.pikamouse.learn_utils.tools.util.ThreadUtil;
import com.example.pikamouse.learn_utils.tools.view.FloatNetInfoView;
import com.example.pikamouse.learn_utils.tools.window.FloatNetInfoWindow;
import com.example.pikamouse.learn_utils.tools.window.FloatWindow;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author: jiangfeng
 * @date: 2019/1/3
 */
public class NetInfoMonitor implements IMonitor{

    private Context mContext;
    private Timer mTimer;
    private FloatNetInfoView mView;
    private FloatNetInfoWindow mWindow;
    private NetInfoTask mTask;
    private final static long DURATION = 2000;
    private int mProcessUid;



    @Override
    public void init(Context context) {
        if (!(context instanceof Application)) {
            throw new IllegalArgumentException("you must init with application context");
        }
        mContext = context;
    }

    @Override
    public void start(String type) {
        if (mContext == null) {
            throw new IllegalStateException("init must be called");
        }
        stop();
        mView = new FloatNetInfoView(mContext);
        mView.setViewVisibility(MonitorManager.ItemBuilder.getItems(type));
        mWindow = new FloatNetInfoWindow(mContext);
        WindowManager.LayoutParams layoutParams = new FloatWindow.WMLayoutParamsBuilder()
                //可以唤起输入法，不接受任何触摸事件全部由下层window接受
                .setFlag(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                .setX(DisplayUtil.getScreenWidth(mContext) - mView.getMeasuredWidth())
                .setY(0)
                .build();
        mWindow.attachToWindow(mView, layoutParams);
        mProcessUid = android.os.Process.myUid();
        mTimer = new Timer();
        mTask = new NetInfoTask();
        mTimer.scheduleAtFixedRate(mTask, 1000 , 1000);
    }

    private class NetInfoTask extends TimerTask {

        private long mLastTotalRxBytes = TrafficStats.getTotalRxBytes();
        private long mLastTimeStamp = System.currentTimeMillis();


        @Override
        public void run() {
            long nowTotalRxBytes = TrafficStats.getTotalRxBytes();
            long nowTimeStamp = System.currentTimeMillis();
            final long tx = TrafficStats.getUidTxBytes(mProcessUid);
            final long rx = TrafficStats.getUidRxBytes(mProcessUid);
            final long rate = ((nowTotalRxBytes - mLastTotalRxBytes) * 1000 / (nowTimeStamp - mLastTimeStamp));
            mLastTotalRxBytes = nowTotalRxBytes;
            mLastTimeStamp = nowTimeStamp;
            ThreadUtil.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mView.setData(tx, rx, rate);
                }
            });
        }
    }
    @Override
    public void stop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mWindow != null) {
            mWindow.release();
        }
    }

}
