package com.openxu.bs;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Instrumentation;
import android.app.VoiceInteractor;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.transition.Scene;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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
public class ViewInfloat extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        //setContentView(new TextView(this));
        //setContentView(new TextView(this),new ViewGroup.LayoutParams(...));
    }



    public class Activity ...{
        private Window mWindow;

        //三个设置视图的方法
        public void setContentView(@LayoutRes int layoutResID) {
            getWindow().setContentView(layoutResID);
            //初始化ActionBar
            initWindowDecorActionBar();
        }
        public void setContentView(View view) {
            getWindow().setContentView(view);
            initWindowDecorActionBar();
        }
        public void setContentView(View view, ViewGroup.LayoutParams params) {
            getWindow().setContentView(view, params);
            initWindowDecorActionBar();
        }

        final void attach(...) {
            ...
            //初始化mWindow
            mWindow = new PhoneWindow(this);
            ...
        }
        public Window getWindow() {
            return mWindow;
        }
    }


public class PhoneWindow extends Window implements MenuBuilder.Callback {
    //所有自定义View的根View, id="@android:id/content"
    private ViewGroup mContentParent;
    //窗口顶层View
    private DecorView mDecor;
    private final class DecorView extends FrameLayout ...{
    }
    //初始化了一个FrameLayout对象
    protected DecorView generateDecor() {
        return new DecorView(getContext(), -1);
    }

    protected ViewGroup generateLayout(DecorView decor) {
        //1.获取<Application android:theme=""/>, <Activity/>节点指定的themes
        //或者代码requestWindowFeature()中指定的Features, 并设置
        TypedArray a = getWindowStyle();
        //设置当前窗口是否有标题
        if (a.getBoolean(R.styleable.Window_windowNoTitle, false)) {
            //请求指定Activity窗口的风格类型
            requestFeature(FEATURE_NO_TITLE);
        } else if (a.getBoolean(R.styleable.Window_windowActionBar, false)) {
            requestFeature(FEATURE_ACTION_BAR);
        }
        ...
        //设置窗口是否全屏
        if (a.getBoolean(R.styleable.Window_windowFullscreen, false)) {
            setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN & (~getForcedWindowFlags()));
        }
        ...

