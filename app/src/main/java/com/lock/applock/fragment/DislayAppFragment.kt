package com.reader.pdfreader.fragment


import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.lock.applock.R
import com.lock.applock.`object`.App
import com.lock.applock.activity.MainActivity
import com.lock.applock.adapter.AppAdapter
import com.lock.applock.db.DbApp
import com.lock.applock.helper.GetListApp
import kotlinx.android.synthetic.main.fragment_dislay_app.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


/**
 * A simple [Fragment] subclass.
 *
 */
class DislayAppFragment : Fragment() {
    var arrApp = ArrayList<App>()
    var arrFileSearch = ArrayList<App>()
    var sharedPreferences: SharedPreferences? = null
    var editor: SharedPreferences.Editor? = null
    private var progressDialog: ProgressDialog? = null
    var i = 0
    var brPo: BroadcastReceiver? = null
    var dbApp: DbApp? = null
    var showPo = false
    var adapter : AppAdapter?=null

    @SuppressLint("CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_dislay_app, container, false)
        val intent = IntentFilter()
        intent.addAction("SEARCH")
        intent.addAction("LOCKEDAPP")
        intent.addAction("POPUP")
        intent.addAction("DARK")
        context!!.registerReceiver(broadcastReceiver, intent)
        sharedPreferences = context!!.getSharedPreferences("hieu", Context.MODE_PRIVATE)
        editor = sharedPreferences?.edit()
        progressDialog = ProgressDialog(context)
        progressDialog!!.setCancelable(true)
        progressDialog!!.isIndeterminate = false
        progressDialog!!.setMessage("Loading...")
        progressDialog!!.max = 100
        dbApp = DbApp(context!!,null)



        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        GetListApp(context!!,this).execute()
        updateDark()

    }


    fun onPost() {
        if (progressDialog != null && progressDialog!!.isShowing) {
                progressDialog!!.dismiss()

            }


    }
    fun onPre(){
        progressDialog!!.show()

    }
    fun onUp(vararg values: Int?){
        progressDialog!!.progress = values[0]!!.inv()
    }
    fun onDoing(list: ArrayList<App>): ArrayList<App>{
        arrApp.clear()
        if (dbApp!!.getApp().size == 0) {
            for (app in list)
                dbApp!!.insertApp(
                    app.name,
                    app.packagename,
                    app.isLock
                )

        }
        if (list.size > dbApp!!.getApp().size) {
            for (app in list) {
                if (!dbApp!!.checkPath(app.packagename)) {
                    dbApp!!.insertApp(
                        app.name,
                        app.packagename,
                        app.isLock
                    )
                }
            }
        }
        if(list.size < dbApp!!.getApp().size){
            for(app in dbApp!!.getApp()){
                if(!checkPathNho(list,app.packagename)){
                    dbApp!!.deleteApp(app.packagename)
                }

            }

        }


        if ( dbApp!!.getApp().size > 0) {
            arrApp = dbApp!!.getApp()
        }
        return arrApp

    }
    private fun checkPathNho(list: ArrayList<App>, pagkageName:String):Boolean{
        for(app in list){
            if(pagkageName == app.packagename) return true
        }
        return false

    }

    fun updateData(list: ArrayList<App>) {
        rv_app.layoutManager = LinearLayoutManager(context)
        recycleView(list)

    }


    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            val action = p1?.action
            val string = p1?.extras?.getString("string")


//                recycleView()

            if (action!!.equals("SEARCH", ignoreCase = true)) {
                dislaySearch(string)
//                Toast.makeText(context,string,Toast.LENGTH_SHORT).show()
            } else if (action.equals("LOCKEDAPP", ignoreCase = true)
                )
             {
//                arrFile = dbPdf!!.getPdf()
                 arrApp = dbApp!!.getApp()
                 recycleView(arrApp)

            }else if (action.equals("POPUP", ignoreCase = true)) {
                val show = p1.extras!!.getBoolean("show")

                showPo = show

            }else if (action.equals("DARK", ignoreCase = true)) {
                var dark = p1.extras!!.getBoolean("dark")
                if(dark) {
                    rl_display.setBackgroundResource(R.color.dark)
                }else{
                    rl_display.setBackgroundResource(R.color.normal)
                }
                adapter!!.notifyDataSetChanged()

            }

        }
    }


        @SuppressLint("DefaultLocale")
        private fun dislaySearch(string: String?) {
            arrFileSearch.clear()
            for (pdf in arrApp) {
                if (pdf.name.toLowerCase().contains(string!!.toLowerCase())) {
                    arrFileSearch.add(pdf)
                }
            }
//        arrFile = arrFileSearch

            val adapter = AppAdapter(context!!, arrFileSearch, object : AppAdapter.ItemListener {
                override fun onClick(position: Int,packageName:String) {
                    if (showPo) {
                        (activity as MainActivity).set()
                        showPo = false


                    } else {
                        if (arrFileSearch[position].isLock == 0) {
                            arrFileSearch[position].isLock = 1
                            dbApp!!.updateLock(packageName, 1)
                        } else {
                            arrFileSearch[position].isLock = 0
                            dbApp!!.updateLock(packageName, 0)
                        }
                        val intent = Intent("LOCKED")
                        context!!.sendBroadcast(intent)
                    }
                }
            })
            rv_app.adapter = adapter

        }

        private fun recycleView(list: ArrayList<App>) {
          adapter = AppAdapter(context!!, list, object : AppAdapter.ItemListener {


                override fun onClick(position: Int,packageName:String) {

                    if (showPo) {
                        (activity as MainActivity).set()
                        showPo = false


                    } else {
                        if (list[position].isLock == 0) {
                            list[position].isLock = 1
                            dbApp!!.updateLock(packageName, 1)
                        } else {
                            list[position].isLock = 0
                            dbApp!!.updateLock(packageName, 0)
                        }

                        val intent = Intent("LOCKED")
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
            rl_display.setBackgroundResource(R.color.dark)
        }else{
            rl_display.setBackgroundResource(R.color.normal)
        }

    }


}
