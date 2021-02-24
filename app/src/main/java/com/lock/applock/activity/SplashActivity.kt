package com.lock.applock.activity

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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
import com.lock.applock.R
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_splash.*


class SplashActivity : AppCompatActivity() {
    var sharedPreferences: SharedPreferences? = null
    var edit: SharedPreferences.Editor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash)
        sharedPreferences = getSharedPreferences("hieu", Context.MODE_PRIVATE)
        edit = sharedPreferences!!.edit()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this) && hasUsageStatsPermission()) {

                ll_per.visibility = View.GONE
                setup()

            } else {
                ll_pattern.visibility=View.GONE
                rl_pin.visibility = View.GONE
                ll_per.visibility = View.VISIBLE
                container.visibility = View.VISIBLE
                checkPermission()
            }
        }else{
            setup()

        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    override fun onResume() {
        super.onResume()
//        setUpFinger()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this) && hasUsageStatsPermission()) {
                setup()
                ll_per.visibility = View.GONE


            } else {
                ll_pattern.visibility=View.GONE
                rl_pin.visibility = View.GONE
                ll_per.visibility = View.VISIBLE
                checkPermission()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)

    }
    override fun onStop() {
        super.onStop()
        if(tv_pass_again_splash.text == resources.getString(R.string.again) && tv_pass_again_splash.visibility == View.VISIBLE){
            edit!!.putString(
                "pattern",
                ""
            )
            edit!!.apply()
            tv_pass_again_splash.text = ""
        }
        if(tv_pass_pin_again_splash.text == resources.getString(R.string.again) && tv_pass_pin_again_splash.visibility == View.VISIBLE){
            edit!!.putString(
                "pattern",
                ""
            )
            edit!!.apply()
            tv_pass_pin_again_splash.text = ""
        }
    }
    fun setup(){
        if(sharedPreferences!!.getString("pattern", "")==""){
            container.visibility = View.VISIBLE
        }else{
            container.visibility = View.GONE
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)

        }

        if(sharedPreferences!!.getBoolean("pin",false)){
            ll_pattern.visibility=View.GONE
            rl_pin.visibility = View.VISIBLE
            setUpPin()
        }else {
            ll_pattern.visibility=View.VISIBLE
            rl_pin.visibility = View.GONE
            setUpPassword()
        }
    }
