package com.reader.pdfreader.fragment


import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.lock.applock.R
import com.lock.applock.`object`.App
import com.lock.applock.activity.MainActivity
import com.lock.applock.adapter.AppAdapter
import com.lock.applock.adapter.AppLockAdapter
import com.lock.applock.db.DbApp
import kotlinx.android.synthetic.main.fragment_app_locked.*
import kotlinx.android.synthetic.main.fragment_dislay_app.*
import kotlinx.android.synthetic.main.fragment_app_locked.view.*
import kotlinx.android.synthetic.main.fragment_dislay_app.rv_app

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


/**
 * A simple [Fragment] subclass.
 *
 */
class AppLockedFragment : Fragment() {
    var arr = ArrayList<App>()
    var adapter: AppLockAdapter? = null
    var arrFileSearch = ArrayList<App>()
    var sharedPreferences: SharedPreferences? = null
    var editor: SharedPreferences.Editor? = null
    var showPo=false
    var dbApp :DbApp?=null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_app_locked, container, false)
        val intent = IntentFilter()
        intent.addAction("SEARCH")
        intent.addAction("LOCKED")
        intent.addAction("POPUP")
        intent.addAction("DARK")
        context!!.registerReceiver(broadcastReceiver, intent)

        sharedPreferences = context!!.getSharedPreferences("hieu", Context.MODE_PRIVATE)
        editor = sharedPreferences?.edit()


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbApp = DbApp(context!!,null)
        arr = dbApp!!.getLocked()
        if(arr.size == 0){
            arr.add(App("App Lock","com.lock.applock","",1))
//            dbApp!!.insertApp("App Lock","com.lock.applock",1)
        }
        view.rv_app.layoutManager = LinearLayoutManager(context)
        recycleview(arr)
        updateDark()


    }



    private fun recycleview(arr: ArrayList<App>) {

        adapter = AppLockAdapter(context!!, arr, object : AppLockAdapter.ItemListener {

            override fun onClick(position: Int,packageName:String) {
                if (showPo) {
                    (activity as MainActivity).set()
                    showPo = false


                } else {
                    dbApp!!.updateLock(packageName, 0)
                    val intent = Intent("LOCKEDAPP")
                    context!!.sendBroadcast(intent)
                }
            }
            
        })
        rv_app.adapter = adapter
    }


    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            val action = p1?.action
            var string = p1?.extras?.getString("string")
//
            if (action!!.equals("SEARCH", ignoreCase = true)) {
//                Toast.makeText(context, "hi", Toast.LENGTH_SHORT).show()
                dislaySearch(string)
            } else if (action.equals("LOCKED", ignoreCase = true)  )
             {
                 arr = dbApp!!.getLocked()
                 rv_app.layoutManager = LinearLayoutManager(context)
                 recycleview(arr)


             }else if (action.equals("POPUP", ignoreCase = true)) {
                val show = p1.extras!!.getBoolean("show")

                showPo = show

            }else if (action.equals("DARK", ignoreCase = true)) {
                var dark = p1.extras!!.getBoolean("dark")
                if(dark) {
                    rl_lock.setBackgroundResource(R.color.dark)
                }else{
                    rl_lock.setBackgroundResource(R.color.normal)
                }
                adapter!!.notifyDataSetChanged()

            }
        }

    }


    @SuppressLint("DefaultLocale")
    private fun dislaySearch(string: String?) {
        arrFileSearch.clear()
        for (pdf in arr) {
            if (pdf.name.toLowerCase().contains(string!!.toLowerCase())) {
                arrFileSearch.add(pdf)
            }
        }
//        arrFile = arrFileSearch

        var adapter = AppLockAdapter(context!!, arrFileSearch, object : AppLockAdapter.ItemListener {
            override fun onClick(position: Int,packageName:String) {
                if (showPo) {
                    (activity as MainActivity).set()
                    showPo = false


                } else {
                    dbApp!!.updateLock(packageName, 0)
                    val intent = Intent("LOCKEDAPP")
                    context!!.sendBroadcast(intent)
                }
            }


        })
        rv_app.adapter = adapter


    }

    override fun onDestroy() {
        super.onDestroy()
        context!!.unregisterReceiver(broadcastReceiver)


    }
    fun updateDark(){
        if (sharedPreferences!!.getBoolean("dark",false)){
            rl_lock.setBackgroundResource(R.color.dark)
        }else{
            rl_lock.setBackgroundResource(R.color.normal)
        }

    }


}
