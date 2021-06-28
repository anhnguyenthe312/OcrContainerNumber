package com.container.number.ocr.ui.main.photo.adapter

import androidx.recyclerview.widget.RecyclerView
import com.container.number.ocr.databinding.ItemContainerNumberModeBinding
import com.container.number.ocr.model.type.OcrAlgorithm

class SelectModeVH(val binding: ItemContainerNumberModeBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(containerNumberMode: OcrAlgorithm) {
        binding.txtContainerModeName.text = itemView.context.getString(containerNumberMode.stringResId)
        binding.ivContainerMode.setImageResource(containerNumberMode.imageResId)
    }
}