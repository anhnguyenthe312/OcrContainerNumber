package com.container.number.ocr.ui.main.photo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.container.number.ocr.R
import com.container.number.ocr.model.type.OcrAlgorithm

class SelectModeAdapter(context: Context) : ArrayAdapter<OcrAlgorithm>(context, R.layout.item_container_number_mode) {

    private val modeList = OcrAlgorithm.values()

    override fun getCount(): Int = modeList.size

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView

        if (v == null) {
            val vi: LayoutInflater = LayoutInflater.from(context)
            v = vi.inflate(R.layout.item_container_number_mode, null)
        }

        val containerNumberMode = getItem(position)

        if (containerNumberMode != null) {
            val txtContainerModeName = v!!.findViewById<TextView>(R.id.txtContainerModeName)
            val ivContainerMode = v.findViewById<ImageView>(R.id.ivContainerMode)
            txtContainerModeName.text = context.getString(containerNumberMode.stringResId)
            ivContainerMode.setImageResource(containerNumberMode.imageResId)
        }

        return v!!
    }

    override fun getItem(position: Int) = modeList[position]
}