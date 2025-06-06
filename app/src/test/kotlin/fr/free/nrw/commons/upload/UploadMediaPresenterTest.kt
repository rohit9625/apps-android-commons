package fr.free.nrw.commons.upload

import android.net.Uri
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailsContract
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaPresenter
import fr.free.nrw.commons.utils.ImageUtils.EMPTY_CAPTION
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import io.github.coordinates2country.Coordinates2Country
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner
import java.util.Collections

/**
 * The class contains unit test cases for UploadMediaPresenter
 */
@RunWith(RobolectricTestRunner::class)
@PrepareForTest(Coordinates2Country::class)
class UploadMediaPresenterTest {
    @Mock
    internal lateinit var repository: UploadRepository

    @Mock
    internal lateinit var view: UploadMediaDetailsContract.View

    private lateinit var uploadMediaPresenter: UploadMediaPresenter

    @Mock
    private lateinit var uploadableFile: UploadableFile

    @Mock
    private lateinit var place: Place

    @Mock
    private lateinit var location: LatLng

    @Mock
    private lateinit var uploadItem: UploadItem

    @Mock
    private lateinit var imageCoordinates: ImageCoordinates

    private lateinit var testObservableUploadItem: Observable<UploadItem>
    private lateinit var testSingleImageResult: Single<Int>

    private lateinit var testScheduler: TestScheduler
    private lateinit var mockedCountry: MockedStatic<Coordinates2Country>

    @Mock
    lateinit var mockActivity: UploadActivity

    /**
     * initial setup unit test environment
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testObservableUploadItem = Observable.just(uploadItem)
        testSingleImageResult = Single.just(1)
        testScheduler = TestScheduler()
        uploadMediaPresenter =
            UploadMediaPresenter(
                repository,
                testScheduler,
                testScheduler,
            )
        uploadMediaPresenter.onAttachView(view)
        mockedCountry = mockStatic(Coordinates2Country::class.java)
    }

    @After
    fun tearDown() {
        mockedCountry.close()
    }

    /**
     * unit test for method UploadMediaPresenter.receiveImage
     */
    @Test
    fun receiveImageTest() {
        whenever(
            repository.preProcessImage(
                ArgumentMatchers.any(UploadableFile::class.java),
                ArgumentMatchers.any(Place::class.java),
                ArgumentMatchers.any(UploadMediaPresenter::class.java),
                ArgumentMatchers.any(LatLng::class.java),
            ),
        ).thenReturn(testObservableUploadItem)
        uploadMediaPresenter.receiveImage(uploadableFile, place, location)
        verify(view).showProgress(true)
        testScheduler.triggerActions()
        verify(view).onImageProcessed(isA())
    }

    /**
     * unit test for method UploadMediaPresenter.getImageQuality (For else case)
     */
    @Test
    fun getImageQualityTest() {
        whenever(repository.getUploads()).thenReturn(listOf(uploadItem))
        whenever(repository.getImageQuality(uploadItem, location))
            .thenReturn(testSingleImageResult)
        whenever(uploadItem.imageQuality).thenReturn(0)
        whenever(uploadItem.gpsCoords)
            .thenReturn(imageCoordinates)
        whenever(uploadItem.gpsCoords?.decimalCoords)
            .thenReturn("imageCoordinates")
        uploadMediaPresenter.getImageQuality(0, location, mockActivity)
        verify(view).showProgress(true)
        testScheduler.triggerActions()
    }

    /**
     * unit test for method UploadMediaPresenter.getImageQuality (For if case)
     */
    @Test
    fun `get ImageQuality Test while coordinates equals to null`() {
        whenever(repository.getUploads()).thenReturn(listOf(uploadItem))
        whenever(repository.getImageQuality(uploadItem, location))
            .thenReturn(testSingleImageResult)
        whenever(uploadItem.imageQuality).thenReturn(0)
        whenever(uploadItem.gpsCoords)
            .thenReturn(imageCoordinates)
        whenever(uploadItem.gpsCoords?.decimalCoords)
            .thenReturn(null)
        uploadMediaPresenter.getImageQuality(0, location, mockActivity)
        testScheduler.triggerActions()
    }

