package com.lock.applock.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.lock.applock.R
import kotlinx.android.synthetic.main.activity_change_lock.*

class ChangeLockActivity : AppCompatActivity() {
    var sharedPreferences: SharedPreferences? = null
    var edit: SharedPreferences.Editor? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_lock)
        sharedPreferences = getSharedPreferences("hieu", Context.MODE_PRIVATE)
        edit = sharedPreferences!!.edit()
        iv_pin.setOnClickListener {

            val intent = Intent(this,PinActivity::class.java)
            startActivity(intent)
        }
        iv_pattern.setOnClickListener {

            val intent = Intent(this,PatternActivity::class.java)
            startActivity(intent)
        }
        if (sharedPreferences!!.getBoolean("dark",false)){
            con.setBackgroundResource(R.drawable.bg_dark)
        }else{
            con.setBackgroundResource(R.drawable.bg)
        }

        iv_back.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        if (sharedPreferences!!.getBoolean("pin",false)){

            iv_tick_pin.setImageResource(R.drawable.ic_tick)
            iv_tick_pattern.setImageResource(R.drawable.ic_tick_no)
        }else{
            iv_tick_pattern.setImageResource(R.drawable.ic_tick)
            iv_tick_pin.setImageResource(R.drawable.ic_tick_no)
            iv_tick_pattern.setImageResource(R.drawable.ic_tick)
        }
    }
}