        //2.获取窗口Features, 设置相应的修饰布局文件，这些xml文件位于frameworks/base/core/res/res/layout下
        int layoutResource;
        int features = getLocalFeatures();
        if ((features & (1 << FEATURE_SWIPE_TO_DISMISS)) != 0) {
            layoutResource = R.layout.screen_swipe_dismiss;
        } else if ((features & ((1 << FEATURE_LEFT_ICON) | (1 << FEATURE_RIGHT_ICON))) != 0) {
          ...

        mDecor.startChanging();
        //3.将上面选定的布局文件inflate为View树，添加到decorView中
        View in = mLayoutInflater.inflate(layoutResource, null);
        decor.addView(in, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        mContentRoot = (ViewGroup) in;

        // 4. 将窗口修饰布局文件中id="@android:id/content"的View赋值给mContentParent,
        // 后续自定义的view/layout都将是其子View
        ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
        if (contentParent == null) {
            throw new RuntimeException("Window couldn't find content container view");
        }
        ...

        mDecor.finishChanging();

        return contentParent;
    }

    private void installDecor() {
        if (mDecor == null) {
            mDecor = generateDecor();   //初始化根视图
            mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            mDecor.setIsRootNamespace(true);
            ...
        }
        if (mContentParent == null) {
            mContentParent = generateLayout(mDecor);


            if (decorContentParent != null) {
                mDecorContentParent = decorContentParent;
                mDecorContentParent.setWindowCallback(getCallback());
                if (mDecorContentParent.getTitle() == null) {
                    mDecorContentParent.setWindowTitle(mTitle);
                }

                final int localFeatures = getLocalFeatures();
                for (int i = 0; i < FEATURE_MAX; i++) {
                    if ((localFeatures & (1 << i)) != 0) {
                        mDecorContentParent.initFeature(i);
                    }
                }

                mDecorContentParent.setUiOptions(mUiOptions);

                if ((mResourcesSetFlags & FLAG_RESOURCE_SET_ICON) != 0 ||
                        (mIconRes != 0 && !mDecorContentParent.hasIcon())) {
                    mDecorContentParent.setIcon(mIconRes);
                } else if ((mResourcesSetFlags & FLAG_RESOURCE_SET_ICON) == 0 &&
                        mIconRes == 0 && !mDecorContentParent.hasIcon()) {
                    mDecorContentParent.setIcon(
                            getContext().getPackageManager().getDefaultActivityIcon());
                    mResourcesSetFlags |= FLAG_RESOURCE_SET_ICON_FALLBACK;
                }
                if ((mResourcesSetFlags & FLAG_RESOURCE_SET_LOGO) != 0 ||
                        (mLogoRes != 0 && !mDecorContentParent.hasLogo())) {
                    mDecorContentParent.setLogo(mLogoRes);
                }

                // Invalidate if the panel menu hasn't been created before this.
                // Panel menu invalidation is deferred avoiding application onCreateOptionsMenu
                // being called in the middle of onCreate or similar.
                // A pending invalidation will typically be resolved before the posted message
                // would run normally in order to satisfy instance state restoration.
                PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
                if (!isDestroyed() && (st == null || st.menu == null) && !mIsStartingWindow) {
                    invalidatePanelMenu(FEATURE_ACTION_BAR);
                }
            } else {
                mTitleView = (TextView)findViewById(R.id.title);
                if (mTitleView != null) {
                    mTitleView.setLayoutDirection(mDecor.getLayoutDirection());
                    if ((getLocalFeatures() & (1 << FEATURE_NO_TITLE)) != 0) {
                        View titleContainer = findViewById(
                                R.id.title_container);
                        if (titleContainer != null) {
                            titleContainer.setVisibility(View.GONE);
                        } else {
                            mTitleView.setVisibility(View.GONE);
                        }
                        if (mContentParent instanceof FrameLayout) {
                            ((FrameLayout)mContentParent).setForeground(null);
                        }
                    } else {
                        mTitleView.setText(mTitle);
                    }
                }
            }

            if (mDecor.getBackground() == null && mBackgroundFallbackResource != 0) {
                mDecor.setBackgroundFallback(mBackgroundFallbackResource);
            }

            // Only inflate or create a new TransitionManager if the caller hasn't
            // already set a custom one.
            if (hasFeature(FEATURE_ACTIVITY_TRANSITIONS)) {
                if (mTransitionManager == null) {
                    final int transitionRes = getWindowStyle().getResourceId(
                            R.styleable.Window_windowContentTransitionManager,
                            0);
                    if (transitionRes != 0) {
                        final TransitionInflater inflater = TransitionInflater.from(getContext());
                        mTransitionManager = inflater.inflateTransitionManager(transitionRes,
                                mContentParent);
                    } else {
                        mTransitionManager = new TransitionManager();
                    }
                }

                mEnterTransition = getTransition(mEnterTransition, null,
                        R.styleable.Window_windowEnterTransition);
                mReturnTransition = getTransition(mReturnTransition, USE_DEFAULT_TRANSITION,
                        R.styleable.Window_windowReturnTransition);
                mExitTransition = getTransition(mExitTransition, null,
                        R.styleable.Window_windowExitTransition);
                mReenterTransition = getTransition(mReenterTransition, USE_DEFAULT_TRANSITION,
                        R.styleable.Window_windowReenterTransition);
                mSharedElementEnterTransition = getTransition(mSharedElementEnterTransition, null,
                        R.styleable.Window_windowSharedElementEnterTransition);
                mSharedElementReturnTransition = getTransition(mSharedElementReturnTransition,
                        USE_DEFAULT_TRANSITION,
                        R.styleable.Window_windowSharedElementReturnTransition);
                mSharedElementExitTransition = getTransition(mSharedElementExitTransition, null,
                        R.styleable.Window_windowSharedElementExitTransition);
                mSharedElementReenterTransition = getTransition(mSharedElementReenterTransition,
                        USE_DEFAULT_TRANSITION,
                        R.styleable.Window_windowSharedElementReenterTransition);
                if (mAllowEnterTransitionOverlap == null) {
                    mAllowEnterTransitionOverlap = getWindowStyle().getBoolean(
                            R.styleable.Window_windowAllowEnterTransitionOverlap, true);
                }
                if (mAllowReturnTransitionOverlap == null) {
                    mAllowReturnTransitionOverlap = getWindowStyle().getBoolean(
                            R.styleable.Window_windowAllowReturnTransitionOverlap, true);
                }
                if (mBackgroundFadeDurationMillis < 0) {
                    mBackgroundFadeDurationMillis = getWindowStyle().getInteger(
                            R.styleable.Window_windowTransitionBackgroundFadeDuration,
                            DEFAULT_BACKGROUND_FADE_DURATION_MS);
                }
                if (mSharedElementsUseOverlay == null) {
                    mSharedElementsUseOverlay = getWindowStyle().getBoolean(
                            R.styleable.Window_windowSharedElementsUseOverlay, true);
                }
            }
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        if (mContentParent == null) {
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            //移除该mContentParent内所有的所有子View
            mContentParent.removeAllViews();
        }
        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            final Scene newScene = Scene.getSceneForLayout(mContentParent, layoutResID,
                    getContext());
            transitionTo(newScene);
        } else {
            //将我们的资源文件通过LayoutInflater对象转换为View树，并且添加至mContentParent视图中
            mLayoutInflater.inflate(layoutResID, mContentParent);
        }
        mContentParent.requestApplyInsets();
        final Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
    }

    @Override
    public void setContentView(View view) {
        setContentView(view, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (mContentParent == null) {
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }
        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            view.setLayoutParams(params);
            final Scene newScene = new Scene(mContentParent, view);
            transitionTo(newScene);
        } else {
            mContentParent.addView(view, params);
        }
        mContentParent.requestApplyInsets();
        final Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
    }
}

}

