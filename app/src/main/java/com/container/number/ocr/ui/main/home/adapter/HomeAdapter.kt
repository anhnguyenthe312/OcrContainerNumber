package com.container.number.ocr.ui.main.home.adapter

import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.container.number.ocr.ui.main.photo.PhotoFragment

class HomeAdapter(
    private val uriList: List<Uri>,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
): FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = uriList.size

    override fun createFragment(position: Int): Fragment {
        return PhotoFragment.newInstance(uriList[position])
    }
}