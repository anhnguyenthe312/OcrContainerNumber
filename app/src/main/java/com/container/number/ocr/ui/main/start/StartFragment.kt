package com.container.number.ocr.ui.main.start

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import com.container.number.ocr.R
import com.container.number.ocr.databinding.FragmentStartBinding
import com.container.number.ocr.ui.main.home.HomeFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class StartFragment : Fragment() {

    companion object {
        fun newInstance() = StartFragment()
        const val REQUEST_CODE = 121
    }

    private lateinit var viewModel: StartVM
    private var binding: FragmentStartBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentStartBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(StartVM::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            btnFolder.setOnClickListener {
                openDirectory()
            }
        }
    }

    private fun openDirectory() {
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE
            && resultCode == Activity.RESULT_OK
        ) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            data?.data?.also { uri ->
                // Perform operations on the document using its URI.
                DocumentFile.fromTreeUri(requireContext(), uri)?.apply {
                    if (this.listFiles().isNotEmpty()){
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.container, HomeFragment.newInstance(uri))
                            .commitNow()
                    }
                    else {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Information!")
                            .setMessage("There is no photo in this folder")
                            .show()
                    }
                }

            }
        }
    }

}