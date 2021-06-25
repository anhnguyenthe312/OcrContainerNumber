package com.container.number.ocr.ui.main.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.container.number.ocr.constant.Constants
import com.container.number.ocr.databinding.FragmentHomeBinding
import com.container.number.ocr.ui.main.home.adapter.HomeAdapter

class HomeFragment : Fragment() {

    companion object {
        fun newInstance(uri: Uri) = HomeFragment().apply {
            arguments = Bundle()
            arguments!!.putString(Constants.EXTRA_URI_STRING, uri.toString())
        }
    }

    private lateinit var viewModel: HomeVM
    private var binding: FragmentHomeBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(HomeVM::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            val folderUriString = requireArguments().getString(Constants.EXTRA_URI_STRING)
            val folderUri = Uri.parse(folderUriString)
            DocumentFile.fromTreeUri(requireContext(), folderUri)?.apply {
                val uriList = listFiles().filter { it.isFile }.map { it.uri }

                //check progress
                viewModel.checkOrcProgress(requireContext(), this.uri, uriList.size)

                //adapter
                val adapter = HomeAdapter(uriList, childFragmentManager, lifecycle)
                homeViewPager.adapter = adapter
                homeViewPager.registerOnPageChangeCallback(object :
                    ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        txtStatus.text = "${position + 1} / ${adapter.itemCount}"
                    }
                })
            }
        }

        viewModel.ocrProgress.observe(viewLifecycleOwner, {
            binding?.apply {
                txtOk.text = it.readOk.toString()
                txtNotGood.text = it.readNotGood.toString()
                txtNotStable.text = it.readNotStable.toString()
                txtRemaining.text = it.remaining.toString()
            }
        })

    }

}