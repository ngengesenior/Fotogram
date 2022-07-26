package com.ngengeapps.fotogram

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ngengeapps.fotogram.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor


@AndroidEntryPoint
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private var preview: Preview? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var imageCapture: ImageCapture? = null
    private val executor:Executor by lazy {
        ContextCompat.getMainExecutor(requireContext())
    }

    private val sharedViewModel:SharedFotogramViewModel by activityViewModels()


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //hideStatusBar()

        val permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { thePermissions ->
                if (thePermissions.isNotEmpty() && thePermissions.all {
                        it.value
                    }) {
                    viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                        startCamera()
                    }
                } else {


                }

            }

        permissionsLauncher.launch(permissions)
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.sharedViewModel= sharedViewModel

        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       binding.fabCaptureImage.setOnClickListener {
           captureImage()
       }


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun startCamera() {
        bindPreviewToSurface()
        initImageCapture()
        val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).await()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector,preview,imageCapture)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding to  camera lifecycle", e)
        }

    }

    private fun initImageCapture() {
        imageCapture = ImageCapture.Builder().setJpegQuality(100).build()

    }

    private fun bindPreviewToSurface() {
        preview = Preview.Builder().build()
        preview?.setSurfaceProvider(binding.previewView.surfaceProvider)
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Fotogram/Images")
            }
        }

        // For android Q and above, you can modify content only if it is an external primary volume
        val imagesCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val outputFileOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireActivity()
                    .contentResolver, imagesCollection,
                contentValues
            ).build()

        imageCapture.takePicture(outputFileOptions,executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                val outputDir = getOutputDirectory(this@MainFragment.requireContext())
                val file = File(outputDir, "$name.jpg")
                val uri = outputFileResults.savedUri ?: Uri.fromFile(file)
                val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(uri),null,BitmapFactory.Options())
                sharedViewModel.loadBitmap(bitmap)
                findNavController().navigate(R.id.action_MainFragment_to_ViewFragment)
                Log.d(TAG, "Photo capture succeeded: $uri")


            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "onError: Error capturing image",exception )
            }

        })

    }


    companion object {

        fun getOutputDirectory(context:Context):File {
            val appCtx = context.applicationContext

            return appCtx.externalMediaDirs.firstOrNull()?.let {
                File(it, appCtx.resources.getString(R.string.app_name)).apply {
                    mkdirs()
                }
            } ?: appCtx.filesDir
        }

        private val permissions = mutableListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        private val TAG = MainFragment::class.java.simpleName
    }
}