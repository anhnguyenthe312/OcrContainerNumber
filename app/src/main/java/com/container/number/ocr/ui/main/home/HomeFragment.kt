package com.container.number.ocr.ui.main.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.container.number.ocr.R
import com.container.number.ocr.constant.Constants
import com.container.number.ocr.databinding.FragmentHomeBinding
import com.container.number.ocr.extension.logcat
import com.container.number.ocr.model.data.Event
import com.container.number.ocr.model.data.EventObserver
import com.container.number.ocr.model.data.Resource
import com.container.number.ocr.ui.main.MainActivityVM
import com.container.number.ocr.ui.main.home.adapter.HomeAdapter
import com.container.number.ocr.ui.main.start.StartFragment
import com.container.number.ocr.utils.BitmapUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeFragment : Fragment() {

    companion object {
        const val REQUEST_CODE = 122
        fun newInstance(uri: Uri) = HomeFragment().apply {
            arguments = Bundle()
            arguments!!.putString(Constants.EXTRA_URI_STRING, uri.toString())
        }
    }

    private lateinit var viewModel: HomeVM
    private var binding: FragmentHomeBinding? = null
    private val activityVM: MainActivityVM by activityViewModels<MainActivityVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(HomeVM::class.java)
        setHasOptionsMenu(true)
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

        viewModel.savingScreenShot.observe(viewLifecycleOwner, {
            binding?.apply {
                when (it.status) {
                    Resource.Status.SUCCESS -> {
                        homeViewPager.isUserInputEnabled = true
                        activityVM.endScreenShot.postValue(true)
                    }
                    Resource.Status.ERROR -> {
                        homeViewPager.isUserInputEnabled = true
                        activityVM.endScreenShot.postValue(true)
                    }
                    Resource.Status.LOADING -> homeViewPager.isUserInputEnabled = false
                }
            }
        })

        activityVM.startScreenShot.observe(viewLifecycleOwner, EventObserver{
            binding?.apply {
                val bitmap = BitmapUtils.captureView(root)
                viewModel.saveScreenShot(requireContext(), it, bitmap)
            }
        })

    }


}