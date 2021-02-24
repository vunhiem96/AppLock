package com.lock.applock.service

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.andrognito.patternlockview.utils.PatternLockUtils
import com.andrognito.patternlockview.utils.ResourceUtils
import com.andrognito.pinlockview.IndicatorDots
import com.andrognito.pinlockview.PinLockListener
import com.andrognito.rxpatternlockview.RxPatternLockView
import com.andrognito.rxpatternlockview.events.PatternLockCompleteEvent
import com.andrognito.rxpatternlockview.events.PatternLockCompoundEvent
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.lock.applock.R
import com.lock.applock.`object`.App
import com.lock.applock.activity.SplashActivity
import com.lock.applock.db.DbApp
import com.lock.applock.fingerprint.FingerprintHandler
import com.lock.applock.helper.Helper
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.window_manager.view.*
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.cert.CertificateException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey


class LockService : Service() {
    var keyStore: KeyStore? = null

    // Variable used for storing the key in the Android Keystore container
    val KEY_NAME = "androidHive"
    var cipher: Cipher? = null
    var params: WindowManager.LayoutParams? = null
    var popupView: View? = null
    var sharedPreferences: SharedPreferences? = null
    var edit: SharedPreferences.Editor? = null
    val CHANNEL_ID = "com.lock.applock"
    var manager: NotificationManager? = null
    var notification: Notification? = null
    var windowManager: WindowManager? = null
    var pakageName = ArrayList<App>()
    var dbApp: DbApp? = null

    var timer: Timer? = null

    companion object {
        var currentApp = ""
        var previousApp = ""
        var mpackageName = ""
        var i = 0
        var k = 0

    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }


    override fun onCreate() {
        super.onCreate()
        dbApp = DbApp(applicationContext, null)
        sharedPreferences = applicationContext.getSharedPreferences("hieu", Context.MODE_PRIVATE)
        edit = sharedPreferences!!.edit()
        pakageName = dbApp!!.getLocked()
        if (pakageName.size == 0) {
            pakageName.add(App("App Lock", "com.lock.applock", "", 1))
//            dbApp!!.insertApp("App Lock","com.lock.applock",1)
        }
        val intent = IntentFilter()
        intent.addAction("LOCKED")
        intent.addAction("LOCKEDAPP")
        intent.addAction("PAKAGENAME")
        intent.addAction("CHANGE")
        intent.addAction("FINGER")
        intent.addAction("FINGER_ERROR")
        intent.addAction("DARK")
        applicationContext.registerReceiver(broadcastReceiver, intent)
        setUp()
        createNotificationChannel()


    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        timer!!.cancel()
        timer = null
        if (popupView != null) {
            if (popupView!!.windowToken != null) {
                windowManager!!.removeViewImmediate(popupView)
            }
        }
        windowManager = null
    }

    private var broadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onReceive(p0: Context?, p1: Intent?) {
            val action = p1?.action

//
            if (action!!.equals("PAKAGENAME", ignoreCase = true)) {

                var lock = p1.extras?.getBoolean("lock")
                if (lock!!) {

                    if (currentApp != "app") {
                        setUpFinger()
                        if (i == 0) {
                            if (sharedPreferences!!.getBoolean("pin", false)) {
                                popupView!!.tv_pass_pin.text =
                                    resources.getString(R.string.enter_pin)

                                Glide
                                    .with(applicationContext)
                                    .load(
                                        applicationContext.packageManager.getApplicationIcon(
                                            mpackageName
                                        )
                                    )
                                    .thumbnail(0.5f)
                                    .transition(
                                        DrawableTransitionOptions()
                                            .crossFade()
                                    )
                                    .into(popupView!!.iv_pin)


                            } else {
                                popupView!!.tv_pass.text =
                                    resources.getString(R.string.draw_pattern)
                                Glide
                                    .with(applicationContext)
                                    .load(
                                        applicationContext.packageManager.getApplicationIcon(
                                            mpackageName
                                        )
                                    )
                                    .thumbnail(0.5f)
                                    .transition(
                                        DrawableTransitionOptions()
                                            .crossFade()
                                    )
                                    .into(popupView!!.iv_pattern)
                            }
                            i = 1
                        }
                        if (k > 0) {

                            popupView!!.container.visibility = View.VISIBLE


                            windowManager!!.updateViewLayout(popupView, params)
                        }
//
                    }


                } else {
                    i = 0


                    currentApp = ""
                    popupView!!.container.visibility = View.GONE
                    windowManager!!.updateViewLayout(popupView, params)

                }
//                Toast.makeText(context, "hi", Toast.LENGTH_SHORT).show()
            } else if (action.equals("LOCKED", ignoreCase = true) || action.equals(
                    "LOCKEDAPP",
                    ignoreCase = true
                )
            ) {

                pakageName = dbApp!!.getLocked()


            } else if (action.equals("CHANGE", ignoreCase = true)) {
                val pattern = p1.extras!!.getBoolean("pattern")
                if (!pattern) {
                    popupView!!.ll_pattern.visibility = View.GONE
                    popupView!!.rl_pin.visibility = View.VISIBLE
                    setUpPin()
                } else {
                    popupView!!.ll_pattern.visibility = View.VISIBLE
                    popupView!!.rl_pin.visibility = View.GONE
                    setUpPassword()
                }
            } else if (action.equals("FINGER", ignoreCase = true)) {
                currentApp = "app"
                popupView!!.container.visibility = View.GONE
                windowManager!!.updateViewLayout(popupView, params)


            } else if (action.equals("DARK", ignoreCase = true)) {
                var dark = p1.extras!!.getBoolean("dark")
                if (dark) {
                    popupView!!.container.setBackgroundResource(R.drawable.bg_dark)

                } else {
                    popupView!!.container.setBackgroundResource(R.drawable.bg)
                }
                windowManager!!.updateViewLayout(popupView, params)


            }


        }

    }

    fun updateDark() {


    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground WinMService Channel",
                NotificationManager.IMPORTANCE_LOW
            )
