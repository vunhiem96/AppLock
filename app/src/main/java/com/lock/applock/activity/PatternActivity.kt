package com.lock.applock.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.andrognito.patternlockview.utils.PatternLockUtils
import com.andrognito.patternlockview.utils.ResourceUtils
import com.andrognito.rxpatternlockview.RxPatternLockView
import com.andrognito.rxpatternlockview.events.PatternLockCompleteEvent
import com.andrognito.rxpatternlockview.events.PatternLockCompoundEvent
import com.lock.applock.R
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_pattern.*
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.android.synthetic.main.activity_splash.patter_lock_view
import kotlinx.android.synthetic.main.activity_splash.tv_pass

class PatternActivity : AppCompatActivity() {
    var sharedPreferences: SharedPreferences? = null
    var edit: SharedPreferences.Editor? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_pattern)
        sharedPreferences = getSharedPreferences("hieu", Context.MODE_PRIVATE)
        edit = sharedPreferences!!.edit()

        setUpPassword()

    }
    override fun onStop() {
        super.onStop()
        if(tv_pass_again.visibility == View.VISIBLE){
            edit!!.putString(
                "passwordNew",
                ""
            )
            edit!!.apply()
            tv_pass.text = resources.getString(R.string.creat_pattern)
            tv_pass_again.visibility= View.GONE
        }
    }

    @SuppressLint("CheckResult")
    private fun setUpPassword() {
        iv_back.setOnClickListener {
            onBackPressed()
        }
        edit!!.putString(
            "passwordNew",
            ""
        )
        edit!!.apply()
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
        if (sharedPreferences!!.getBoolean("dark",false)){
            con_pattern.setBackgroundResource(R.drawable.bg_dark)

        }else{
            con_pattern.setBackgroundResource(R.drawable.bg)

        }
        patter_lock_view.correctStateColor = ResourceUtils.getColor(
            this,
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

            if (sharedPreferences!!.getString("passwordNew", "") == PatternLockUtils.patternToString(
                    patter_lock_view,
                    pattern
                )
            ) {
                tv_pass.text = resources.getString(R.string.creat_pattern)
                edit!!.putString(
                    "pattern", PatternLockUtils.patternToString(patter_lock_view, pattern)
                )
                edit!!.putString(
                    "passwordNew", ""
                )
                edit!!.putBoolean("pin",false)
                edit!!.apply()
                onBackPressed()
                val intent = Intent("CHANGE")
                intent.putExtra("pattern",true)
                sendBroadcast(intent)

            } else {
                if (sharedPreferences!!.getString("passwordNew", "") == "") {
                    edit!!.putString(
                        "passwordNew",
                        PatternLockUtils.patternToString(patter_lock_view, pattern)
                    )
                    edit!!.apply()
//                    tv_pass.text = "Creat a Pattern \n (Again)"
                    tv_pass_again.text = resources.getString(R.string.again)
                }else{
                    tv_pass.visibility = View.GONE
                    tv_pass_wrong_pattern.visibility = View.VISIBLE
                    tv_pass_again.visibility = View.GONE
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
            tv_pass.visibility = View.VISIBLE
            tv_pass_wrong_pattern.visibility = View.GONE
            tv_pass_again.visibility = View.VISIBLE
        }

    }

}
