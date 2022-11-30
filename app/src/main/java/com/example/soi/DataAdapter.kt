package com.example.soi

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class DataAdapter (private val context: Context,
                   private val dataSource: ArrayList<DataClass>) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(p0: Int): Any {
        return dataSource[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.item, parent, false)
        val d = getItem(position) as DataClass
        rowView.findViewById<TextView>(R.id.item_name).text = d.name
        rowView.findViewById<TextView>(R.id.item_value).text = d.value.toString()
        var unit = "C"
        if (position == 1){
            unit = "hPa"
        }
        rowView.findViewById<TextView>(R.id.item_unit).text = unit
        return  rowView
    }
}