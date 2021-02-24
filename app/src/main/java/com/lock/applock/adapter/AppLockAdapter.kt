package com.lock.applock.adapter

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.lock.applock.R
import com.lock.applock.`object`.App
import kotlinx.android.synthetic.main.adapter_app.view.*
import kotlinx.android.synthetic.main.adapter_app.view.iv
import kotlinx.android.synthetic.main.adapter_app.view.iv_pop_menu
import kotlinx.android.synthetic.main.adapter_app_lock.view.*

class AppLockAdapter(private val context: Context,
                     private var arr : ArrayList<App>,
                     private val listener: ItemListener
) : RecyclerView.Adapter<AppLockAdapter.ViewHolder>(){
    var sharedPreferences: SharedPreferences? = null
    var edit: SharedPreferences.Editor? = null
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        sharedPreferences = context.getSharedPreferences("hieu", Context.MODE_PRIVATE)
        edit = sharedPreferences!!.edit()
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_app_lock,p0,false))

    }


    override fun getItemCount(): Int =arr.size


    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        p0.bindData()

    }


    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v){
        fun bindData() {
            itemView.tv_name_lock.text = arr[adapterPosition].name
            itemView.iv_pop_menu.setOnClickListener {
                if (arr[adapterPosition].packagename != context.packageName) {
                    listener.onClick(adapterPosition, arr[adapterPosition].packagename)
                    removeItem(adapterPosition)
                }
            }
            Glide
                .with(context)
                .load(context.packageManager.getApplicationIcon(arr[adapterPosition].packagename))
                .thumbnail(0.5f)
                .transition(
                    DrawableTransitionOptions()
                        .crossFade()
                )
                .into(itemView.iv)
            if (sharedPreferences!!.getBoolean("dark",false)){
                itemView.tv_name_lock.setTextColor(Color.parseColor("#FFFFFF"))
            }else{
                itemView.tv_name_lock.setTextColor(Color.parseColor("#000000"))
            }
        }



    }
    interface ItemListener {
        fun onClick(position:Int,packageName:String)
    }
    fun removeItem(position: Int) {
        arr.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, arr.size);
    }

}