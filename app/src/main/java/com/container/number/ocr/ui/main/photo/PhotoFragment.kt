package com.container.number.ocr.ui.main.photo

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.container.number.ocr.R
import com.container.number.ocr.constant.Constants
import com.container.number.ocr.databinding.FragmentPhotoBinding
import com.container.number.ocr.model.data.Event
import com.container.number.ocr.model.data.EventObserver
import com.container.number.ocr.model.data.Resource
import com.container.number.ocr.model.entity.PhotoOcr
import com.container.number.ocr.model.type.Evaluate
import com.container.number.ocr.model.type.OcrAlgorithm
import com.container.number.ocr.ui.main.MainActivity
import com.container.number.ocr.ui.main.MainActivityVM
import com.container.number.ocr.ui.main.photo.adapter.SelectModeAdapter
import com.container.number.ocr.utils.BitmapUtils
import com.container.number.ocr.utils.TextOnImageAnalyzer
import com.container.number.ocr.utils.WidthHeightUtils
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptions

class PhotoFragment : Fragment(), TextOnImageAnalyzer.TextRecognizedListener {

    companion object {
        fun newInstance(uri: Uri): PhotoFragment {
            return PhotoFragment().apply {
                arguments = Bundle()
                arguments!!.putString(Constants.EXTRA_URI_STRING, uri.toString())
            }
        }
    }

    private var binding: FragmentPhotoBinding? = null
    private lateinit var viewModel: PhotoVM
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        obServe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PhotoVM::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            val uri = getPhotoUri()

            //decrease size photo to fast load
            Glide.with(requireContext())
                .load(uri)
                .override(350, 350)
                .into(ivPhoto)

            viewModel.loadPhotoFromUri(requireContext(), uri)

            btnEvaluate.setOnClickListener {
                showEvaluateDialog()
            }
            bindProgressButton(btnEvaluate)
            btnEvaluate.attachTextChangeAnimator()

            viewContainerMode.setOnClickListener {
                showSelectContainerMode()
            }

            updateContainerModeView()
        }
    }

    private fun getPhotoUri(): Uri {
        val uriStr = requireArguments().getString(Constants.EXTRA_URI_STRING)
        return Uri.parse(uriStr)
    }

    private fun obServe() {
        viewModel.originalBitmapEvent.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    binding?.apply {
                        val uri = getPhotoUri()
                        viewModel.checkPhotoOcrExist(requireContext(), uri)

                        BitmapUtils.loadBitmapEfficientlyToImageView(
                            viewModel.originalBitmap!!,
                            ivPhoto,
                            requireContext(),
                            (WidthHeightUtils.getScreenWidth(requireActivity()) * 0.9F).toInt()
                        )
                    }
                }
                Resource.Status.ERROR -> {
                    binding?.apply {
                        btnEvaluate.isEnabled = true
                    }
                }
                Resource.Status.LOADING -> {
                    binding?.apply {
                        btnEvaluate.isEnabled = false
                    }
                }
            }

        })
        viewModel.photoBitmap.observe(viewLifecycleOwner, {
            binding?.apply {
                BitmapUtils.loadBitmapEfficientlyToImageView(
                    it,
                    ivPhoto,
                    requireContext(),
                    (WidthHeightUtils.getScreenWidth(requireActivity()) * 0.9F).toInt()
                )
            }
        })
        viewModel.startOcrOnPhoto.observe(viewLifecycleOwner, EventObserver {
            if (it) {
                analyzePhoto()
            }
        })

        viewModel.containerNumberLiveData.observe(viewLifecycleOwner, EventObserver {
            binding?.apply {
                txtContainerNumber.text = it
                btnEvaluate.isEnabled = true
            }
        })

        viewModel.updatePhotoOcrToUI.observe(viewLifecycleOwner, EventObserver {
            binding?.apply {
                btnEvaluate.isEnabled = true
                updateUI(it)
            }
        })

        viewModel.saveDbSuccess.observe(viewLifecycleOwner, EventObserver{
            binding?.apply {
                btnEvaluate.tag?.let {
                    btnEvaluate.showProgress{
                        this.progressColor = Color.WHITE
                    }
                }
            }
            activityVM.startScreenShot.postValue(Event(getPhotoUri()))
        })

        activityVM.endScreenShot.observe(viewLifecycleOwner, {
            binding?.apply {
                btnEvaluate.tag?.let {
                    val lastText = it.toString()
                    btnEvaluate.hideProgress(lastText)
                }
            }
        })

    }

    override fun onRecognized(text: Text, croppedBitmap: Bitmap?) {
        viewModel.onRecognized(text, viewModel.currentAlgorithm)
    }

    override fun onRecognizedError(e: Exception) {
        e.printStackTrace()
    }

    private fun analyzePhoto() {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
        lifecycle.addObserver(recognizer)
        TextOnImageAnalyzer.analyzeAnImage(
            recognizer = recognizer,
            inputImage = InputImage.fromBitmap(viewModel.originalBitmap!!, 0),
            null,
            listener = this
        )
    }

    private val activityVM: MainActivityVM by activityViewModels<MainActivityVM>()
    private fun showEvaluateDialog() {
        val selectList = Evaluate.values().filter { it != Evaluate.NOT_READ }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set your evaluation!")
            .setItems(selectList.map { getString(it.resId) }
                .toTypedArray()) { dialog, which ->
                val evaluate = selectList[which]

                binding?.apply {
                    val text = getString(evaluate.resId)
                    btnEvaluate.tag = text
                    btnEvaluate.text = text
                    btnEvaluate.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
                }

                viewModel.saveEvaluate(
                    requireContext(),
                    getPhotoUri(),
                    evaluate,
                    viewModel.currentAlgorithm
                )
            }
            .show()
    }

    private fun updateUI(photoOcr: PhotoOcr) {
        binding?.apply {
            viewModel.currentAlgorithm = photoOcr.algorithm
            updateContainerModeView()

            txtContainerNumber.text = photoOcr.containerNumber
            btnEvaluate.text = getString(photoOcr.evaluate.resId)
            viewModel.drawRectOnly(
                photoOcr.boundingRect,
                photoOcr.containerNumber,
                getString(photoOcr.evaluate.resId)
            )
            btnEvaluate.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            txtContainerModeName.text = getString(photoOcr.algorithm.stringResId)
            ivContainerMode.setImageResource(photoOcr.algorithm.imageResId)
        }
    }

    private fun showSelectContainerMode() {
        val adapter = SelectModeAdapter(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_container_number_mode))
            .setAdapter(
                adapter,
            ) { dialog, which ->
                viewModel.currentAlgorithm = OcrAlgorithm.values()[which]
                updateContainerModeView()
                analyzePhoto()
            }
            .show()
    }

    private fun updateContainerModeView() {
        binding?.apply {
            txtContainerModeName.text =
                getString(viewModel.currentAlgorithm.stringResId)
            ivContainerMode.setImageResource(viewModel.currentAlgorithm.imageResId)
        }
    }
}