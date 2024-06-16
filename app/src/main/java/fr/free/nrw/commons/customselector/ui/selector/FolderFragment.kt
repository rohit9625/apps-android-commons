package fr.free.nrw.commons.customselector.ui.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.ui.adapter.FolderAdapter
import fr.free.nrw.commons.databinding.FragmentCustomSelectorBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import javax.inject.Inject

/**
 * Custom selector folder fragment.
 */
class FolderFragment : CommonsDaggerSupportFragment() {

    /**
     * ViewBinding
     */
    private var _binding: FragmentCustomSelectorBinding? = null
    private val binding get() = _binding

    private lateinit var composeGridView: ComposeView

    /**
     * View Model for images.
     */
    private var viewModel: CustomSelectorViewModel? = null

    /**
     * View Elements
     */
    private var selectorRV: RecyclerView? = null
    private var loader: ProgressBar? = null

    /**
     * View Model Factory.
     */
    var customSelectorViewModelFactory: CustomSelectorViewModelFactory? = null
        @Inject set

    var fileProcessor: FileProcessor? = null
        @Inject set

    var mediaClient: MediaClient? = null
        @Inject set
    /**
     * Folder Adapter.
     */
    private lateinit var folderAdapter: FolderAdapter

    /**
     * Grid Layout Manager for recycler view.
     */
    private lateinit var gridLayoutManager: GridLayoutManager

    /**
     * Folder List.
     */
    private lateinit var folders : ArrayList<Folder>

    /**
     * Companion newInstance.
     */
    companion object{
        fun newInstance(): FolderFragment {
            return FolderFragment()
        }
    }

    /**
     * OnCreate Fragment, get the view model.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(),customSelectorViewModelFactory!!).get(CustomSelectorViewModel::class.java)

    }

    /**
     * OnCreateView.
     * Inflate Layout, init adapter, init gridLayoutManager, setUp recycler view, observe the view model for result.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        _binding = FragmentCustomSelectorBinding.inflate(inflater, container, false)
        folderAdapter = FolderAdapter(requireActivity(), activity as FolderClickListener)
        gridLayoutManager = GridLayoutManager(context, columnCount())
        selectorRV = binding?.selectorRv
        loader = binding?.loader
        with(binding?.selectorRv){
            this?.layoutManager = gridLayoutManager
            this?.setHasFixedSize(true)
            this?.adapter = folderAdapter
        }
        viewModel?.result?.observe(viewLifecycleOwner) {
            handleResult(it)
        }

//        return _binding!!.root
        return ComposeView(requireContext()).also {
            composeGridView = it
        }
    }

    /**
     * Handle view model result.
     * Get folders from images.
     * Load adapter.
     */
    private fun handleResult(result: Result) {
        if(result.status is CallbackStatus.SUCCESS){
            val images = result.images
            if(images.isNullOrEmpty())
            {
                binding?.emptyText?.let {
                    it.visibility = View.VISIBLE
                }
            }
            folders = ImageHelper.folderListFromImages(result.images)
            folderAdapter.init(folders)
            folderAdapter.notifyDataSetChanged()
            selectorRV?.let {
                it.visibility = View.VISIBLE
            }
        }
        loader?.let {
            it.visibility = if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        composeGridView.setContent {
            SelectorGridLayout(folders = folders)
        }
    }

    @Composable
    fun SelectorGridLayout(
        folders: List<Folder>,
        modifier: Modifier = Modifier
    ) {
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = modifier) {
            items(folders, key = { it.bucketId }) {
                SelectorGridItem(
                    folder = it,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }

    @Preview
    @Composable
    private fun SelectorGridLayoutPreview() {
        val folders = listOf(
            Folder(bucketId = 1235L, name = "My Folder 1"),
            Folder(bucketId = 1232L, name = "My Folder 2"),
            Folder(bucketId = 1236L, name = "My Folder 3"),
            Folder(bucketId = 1231L, name = "My Folder 4"),
            Folder(bucketId = 11231L, name = "My Folder 4"),
            Folder(bucketId = 12121L, name = "My Folder 4"),
            Folder(bucketId = 32521L, name = "My Folder 4"),
            Folder(bucketId = 12341L, name = "My Folder 4"),
            Folder(bucketId = 12231L, name = "My Folder 4"),
        )

        SelectorGridLayout(folders = folders)
    }

    @Composable
    fun SelectorGridItem(
        folder: Folder,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier.size(164.dp).background(color = Color.LightGray)
        ) {
            Text(
                text = folder.name,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }

    @Preview
    @Composable
    fun SelectorGridItemPreview(modifier: Modifier = Modifier) {
        SelectorGridItem(folder = Folder(bucketId = 1232L, name = "My Folder"))
    }

    /**
     * onResume
     * notifyDataSetChanged, rebuild the holder views to account for deleted images, folders.
     */
    override fun onResume() {
        folderAdapter.notifyDataSetChanged()
        super.onResume()
    }

    /**
     * onDestroyView
     * clearing view binding
     */
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /**
     * Return Column count ie span count for grid view adapter.
     */
    private fun columnCount(): Int {
        return 2
        // todo change column count depending on the orientation of the device.
    }
}
