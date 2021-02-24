package com.lock.applock.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.lock.applock.R
import com.lock.applock.`object`.ItemMain
import com.lock.applock.adapter.ItemMainAdapter
import com.lock.applock.adapter.TabAdapter
import com.lock.applock.helper.Helper
import com.lock.applock.helper.KeyboardToggleListener
import com.lock.applock.service.LockService
import com.reader.pdfreader.fragment.AppLockedFragment
import com.reader.pdfreader.fragment.DislayAppFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var arrFragment = ArrayList<Fragment>()
    var arrIcon = ArrayList<String>()
    var tabAdapter: TabAdapter? = null
    var adapter: ItemMainAdapter? = null
    var sharedPreferences: SharedPreferences? = null
    var edit: SharedPreferences.Editor? = null
    lateinit var mAdView : AdView
    companion object {
        var popup: ListPopupWindow? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        mAdView.adListener = object: AdListener() {
            override fun onAdLoaded() {

            }

            override fun onAdFailedToLoad(errorCode : Int) {
              Log.i("hihih","fail $errorCode")
            }

            override fun onAdOpened() {

            }

            override fun onAdClicked() {
                mAdView.loadAd(AdRequest.Builder().build())
            }

            override fun onAdLeftApplication() {

            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        }



        val serviceIntent = Intent(this, LockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(serviceIntent)
        } else {
            this.startService(serviceIntent)
        }
        popup = ListPopupWindow(this)

        sharedPreferences = getSharedPreferences("hieu", Context.MODE_PRIVATE)
        edit = sharedPreferences!!.edit()



        setUpViewPager()
        setTabView()
        setUpSearch()
        updateDark()



        this.addKeyboardToggleListener {
            contener.viewTreeObserver.addOnGlobalLayoutListener {


                Handler().postDelayed({
                    val heightDiff = contener.rootView.height - contener.height
                    if (heightDiff > 300) {
                    } else {
                        if (TextUtils.isEmpty(ed_search.text.toString())) {
                            iv_search.visibility = View.VISIBLE
                            iv_menu.visibility = View.VISIBLE
                            ed_search.visibility = View.GONE
                            tv_title.visibility = View.VISIBLE
                            iv_search_logic.visibility = View.GONE
                            ed_search.text.clear()
                        }

                    }
                }, 500)


            }


        }
    }

//    override fun onRestart() {
//        super.onRestart()
//        val intent = Intent(this, SplashActivity::class.java)
//        startActivity(intent)
//    }


    override fun onBackPressed() {
        if (ed_search.hasFocus()) {
            iv_search.visibility = View.VISIBLE
            ed_search.visibility = View.GONE
            tv_title.visibility = View.VISIBLE
            iv_search_logic.visibility = View.GONE
            ed_search.text.clear()
        } else {
            if (popup!!.isShowing) {
                popup!!.dismiss()
            } else {
                finishAffinity()
                super.onBackPressed()
            }
        }

    }

    private fun setUpViewPager() {
        setSupportActionBar(toolbar)
        tabAdapter = TabAdapter(this, arrFragment, arrIcon, supportFragmentManager)
        tabAdapter!!.addViewFragment(
            DislayAppFragment(),
            resources.getString(R.string.app)
        )
        tabAdapter!!.addViewFragment(
            AppLockedFragment(),
            resources.getString(R.string.app_lock)
        )

        viewpager.offscreenPageLimit = 2
        viewpager.adapter = tabAdapter

    }

    private fun setTabView() {
        sliding_tabs.setupWithViewPager(viewpager)
        for (i in 0 until sliding_tabs.tabCount) {
            sliding_tabs.getTabAt(i)!!.customView = tabAdapter?.getTabView(i)
        }


    }

    private fun setUpSearch() {
        iv_search.setOnClickListener {
            iv_menu.visibility = View.GONE
            iv_search.visibility = View.GONE
            ed_search.visibility = View.VISIBLE
            tv_title.visibility = View.GONE
            ed_search.requestFocus()
            val imm: InputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(ed_search, InputMethodManager.SHOW_IMPLICIT)
            ed_search.addTextChangedListener(object : TextWatcher {
                @SuppressLint("DefaultLocale")
                override fun afterTextChanged(p0: Editable?) {
                    if (!TextUtils.isEmpty(p0!!.toString())) {
                        iv_search_logic.visibility = View.VISIBLE
                        iv_search_logic.setImageDrawable(resources.getDrawable(R.drawable.close))
                        iv_search_logic.setOnClickListener {
                            ed_search.text.clear()

                        }

                    } else {
                        iv_search_logic.visibility = View.GONE
                    }
//                    arrFilter = Helper.getAllDocuments(this@MainActivity)
                    val intent = Intent("SEARCH")
                    intent.putExtra("string", p0.toString())
                    sendBroadcast(intent)
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

            })
        }

        iv_menu.setOnClickListener {
            con.visibility = View.VISIBLE
            showListPopupWindow(it)
            val intent = Intent("POPUP")
            intent.putExtra("show", true)
            sendBroadcast(intent)

        }
    }

    fun set() {
        if (popup!!.isShowing) {
            popup!!.dismiss()

        }
    }

    private fun Activity.addKeyboardToggleListener(onKeyboardToggleAction: (shown: Boolean) -> Unit): KeyboardToggleListener? {
        val root = findViewById<View>(android.R.id.content)
        val listener = KeyboardToggleListener(
            root,
            onKeyboardToggleAction
        )
        return root?.viewTreeObserver?.run {
            addOnGlobalLayoutListener(listener)
            listener
        }
    }


    private fun showListPopupWindow(anchor: View) {
        val listPopupItems = ArrayList<ItemMain>()
        listPopupItems.add(ItemMain(resources.getString(R.string.change_lock), 1))
        if (!sharedPreferences!!.getBoolean("finger", false)) {
            listPopupItems.add(ItemMain(resources.getString(R.string.finger), R.drawable.ic_off))
        } else {
            listPopupItems.add(ItemMain(resources.getString(R.string.finger), R.drawable.ic_on))


        }
//        if (!sharedPreferences!!.getBoolean("dark", false)) {
//            listPopupItems.add(ItemMain(resources.getString(R.string.dark), R.drawable.ic_off))
//        }else{
//            listPopupItems.add(ItemMain(resources.getString(R.string.dark), R.drawable.ic_on))
//
//        }
        listPopupItems.add(ItemMain(resources.getString(R.string.privacy), 1))

//


        val listPopupWindow = createListPopupWindow(anchor, listPopupItems)
        listPopupWindow.show()

    }


    private fun createListPopupWindow(
        anchor: View,
        items: ArrayList<ItemMain>
    ): ListPopupWindow {

        adapter = ItemMainAdapter(this, items, object : ItemMainAdapter.ItemListener {
            override fun onClick(position: Int, it: View) {
                when (position) {
                    0 -> {
                        val intent = Intent(this@MainActivity, ChangeLockActivity::class.java)
                        startActivity(intent)
                        popup!!.dismiss()

                    }
                    1 -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (sharedPreferences!!.getBoolean("finger", false)) {
                                adapter!!.updateFinger(it, false)
                                edit!!.putBoolean("finger", false)
                                edit!!.apply()

                            } else {
                                if (!Helper.fingerprint(this@MainActivity, false)) {

                                } else {
                                    adapter!!.updateFinger(it, true)
                                    edit!!.putBoolean("finger", true)
                                    edit!!.apply()
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                resources.getString(R.string.erro),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                    2 -> {
//                        val intent = Intent("DARK")
//                        if (!sharedPreferences!!.getBoolean("dark", false)) {
//                            edit!!.putBoolean("dark",true)
//                            edit!!.apply()
//                            adapter!!.updateDark(it,true)
//                            intent.putExtra("dark",true)
//                            sliding_tabs.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_tab_dark)
//                            toolbar.background =ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_gradient_dark)
//
//                        }else{
//                            edit!!.putBoolean("dark",false)
//                            edit!!.apply()
//                            adapter!!.updateDark(it,false)
//                            intent.putExtra("dark",false)
//                            sliding_tabs.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_tab)
//                            toolbar.background =ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_gradient)
//                        }
//                        sendBroadcast(intent)
//                        popup!!.dismiss()

                    }
                    3 -> {

                    }

                }
//                showListPopupWindow(it)
            }

        })
        popup!!.anchorView = anchor
        popup!!.width = convertToPx(180)
        popup!!.height = convertToPx(180)
        popup!!.setBackgroundDrawable(resources.getDrawable(R.drawable.popup))
        popup!!.setAdapter(adapter)
        popup!!.setOnDismissListener {
            con.visibility = View.GONE
        }

        return popup!!
    }

    private fun convertToPx(dp: Int): Int {
        // Get the screen's density scale
        val scale = resources.displayMetrics.density
        // Convert the dps to pixels, based on density scale
        return (dp * scale + 0.5f).toInt()
    }
    fun updateDark(){
        if (sharedPreferences!!.getBoolean("dark",false)){
            sliding_tabs.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_tab_dark)
            toolbar.background =ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_gradient_dark)
        }else{
            sliding_tabs.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_tab_dark)
            toolbar.background =ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_gradient_dark)
        }

    }


}