//    fun setUpFinger(){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if(sharedPreferences!!.getBoolean("finger",false)) {
//                fingerprint(this)
//            }
//        }
//    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermission() {
        rl_draw.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
                startActivityForResult(intent, 1234)
            }
        }

        rl_use.setOnClickListener {
            if (!hasUsageStatsPermission()) {
                startActivityForResult(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                    12345
                )

            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun hasUsageStatsPermission(): Boolean {
        val appOps =
            getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        var mode = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @SuppressLint("CheckResult")
    private fun setUpPassword() {
        if(sharedPreferences!!.getString("pattern", "")!=""){
            tv_pass.text ="Enter Pattern"
        }
        patter_lock_view.dotCount = 3
        patter_lock_view.dotNormalSize = ResourceUtils.getDimensionInPx(
            this,
            R.dimen.pattern_lock_dot_size
        ).toInt()
        patter_lock_view.dotSelectedSize = ResourceUtils.getDimensionInPx(
            this,
            R.dimen.pattern_lock_dot_selected_size
        ).toInt()
        patter_lock_view.pathWidth = ResourceUtils.getDimensionInPx(
            this,
            R.dimen.pattern_lock_path_width
        ).toInt()
        patter_lock_view.isAspectRatioEnabled = true
        patter_lock_view.aspectRatio = PatternLockView.AspectRatio.ASPECT_RATIO_HEIGHT_BIAS
        patter_lock_view.setViewMode(PatternLockView.PatternViewMode.CORRECT)
        patter_lock_view.dotAnimationDuration = 150
        patter_lock_view.pathEndAnimationDuration = 100
        patter_lock_view.correctStateColor = ResourceUtils.getColor(this,
            R.color.white
        )
        patter_lock_view.isInStealthMode = false
        patter_lock_view.isTactileFeedbackEnabled = true
        patter_lock_view.isInputEnabled = true
        patter_lock_view.addPatternLockListener(mPatternLockViewListener)

        RxPatternLockView.patternComplete(patter_lock_view)
            .subscribe(object : Consumer<PatternLockCompleteEvent> {
                @Throws(Exception::class)
                override fun accept(patternLockCompleteEvent: PatternLockCompleteEvent) {
                    Log.d(
                        javaClass.name,
                        "Complete: " + patternLockCompleteEvent.pattern.toString()
                    )
                }
            })

        RxPatternLockView.patternChanges(patter_lock_view)
            .subscribe(object : Consumer<PatternLockCompoundEvent> {
                @Throws(Exception::class)
                override fun accept(event: PatternLockCompoundEvent) {
                    if (event.eventType == PatternLockCompoundEvent.EventType.PATTERN_STARTED) {
                        Log.d(javaClass.name, "Pattern drawing started")
                    } else if (event.eventType == PatternLockCompoundEvent.EventType.PATTERN_PROGRESS) {
                        Log.d(
                            javaClass.name, "Pattern progress: " +
                                    PatternLockUtils.patternToString(
                                        patter_lock_view,
                                        event.pattern
                                    )
                        )
                    } else if (event.eventType == PatternLockCompoundEvent.EventType.PATTERN_COMPLETE) {
                        Log.d(
                            javaClass.name, "Pattern complete: " +
                                    PatternLockUtils.patternToString(
                                        patter_lock_view,
                                        event.pattern
                                    )
                        )
                    } else if (event.eventType == PatternLockCompoundEvent.EventType.PATTERN_CLEARED) {
                        Log.d(javaClass.name, "Pattern has been cleared")
                    }
                }
            })
    }


    private var mPatternLockViewListener = object : PatternLockViewListener {
        override fun onComplete(pattern: MutableList<PatternLockView.Dot>?) {

            if (sharedPreferences!!.getString("pattern", "") == PatternLockUtils.patternToString(
                    patter_lock_view,
                    pattern
                )
            ) {
                tv_pass.visibility = View.GONE

                tv_pass_again_splash.visibility = View.GONE
                tv_pass.text = resources.getString(R.string.draw_pattern)
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                edit!!.putString(
                    "pattern",
                    PatternLockUtils.patternToString(patter_lock_view, pattern)
                )
                edit!!.apply()
                patter_lock_view!!.clearPattern()

            } else {
                if (sharedPreferences!!.getString("pattern", "") == "") {
                    edit!!.putString(
                        "pattern",
                        PatternLockUtils.patternToString(patter_lock_view, pattern)
                    )
                    edit!!.apply()
                    tv_pass.text = resources.getString(R.string.creat_pattern)
                    tv_pass_again_splash.text = resources.getString(R.string.again)
                }else{
                    tv_pass_wrong.visibility = View.VISIBLE
                    tv_pass.visibility = View.GONE
                    tv_pass_again_splash.visibility = View.GONE
                }
                Handler().postDelayed({
                    patter_lock_view!!.clearPattern()
                }, 500)

            }

        }


        override fun onCleared() {
        }

        override fun onStarted() {
        }

        override fun onProgress(progressPattern: MutableList<PatternLockView.Dot>?) {
            tv_pass_wrong.visibility = View.GONE
            tv_pass.visibility = View.VISIBLE
            tv_pass_again_splash.visibility = View.VISIBLE
        }

    }
    private fun setUpPin(){
        if(sharedPreferences!!.getString("pattern", "")!=""){
            tv_pass_pin.text ="Enter Pin"
        }
        pin_lock_view_pin.attachIndicatorDots(indicator_dots_pin)
        pin_lock_view_pin.setPinLockListener(mPinLockListener)
        pin_lock_view_pin.pinLength = 4
        pin_lock_view_pin.textColor = ContextCompat.getColor(this, R.color.white)

        indicator_dots_pin.indicatorType = IndicatorDots.IndicatorType.FILL_WITH_ANIMATION
    }
    private val mPinLockListener: PinLockListener = object : PinLockListener {
        override fun onComplete(pin: String) {
            if (sharedPreferences!!.getString("pattern", "") == pin

            ) {
                tv_pass_pin.visibility = View.GONE
                tv_pass_again_splash.visibility = View.GONE
                tv_pass_pin.text = resources.getString(R.string.enter_pin)
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                edit!!.putString(
                    "pattern",
                    pin
                )
                edit!!.apply()
                pin_lock_view_pin.resetPinLockView()

            } else {
                if (sharedPreferences!!.getString("pattern", "") == "") {
                    edit!!.putString(
                        "pattern",
                        pin
                    )
                    edit!!.apply()
                    tv_pass.text = resources.getString(R.string.creat_pattern)
                    tv_pass_pin_again_splash.text = resources.getString(R.string.again)
                }else{
                    tv_pass_pin_wrong.visibility = View.VISIBLE
                    tv_pass_pin.visibility = View.GONE
                    tv_pass_pin_again_splash.visibility = View.GONE
                }
                Handler().postDelayed({
                    pin_lock_view_pin.resetPinLockView()
                }, 500)

            }
        }

        override fun onEmpty() {
        }

        override fun onPinChange(pinLength: Int, intermediatePin: String) {
            tv_pass_pin_wrong.visibility = View.GONE
            tv_pass_pin.visibility = View.VISIBLE
            tv_pass_pin_again_splash.visibility = View.VISIBLE
        }
    }



}
