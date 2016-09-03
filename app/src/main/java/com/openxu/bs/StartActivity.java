package com.openxu.bs;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ApplicationErrorReport;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriPermission;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.EventLogTags;
import android.util.Log;
import android.view.View;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_TASK_ON_HOME;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * author : openXu
 * created time : 16/9/3 下午5:33
 * blog : http://blog.csdn.net/xmxkf
 * github : http://blog.csdn.net/xmxkf
 * class name : StartActivity
 * discription :
 */
public class StartActivity extends Activity{

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        startActivity(new Intent(this, StartActivity.class));

    }

    //step1 : Activity
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

    //step2 : Instrumentation
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

    //step3 :  ActivityManagerNative
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

    //step4 : ActivityManagerService
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


    //step5 : ActivityStackSupervisor.startActivityMayWait()
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

    //step6 : ActivityStackSupervisor.startActivityLocked()
    final int startActivityLocked(IApplicationThread caller,
                                  Intent intent, String resolvedType, ActivityInfo aInfo,
                                  IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor,
                                  IBinder resultTo, String resultWho, int requestCode,
                                  int callingPid, int callingUid, String callingPackage,
                                  int realCallingPid, int realCallingUid, int startFlags, Bundle options,
                                  boolean componentSpecified, ActivityRecord[] outActivity, ActivityContainer container,
                                  TaskRecord inTask) {
        int err = ActivityManager.START_SUCCESS;
        //调用者的进程信息，也就是哪个应用要开启此Activity的
        ProcessRecord callerApp = null;
        if (caller != null) {
            callerApp = mService.getRecordForAppLocked(caller);
            if (callerApp != null) {
                callingPid = callerApp.pid;
                callingUid = callerApp.info.uid;
            } else {
                Slog.w(TAG, "Unable to find app for caller " + caller
                        + " (pid=" + callingPid + ") when starting: "
                        + intent.toString());
                err = ActivityManager.START_PERMISSION_DENIED;
            }
        }
        ...

        ActivityRecord sourceRecord = null;
        ActivityRecord resultRecord = null;
        if (resultTo != null) {
            sourceRecord = isInAnyStackLocked(resultTo);
            if (DEBUG_RESULTS) Slog.v(
                    TAG, "Will send result to " + resultTo + " " + sourceRecord);
            if (sourceRecord != null) {
                if (requestCode >= 0 && !sourceRecord.finishing) {
                    resultRecord = sourceRecord;
                }
            }
        }

        final int launchFlags = intent.getFlags();

        if ((launchFlags&Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0 && sourceRecord != null) {
            // Transfer the result target from the source activity to the new
            // one being started, including any failures.
            if (requestCode >= 0) {
                ActivityOptions.abort(options);
                return ActivityManager.START_FORWARD_AND_REQUEST_CONFLICT;
            }
            resultRecord = sourceRecord.resultTo;
            resultWho = sourceRecord.resultWho;
            requestCode = sourceRecord.requestCode;
            sourceRecord.resultTo = null;
            if (resultRecord != null) {
                resultRecord.removeResultsLocked(sourceRecord, resultWho, requestCode);
            }
            if (sourceRecord.launchedFromUid == callingUid) {
                // The new activity is being launched from the same uid as the previous
                // activity in the flow, and asking to forward its result back to the
                // previous.  In this case the activity is serving as a trampoline between
                // the two, so we also want to update its launchedFromPackage to be the
                // same as the previous activity.  Note that this is safe, since we know
                // these two packages come from the same uid; the caller could just as
                // well have supplied that same package name itself.  This specifially
                // deals with the case of an intent picker/chooser being launched in the app
                // flow to redirect to an activity picked by the user, where we want the final
                // activity to consider it to have been launched by the previous app activity.
                callingPackage = sourceRecord.launchedFromPackage;
            }
        }

        if (err == ActivityManager.START_SUCCESS && intent.getComponent() == null) {
            err = ActivityManager.START_INTENT_NOT_RESOLVED;
        }

        if (err == ActivityManager.START_SUCCESS && aInfo == null) {
            // 未找到需要打开的activity的class文件
            err = ActivityManager.START_CLASS_NOT_FOUND;
        }

        ...

        final ActivityStack resultStack = resultRecord == null ? null : resultRecord.task.stack;

        if (err != ActivityManager.START_SUCCESS) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1,
                        resultRecord, resultWho, requestCode,
                        Activity.RESULT_CANCELED, null);
            }
            ActivityOptions.abort(options);
            return err;
        }

        //检查权限
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
            //权限被拒绝
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
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }

        boolean abort = !mService.mIntentFirewall.checkStartActivity(intent, callingUid,
                callingPid, resolvedType, aInfo.applicationInfo);

        if (mService.mController != null) {
            try {
                // The Intent we give to the watcher has the extra data
                // stripped off, since it can contain private information.
                Intent watchIntent = intent.cloneFilter();
                abort |= !mService.mController.activityStarting(watchIntent,
                        aInfo.applicationInfo.packageName);
            } catch (RemoteException e) {
                mService.mController = null;
            }
        }

        if (abort) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode,
                        Activity.RESULT_CANCELED, null);
            }
            // We pretend to the caller that it was really started, but
            // they will just get a cancel result.
            ActivityOptions.abort(options);
            return ActivityManager.START_SUCCESS;
        }

        ActivityRecord r = new ActivityRecord(mService, callerApp, callingUid, callingPackage,
                intent, resolvedType, aInfo, mService.mConfiguration, resultRecord, resultWho,
                requestCode, componentSpecified, this, container, options);
        if (outActivity != null) {
            outActivity[0] = r;
        }

        final ActivityStack stack = getFocusedStack();
        if (voiceSession == null && (stack.mResumedActivity == null
                || stack.mResumedActivity.info.applicationInfo.uid != callingUid)) {
            if (!mService.checkAppSwitchAllowedLocked(callingPid, callingUid,
                    realCallingPid, realCallingUid, "Activity start")) {
                PendingActivityLaunch pal =
                        new PendingActivityLaunch(r, sourceRecord, startFlags, stack);
                mPendingActivityLaunches.add(pal);
                ActivityOptions.abort(options);
                return ActivityManager.START_SWITCHES_CANCELED;
            }
        }

        if (mService.mDidAppSwitch) {
            // This is the second allowed switch since we stopped switches,
            // so now just generally allow switches.  Use case: user presses
            // home (switches disabled, switch to home, mDidAppSwitch now true);
            // user taps a home icon (coming from home so allowed, we hit here
            // and now allow anyone to switch again).
            mService.mAppSwitchesAllowedTime = 0;
        } else {
            mService.mDidAppSwitch = true;
        }

        doPendingActivityLaunchesLocked(false);

        err = startActivityUncheckedLocked(r, sourceRecord, voiceSession, voiceInteractor,
                startFlags, true, options, inTask);

        if (err < 0) {
            // If someone asked to have the keyguard dismissed on the next
            // activity start, but we are not actually doing an activity
            // switch...  just dismiss the keyguard now, because we
            // probably want to see whatever is behind it.
            notifyActivityDrawnForKeyguard();
        }
        return err;
    }

    //step7 : ActivityStackSupervisor.startActivityUncheckedLocked()
    final int startActivityUncheckedLocked(ActivityRecord r, ActivityRecord sourceRecord,
                                           IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, int startFlags,
                                           boolean doResume, Bundle options, TaskRecord inTask) {
        ...
        //1⃣️  获取并配置activity配置的启动模式
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
        //根据activity的设置，如果满足条件，将launchFlags置为FLAG_ACTIVITY_NEW_TASK（创建新进程）
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
         * 2⃣️  我们尝试将新的activity放在一个现有的任务中。但是如果activity被要求是singleTask或者singleInstance，
         * 我们会将activity放入一个新的task中.下面的if中主要处理将进程前置，然后将目标activity显示
         */
        if (((launchFlags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0 &&
                (launchFlags & Intent.FLAG_ACTIVITY_MULTIPLE_TASK) == 0)
                || launchSingleInstance || launchSingleTask) {
            //如果被开启的activity不是需要开启新的进程，而是single instance或者singleTask，
            // 需要检查此activity是否已经开启，并将进程置于最上层
            if (inTask == null && r.resultTo == null) {
                //查找此activity是否已经开启了
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

        //3⃣️    判断包名是否解析成功，如果包名解析不成功无法开启activity
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

        //4⃣️   判断activiy应该在那个进程中启动，如果该进程中已经存在目标activity，根据启动模式做相应处理
        ...
        // 判断是否需要开启新进程?
        if (r.resultTo == null && inTask == null && !addingToTask
                && (launchFlags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            ...
            newTask = true;
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
            // this case should never happen.不可能发生
            ...
        }

        ...
        ActivityStack.logStartActivity(EventLogTags.AM_CREATE_ACTIVITY, r, r.task);
        targetStack.mLastPausedActivity = null;
        //5⃣️   真正开启Activity的地方， ActivityStack targetStack.startActivityLocked()
        targetStack.startActivityLocked(r, newTask, doResume, keepCurTransition, options);
        if (!launchTaskBehind) {
            // Don't set focus on an activity that's going to the back.
            mService.setFocusedActivityLocked(r, "startedActivity");
        }
        return ActivityManager.START_SUCCESS;
    }


    //step8 : ActivityStack.startActivityLocked()



    //step9 :

    //step10 :


















































}