//            serviceChannel.lightColor = Color.BLUE
//            serviceChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

            manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            manager?.createNotificationChannel(serviceChannel)
        }
        val notificationIntent = Intent(this, SplashActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            100, notificationIntent, 0
        )

        notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Xbar")
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle())

            .build()
        startForeground(1, notification)


    }

    private val updateTask: TimerTask = object : TimerTask() {
        override fun run() {

            val intent = Intent("PAKAGENAME")

            if (isConcernedAppIsInForeground()) {
                intent.putExtra("lock", true)

                Log.d("isConcernedAppIsInFrgnd", "true")


            } else {
                mpackageName = " "
                intent.putExtra("lock", false)

                Log.d("isConcernedAppIsInFrgnd", "false")
            }
            applicationContext.sendBroadcast(intent)

        }
    }

    fun setUp() {
        timer = Timer("LockServices")
        timer!!.schedule(updateTask, 0, 500L)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,

            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        )
        params!!.gravity = Gravity.CENTER
        params!!.x = WindowManager.LayoutParams.MATCH_PARENT
        params!!.y = WindowManager.LayoutParams.MATCH_PARENT

        val layoutInflater =
            baseContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        popupView = layoutInflater.inflate(R.layout.window_manager, null)

//        try {
            windowManager?.addView(popupView, params)
//        } catch (e: Exception) {
//
//        }

        if (sharedPreferences!!.getBoolean("dark", false)) {
            popupView!!.container.setBackgroundResource(R.drawable.bg_dark)

        } else {
            popupView!!.container.setBackgroundResource(R.drawable.bg)
        }
        windowManager!!.updateViewLayout(popupView, params)

        if (sharedPreferences!!.getBoolean("pin", false)) {
            popupView!!.ll_pattern.visibility = View.GONE
            popupView!!.rl_pin.visibility = View.VISIBLE
            setUpPin()
        } else {
            popupView!!.ll_pattern.visibility = View.VISIBLE
            popupView!!.rl_pin.visibility = View.GONE
            setUpPassword()
        }
    }

    fun setUpFinger() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (sharedPreferences!!.getBoolean("finger", false)) {
                fingerprint(applicationContext, true)
            }
        }
    }


    fun isConcernedAppIsInForeground(): Boolean {
        val manager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val task =
            manager.getRunningTasks(5)
        if (Build.VERSION.SDK_INT <= 20) {
            if (task.size > 0) {
                val componentInfo = task[0].topActivity
                mpackageName = componentInfo!!.packageName
                for (i in pakageName) {
                    if (mpackageName != packageName) {
                        k++
                    }

                    if (mpackageName == i.packagename) {
//                    mpackageName = i.packagename
                        return true


                    }
                }
            }
        } else {
            val usage =
                applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 10000
            var result = ""
            val event = UsageEvents.Event()
            val usageEvents = usage.queryEvents(beginTime, endTime)
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    result = event.packageName
                }
            }
            if (!TextUtils.isEmpty(result)) {
                mpackageName = result

            }
