package com.lock.applock.helper

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.AsyncTask
import com.lock.applock.`object`.App
import com.reader.pdfreader.fragment.DislayAppFragment
import kotlin.collections.ArrayList


class GetListApp(private var context: Context,
                 private var fragment: DislayAppFragment) : AsyncTask<Void, Int, ArrayList<App>>() {
    override fun doInBackground(vararg p0: Void?): ArrayList<App> {
        val list = getListOfInstalledApp(context)
        return fragment.onDoing(list)
    }
    private fun getListOfInstalledApp(context: Context): ArrayList<App> {
        val packageManager = context.packageManager
        val installedApps =
            ArrayList<App>()
        val apps =
            packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        if (apps != null && !apps.isEmpty()) {
            for (i in apps.indices) {
                val p = apps[i]
                val appInfo: ApplicationInfo? = null
                try {
                    if (null != packageManager.getLaunchIntentForPackage(p.packageName)) {
                        if(p.packageName==context.packageName) {
                            val app = App(
                                p.applicationInfo.loadLabel(packageManager).toString(),
                                p.packageName,
                                p.versionName,

                                1
                            )

                            installedApps.add(app)
                        }else{
                            val app = App(
                                p.applicationInfo.loadLabel(packageManager).toString(),
                                p.packageName,
                                p.versionName,

                                0
                            )

                            installedApps.add(app)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
        return installedApps
    }

    override fun onPostExecute(result: ArrayList<App>) {
        super.onPostExecute(result)
        fragment.onPost()
        fragment.updateData(result)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        fragment.onPre()
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        fragment.onUp(*values)
    }



}



