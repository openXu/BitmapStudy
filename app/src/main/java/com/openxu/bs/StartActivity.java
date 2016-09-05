package com.openxu.bs;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ApplicationErrorReport;
import android.app.Instrumentation;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriPermission;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
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
import android.text.TextUtils;
import android.util.EventLog;
import android.util.EventLogTags;
import android.util.Log;
import android.util.LogPrinter;
import android.view.View;

import com.openxu.bs.androidsuorce.ActivityManagerService;
import com.openxu.bs.androidsuorce.ActivityRecord;
import com.openxu.bs.androidsuorce.ActivityStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_TASK_ON_HOME;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * author : openXu
 * created time : 16/9/3 下午5:33
 * blog : http://blog.csdn.net/xmxkf
 * github : http://blog.csdn.net/xmxkf
 * class name : StartActivity
 * http://blog.csdn.net/luoshengyang/article/details/6689748
 * discription :
 */
public class StartActivity extends Activity{

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        startActivity(new Intent(this, StartActivity.class));

    }

    /**
     * step1 : Activity.startActivityForResult()
     * 在Android系统中，我们比较熟悉的打开Activity通常有两种方式，
     * 第一种是点击应用程序图标，Launcher会启动应用程序的主Activity，
     * 我们知道Launcher其实也是一个应用程序，他是怎样打开我们的主Activity的呢？
     * 在应用程序被安装的时候，系统会找到AndroidManifest.xml中activity的配置信息，
     * 并将action=android.intent.action.MAIN&category=android.intent.category.LAUNCHER
     * 的activity记录下来，形成应用程序与主Activity 的映射关系，当点击启动图标时，
     * Launcher就会找到应用程序对应的主activity并将它启动。第二种是当主Activity
     * 启动之后，在应用程序内部可以调用startActivity()开启新的Activity，这种方式又可分为显示启动和隐式启动。
     * 不管使用哪种方式启动Activity，其实最终调用的都是startActivity()方法。所以如果要分析Activity的启动过程，
     * 我们就从startActivity()方法分析。跟踪发现Activity中重载的startActivity()方法最终都是调用
     * startActivityForResult(intent, requestCode , bundle)：
     */
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        //一般Activity的mParent都为null，mParent常用在ActivityGroup中，ActivityGroup已废弃
        if (mParent == null) {
            //启动新的Activity，核心功能都在mMainThread.getApplicationThread()中完成
            Instrumentation.ActivityResult ar =
                    mInstrumentation.execStartActivity(
                            this, mMainThread.getApplicationThread(), mToken, this,
                            intent, requestCode, options);
            if (ar != null) {
                //跟踪execStartActivity()，发现开启activity失败ar才可能为null，这时会调用onActivityResult
                mMainThread.sendActivityResult(
                        mToken, mEmbeddedID, requestCode, ar.getResultCode(),
                        ar.getResultData());
            }
            if (requestCode >= 0) {
                // If this start is requesting a result, we can avoid making
                // the activity visible until the result is received.  Setting
                // this code during onCreate(Bundle savedInstanceState) or onResume() will keep the
                // activity hidden during this time, to avoid flickering.
                // This can only be done when a result is requested because
                // that guarantees we will get information back when the
                // activity is finished, no matter what happens to it.
                mStartedActivity = true;
            }

            final View decor = mWindow != null ? mWindow.peekDecorView() : null;
            if (decor != null) {
                decor.cancelPendingInputEvents();
            }
        } else {
            //在ActivityGroup内部的Activity调用startActivity的时候会走到这里，处理逻辑和上面是类似的
            if (options != null) {
                mParent.startActivityFromChild(this, intent, requestCode, options);
            } else {
                // Note we want to go through this method for compatibility with
                // existing applications that may have overridden it.
                mParent.startActivityFromChild(this, intent, requestCode);
            }
        }
    }

    /**
     * step2 : Instrumentation
     *  mInstrumentation的execStartActivity(）方法中首先检查activity是否能打开，
     *  如果不能打开直接返回，否则继续调用ActivityManagerNative.getDefault().startActivity()开启activity，
     *  然后检查开启结果，如果开启失败，会抛出异常（比如未在AndroidManifest.xml注册）
     */
    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, String target,
            Intent intent, int requestCode, Bundle options) {
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        //mActivityMonitors是所有ActivityMonitor的集合，用于监视应用的Activity(记录状态)
        if (mActivityMonitors != null) {
            synchronized (mSync) {
                //先查找一遍看是否存在这个activity
                final int N = mActivityMonitors.size();
                for (int i=0; i<N; i++) {
                    final ActivityMonitor am = mActivityMonitors.get(i);
                    if (am.match(who, null, intent)) {
                        //如果找到了就跳出循环
                        am.mHits++;
                        //如果目标activity无法打开，直接return
                        if (am.isBlocking()) {
                            return requestCode >= 0 ? am.getResult() : null;
                        }
                        break;
                    }
                }
            }
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess();
            //这里才是真正开启activity的地方，ActivityManagerNative中实际上调用的是ActivityManagerProxy的方法
            int result = ActivityManagerNative.getDefault()
                    .startActivity(whoThread, who.getBasePackageName(), intent,
                            intent.resolveTypeIfNeeded(who.getContentResolver()),
                            token, target, requestCode, 0, null, options);
            //checkStartActivityResult方法是抛异常专业户，它对上面开启activity的结果进行检查，如果无法打开activity，
            //则抛出诸如ActivityNotFoundException类似的各种异常
            checkStartActivityResult(result, intent);
        } catch (RemoteException e) {
            throw new RuntimeException("Failure from system", e);
        }
        return null;
    }

    /**
     * step3 :  ActivityManagerNative
     * 在讲解此步骤之前，先大致介绍一下ActivityManagerNative这个抽象类。
     * ActivityManagerNative继承自Binder(实现了IBinder)，实现了IActivityManager接口，但是没有实现他的抽象方法。
     *
     * ActivityManagerNative中有一个内部类ActivityManagerProxy，ActivityManagerProxy也实现
     * 了IActivityManager接口，并实现了IActivityManager中所有的方法。IActivityManager里面有很
     * 多像startActivity()、startService()、bindService()、registerReveicer()等方法，所以
     * IActivityManager为Activity管理器提供了统一的API。
     *
     * ActivityManagerNative通过getDefault()获取到一个ActivityManagerProxy的示例，并将远程代理对象IBinder
     * 传递给他，然后调用他的starXxx方法开启Service、Activity或者registReceiver，可以看出ActivityManagerNative
     * 只是一个装饰类，真正工作的是其内部类ActivityManagerProxy。
     *
     */
    public abstract class ActivityManagerNative extends Binder
            implements IActivityManager{
        static public IActivityManager getDefault() {
            //此处返回的IActivityManager示例是ActivityManagerProxy的对象
            return gDefault.get();
        }
        private static final Singleton<IActivityManager> gDefault = new Singleton<IActivityManager>() {
            protected IActivityManager create() {
                //android.os.ServiceManager中维护了HashMap<String, IBinder> sCache，他是系统Service对应的IBinder代理对象的集合
                //通过名称获取到ActivityManagerService对应的IBinder代理对象
                IBinder b = ServiceManager.getService("activity");
                if (false) {
                    Log.v("ActivityManager", "default service binder = " + b);
                }
                //返回一个IActivityManager对象，这个对象实际上是ActivityManagerProxy的对象
                IActivityManager am = asInterface(b);
                if (false) {
                    Log.v("ActivityManager", "default service = " + am);
                }
                return am;
            }
        };
        static public IActivityManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IActivityManager in =
                    (IActivityManager)obj.queryLocalInterface(descriptor);
            if (in != null) {
                return in;
            }
            //返回ActivityManagerProxy对象
            return new ActivityManagerProxy(obj);
        }

    }

    class ActivityManagerProxy implements IActivityManager {

        private IBinder mRemote;

        public ActivityManagerProxy(IBinder remote) {
            mRemote = remote;
        }
        public IBinder asBinder() {
            return mRemote;
        }

        /*
         * Activity生命周期相关方法
         */
        public int startActivity(IApplicationThread caller, String callingPackage, Intent intent,
                                 String resolvedType, IBinder resultTo, String resultWho, int requestCode,
                                 int startFlags, ProfilerInfo profilerInfo, Bundle options) throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            //下面的代码将参数持久化，便于ActivityManagerService中获取
            data.writeInterfaceToken(IActivityManager.descriptor);
            data.writeStrongBinder(caller != null ? caller.asBinder() : null);
            data.writeString(callingPackage);
            intent.writeToParcel(data, 0);
            data.writeString(resolvedType);
            data.writeStrongBinder(resultTo);
            data.writeString(resultWho);
            data.writeInt(requestCode);
            data.writeInt(startFlags);
            if (profilerInfo != null) {
                data.writeInt(1);
                profilerInfo.writeToParcel(data, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
            } else {
                data.writeInt(0);
            }
            if (options != null) {
                data.writeInt(1);
                options.writeToParcel(data, 0);
            } else {
                data.writeInt(0);
            }
            //mRemote就是ActivityManagerService的远程代理对象，这句代码之后就进入到ActivityManagerService中了
            mRemote.transact(START_ACTIVITY_TRANSACTION, data, reply, 0);
            reply.readException();
            int result = reply.readInt();
            reply.recycle();
            data.recycle();
            return result;
        }
        public void activityResumed(IBinder token) throws RemoteException
        {...
            mRemote.transact(ACTIVITY_RESUMED_TRANSACTION, data, reply, 0);
        }
        public void activityPaused(IBinder token) throws RemoteException
        {...
            mRemote.transact(ACTIVITY_PAUSED_TRANSACTION, data, reply, 0);
        }
        public void activityStopped(IBinder token, Bundle state,
                                    PersistableBundle persistentState, CharSequence description) throws RemoteException
        {...
            mRemote.transact(ACTIVITY_STOPPED_TRANSACTION, data, reply, IBinder.FLAG_ONEWAY);
        }
        public void activityDestroyed(IBinder token) throws RemoteException
        {...
            mRemote.transact(ACTIVITY_DESTROYED_TRANSACTION, data, reply, IBinder.FLAG_ONEWAY);
        }
        public void moveTaskToFront(int task, int flags, Bundle options) throws RemoteException
        {...
            mRemote.transact(MOVE_TASK_TO_FRONT_TRANSACTION, data, reply, 0);
        }

        /*
         * Receiver广播注册等相关方法
         */
        public Intent registerReceiver(IApplicationThread caller, String packageName,
                                       IIntentReceiver receiver,
                                       IntentFilter filter, String perm, int userId) throws RemoteException
        {...
            mRemote.transact(REGISTER_RECEIVER_TRANSACTION, data, reply, 0);
        }
        public void unregisterReceiver(IIntentReceiver receiver) throws RemoteException
        {...
            mRemote.transact(UNREGISTER_RECEIVER_TRANSACTION, data, reply, 0);
        }

        /*
         * Servicek开启和关闭相关
         */
        public ComponentName startService(IApplicationThread caller, Intent service,
                                          String resolvedType, String callingPackage, int userId) throws RemoteException
        {...
            mRemote.transact(START_SERVICE_TRANSACTION, data, reply, 0);
        }
        public int stopService(IApplicationThread caller, Intent service,
                               String resolvedType, int userId) throws RemoteException
        {...
            mRemote.transact(STOP_SERVICE_TRANSACTION, data, reply, 0);
        }
        ...
        public int bindService(IApplicationThread caller, IBinder token,
                               Intent service, String resolvedType, IServiceConnection connection,
                               int flags,  String callingPackage, int userId) throws RemoteException {
            ...
            mRemote.transact(BIND_SERVICE_TRANSACTION, data, reply, 0);
        }
        public boolean unbindService(IServiceConnection connection) throws RemoteException
        {...
            mRemote.transact(UNBIND_SERVICE_TRANSACTION, data, reply, 0);
        }

        ...
        //获取内存信息
        public void getMemoryInfo(ActivityManager.MemoryInfo outInfo) throws RemoteException {
            mRemote.transact(GET_MEMORY_INFO_TRANSACTION, data, reply, 0);}
        public Debug.MemoryInfo[] getProcessMemoryInfo(int[] pids) throws RemoteException {
            mRemote.transact(GET_PROCESS_MEMORY_INFO_TRANSACTION, data, reply, 0);}
        //杀死进程
        public void killBackgroundProcesses(String packageName, int userId) throws RemoteException {
            mRemote.transact(KILL_BACKGROUND_PROCESSES_TRANSACTION, data, reply, 0);}
        public void killApplicationWithAppId(String pkg, int appid, String reason) throws RemoteException {
            mRemote.transact(KILL_APPLICATION_WITH_APPID_TRANSACTION, data, reply, 0);}
        ...

    }

    /**
     * step4 : ActivityManagerService
     * 直接转调用ActivityStackSupervisor的startActivityMayWait()方法
     */
    public final class ActivityManagerService extends ActivityManagerNative
            implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        ...
        @Override
        public final int startActivity(IApplicationThread caller, String callingPackage,
                                       Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
                                       int startFlags, ProfilerInfo profilerInfo, Bundle options) {
            return startActivityAsUser(caller, callingPackage, intent, resolvedType, resultTo,
                    resultWho, requestCode, startFlags, profilerInfo, options,
                    UserHandle.getCallingUserId());
        }
        @Override
        public final int startActivityAsUser(IApplicationThread caller, String callingPackage,
                                             Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode,
                                             int startFlags, ProfilerInfo profilerInfo, Bundle options, int userId) {
            enforceNotIsolatedCaller("startActivity");
            userId = handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId,
                    false, ALLOW_FULL_ONLY, "startActivity", null);
            //mStackSupervisor的类型是ActivityStackSupervisor
            return mStackSupervisor.startActivityMayWait(caller, -1, callingPackage, intent,
                    resolvedType, null, null, resultTo, resultWho, requestCode, startFlags,
                    profilerInfo, null, null, options, userId, null, null);
        }
        ...
    }


    /**
     * step5 : ActivityStackSupervisor.startActivityMayWait()
     * 根据上面传递的参数和应用信息重新封装一些参数，然后调用startActivityLocked()方法
     */
    final int startActivityMayWait(IApplicationThread caller, int callingUid,
                                   String callingPackage, Intent intent, String resolvedType,
                                   IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
                                   IBinder resultTo, String resultWho, int requestCode, int startFlags,
                                   ProfilerInfo profilerInfo, WaitResult outResult, Configuration config,
                                   Bundle options, int userId, IActivityContainer iContainer, TaskRecord inTask) {
        ...

        // Don't modify the client's object!
        intent = new Intent(intent);

        // 调用resolveActivity()根据意图intent，解析目标Activity的一些信息保存到aInfo中，
        // 这些信息包括activity的aInfo.applicationInfo.packageName、name、applicationInfo、processName、theme、launchMode、permission、flags等等
        // 这都是在AndroidManifest.xml中为activity配置的
        ActivityInfo aInfo = resolveActivity(intent, resolvedType, startFlags,
                profilerInfo, userId);

        ...
        synchronized (mService) {
            //下面省略的代码用于重新组织startActivityLocked()方法需要的参数
            ...
            //调用startActivityLocked开启目标activity
            int res = startActivityLocked(caller, intent, resolvedType, aInfo,
                    voiceSession, voiceInteractor, resultTo, resultWho,
                    requestCode, callingPid, callingUid, callingPackage,
                    realCallingPid, realCallingUid, startFlags, options,
                    componentSpecified, null, container, inTask);
            ...

            if (outResult != null) {
                //如果outResult不为null,则设置开启activity的结果
                outResult.result = res;
                ...

                return res;
            }
        }
    }

    /**
     * step6 : ActivityStackSupervisor.startActivityLocked()
     * 这个方法主要是判断一些错误信息和检查权限，如果没有发现错误（err==START_SUCCESS）就继续开启activity，
     * 否则直接返回错误码
     */
    final int startActivityLocked(IApplicationThread caller,
                                  Intent intent, String resolvedType, ActivityInfo aInfo,
                                  IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
                                  IBinder resultTo, String resultWho, int requestCode,
                                  int callingPid, int callingUid, String callingPackage,
                                  int realCallingPid, int realCallingUid, int startFlags, Bundle options,
                                  boolean componentSpecified, ActivityRecord[] outActivity, ActivityContainer container,
                                  TaskRecord inTask) {
        int err = ActivityManager.START_SUCCESS;
        //调用者的进程信息，也就是哪个进程要开启此Activity的
        ProcessRecord callerApp = null;
        //下面有很多if语句，用于判断一些错误信息，并给err赋值相应的错误码
        if (caller != null) {
            callerApp = mService.getRecordForAppLocked(caller);
            if (callerApp != null) {
                callingPid = callerApp.pid;
                callingUid = callerApp.info.uid;
            } else {
                err = ActivityManager.START_PERMISSION_DENIED;
            }
        }
        ...
        if (err == ActivityManager.START_SUCCESS && intent.getComponent() == null) {
            err = ActivityManager.START_INTENT_NOT_RESOLVED;
        }
        if (err == ActivityManager.START_SUCCESS && aInfo == null) {
            // 未找到需要打开的activity的class文件
            err = ActivityManager.START_CLASS_NOT_FOUND;
        }
        ...
        //上面判断完成之后，接着判断如果err不为START_SUCCESS，则说明开启activity失败，直接返回错误码
        if (err != ActivityManager.START_SUCCESS) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1,
                        resultRecord, resultWho, requestCode,
                        Activity.RESULT_CANCELED, null);
            }
            ActivityOptions.abort(options);
            return err;
        }

        //检查权限，有些activity在清单文件中注册了权限，比如要开启系统相机，就需要注册相机权限，否则此处就会跑出异常
        final int startAnyPerm = mService.checkPermission(
                START_ANY_ACTIVITY, callingPid, callingUid);
        final int componentPerm = mService.checkComponentPermission(aInfo.permission, callingPid,
                callingUid, aInfo.applicationInfo.uid, aInfo.exported);
        if (startAnyPerm != PERMISSION_GRANTED && componentPerm != PERMISSION_GRANTED) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1,
                        resultRecord, resultWho, requestCode,
                        Activity.RESULT_CANCELED, null);
            }
            String msg;
            //权限被拒绝，抛出异常
            if (!aInfo.exported) {
                msg = "Permission Denial: starting " + intent.toString()
                        + " from " + callerApp + " (pid=" + callingPid
                        + ", uid=" + callingUid + ")"
                        + " not exported from uid " + aInfo.applicationInfo.uid;
            } else {
                msg = "Permission Denial: starting " + intent.toString()
                        + " from " + callerApp + " (pid=" + callingPid
                        + ", uid=" + callingUid + ")"
                        + " requires " + aInfo.permission;
            }
            throw new SecurityException(msg);
        }

        //★★★经过上面的判断后，创建一个新的Activity记录，
        // 这个ActivityRecord就是被创建的activity在历史堆栈中的一个条目，表示一个活动
        ActivityRecord r = new ActivityRecord(mService, callerApp, callingUid, callingPackage,
                intent, resolvedType, aInfo, mService.mConfiguration, resultRecord, resultWho,
                requestCode, componentSpecified, this, container, options);
        ...
        //继续调用startActivityUncheckedLocked()
        err = startActivityUncheckedLocked(r, sourceRecord, voiceSession, voiceInteractor,
                startFlags, true, options, inTask);
        ...
        return err;
    }

    /**
     * step7 : ActivityStackSupervisor.startActivityUncheckedLocked()
     * 通过第6步，新的activity记录已经创建了，接下来就是将这个activity放入到某个进程堆栈中；
     * startActivityLocked()中首先获取activity的启动模式（AndroidManifest.xml中为activity配置的launchMode属值），
     * 启动模式一共有四种ActivityInfo.LAUNCH_MULTIPLE（standard标准）、
     *               ActivityInfo.LAUNCH_SINGLE_INSTANCE（全局单例）、
     *               ActivityInfo.LAUNCH_SINGLE_TASK（进程中单例）、
     *               ActivityInfo.LAUNCH_SINGLE_TOP（栈顶单例）
     * 不清楚的可以参考官方网站http://developer.android.com/reference/android/content/pm/ActivityInfo.html
     *
     * 如果目标activity已经在某个历史进程中存在，需要根据启动模式分别判断并做相应处理，
     * 举个例子：如果启动模式为LAUNCH_SINGLE_INSTANCE，发现目标activity在某个进程中已经被启动过，
     *         这时候就将此进程置于进程堆栈栈顶，然后清除位于目标activity之上的activity，
     *         这样目标activity就位于栈顶了，这种情况就算是activity启动成功，直接返回
     *
     * 经过上面的判断处理，发现必须创建新的activity，并将其放入到某个进程中，就会进一步获取需要栖息的进程堆栈targetStack（创建新进程or已有的进程），
     * 最后调用(ActivityStack)targetStack.startActivityLocked()方法开启新的activity
     */
    final int startActivityUncheckedLocked(ActivityRecord r, ActivityRecord sourceRecord,
                                           IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags,
                                           boolean doResume, Bundle options, TaskRecord inTask) {
        ...
        //① 获取并配置activity配置的启动模式
        int launchFlags = intent.getFlags();
        if ((launchFlags & Intent.FLAG_ACTIVITY_NEW_DOCUMENT) != 0 &&
                (launchSingleInstance || launchSingleTask)) {
            launchFlags &=
                    ~(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        } else {
           ...
        }
        ...
        /*
         * 如果调用者不是来自另一个activity（不是在activity中调用startActivity）,
         * 但是给了我们用于放入心activity的一个明确的task，将执行下面代码
         *
         * 我们往上追溯，发现inTask是step4 中 ActivityManagerService.startActivityAsUser()方法传递的null，
         * 所以if里面的不会执行
         */
        if (sourceRecord == null && inTask != null && inTask.stack != null) {
            ...
        } else {
            inTask = null;
        }
        //根据activity的设置，如果满足下列条件，将launchFlags置为FLAG_ACTIVITY_NEW_TASK（创建新进程）
        if (inTask == null) {
            if (sourceRecord == null) {
                // This activity is not being started from another...  in this
                // case we -always- start a new task.
                //如果调用者为null，将launchFlags置为 创建一个新进程
                if ((launchFlags & Intent.FLAG_ACTIVITY_NEW_TASK) == 0 && inTask == null) {
                    launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
                }
            } else if (sourceRecord.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
                // 如果调用者的模式是SINGLE_INSTANCE，需要开启新进程
                launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
            } else if (launchSingleInstance || launchSingleTask) {
                // 如果需要开启的activity的模式是SingleInstance或者SingleTask，也需要开新进程
                launchFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;
            }
        }

        ActivityInfo newTaskInfo = null;   //新进程
        Intent newTaskIntent = null;
        ActivityStack sourceStack;    //调用者所在的进程
        //下面省略的代码是为上面三个变量赋值
        ...

        /*
         * ② 我们尝试将新的activity放在一个现有的任务中。但是如果activity被要求是singleTask或者singleInstance，
         * 我们会将activity放入一个新的task中.下面的if中主要处理将目标进程置于栈顶，然后将目标activity显示
         */
        if (((launchFlags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0 &&
                (launchFlags & Intent.FLAG_ACTIVITY_MULTIPLE_TASK) == 0)
                || launchSingleInstance || launchSingleTask) {
            //如果被开启的activity不是需要开启新的进程，而是single instance或者singleTask，
            if (inTask == null && r.resultTo == null) {
                //检查此activity是否已经开启了,findTaskLocked()方法用于查找目标activity所在的进程
                ActivityRecord intentActivity = !launchSingleInstance ?
                        findTaskLocked(r) : findActivityLocked(intent, r.info);
                if (intentActivity != null) {
                    ...
                    targetStack = intentActivity.task.stack;
                    ...
                    //如果目标activity已经开启，目标进程不在堆栈顶端，我们需要将它置顶
                    final ActivityStack lastStack = getLastStack();
                    ActivityRecord curTop = lastStack == null? null : lastStack.topRunningNonDelayedActivityLocked(notTop);
                    boolean movedToFront = false;
                    if (curTop != null && (curTop.task != intentActivity.task ||
                            curTop.task != lastStack.topTask())) {
                        r.intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                        if (sourceRecord == null || (sourceStack.topActivity() != null &&
                                sourceStack.topActivity().task == sourceRecord.task)) {
                            ...
                            //置顶进程
                            targetStack.moveTaskToFrontLocked(intentActivity.task, r, options,
                                    "bringingFoundTaskToFront");
                            ...
                            movedToFront = true;
                        }
                    }
                    ...
                    if ((launchFlags &
                            (Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK))
                            == (Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)) {
                        //如果调用者要求完全替代已经存在的进程
                        reuseTask = intentActivity.task;
                        reuseTask.performClearTaskLocked();
                        reuseTask.setIntent(r);
                    } else if ((launchFlags&Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0
                            || launchSingleInstance || launchSingleTask) {
                        //将进程堆栈中位于目标activity上面的其他activitys清理掉
                        ActivityRecord top = intentActivity.task.performClearTaskLocked(r, launchFlags);
                    } else if (r.realActivity.equals(intentActivity.task.realActivity)) {
                        //如果进程最上面的activity就是目标activity,进行一些设置操作
                        ...
                    } else if ((launchFlags&Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) == 0) {
                        ...
                    } else if (!intentActivity.task.rootWasReset) {
                        ...
                    }
                    if (!addingToTask && reuseTask == null) {
                        //让目标activity显示，会调用onResume
                        if (doResume) {
                            targetStack.resumeTopActivityLocked(null, options);
                        } else {
                            ActivityOptions.abort(options);
                        }
                        //直接返回
                        return ActivityManager.START_TASK_TO_FRONT;
                    }
                }
            }
        }

        // ③ 判断包名是否解析成功，如果包名解析不成功无法开启activity
        if (r.packageName != null) {
            //当前处于堆栈顶端的进程和activity
            ActivityStack topStack = getFocusedStack();
            ActivityRecord top = topStack.topRunningNonDelayedActivityLocked(notTop);
            if (top != null && r.resultTo == null) {
                if (top.realActivity.equals(r.realActivity) && top.userId == r.userId) {
                    if (top.app != null && top.app.thread != null) {
                        if ((launchFlags & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0
                                || launchSingleTop || launchSingleTask) {
                            ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, top,
                                    top.task);
                            ...
                            return ActivityManager.START_DELIVERED_TO_TOP;
                        }
                    }
                }
            }

        } else {
            // 包名为空，直接返回，没有找到要打开的activity
            ...
            return ActivityManager.START_CLASS_NOT_FOUND;
        }

        // ④ 判断activiy应该在那个进程中启动，如果该进程中已经存在目标activity，根据启动模式做相应处理
        ...
        // 判断是否需要开启新进程?
        boolean newTask = false;
        if (r.resultTo == null && inTask == null && !addingToTask
                && (launchFlags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            ...
            newTask = true;   //如果有FLAG_ACTIVITY_NEW_TASK标志，将为目标activity开启新的进程
            //创建新进程，并置前
            targetStack = adjustStackFocus(r, newTask);
            if (!launchTaskBehind) {
                targetStack.moveToFront("startingNewTask");
            }
            if (reuseTask == null) {
                r.setTask(targetStack.createTaskRecord(getNextTaskId(),
                        newTaskInfo != null ? newTaskInfo : r.info,
                        newTaskIntent != null ? newTaskIntent : intent,
                        voiceSession, voiceInteractor, !launchTaskBehind /* toTop */),
                        taskToAffiliate);
                if (DEBUG_TASKS) Slog.v(TAG, "Starting new activity " + r + " in new task " +
                        r.task);
            } else {
                r.setTask(reuseTask, taskToAffiliate);
            }
            ...
        } else if (sourceRecord != null) {
            //调用者不为空
            final TaskRecord sourceTask = sourceRecord.task;
            //默认在调用者所在进程启动，需要将进程置前
            targetStack = sourceTask.stack;
            targetStack.moveToFront("sourceStackToFront");
            final TaskRecord topTask = targetStack.topTask();
            if (topTask != sourceTask) {
                targetStack.moveTaskToFrontLocked(sourceTask, r, options, "sourceTaskToFront");
            }
            if (!addingToTask && (launchFlags&Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0) {
                //没有FLAG_ACTIVITY_CLEAR_TOP标志时，开启activity
                ActivityRecord top = sourceTask.performClearTaskLocked(r, launchFlags);
                if (top != null) {
                    ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, r, top.task);
                    top.deliverNewIntentLocked(callingUid, r.intent, r.launchedFromPackage);
                    ...
                    targetStack.resumeTopActivityLocked(null);
                    return ActivityManager.START_DELIVERED_TO_TOP;
                }
            } else if (!addingToTask &&
                    (launchFlags&Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) != 0) {
                //如果activity在当前进程中已经开启，清除位于他之上的activity
                final ActivityRecord top = sourceTask.findActivityInHistoryLocked(r);
                if (top != null) {
                    final TaskRecord task = top.task;
                    task.moveActivityToFrontLocked(top);
                    ActivityStack.logStartActivity(EventLogTags.AM_NEW_INTENT, r, task);
                    ...
                    return ActivityManager.START_DELIVERED_TO_TOP;
                }
            }
            r.setTask(sourceTask, null);
            if (DEBUG_TASKS) Slog.v(TAG, "Starting new activity " + r + " in existing task " + r.task + " from source " + sourceRecord);

        } else if (inTask != null) {
            //在调用者指定的确定的进程中开启目标activity
            ...
            if (DEBUG_TASKS) Slog.v(TAG, "Starting new activity " + r  + " in explicit task " + r.task);
        } else {
            ...
        }

        ...
        ActivityStack.logStartActivity(EventLogTags.AM_CREATE_ACTIVITY, r, r.task);
        targetStack.mLastPausedActivity = null;
        //⑤ 继续调用目标堆栈ActivityStack的startActivityLocked()方法，这个方法没有返回值，执行完毕之后直接返回START_SUCCESS
        targetStack.startActivityLocked(r, newTask, doResume, keepCurTransition, options);
        if (!launchTaskBehind) {
            // Don't set focus on an activity that's going to the back.
            mService.setFocusedActivityLocked(r, "startedActivity");
        }
        return ActivityManager.START_SUCCESS;
    }


    /**
     * step8 : ActivityStack.startActivityLocked()
     * ActivityStack用于管理activity的堆栈状态，startActivityLocked()方法就是将某个activity记录放入堆栈中
     * startActivityLocked()方法接受的参数：
     *                      r为需要启动的activity的记录信息；
     *                      newTask：是否需要启动新进程
     *                      doResume：是否需要获取焦点（true）
     *                      ...
     *
     */
    final void startActivityLocked(ActivityRecord r, boolean newTask,
                                   boolean doResume, boolean keepCurTransition, Bundle options) {
        TaskRecord rTask = r.task;
        final int taskId = rTask.taskId;
        // mLaunchTaskBehind tasks get placed at the back of the task stack.
        if (!r.mLaunchTaskBehind && (taskForIdLocked(taskId) == null || newTask)) {
            // Last activity in task had been removed or ActivityManagerService is reusing task.
            // Insert or replace.
            // Might not even be in.
            insertTaskAtTop(rTask);
            mWindowManager.moveTaskToTop(taskId);
        }
        TaskRecord task = null;
        //①.不用创建新进程的情况，需要做一些任务切换操作
        if (!newTask) {
            boolean startIt = true;
            //遍历所有的任务，找到目标activity所在的堆栈，taskNdx为所有的task的数量，肯定是大于0
            for (int taskNdx = mTaskHistory.size() - 1; taskNdx >= 0; --taskNdx) {
                task = mTaskHistory.get(taskNdx);
                if (task.getTopActivity() == null) {
                    // 如果进程中activity为空，继续遍历
                    continue;
                }
                if (task == r.task) {
                    //如果当前task==需要开启的activity的进程
                    if (!startIt) {
                        if (DEBUG_ADD_REMOVE) Slog.i(TAG, "Adding activity " + r + " to task "
                                + task, new RuntimeException("here").fillInStackTrace());
                        // 将需要启动的activity的记录放入task堆栈的顶层
                        task.addActivityToTop(r);
                        r.putInHistory();
                        mWindowManager.addAppToken(task.mActivities.indexOf(r), r.appToken,
                                r.task.taskId, mStackId, r.info.screenOrientation, r.fullscreen,
                                (r.info.flags & ActivityInfo.FLAG_SHOW_ON_LOCK_SCREEN) != 0,
                                r.userId, r.info.configChanges, task.voiceSession != null,
                                r.mLaunchTaskBehind);
                        ...
                        return;
                    }
                    break;
                } else if (task.numFullscreen > 0) {
                    startIt = false;
                }
            }
        }

        //②. 在处于栈顶的进程中放置新的activity，这个activity将是即将和用户交互的界面
        task = r.task;
        //将activity插入历史堆栈顶层
        task.addActivityToTop(r);
        task.setFrontOfTask();
        r.putInHistory();
        if (!isHomeStack() || numActivities() > 0) {
            /*
             * 如果我们需要切换到一个新的任务，或者下一个activity不是当前正在运行的，
             * 我们需要显示启动预览窗口，在这里可能执行一切窗口切换的动画效果
             */
            boolean showStartingIcon = newTask;
            ProcessRecord proc = r.app;
            ...
            if ((r.intent.getFlags()&Intent.FLAG_ACTIVITY_NO_ANIMATION) != 0) {
                mWindowManager.prepareAppTransition(AppTransition.TRANSIT_NONE, keepCurTransition);
                mNoAnimActivities.add(r);
            } else {
                //执行切换动画
                mWindowManager.prepareAppTransition(...);
                mNoAnimActivities.remove(r);
            }
            mWindowManager.addAppToken(task.mActivities.indexOf(r),
                    r.appToken, r.task.taskId, mStackId, r.info.screenOrientation, r.fullscreen,
                    (r.info.flags & ActivityInfo.FLAG_SHOW_ON_LOCK_SCREEN) != 0, r.userId,
                    r.info.configChanges, task.voiceSession != null, r.mLaunchTaskBehind);
            ...
        } else {
            /*
             * 如果需要启动的activity的信息已经是堆栈中第一个，不需要执行动画
             */
            mWindowManager.addAppToken(task.mActivities.indexOf(r), r.appToken,
                    r.task.taskId, mStackId, r.info.screenOrientation, r.fullscreen,
                    (r.info.flags & ActivityInfo.FLAG_SHOW_ON_LOCK_SCREEN) != 0, r.userId,
                    r.info.configChanges, task.voiceSession != null, r.mLaunchTaskBehind);
            ...
        }
        ...
        if (doResume) {
            //此处的doResume参数为true，继续调用ActivityStackSupervisor.resumeTopActivitiesLocked()
            mStackSupervisor.resumeTopActivitiesLocked(this, r, options);
        }
    }


    /**
     * step9 : ActivityStackSupervisor.resumeTopActivitiesLocked()
     * 这个方法按方法名可以猜测它会将堆栈中的最顶部的activity显示出来，
     */
    boolean resumeTopActivitiesLocked(ActivityStack targetStack, ActivityRecord target,
                                      Bundle targetOptions) {
        if (targetStack == null) {
            targetStack = getFocusedStack();
        }
        // Do targetStack first.
        boolean result = false;
        //是否是栈顶的任务
        if (isFrontStack(targetStack)) {
            result = targetStack.resumeTopActivityLocked(target, targetOptions);
        }
        for (int displayNdx = mActivityDisplays.size() - 1; displayNdx >= 0; --displayNdx) {
            final ArrayList<ActivityStack> stacks = mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; --stackNdx) {
                final ActivityStack stack = stacks.get(stackNdx);
                if (stack == targetStack) {
                    // Already started above.
                    continue;
                }
                if (isFrontStack(stack)) {
                    //会调用resumeTopActivityLocked(ActivityRecord prev, Bundle options)
                    stack.resumeTopActivityLocked(null);
                }
            }
        }
        return result;
    }


    /**
     * step10 : ActivityStack.resumeTopActivityLocked()
     * prev：以前恢复活性，当在暂停过程；可以零称从别处。
     */
    final boolean resumeTopActivityLocked(ActivityRecord prev, Bundle options) {
        if (mStackSupervisor.inResumeTopActivity) {
            // Don't even start recursing.
            return false;
        }

        boolean result = false;
        try {
            // Protect against recursion.
            mStackSupervisor.inResumeTopActivity = true;
            if (mService.mLockScreenShown == ActivityManagerService.LOCK_SCREEN_LEAVING) {
                mService.mLockScreenShown = ActivityManagerService.LOCK_SCREEN_HIDDEN;
                mService.updateSleepIfNeededLocked();
            }
            result = resumeTopActivityInnerLocked(prev, options);
        } finally {
            mStackSupervisor.inResumeTopActivity = false;
        }
        return result;
    }

    /**
     * step10 : ActivityStack.resumeTopActivityInnerLocked
     */
    final boolean resumeTopActivityInnerLocked(ActivityRecord prev, Bundle options) {
        ...

        ActivityRecord parent = mActivityContainer.mParentActivity;
        if ((parent != null && parent.state != ActivityState.RESUMED) ||
                !mActivityContainer.isAttachedLocked()) {
            // Do not resume this stack if its parent is not resumed.
            // TODO: If in a loop, make sure that parent stack resumeTopActivity is called 1st.
            return false;
        }

        cancelInitializingActivities();

        // Find the first activity that is not finishing.
        final ActivityRecord next = topRunningActivityLocked(null);

        // Remember how we'll process this pause/resume situation, and ensure
        // that the state is reset however we wind up proceeding.
        final boolean userLeaving = mStackSupervisor.mUserLeaving;
        mStackSupervisor.mUserLeaving = false;

        final TaskRecord prevTask = prev != null ? prev.task : null;
        if (next == null) {
            // There are no more activities!  Let's just start up the
            // Launcher...
            ActivityOptions.abort(options);
            if (DEBUG_STATES) Slog.d(TAG, "resumeTopActivityLocked: No more activities go home");
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            // Only resume home if on home display
            final int returnTaskType = prevTask == null || !prevTask.isOverHomeStack() ?
                    HOME_ACTIVITY_TYPE : prevTask.getTaskToReturnTo();
            return isOnHomeDisplay() &&
                    mStackSupervisor.resumeHomeStackTask(returnTaskType, prev, "noMoreActivities");
        }

        next.delayedResume = false;

        // If the top activity is the resumed one, nothing to do.
        if (mResumedActivity == next && next.state == ActivityState.RESUMED &&
                mStackSupervisor.allResumedActivitiesComplete()) {
            // Make sure we have executed any pending transitions, since there
            // should be nothing left to do at this point.
            mWindowManager.executeAppTransition();
            mNoAnimActivities.clear();
            ActivityOptions.abort(options);
            if (DEBUG_STATES) Slog.d(TAG, "resumeTopActivityLocked: Top activity resumed " + next);
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }

        final TaskRecord nextTask = next.task;
        if (prevTask != null && prevTask.stack == this &&
                prevTask.isOverHomeStack() && prev.finishing && prev.frontOfTask) {
            if (DEBUG_STACK)  mStackSupervisor.validateTopActivitiesLocked();
            if (prevTask == nextTask) {
                prevTask.setFrontOfTask();
            } else if (prevTask != topTask()) {
                // This task is going away but it was supposed to return to the home stack.
                // Now the task above it has to return to the home task instead.
                final int taskNdx = mTaskHistory.indexOf(prevTask) + 1;
                mTaskHistory.get(taskNdx).setTaskToReturnTo(HOME_ACTIVITY_TYPE);
            } else if (!isOnHomeDisplay()) {
                return false;
            } else if (!isHomeStack()){
                if (DEBUG_STATES) Slog.d(TAG,
                        "resumeTopActivityLocked: Launching home next");
                final int returnTaskType = prevTask == null || !prevTask.isOverHomeStack() ?
                        HOME_ACTIVITY_TYPE : prevTask.getTaskToReturnTo();
                return mStackSupervisor.resumeHomeStackTask(returnTaskType, prev, "prevFinished");
            }
        }

        // If we are sleeping, and there is no resumed activity, and the top
        // activity is paused, well that is the state we want.
        if (mService.isSleepingOrShuttingDown()
                && mLastPausedActivity == next
                && mStackSupervisor.allPausedActivitiesComplete()) {
            // Make sure we have executed any pending transitions, since there
            // should be nothing left to do at this point.
            mWindowManager.executeAppTransition();
            mNoAnimActivities.clear();
            ActivityOptions.abort(options);
            if (DEBUG_STATES) Slog.d(TAG, "resumeTopActivityLocked: Going to sleep and all paused");
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }

        // Make sure that the user who owns this activity is started.  If not,
        // we will just leave it as is because someone should be bringing
        // another user's activities to the top of the stack.
        if (mService.mStartedUsers.get(next.userId) == null) {
            Slog.w(TAG, "Skipping resume of top activity " + next
                    + ": user " + next.userId + " is stopped");
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }

        // The activity may be waiting for stop, but that is no longer
        // appropriate for it.
        mStackSupervisor.mStoppingActivities.remove(next);
        mStackSupervisor.mGoingToSleepActivities.remove(next);
        next.sleeping = false;
        mStackSupervisor.mWaitingVisibleActivities.remove(next);

        if (DEBUG_SWITCH) Slog.v(TAG, "Resuming " + next);

        // If we are currently pausing an activity, then don't do anything
        // until that is done.
        if (!mStackSupervisor.allPausedActivitiesComplete()) {
            if (DEBUG_SWITCH || DEBUG_PAUSE || DEBUG_STATES) Slog.v(TAG,
                    "resumeTopActivityLocked: Skip resume: some activity pausing.");
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }

        // Okay we are now going to start a switch, to 'next'.  We may first
        // have to pause the current activity, but this is an important point
        // where we have decided to go to 'next' so keep track of that.
        // XXX "App Redirected" dialog is getting too many false positives
        // at this point, so turn off for now.
        if (false) {
            if (mLastStartedActivity != null && !mLastStartedActivity.finishing) {
                long now = SystemClock.uptimeMillis();
                final boolean inTime = mLastStartedActivity.startTime != 0
                        && (mLastStartedActivity.startTime + START_WARN_TIME) >= now;
                final int lastUid = mLastStartedActivity.info.applicationInfo.uid;
                final int nextUid = next.info.applicationInfo.uid;
                if (inTime && lastUid != nextUid
                        && lastUid != next.launchedFromUid
                        && mService.checkPermission(
                        android.Manifest.permission.STOP_APP_SWITCHES,
                        -1, next.launchedFromUid)
                        != PackageManager.PERMISSION_GRANTED) {
                    mService.showLaunchWarningLocked(mLastStartedActivity, next);
                } else {
                    next.startTime = now;
                    mLastStartedActivity = next;
                }
            } else {
                next.startTime = SystemClock.uptimeMillis();
                mLastStartedActivity = next;
            }
        }

        // We need to start pausing the current activity so the top one
        // can be resumed...
        boolean dontWaitForPause = (next.info.flags&ActivityInfo.FLAG_RESUME_WHILE_PAUSING) != 0;
        boolean pausing = mStackSupervisor.pauseBackStacks(userLeaving, true, dontWaitForPause);
        if (mResumedActivity != null) {
            if (DEBUG_STATES) Slog.d(TAG, "resumeTopActivityLocked: Pausing " + mResumedActivity);
            pausing |= startPausingLocked(userLeaving, false, true, dontWaitForPause);
        }
        if (pausing) {
            if (DEBUG_SWITCH || DEBUG_STATES) Slog.v(TAG,
                    "resumeTopActivityLocked: Skip resume: need to start pausing");
            // At this point we want to put the upcoming activity's process
            // at the top of the LRU list, since we know we will be needing it
            // very soon and it would be a waste to let it get killed if it
            // happens to be sitting towards the end.
            if (next.app != null && next.app.thread != null) {
                mService.updateLruProcessLocked(next.app, true, null);
            }
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            return true;
        }

        // If the most recent activity was noHistory but was only stopped rather
        // than stopped+finished because the device went to sleep, we need to make
        // sure to finish it as we're making a new activity topmost.
        if (mService.isSleeping() && mLastNoHistoryActivity != null &&
                !mLastNoHistoryActivity.finishing) {
            if (DEBUG_STATES) Slog.d(TAG, "no-history finish of " + mLastNoHistoryActivity +
                    " on new resume");
            requestFinishActivityLocked(mLastNoHistoryActivity.appToken, Activity.RESULT_CANCELED,
                    null, "no-history", false);
            mLastNoHistoryActivity = null;
        }

        if (prev != null && prev != next) {
            if (!prev.waitingVisible && next != null && !next.nowVisible) {
                prev.waitingVisible = true;
                mStackSupervisor.mWaitingVisibleActivities.add(prev);
                if (DEBUG_SWITCH) Slog.v(
                        TAG, "Resuming top, waiting visible to hide: " + prev);
            } else {
                // The next activity is already visible, so hide the previous
                // activity's windows right now so we can show the new one ASAP.
                // We only do this if the previous is finishing, which should mean
                // it is on top of the one being resumed so hiding it quickly
                // is good.  Otherwise, we want to do the normal route of allowing
                // the resumed activity to be shown so we can decide if the
                // previous should actually be hidden depending on whether the
                // new one is found to be full-screen or not.
                if (prev.finishing) {
                    mWindowManager.setAppVisibility(prev.appToken, false);
                    if (DEBUG_SWITCH) Slog.v(TAG, "Not waiting for visible to hide: "
                            + prev + ", waitingVisible="
                            + (prev != null ? prev.waitingVisible : null)
                            + ", nowVisible=" + next.nowVisible);
                } else {
                    if (DEBUG_SWITCH) Slog.v(TAG, "Previous already visible but still waiting to hide: "
                            + prev + ", waitingVisible="
                            + (prev != null ? prev.waitingVisible : null)
                            + ", nowVisible=" + next.nowVisible);
                }
            }
        }

        // Launching this app's activity, make sure the app is no longer
        // considered stopped.
        try {
            AppGlobals.getPackageManager().setPackageStoppedState(
                    next.packageName, false, next.userId); /* TODO: Verify if correct userid */
        } catch (RemoteException e1) {
        } catch (IllegalArgumentException e) {
            Slog.w(TAG, "Failed trying to unstop package "
                    + next.packageName + ": " + e);
        }

        // We are starting up the next activity, so tell the window manager
        // that the previous one will be hidden soon.  This way it can know
        // to ignore it when computing the desired screen orientation.
        boolean anim = true;
        if (prev != null) {
            if (prev.finishing) {
                if (DEBUG_TRANSITION) Slog.v(TAG,
                        "Prepare close transition: prev=" + prev);
                if (mNoAnimActivities.contains(prev)) {
                    anim = false;
                    mWindowManager.prepareAppTransition(AppTransition.TRANSIT_NONE, false);
                } else {
                    mWindowManager.prepareAppTransition(prev.task == next.task
                            ? AppTransition.TRANSIT_ACTIVITY_CLOSE
                            : AppTransition.TRANSIT_TASK_CLOSE, false);
                }
                mWindowManager.setAppWillBeHidden(prev.appToken);
                mWindowManager.setAppVisibility(prev.appToken, false);
            } else {
                if (DEBUG_TRANSITION) Slog.v(TAG, "Prepare open transition: prev=" + prev);
                if (mNoAnimActivities.contains(next)) {
                    anim = false;
                    mWindowManager.prepareAppTransition(AppTransition.TRANSIT_NONE, false);
                } else {
                    mWindowManager.prepareAppTransition(prev.task == next.task
                            ? AppTransition.TRANSIT_ACTIVITY_OPEN
                            : next.mLaunchTaskBehind
                            ? AppTransition.TRANSIT_TASK_OPEN_BEHIND
                            : AppTransition.TRANSIT_TASK_OPEN, false);
                }
            }
            if (false) {
                mWindowManager.setAppWillBeHidden(prev.appToken);
                mWindowManager.setAppVisibility(prev.appToken, false);
            }
        } else {
            if (DEBUG_TRANSITION) Slog.v(TAG, "Prepare open transition: no previous");
            if (mNoAnimActivities.contains(next)) {
                anim = false;
                mWindowManager.prepareAppTransition(AppTransition.TRANSIT_NONE, false);
            } else {
                mWindowManager.prepareAppTransition(AppTransition.TRANSIT_ACTIVITY_OPEN, false);
            }
        }

        Bundle resumeAnimOptions = null;
        if (anim) {
            ActivityOptions opts = next.getOptionsForTargetActivityLocked();
            if (opts != null) {
                resumeAnimOptions = opts.toBundle();
            }
            next.applyOptionsLocked();
        } else {
            next.clearOptionsLocked();
        }

        ActivityStack lastStack = mStackSupervisor.getLastStack();
        if (next.app != null && next.app.thread != null) {
            if (DEBUG_SWITCH) Slog.v(TAG, "Resume running: " + next);

            // This activity is now becoming visible.
            mWindowManager.setAppVisibility(next.appToken, true);

            // schedule launch ticks to collect information about slow apps.
            next.startLaunchTickingLocked();

            ActivityRecord lastResumedActivity =
                    lastStack == null ? null :lastStack.mResumedActivity;
            ActivityState lastState = next.state;

            mService.updateCpuStats();

            if (DEBUG_STATES) Slog.v(TAG, "Moving to RESUMED: " + next + " (in existing)");
            next.state = ActivityState.RESUMED;
            mResumedActivity = next;
            next.task.touchActiveTime();
            mService.addRecentTaskLocked(next.task);
            mService.updateLruProcessLocked(next.app, true, null);
            updateLRUListLocked(next);
            mService.updateOomAdjLocked();

            // Have the window manager re-evaluate the orientation of
            // the screen based on the new activity order.
            boolean notUpdated = true;
            if (mStackSupervisor.isFrontStack(this)) {
                Configuration config = mWindowManager.updateOrientationFromAppTokens(
                        mService.mConfiguration,
                        next.mayFreezeScreenLocked(next.app) ? next.appToken : null);
                if (config != null) {
                    next.frozenBeforeDestroy = true;
                }
                notUpdated = !mService.updateConfigurationLocked(config, next, false, false);
            }

            if (notUpdated) {
                // The configuration update wasn't able to keep the existing
                // instance of the activity, and instead started a new one.
                // We should be all done, but let's just make sure our activity
                // is still at the top and schedule another run if something
                // weird happened.
                ActivityRecord nextNext = topRunningActivityLocked(null);
                if (DEBUG_SWITCH || DEBUG_STATES) Slog.i(TAG,
                        "Activity config changed during resume: " + next
                                + ", new next: " + nextNext);
                if (nextNext != next) {
                    // Do over!
                    mStackSupervisor.scheduleResumeTopActivities();
                }
                if (mStackSupervisor.reportResumedActivityLocked(next)) {
                    mNoAnimActivities.clear();
                    if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
                    return true;
                }
                if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
                return false;
            }

            try {
                // Deliver all pending results.
                ArrayList<ResultInfo> a = next.results;
                if (a != null) {
                    final int N = a.size();
                    if (!next.finishing && N > 0) {
                        if (DEBUG_RESULTS) Slog.v(
                                TAG, "Delivering results to " + next
                                        + ": " + a);
                        next.app.thread.scheduleSendResult(next.appToken, a);
                    }
                }

                if (next.newIntents != null) {
                    next.app.thread.scheduleNewIntent(next.newIntents, next.appToken);
                }

                EventLog.writeEvent(EventLogTags.AM_RESUME_ACTIVITY, next.userId,
                        System.identityHashCode(next), next.task.taskId, next.shortComponentName);

                next.sleeping = false;
                mService.showAskCompatModeDialogLocked(next);
                next.app.pendingUiClean = true;
                next.app.forceProcessStateUpTo(ActivityManager.PROCESS_STATE_TOP);
                next.clearOptionsLocked();
                next.app.thread.scheduleResumeActivity(next.appToken, next.app.repProcState,
                        mService.isNextTransitionForward(), resumeAnimOptions);

                mStackSupervisor.checkReadyForSleepLocked();

                if (DEBUG_STATES) Slog.d(TAG, "resumeTopActivityLocked: Resumed " + next);
            } catch (Exception e) {
                // Whoops, need to restart this activity!
                if (DEBUG_STATES) Slog.v(TAG, "Resume failed; resetting state to "
                        + lastState + ": " + next);
                next.state = lastState;
                if (lastStack != null) {
                    lastStack.mResumedActivity = lastResumedActivity;
                }
                Slog.i(TAG, "Restarting because process died: " + next);
                if (!next.hasBeenLaunched) {
                    next.hasBeenLaunched = true;
                } else  if (SHOW_APP_STARTING_PREVIEW && lastStack != null &&
                        mStackSupervisor.isFrontStack(lastStack)) {
                    mWindowManager.setAppStartingWindow(
                            next.appToken, next.packageName, next.theme,
                            mService.compatibilityInfoForPackageLocked(next.info.applicationInfo),
                            next.nonLocalizedLabel, next.labelRes, next.icon, next.logo,
                            next.windowFlags, null, true);
                }
                mStackSupervisor.startSpecificActivityLocked(next, true, false);
                if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
                return true;
            }

            // From this point on, if something goes wrong there is no way
            // to recover the activity.
            try {
                next.visible = true;
                completeResumeLocked(next);
            } catch (Exception e) {
                // If any exception gets thrown, toss away this
                // activity and try the next one.
                Slog.w(TAG, "Exception thrown during resume of " + next, e);
                requestFinishActivityLocked(next.appToken, Activity.RESULT_CANCELED, null,
                        "resume-exception", true);
                if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
                return true;
            }
            next.stopped = false;

        } else {
            // Whoops, need to restart this activity!
            if (!next.hasBeenLaunched) {
                next.hasBeenLaunched = true;
            } else {
                if (SHOW_APP_STARTING_PREVIEW) {
                    mWindowManager.setAppStartingWindow(
                            next.appToken, next.packageName, next.theme,
                            mService.compatibilityInfoForPackageLocked(
                                    next.info.applicationInfo),
                            next.nonLocalizedLabel,
                            next.labelRes, next.icon, next.logo, next.windowFlags,
                            null, true);
                }
                if (DEBUG_SWITCH) Slog.v(TAG, "Restarting: " + next);
            }
            if (DEBUG_STATES) Slog.d(TAG, "resumeTopActivityLocked: Restarting " + next);
            mStackSupervisor.startSpecificActivityLocked(next, true, true);
        }

        if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
        return true;
    }


    /**
     * step11：ActivityStackSupervisor.startSpecificActivityLocked()
     * 这个方法用于判断需要开启的activity所在的进程是否已经启动，
     * 如果已经启动，会执行第①中情况开启activity，
     * 如果没有启动，将会走第②中情况，先去启动进程，然后在开启activity。
     * 由于第②种情况是一个比较完整的过程，并且后面也会调用realStartActivityLocked()方法开启activity，
     * 所以，我们继续分析第②种。
     */
    void startSpecificActivityLocked(ActivityRecord r,
                                     boolean andResume, boolean checkConfig) {
        //判断activity所属的应用程序的进程（process + uid）是否已经启动
        ProcessRecord app = mService.getProcessRecordLocked(r.processName,
                r.info.applicationInfo.uid, true);
        r.task.stack.setLaunchTime(r);
        if (app != null && app.thread != null) {
            try {
                ...
                /*
                 * ① 如果应用已经启动，并且进程中的thread对象不为空，
                 *   调用realStartActivityLocked()方法创建activity对象
                 *
                 *   继续跟下去，会发现调用activity的onCreate方法
                 */
                realStartActivityLocked(r, app, andResume, checkConfig);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception when starting activity "
                        + r.intent.getComponent().flattenToShortString(), e);
            }
            // If a dead object exception was thrown -- fall through to
            // restart the application.
        }
        //② 如果抛出了异常或者获取的应用进程为空，需用重新启动应用程序，点击Launcher桌面上图表时走这里
        mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0,
                "activity", r.intent.getComponent(), false, false, true);
    }

    /**
     * step12: ActivityManagerService.startProcessLocked()
     * 创建新进程。
     */
    final ProcessRecord startProcessLocked(String processName,
                                           ApplicationInfo info, boolean knownToBeDead, int intentFlags,
                                           String hostingType, ComponentName hostingName, boolean allowWhileBooting,
                                           boolean isolated, boolean keepIfLarge) {
        return startProcessLocked(processName, info, knownToBeDead, intentFlags, hostingType,
                hostingName, allowWhileBooting, isolated, 0 /* isolatedUid */, keepIfLarge,
                null /* ABI override */, null /* entryPoint */, null /* entryPointArgs */,
                null /* crashHandler */);
    }
    final ProcessRecord startProcessLocked(String processName, ApplicationInfo info,
                                           boolean knownToBeDead, int intentFlags, String hostingType, ComponentName hostingName,
                                           boolean allowWhileBooting, boolean isolated, int isolatedUid, boolean keepIfLarge,
                                           String abiOverride, String entryPoint, String[] entryPointArgs, Runnable crashHandler) {
        long startTime = SystemClock.elapsedRealtime();
        ProcessRecord app;
        //isolated==false
        if (!isolated) {
            //再次检查是否已经有以process + uid命名的进程存在
            app = getProcessRecordLocked(processName, info.uid, keepIfLarge);
            checkTime(startTime, "startProcess: after getProcessRecord");
        } else {
            app = null;
        }
        //接下来有一些if语句，用于判断是否需要创建新进程，如果满足下面三种情况，就不会创建新进程
        // We don't have to do anything more if:
        // (1) There is an existing application record; and
        // (2) The caller doesn't think it is dead, OR there is no thread
        //     object attached to it so we know it couldn't have crashed; and
        // (3) There is a pid assigned to it, so it is either starting or
        //     already running.
        ...

        //创建新进程
        startProcessLocked(app, hostingType, hostingNameStr, abiOverride, entryPoint, entryPointArgs);
        return (app.pid != 0) ? app : null;
    }

    private final void startProcessLocked(ProcessRecord app, String hostingType,
                                          String hostingNameStr, String abiOverride, String entryPoint, String[] entryPointArgs) {
        ...
        try {
            int uid = app.uid;
            int[] gids = null;
            int mountExternal = Zygote.MOUNT_EXTERNAL_NONE;
            if (!app.isolated) {
                int[] permGids = null;
                try {
                    final PackageManager pm = mContext.getPackageManager();
                    permGids = pm.getPackageGids(app.info.packageName);

                    ...
                } catch (PackageManager.NameNotFoundException e) {
                }
                if (permGids == null) {
                    gids = new int[2];
                } else {
                    gids = new int[permGids.length + 2];
                    System.arraycopy(permGids, 0, gids, 2, permGids.length);
                }
                gids[0] = UserHandle.getSharedAppGid(UserHandle.getAppId(uid));
                gids[1] = UserHandle.getUserGid(UserHandle.getUserId(uid));
            }
            ...

            app.gids = gids;
            app.requiredAbi = requiredAbi;
            app.instructionSet = instructionSet;

            //是否是Activity所在的进程，此处entryPoint为null所以isActivityProcess为true
            boolean isActivityProcess = (entryPoint == null);
            if (entryPoint == null)
                entryPoint = "android.app.ActivityThread";
            //调用Process.start接口来创建一个新的进程，并会创建一个android.app.ActivityThread类的对象，
            //并且执行它的main函数，ActivityThread是应用程序的主线程
            Process.ProcessStartResult startResult =
                    Process.start(entryPoint,
                    app.processName, uid, uid, gids, debugFlags, mountExternal,
                    app.info.targetSdkVersion, app.info.seinfo, requiredAbi, instructionSet,
                    app.info.dataDir, entryPointArgs);
            ...
        } catch (RuntimeException e) {
            //创建进程失败
            Slog.e(TAG, "Failure starting process " + app.processName, e);
        }
    }

    /**
     * step13: ActivityThread
     * ActivityThread是应用程序进程中的主线程，他的作用是调度和执行activities、广播和其他操作
     */
    public final class ActivityThread {
        final ApplicationThread mAppThread = new ApplicationThread();
        //新的进程创建后就会执行这个main方法
        public static void main(String[] args) {
            ...

            Looper.prepareMainLooper();

            //创建一个ActivityThread实例，并调用他的attach方法
            ActivityThread thread = new ActivityThread();
            thread.attach(false);
            ...

            if (false) {
                Looper.myLooper().setMessageLogging(new
                        LogPrinter(Log.DEBUG, "ActivityThread"));
            }
            ...
            //进入消息循环
            Looper.loop();

            throw new RuntimeException("Main thread loop unexpectedly exited");
        }

        private void attach(boolean system) {
            sCurrentActivityThread = this;
            mSystemThread = system;
            if (!system) {
                ...
                /*
                 * ActivityManagerNative.getDefault()方法返回的是一个ActivityManagerProxy对象，
                 * ActivityManagerProxy实现了IActivityManager接口，并维护了一个mRemote，
                 * 这个mRemote就是ActivityManagerService的远程代理对象
                 */
                final IActivityManager mgr = ActivityManagerNative.getDefault();
                try {
                    //调用attachApplication()，并将mAppThread传入，mAppThread是ApplicationThread类的示例，他的作用是用来进程间通信的
                    mgr.attachApplication(mAppThread);
                } catch (RemoteException ex) {
                    // Ignore
                }
                ...
            } else {
               ...
            }
         ...
        }
    }

    /**
     * step14: ActivityManagerNative内部类ActivityManagerProxy.attachApplication()
     * 通过ActivityManagerService的远程代理对象mRemote，进入ActivityManagerService的attachApplication
     */
    public void attachApplication(IApplicationThread app) throws RemoteException
    {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IActivityManager.descriptor);
        data.writeStrongBinder(app.asBinder());
        mRemote.transact(ATTACH_APPLICATION_TRANSACTION, data, reply, 0);
        reply.readException();
        data.recycle();
        reply.recycle();
    }

    /**
     * step15: ActivityManagerService.attachApplication()
     */
    @Override
    public final void attachApplication(IApplicationThread thread) {
        synchronized (this) {
            int callingPid = Binder.getCallingPid();
            final long origId = Binder.clearCallingIdentity();
            //接着调用
            attachApplicationLocked(thread, callingPid);
            Binder.restoreCallingIdentity(origId);
        }
    }

    /**
     * 在前面的Step 23中，已经创建了一个ProcessRecord，这里首先通过pid将它取回来，放在app变量中，
     * 然后对app的其它成员进行初始化，最后调用mMainStack.realStartActivityLocked执行
     * 真正的Activity启动操作。这里要启动的Activity通过
     * 调用mMainStack.topRunningActivityLocked(null)从堆栈顶端取回来，
     * 这时候在堆栈顶端的Activity就是MainActivity了。
     */
    private final boolean attachApplicationLocked(IApplicationThread thread,
                                                  int pid) {

        // Find the application record that is being attached...  either via
        // the pid if we are running in multiple processes, or just pull the
        // next app record if we are emulating process with anonymous threads.
        ProcessRecord app;
        if (pid != MY_PID && pid >= 0) {
            synchronized (mPidsSelfLocked) {
                app = mPidsSelfLocked.get(pid);
            }
        } else {
            app = null;
        }

        if (app == null) {
            Slog.w(TAG, "No pending application record for pid " + pid
                    + " (IApplicationThread " + thread + "); dropping process");
            EventLog.writeEvent(EventLogTags.AM_DROP_PROCESS, pid);
            if (pid > 0 && pid != MY_PID) {
                Process.killProcessQuiet(pid);
                //TODO: Process.killProcessGroup(app.info.uid, pid);
            } else {
                try {
                    thread.scheduleExit();
                } catch (Exception e) {
                    // Ignore exceptions.
                }
            }
            return false;
        }

        // If this application record is still attached to a previous
        // process, clean it up now.
        if (app.thread != null) {
            handleAppDiedLocked(app, true, true);
        }

        // Tell the process all about itself.

        if (localLOGV) Slog.v(
                TAG, "Binding process pid " + pid + " to record " + app);

        final String processName = app.processName;
        try {
            AppDeathRecipient adr = new AppDeathRecipient(
                    app, pid, thread);
            thread.asBinder().linkToDeath(adr, 0);
            app.deathRecipient = adr;
        } catch (RemoteException e) {
            app.resetPackageList(mProcessStats);
            startProcessLocked(app, "link fail", processName);
            return false;
        }

        EventLog.writeEvent(EventLogTags.AM_PROC_BOUND, app.userId, app.pid, app.processName);

        app.makeActive(thread, mProcessStats);
        app.curAdj = app.setAdj = -100;
        app.curSchedGroup = app.setSchedGroup = Process.THREAD_GROUP_DEFAULT;
        app.forcingToForeground = null;
        updateProcessForegroundLocked(app, false, false);
        app.hasShownUi = false;
        app.debugging = false;
        app.cached = false;
        app.killedByAm = false;

        mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);

        boolean normalMode = mProcessesReady || isAllowedWhileBooting(app.info);
        List<ProviderInfo> providers = normalMode ? generateApplicationProvidersLocked(app) : null;

        if (!normalMode) {
            Slog.i(TAG, "Launching preboot mode app: " + app);
        }

        if (localLOGV) Slog.v(
                TAG, "New app record " + app
                        + " thread=" + thread.asBinder() + " pid=" + pid);
        try {
            int testMode = IApplicationThread.DEBUG_OFF;
            if (mDebugApp != null && mDebugApp.equals(processName)) {
                testMode = mWaitForDebugger
                        ? IApplicationThread.DEBUG_WAIT
                        : IApplicationThread.DEBUG_ON;
                app.debugging = true;
                if (mDebugTransient) {
                    mDebugApp = mOrigDebugApp;
                    mWaitForDebugger = mOrigWaitForDebugger;
                }
            }
            String profileFile = app.instrumentationProfileFile;
            ParcelFileDescriptor profileFd = null;
            int samplingInterval = 0;
            boolean profileAutoStop = false;
            if (mProfileApp != null && mProfileApp.equals(processName)) {
                mProfileProc = app;
                profileFile = mProfileFile;
                profileFd = mProfileFd;
                samplingInterval = mSamplingInterval;
                profileAutoStop = mAutoStopProfiler;
            }
            boolean enableOpenGlTrace = false;
            if (mOpenGlTraceApp != null && mOpenGlTraceApp.equals(processName)) {
                enableOpenGlTrace = true;
                mOpenGlTraceApp = null;
            }

            // If the app is being launched for restore or full backup, set it up specially
            boolean isRestrictedBackupMode = false;
            if (mBackupTarget != null && mBackupAppName.equals(processName)) {
                isRestrictedBackupMode = (mBackupTarget.backupMode == BackupRecord.RESTORE)
                        || (mBackupTarget.backupMode == BackupRecord.RESTORE_FULL)
                        || (mBackupTarget.backupMode == BackupRecord.BACKUP_FULL);
            }

            ensurePackageDexOpt(app.instrumentationInfo != null
                    ? app.instrumentationInfo.packageName
                    : app.info.packageName);
            if (app.instrumentationClass != null) {
                ensurePackageDexOpt(app.instrumentationClass.getPackageName());
            }
            if (DEBUG_CONFIGURATION) Slog.v(TAG, "Binding proc "
                    + processName + " with config " + mConfiguration);
            ApplicationInfo appInfo = app.instrumentationInfo != null
                    ? app.instrumentationInfo : app.info;
            app.compat = compatibilityInfoForPackageLocked(appInfo);
            if (profileFd != null) {
                profileFd = profileFd.dup();
            }
            ProfilerInfo profilerInfo = profileFile == null ? null
                    : new ProfilerInfo(profileFile, profileFd, samplingInterval, profileAutoStop);
            thread.bindApplication(processName, appInfo, providers, app.instrumentationClass,
                    profilerInfo, app.instrumentationArguments, app.instrumentationWatcher,
                    app.instrumentationUiAutomationConnection, testMode, enableOpenGlTrace,
                    isRestrictedBackupMode || !normalMode, app.persistent,
                    new Configuration(mConfiguration), app.compat,
                    getCommonServicesLocked(app.isolated),
                    mCoreSettingsObserver.getCoreSettingsLocked());
            updateLruProcessLocked(app, false, null);
            app.lastRequestedGc = app.lastLowMemory = SystemClock.uptimeMillis();
        } catch (Exception e) {
            // todo: Yikes!  What should we do?  For now we will try to
            // start another process, but that could easily get us in
            // an infinite loop of restarting processes...
            Slog.wtf(TAG, "Exception thrown during bind of " + app, e);

            app.resetPackageList(mProcessStats);
            app.unlinkDeathRecipient();
            startProcessLocked(app, "bind fail", processName);
            return false;
        }

        // Remove this record from the list of starting applications.
        mPersistentStartingProcesses.remove(app);
        if (DEBUG_PROCESSES && mProcessesOnHold.contains(app)) Slog.v(TAG,
                "Attach application locked removing on hold: " + app);
        mProcessesOnHold.remove(app);

        boolean badApp = false;
        boolean didSomething = false;

        // See if the top visible activity is waiting to run in this process...
        if (normalMode) {
            try {
                if (mStackSupervisor.attachApplicationLocked(app)) {
                    didSomething = true;
                }
            } catch (Exception e) {
                Slog.wtf(TAG, "Exception thrown launching activities in " + app, e);
                badApp = true;
            }
        }

        // Find any services that should be running in this process...
        if (!badApp) {
            try {
                didSomething |= mServices.attachApplicationLocked(app, processName);
            } catch (Exception e) {
                Slog.wtf(TAG, "Exception thrown starting services in " + app, e);
                badApp = true;
            }
        }

        // Check if a next-broadcast receiver is in this process...
        if (!badApp && isPendingBroadcastProcessLocked(pid)) {
            try {
                didSomething |= sendPendingBroadcastsLocked(app);
            } catch (Exception e) {
                // If the app died trying to launch the receiver we declare it 'bad'
                Slog.wtf(TAG, "Exception thrown dispatching broadcasts in " + app, e);
                badApp = true;
            }
        }

        // Check whether the next backup agent is in this process...
        if (!badApp && mBackupTarget != null && mBackupTarget.appInfo.uid == app.uid) {
            if (DEBUG_BACKUP) Slog.v(TAG, "New app is backup target, launching agent for " + app);
            ensurePackageDexOpt(mBackupTarget.appInfo.packageName);
            try {
                thread.scheduleCreateBackupAgent(mBackupTarget.appInfo,
                        compatibilityInfoForPackageLocked(mBackupTarget.appInfo),
                        mBackupTarget.backupMode);
            } catch (Exception e) {
                Slog.wtf(TAG, "Exception thrown creating backup agent in " + app, e);
                badApp = true;
            }
        }

        if (badApp) {
            app.kill("error during init", true);
            handleAppDiedLocked(app, false, true);
            return false;
        }

        if (!didSomething) {
            updateOomAdjLocked();
        }

        return true;
    }


    /**
     * ★★step16: ActivityStackSupervisor.realStartActivityLocked()
     * 这个方法真正的打开一个activity
     */
    final boolean realStartActivityLocked(ActivityRecord r,
                                          ProcessRecord app, boolean andResume, boolean checkConfig)
            throws RemoteException {

        r.startFreezingScreenLocked(app, 0);
        if (false) Slog.d(TAG, "realStartActivity: setting app visibility true");
        mWindowManager.setAppVisibility(r.appToken, true);

        // schedule launch ticks to collect information about slow apps.
        //附表推出蜱收集缓慢的应用程序的信息
        r.startLaunchTickingLocked();

        // Have the window manager re-evaluate the orientation of
        // the screen based on the new activity order.  Note that
        // as a result of this, it can call back into the activity
        // manager with a new orientation.  We don't care about that,
        // because the activity is not currently running so we are
        // just restarting it anyway.
        /*
         * 让窗口管理器重新评估基于新的活动顺序的屏幕的方向。请注意，由于这一结果，
         * 它可以调用一个新的方向的活动管理器。我们不关心，
         * 因为活动是不是目前运行，所以我们只是重新启动它反正。
         */
        if (checkConfig) {
            Configuration config = mWindowManager.updateOrientationFromAppTokens(
                    mService.mConfiguration,
                    r.mayFreezeScreenLocked(app) ? r.appToken : null);
            mService.updateConfigurationLocked(config, r, false, false);
        }

        r.app = app;
        app.waitingToKill = null;
        r.launchCount++;
        r.lastLaunchTime = SystemClock.uptimeMillis();

        if (localLOGV) Slog.v(TAG, "Launching: " + r);

        int idx = app.activities.indexOf(r);
        if (idx < 0) {
            app.activities.add(r);
        }
        mService.updateLruProcessLocked(app, true, null);
        mService.updateOomAdjLocked();

        final ActivityStack stack = r.task.stack;
        try {
            if (app.thread == null) {
                throw new RemoteException();
            }
            List<ResultInfo> results = null;
            List<ReferrerIntent> newIntents = null;
            if (andResume) {
                results = r.results;
                newIntents = r.newIntents;
            }
            if (DEBUG_SWITCH) Slog.v(TAG, "Launching: " + r
                    + " icicle=" + r.icicle
                    + " with results=" + results + " newIntents=" + newIntents
                    + " andResume=" + andResume);
            if (andResume) {
                EventLog.writeEvent(EventLogTags.AM_RESTART_ACTIVITY,
                        r.userId, System.identityHashCode(r),
                        r.task.taskId, r.shortComponentName);
            }
            if (r.isHomeActivity() && r.isNotResolverActivity()) {
                // Home process is the root process of the task.
                mService.mHomeProcess = r.task.mActivities.get(0).app;
            }
            mService.ensurePackageDexOpt(r.intent.getComponent().getPackageName());
            r.sleeping = false;
            r.forceNewConfig = false;
            mService.showAskCompatModeDialogLocked(r);
            r.compat = mService.compatibilityInfoForPackageLocked(r.info.applicationInfo);
            String profileFile = null;
            ParcelFileDescriptor profileFd = null;
            if (mService.mProfileApp != null && mService.mProfileApp.equals(app.processName)) {
                if (mService.mProfileProc == null || mService.mProfileProc == app) {
                    mService.mProfileProc = app;
                    profileFile = mService.mProfileFile;
                    profileFd = mService.mProfileFd;
                }
            }
            app.hasShownUi = true;
            app.pendingUiClean = true;
            if (profileFd != null) {
                try {
                    profileFd = profileFd.dup();
                } catch (IOException e) {
                    if (profileFd != null) {
                        try {
                            profileFd.close();
                        } catch (IOException o) {
                        }
                        profileFd = null;
                    }
                }
            }

            ProfilerInfo profilerInfo = profileFile != null
                    ? new ProfilerInfo(profileFile, profileFd, mService.mSamplingInterval,
                    mService.mAutoStopProfiler) : null;
            app.forceProcessStateUpTo(ActivityManager.PROCESS_STATE_TOP);
            app.thread.scheduleLaunchActivity(new Intent(r.intent), r.appToken,
                    System.identityHashCode(r), r.info, new Configuration(mService.mConfiguration),
                    r.compat, r.launchedFromPackage, r.task.voiceInteractor, app.repProcState,
                    r.icicle, r.persistentState, results, newIntents, !andResume,
                    mService.isNextTransitionForward(), profilerInfo);

            if ((app.info.flags&ApplicationInfo.FLAG_CANT_SAVE_STATE) != 0) {
                // This may be a heavy-weight process!  Note that the package
                // manager will ensure that only activity can run in the main
                // process of the .apk, which is the only thing that will be
                // considered heavy-weight.
                if (app.processName.equals(app.info.packageName)) {
                    if (mService.mHeavyWeightProcess != null
                            && mService.mHeavyWeightProcess != app) {
                        Slog.w(TAG, "Starting new heavy weight process " + app
                                + " when already running "
                                + mService.mHeavyWeightProcess);
                    }
                    mService.mHeavyWeightProcess = app;
                    Message msg = mService.mHandler.obtainMessage( ActivityManagerService.POST_HEAVY_NOTIFICATION_MSG);
                    msg.obj = r;
                    mService.mHandler.sendMessage(msg);
                }
            }

        } catch (RemoteException e) {
            if (r.launchFailed) {
                // This is the second time we failed -- finish activity
                // and give up.
                Slog.e(TAG, "Second failure launching "
                        + r.intent.getComponent().flattenToShortString()
                        + ", giving up", e);
                mService.appDiedLocked(app);
                stack.requestFinishActivityLocked(r.appToken, Activity.RESULT_CANCELED, null,
                        "2nd-crash", false);
                return false;
            }

            // This is the first time we failed -- restart process and
            // retry.
            app.activities.remove(r);
            throw e;
        }

        r.launchFailed = false;
        if (stack.updateLRUListLocked(r)) {
            Slog.w(TAG, "Activity " + r
                    + " being launched, but already in LRU list");
        }

        if (andResume) {
            // As part of the process of launching, ActivityThread also performs
            // a resume.
            stack.minimalResumeActivityLocked(r);
        } else {
            // This activity is not starting in the resumed state... which
            // should look like we asked it to pause+stop (but remain visible),
            // and it has done so and reported back the current icicle and
            // other state.
            if (DEBUG_STATES) Slog.v(TAG, "Moving to STOPPED: " + r
                    + " (starting in stopped state)");
            r.state = ActivityState.STOPPED;
            r.stopped = true;
        }

        // Launch the new version setup screen if needed.  We do this -after-
        // launching the initial activity (that is, home), so that it can have
        // a chance to initialize itself while in the background, making the
        // switch back to it faster and look better.
        if (isFrontStack(stack)) {
            mService.startSetupActivityLocked();
        }

        // Update any services we are bound to that might care about whether
        // their client may have activities.
        mService.mServices.updateServiceConnectionActivitiesLocked(r.app);

        return true;
    }





















}
