package fr.free.nrw.commons.customselector.ui.selector

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Response
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.ui.screens.SelectableImage
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Custom Selector view model.
 */
class CustomSelectorViewModel(
    private val imageFileLoader: ImageFileLoader,
    private val contributionDao: ContributionDao
) : ViewModel() {

    /**
     * Scope for coroutine task (image fetch).
     */
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Stores selected images.
     */
    var selectedImages: MutableLiveData<ArrayList<Image>> = MutableLiveData()

    /**
     * Result Live Data.
     */
    val result = MutableLiveData(Result(CallbackStatus.IDLE, arrayListOf()))
    private val _folderState = MutableStateFlow<Response<List<Folder>>>(Response.Loading())
    val folderState = _folderState.asStateFlow()

    private val _imageState = MutableStateFlow<Response<ArrayList<Image>>>(Response.Loading())

    private val _filteredImages = MutableStateFlow<List<SelectableImage>>(arrayListOf())
    val filteredImages = _filteredImages.asStateFlow()


    /**
     * Fetch Images and supply to result.
     */

    init {
        fetchImages()
    }
    fun fetchImages() {
        result.postValue(Result(CallbackStatus.FETCHING, arrayListOf()))
        scope.cancel()
        imageFileLoader.loadDeviceImages(object: ImageLoaderListener {
            override fun onImageLoaded(images: ArrayList<Image>) {
                val folders = ImageHelper.folderListFromImages(images)
                _imageState.value = Response.Success(images)
                _folderState.value = Response.Success(folders)
                result.postValue(Result(CallbackStatus.SUCCESS, images))
            }

            override fun onFailed(throwable: Throwable) {
                _folderState.value = Response.Error(error = throwable.message.toString())
                _imageState.value = Response.Error(error = throwable.message.toString())
                result.postValue(Result(CallbackStatus.SUCCESS, arrayListOf()))
            }
        })
    }

    fun filterImagesByBucket(bucketId: Long) {
        val images = ImageHelper.filterImages(_imageState.value.data!!, bucketId)
        val uploadedImages = getUploadingContributions()

        _filteredImages.value = images.map {image->
            val isUploaded = uploadedImages.any {
                image.sha1 == it.imageSHA1
            }
            SelectableImage(image = image, alreadyActioned = isUploaded)
        }
    }

    fun toggleImageSelection(imageId: Long) {
        _filteredImages.value = _filteredImages.value.map {
            if (it.image.id == imageId) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
    }

    private fun getUploadingContributions(): List<Contribution> {
        return  contributionDao.getContribution(
            listOf(Contribution.STATE_IN_PROGRESS, Contribution.STATE_FAILED, Contribution.STATE_QUEUED, Contribution.STATE_PAUSED)
        )?.subscribeOn(Schedulers.io())?.blockingGet() ?: emptyList()
    }

    /**
     * Clear the coroutine task linked with context.
     */
    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}