    /**
     * Test for empty file name when the user presses the NEXT button
     */
    @Test
    fun emptyFileNameTest() {
        uploadMediaPresenter.handleCaptionResult(EMPTY_CAPTION, uploadItem)
        verify(view).showMessage(R.string.add_caption_toast, R.color.color_error)
    }

    /**
     * Test for duplicate file name when the user presses the NEXT button
     */
    @Test
    fun duplicateFileNameTest() {
        uploadMediaPresenter.handleCaptionResult(FILE_NAME_EXISTS, uploadItem)
        verify(view).showDuplicatePicturePopup(uploadItem)
    }

    /**
     * Test for correct file name when the user presses the NEXT button
     */
    @Test
    fun correctFileNameTest() {
        uploadMediaPresenter.handleCaptionResult(IMAGE_OK, uploadItem)
        verify(view).onImageValidationSuccess()
    }

    @Test
    fun addSingleCaption() {
        val uploadMediaDetail = UploadMediaDetail()
        uploadMediaDetail.captionText = "added caption"
        uploadMediaDetail.languageCode = "en"
        val uploadMediaDetailList: ArrayList<UploadMediaDetail> = ArrayList()
        uploadMediaDetailList.add(uploadMediaDetail)
        uploadItem.uploadMediaDetails = uploadMediaDetailList
        Mockito.`when`(repository.getImageQuality(uploadItem, location)).then {
            verify(view).showProgress(true)
            testScheduler.triggerActions()
            verify(view).showProgress(true)
            verify(view).onImageValidationSuccess()
        }
    }

    @Test
    fun addMultipleCaptions() {
        val uploadMediaDetail = UploadMediaDetail()
        uploadMediaDetail.captionText = "added caption"
        uploadMediaDetail.languageCode = "en"
        uploadMediaDetail.captionText = "added caption"
        uploadMediaDetail.languageCode = "eo"
        uploadItem.uploadMediaDetails = Collections.singletonList(uploadMediaDetail)
        Mockito.`when`(repository.getImageQuality(uploadItem, location)).then {
            verify(view).showProgress(true)
            testScheduler.triggerActions()
            verify(view).showProgress(true)
            verify(view).onImageValidationSuccess()
        }
    }

    /**
     * Test fetch image title when there was one
     */
    @Test
    fun fetchImageAndTitleTest() {
        whenever(repository.getUploads()).thenReturn(listOf(uploadItem))
        whenever(repository.getUploadItem(ArgumentMatchers.anyInt())).thenReturn(uploadItem)
        whenever(uploadItem.uploadMediaDetails).thenReturn(mutableListOf())

        uploadMediaPresenter.fetchTitleAndDescription(0)
        verify(view).updateMediaDetails(isA())
    }

    /**
     * Test show SimilarImageFragment
     */
    @Test
    fun showSimilarImageFragmentTest() {
        val similar: ImageCoordinates = mock()
        uploadMediaPresenter.showSimilarImageFragment("original", "possible", similar)
        verify(view).showSimilarImageFragment("original", "possible", similar)
    }

    @Test
    fun setCorrectCountryCodeForReceivedImage() {
        val germanyAsPlace =
            Place(null, null, null, null, LatLng(50.1, 10.2, 1.0f), null, null, null, true, null)
        germanyAsPlace.isMonument = true

        whenever(
            Coordinates2Country.country(
                ArgumentMatchers.eq(germanyAsPlace.getLocation().latitude),
                ArgumentMatchers.eq(germanyAsPlace.getLocation().longitude),
            ),
        ).thenReturn("Germany")

        val item: Observable<UploadItem> =
            Observable.just(UploadItem(Uri.EMPTY, null, null, germanyAsPlace, 0, null, null, null))

        whenever(
            repository.preProcessImage(
                ArgumentMatchers.any(UploadableFile::class.java),
                ArgumentMatchers.any(Place::class.java),
                ArgumentMatchers.any(UploadMediaPresenter::class.java),
                ArgumentMatchers.any(LatLng::class.java),
            ),
        ).thenReturn(item)

        uploadMediaPresenter.receiveImage(uploadableFile, germanyAsPlace, location)
        verify(view).showProgress(true)
        testScheduler.triggerActions()

        val captor = argumentCaptor<UploadItem>()
        verify(view).onImageProcessed(captor.capture())

        assertEquals("Exptected contry code", "de", captor.firstValue.countryCode)
    }
}
