package com.example.dronemr.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dronemr.MainActivity
import com.example.dronemr.databinding.FragmentGalleryBinding
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem


class GalleryFragment : Fragment() {



    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var mainActivity: MainActivity

    /** reference to the mediastore of the drone */
    private var mediaStoreRef: Ref<MediaStore>? = null

    /** data stored in the mediastore*/
    private lateinit var mediaList : Ref<List<@JvmSuppressWildcards MediaItem>>
    private lateinit var observer : Ref.Observer<List<@JvmSuppressWildcards MediaItem>>


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        mainActivity = activity as MainActivity


        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root



        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getMedia() {
        mediaStoreRef = mainActivity.drone?.getPeripheral(MediaStore::class.java) {

            if(it != null) {
                mediaList = it.browse(null, observer)
            }
        }

    }
}