package com.openxu.bs;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppOpsManager;
import android.app.Application;
import android.app.ApplicationErrorReport;
import android.app.Instrumentation;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.VoiceInteractor;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.UriPermission;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.transition.Scene;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.EventLog;
import android.util.Log;
import android.util.LogPrinter;
import android.util.TypedValue;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.InflateException;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_TASK_ON_HOME;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
import static android.view.WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;
import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
import static android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;

/**
 * author : openXu
 * create at : 2016/8/22 16:29
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : BitmapStudy
 * class name : ViewInfloat
 * version : 1.0
 * class describe：
 */
public class ViewInfloat extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        startActivity(new );
        Activity
        //setContentView(new TextView(this));
        //setContentView(new TextView(this),new ViewGroup.LayoutParams(...));
    }





    public static void main(String[] args) {
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "ActivityThreadMain");
        SamplingProfilerIntegration.start();

        // CloseGuard defaults to true and can be quite spammy.  We
        // disable it here, but selectively enable it later (via
        // StrictMode) on debug builds, but using DropBox, not logs.
        CloseGuard.setEnabled(false);

        Environment.initForCurrentUser();

        // Set the reporter for event logging in libcore
        EventLogger.setReporter(new EventLoggingReporter());

        AndroidKeyStoreProvider.install();

        // Make sure TrustedCertificateStore looks in the right place for CA certificates
        final File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());
        TrustedCertificateStore.setDefaultUserDirectory(configDir);

        Process.setArgV0("<pre-initialized>");

        Looper.prepareMainLooper();

        ActivityThread thread = new ActivityThread();
        thread.attach(false);

        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }

        if (false) {
            Looper.myLooper().setMessageLogging(new
                    LogPrinter(Log.DEBUG, "ActivityThread"));
        }

        // End of event ActivityThreadMain.
        Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        Looper.loop();

        throw new RuntimeException("Main thread loop unexpectedly exited");
    }


    ViewGroup
    public void addView(View child, int index, LayoutParams params) {
        if (DBG) {
            System.out.println(this + " addView");
        }

        if (child == null) {
            throw new IllegalArgumentException("Cannot add a null child view to a ViewGroup");
        }

        // addViewInner() will call child.requestLayout() when setting the new LayoutParams
        // therefore, we call requestLayout() on ourselves before, so that the child's request
        // will be blocked at our level
        requestLayout();
        invalidate(true);
        addViewInner(child, index, params, false);
    }

    View
    @CallSuper
    public void requestLayout() {
        if (mMeasureCache != null) mMeasureCache.clear();

        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == null) {
            // Only trigger request-during-layout logic if this is the view requesting it,
            // not the views in its parent hierarchy
            ViewRootImpl viewRoot = getViewRootImpl();
            if (viewRoot != null && viewRoot.isInLayout()) {
                if (!viewRoot.requestLayoutDuringLayout(this)) {
                    return;
                }
            }
            mAttachInfo.mViewRequestingLayout = this;
        }

        mPrivateFlags |= PFLAG_FORCE_LAYOUT;
        mPrivateFlags |= PFLAG_INVALIDATED;

        if (mParent != null && !mParent.isLayoutRequested()) {
            mParent.requestLayout();
        }
        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == this) {
            mAttachInfo.mViewRequestingLayout = null;
        }
    }



    ActivityThread创建Activity




    final void handleResumeActivity(IBinder token,
                                    boolean clearHide, boolean isForward, boolean reallyResume) {
        if (r != null) {
            final Activity a = r.activity;

            final int forwardBit = isForward ?
                    WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION : 0;

            boolean willBeVisible = !a.mStartedActivity;
            ...
            /*
             * 如果window没有被添加到WindowManager中，并且当前activity没有finish()或者开启另一个Activity;
             * 意思就是当前activity正要被显示出来了，这时候将window添加到windowManager中
             */
            if (r.window == null && !a.mFinished && willBeVisible) {
                r.window = r.activity.getWindow();
                //获取activity中mWindow（PhoneWindow对象）的mDecor，也就是顶层窗口
                View decor = r.window.getDecorView();
                //设置顶层窗口可见
                decor.setVisibility(View.INVISIBLE);
                //获取WindowManager对象
                ViewManager wm = a.getWindowManager();
                //创建一个布局参数
                WindowManager.LayoutParams l = r.window.getAttributes();
                a.mDecor = decor;
                l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
                l.softInputMode |= forwardBit;
                if (a.mVisibleFromClient) {
                    a.mWindowAdded = true;
                    //将顶层窗口mDecor添加到WindowManager中
                    wm.addView(decor, l);
                }

            } else if (!willBeVisible) {
                //如果window已经被添加，但在resume之前开启了另一个activity，这时候当前activity没有在最顶端（失去焦点）
                r.hideForNow = true;
            }

            // Get rid of anything left hanging around.
            cleanUpPendingRemoveWindows(r);

            // The window is now visible if it has been added, we are not
            // simply finishing, and we are not starting another activity.
            if (!r.activity.mFinished && willBeVisible
                    && r.activity.mDecor != null && !r.hideForNow) {
                if (r.newConfig != null) {
                    r.tmpConfig.setTo(r.newConfig);
                    if (r.overrideConfig != null) {
                        r.tmpConfig.updateFrom(r.overrideConfig);
                    }
                    if (DEBUG_CONFIGURATION) Slog.v(TAG, "Resuming activity "
                            + r.activityInfo.name + " with newConfig " + r.tmpConfig);
                    performConfigurationChanged(r.activity, r.tmpConfig);
                    freeTextLayoutCachesIfNeeded(r.activity.mCurrentConfig.diff(r.tmpConfig));
                    r.newConfig = null;
                }
                if (localLOGV) Slog.v(TAG, "Resuming " + r + " with isForward="
                        + isForward);
                WindowManager.LayoutParams l = r.window.getAttributes();
                if ((l.softInputMode
                        & WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION)
                        != forwardBit) {
                    l.softInputMode = (l.softInputMode
                            & (~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION))
                            | forwardBit;
                    if (r.activity.mVisibleFromClient) {
                        ViewManager wm = a.getWindowManager();
                        View decor = r.window.getDecorView();
                        wm.updateViewLayout(decor, l);
                    }
                }
                r.activity.mVisibleFromServer = true;
                mNumVisibleActivities++;
                if (r.activity.mVisibleFromClient) {
                    r.activity.makeVisible();
                }
            }

            if (!r.onlyLocalRequest) {
                r.nextIdle = mNewActivities;
                mNewActivities = r;
                if (localLOGV) Slog.v(
                        TAG, "Scheduling idle handler for " + r);
                Looper.myQueue().addIdleHandler(new Idler());
            }
            r.onlyLocalRequest = false;

            // Tell the activity manager we have resumed.
            if (reallyResume) {
                try {
                    ActivityManagerNative.getDefault().activityResumed(token);
                } catch (RemoteException ex) {
                }
            }

        } else {
            // If an exception was thrown when trying to resume, then
            // just end this activity.
            try {
                ActivityManagerNative.getDefault()
                        .finishActivity(token, Activity.RESULT_CANCELED, null, false);
            } catch (RemoteException ex) {
            }
        }
    }


}













































111111111111