//

            for (i in pakageName) {
                if (mpackageName != packageName) {
                    k++
                }

                if (mpackageName == i.packagename) {
//                    mpackageName = i.packagename
                    return true


                }
            }
//            if (mpackageName == "android") {
//                return true
//            }

            Log.d("isConcernedAppIsInFrgnd", mpackageName)
        }
        return false
    }

    @SuppressLint("CheckResult")
    private fun setUpPassword() {

        popupView!!.tv_pass.text = resources.getString(R.string.draw_pattern)

        popupView!!.patter_lock_view.dotCount = 3
        popupView!!.patter_lock_view.dotNormalSize = ResourceUtils.getDimensionInPx(
            this,
            R.dimen.pattern_lock_dot_size
        ).toInt()
        popupView!!.patter_lock_view.dotSelectedSize = ResourceUtils.getDimensionInPx(
            this,
            R.dimen.pattern_lock_dot_selected_size
        ).toInt()
        popupView!!.patter_lock_view.pathWidth = ResourceUtils.getDimensionInPx(
            this,
            R.dimen.pattern_lock_path_width
        ).toInt()
        popupView!!.patter_lock_view.isAspectRatioEnabled = true
        popupView!!.patter_lock_view.aspectRatio =
            PatternLockView.AspectRatio.ASPECT_RATIO_HEIGHT_BIAS
        popupView!!.patter_lock_view.setViewMode(PatternLockView.PatternViewMode.CORRECT)
        popupView!!.patter_lock_view.dotAnimationDuration = 150
        popupView!!.patter_lock_view.pathEndAnimationDuration = 100
//        popupView!!.patter_lock_view.normalStateColor = ResourceUtils.getColor(this,
//            R.color.pattern)
        popupView!!.patter_lock_view.correctStateColor = ResourceUtils.getColor(
            this,
            R.color.white
        )
//        popupView!!.patter_lock_view.normalStateColor = ResourceUtils.getColor(this,
//            R.color.pattern)
//        if (sharedPreferences!!.getBoolean("dark",false)){
//        popupView!!.patter_lock_view.correctStateColor = ResourceUtils.getColor(this,
//            R.color.white
//        )}else{
//            popupView!!.patter_lock_view.correctStateColor = ResourceUtils.getColor(this,
//                R.color.pattern)
//        }

        popupView!!.patter_lock_view.isInStealthMode = false
        popupView!!.patter_lock_view.isTactileFeedbackEnabled = true
        popupView!!.patter_lock_view.isInputEnabled = true
        popupView!!.patter_lock_view.addPatternLockListener(mPatternLockViewListener)
//        popupView!!.patter_lock_view.setInStealthMode(true)

//        RxPatternLockView.patternComplete(popupView!!.patter_lock_view)
//            .subscribe(object : Consumer<PatternLockCompleteEvent> {
//                @Throws(Exception::class)
//                override fun accept(patternLockCompleteEvent: PatternLockCompleteEvent) {
//                    Log.d(
//                        javaClass.name,
//                        "Complete: " + patternLockCompleteEvent.pattern.toString()
//                    )
//                }
//            })
//
//        RxPatternLockView.patternChanges(popupView!!.patter_lock_view)
//            .subscribe(object : Consumer<PatternLockCompoundEvent> {
//                @Throws(Exception::class)
//                override fun accept(event: PatternLockCompoundEvent) {
//                    if (event.eventType == PatternLockCompoundEvent.EventType.PATTERN_STARTED) {
//                        Log.d(javaClass.name, "Pattern drawing started")
//                    } else if (event.eventType == PatternLockCompoundEvent.EventType.PATTERN_PROGRESS) {
//                        Log.d(
//                            javaClass.name, "Pattern progress: " +
//                                    PatternLockUtils.patternToString(
//                                        popupView!!.patter_lock_view,
//                                        event.pattern
//                                    )
//                        )
//                    } else if (event.eventType == PatternLockCompoundEvent.EventType.PATTERN_COMPLETE) {
//                        Log.d(
//                            javaClass.name, "Pattern complete: " +
//                                    PatternLockUtils.patternToString(
//                                        popupView!!.patter_lock_view,
//                                        event.pattern
//                                    )
//                        )
//                    } else if (event.eventType == PatternLockCompoundEvent.EventType.PATTERN_CLEARED) {
//                        Log.d(javaClass.name, "Pattern has been cleared")
//                    }
//                }
//            })
    }


    private var mPatternLockViewListener = object : PatternLockViewListener {
        override fun onComplete(pattern: MutableList<PatternLockView.Dot>?) {

            if (sharedPreferences!!.getString("pattern", "") == PatternLockUtils.patternToString(
                    popupView!!.patter_lock_view,
                    pattern
                )
            ) {
                popupView!!.container.visibility = View.GONE
                currentApp = "app"
                popupView!!.patter_lock_view!!.clearPattern()
                windowManager!!.updateViewLayout(popupView, params)


            } else {
                popupView!!.tv_pass.text = resources.getString(R.string.pattern_wrong)

                Handler().postDelayed({
                    popupView!!.patter_lock_view!!.clearPattern()
                }, 500)

            }

        }


        override fun onCleared() {
        }

        override fun onStarted() {
        }

        override fun onProgress(progressPattern: MutableList<PatternLockView.Dot>?) {
            popupView!!.tv_pass.text = resources.getString(R.string.draw_pattern)
        }

    }

    private fun setUpPin() {

        popupView!!.tv_pass_pin.text = resources.getString(R.string.enter_pin)

        popupView!!.pin_lock_view_pin.attachIndicatorDots(popupView!!.indicator_dots_pin)
        popupView!!.pin_lock_view_pin.setPinLockListener(mPinLockListener)
        popupView!!.pin_lock_view_pin.pinLength = 4
        popupView!!.pin_lock_view_pin.textColor = ContextCompat.getColor(this, R.color.white)

        popupView!!.indicator_dots_pin.indicatorType =
            IndicatorDots.IndicatorType.FILL_WITH_ANIMATION
    }

    private val mPinLockListener: PinLockListener = object : PinLockListener {
        override fun onComplete(pin: String) {
            if (sharedPreferences!!.getString("pattern", "") == pin

            ) {

                popupView!!.container.visibility = View.GONE
                currentApp = "app"
                popupView!!.pin_lock_view_pin.resetPinLockView()
                windowManager!!.updateViewLayout(popupView, params)

            } else {
                popupView!!.tv_pass_pin.text = resources.getString(R.string.pin_wrong)
                Handler().postDelayed({
                    popupView!!.pin_lock_view_pin.resetPinLockView()
                }, 500)

            }
        }

        override fun onEmpty() {
        }

        override fun onPinChange(pinLength: Int, intermediatePin: String) {
            popupView!!.tv_pass_pin.text = resources.getString(R.string.enter_pin)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val keyGenerator: KeyGenerator
        keyGenerator = try {
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get KeyGenerator instance", e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Failed to get KeyGenerator instance", e)
        }
        try {
            keyStore!!.load(null)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or
                            KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_PKCS7
                    )
                    .build()
            )
            keyGenerator.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @TargetApi(Build.VERSION_CODES.M)
    fun cipherInit(): Boolean {
        cipher = try {
            Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get Cipher", e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("Failed to get Cipher", e)
        }
        return try {
            keyStore!!.load(null)
            val key = keyStore!!.getKey(
                KEY_NAME,
                null
            ) as SecretKey
            cipher!!.init(Cipher.ENCRYPT_MODE, key)
            true
        } catch (e: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun fingerprint(context: Context, check: Boolean): Boolean {
        val keyguardManager =
            context.getSystemService(AppCompatActivity.KEYGUARD_SERVICE) as KeyguardManager
        val fingerprintManager =
            context.getSystemService(AppCompatActivity.FINGERPRINT_SERVICE) as FingerprintManager


        // Check whether the device has a Fingerprint sensor.
        if (!fingerprintManager.isHardwareDetected) {
        } else { // Checks whether fingerprint permission is set on manifest
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.USE_FINGERPRINT
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
            } else { // Check whether at least one fingerprint is registered
                if (!fingerprintManager.hasEnrolledFingerprints()) {

                } else { // Checks whether lock screen security is enabled or not
                    if (!keyguardManager.isKeyguardSecure) {
                    } else {
                        if (check) {
                            generateKey()
                            if (cipherInit()) {
                                val cryptoObject: FingerprintManager.CryptoObject =
                                    FingerprintManager.CryptoObject(cipher!!)
                                val helper = FingerprintHandler(context)
                                helper.startAuth(fingerprintManager, cryptoObject)

                            }
                        } else {

                        }
                        return true
                    }

                }
            }
        }
        return false
    }


}
