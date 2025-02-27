package fr.free.nrw.commons.nearby.fragments

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import androidx.preference.PreferenceManager
import android.provider.Settings
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding3.appcompat.queryTextChanges
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.MapController.NearbyPlacesInfo
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.navtab.NavTab
import fr.free.nrw.commons.nearby.BottomSheetAdapter
import fr.free.nrw.commons.nearby.BottomSheetAdapter.ItemClickListener
import fr.free.nrw.commons.nearby.CheckBoxTriStates
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.NearbyFilterSearchRecyclerViewAdapter
import fr.free.nrw.commons.nearby.NearbyFilterState
import fr.free.nrw.commons.nearby.NearbyUtil
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.WikidataFeedback
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.model.BottomSheetItem
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ExecutorUtils.get
import fr.free.nrw.commons.utils.LayoutUtils.getScreenWidth
import fr.free.nrw.commons.utils.LayoutUtils.setLayoutHeightAlignedToWidth
import fr.free.nrw.commons.utils.MapUtils.defaultLatLng
import fr.free.nrw.commons.utils.NearbyFABUtils.addAnchorToBigFABs
import fr.free.nrw.commons.utils.NearbyFABUtils.addAnchorToSmallFABs
import fr.free.nrw.commons.utils.NearbyFABUtils.removeAnchorFromFAB
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import fr.free.nrw.commons.utils.SystemThemeUtils
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.wikidata.WikidataConstants
import fr.free.nrw.commons.wikidata.WikidataEditListener
import fr.free.nrw.commons.wikidata.WikidataEditListener.WikidataP18EditListener
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.constants.GeoConstants.UnitOfMeasure
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.ScaleDiskOverlay
import org.osmdroid.views.overlay.TilesOverlay
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import kotlin.concurrent.Volatile


class NearbyParentFragment : CommonsDaggerSupportFragment(),
    NearbyParentFragmentContract.View,
    WikidataP18EditListener,
    LocationUpdateListener,
    LocationPermissionCallback,
    ItemClickListener,
    MediaDetailPagerFragment.MediaDetailProvider {
    var binding: FragmentNearbyParentBinding? = null

    private val mapEventsOverlay: MapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
            if (clickedMarker != null) {
                clickedMarker!!.closeInfoWindow()
            } else {
                Timber.e("CLICKED MARKER IS NULL")
            }
            if (isListBottomSheetExpanded) {
                // Back should first hide the bottom sheet if it is expanded
                hideBottomSheet()
            } else if (isDetailsBottomSheetVisible) {
                hideBottomDetailsSheet()
            }
            return true
        }

        override fun longPressHelper(p: GeoPoint): Boolean {
            return false
        }
    })

    @Inject
    lateinit var locationManager: LocationServiceManager


    @Inject
    lateinit var nearbyController: NearbyController

    @Inject
    @Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    @Inject
    lateinit var mediaClient: MediaClient

    lateinit var mediaDetails: MediaDetailPagerFragment

    lateinit var media: Media

    @Inject
    lateinit var bookmarkLocationDao: BookmarkLocationsDao

    @Inject
    lateinit var placesRepository: PlacesRepository

    @Inject
    lateinit var controller: ContributionController

    @Inject
    lateinit var wikidataEditListener: WikidataEditListener

    @Inject
    lateinit var systemThemeUtils: SystemThemeUtils

    @Inject
    lateinit var commonPlaceClickActions: CommonPlaceClickActions

    private var locationPermissionsHelper: LocationPermissionsHelper? = null
    private var nearbyFilterSearchRecyclerViewAdapter: NearbyFilterSearchRecyclerViewAdapter? = null
    private var bottomSheetListBehavior: BottomSheetBehavior<*>? = null
    private var bottomSheetDetailsBehavior: BottomSheetBehavior<*>? = null
    private var rotateBackward: Animation? = null
    private var fabClose: Animation? = null
    private var fabOpen: Animation? = null
    private var rotateForward: Animation? = null
    private val networkIntentAction = "android.net.conn.CONNECTIVITY_CHANGE"
    private var broadcastReceiver: BroadcastReceiver? = null
    private var isNetworkErrorOccurred = false
    private var snackbar: Snackbar? = null
    private var view: View? = null
    private var scope: LifecycleCoroutineScope? = null
    private var presenter: NearbyParentFragmentPresenter? = null
    private var _isDarkTheme = false
    private var isFABsExpanded = false
    private var selectedPlace: Place? = null
    private var clickedMarker: Marker? = null
    private var progressDialog: ProgressDialog? = null
    private var isPermissionDenied = false
    private var recenterToUserLocation = false
    private var mapCenter: GeoPoint? = null
    private var intentFilter: IntentFilter = IntentFilter(networkIntentAction)
    private var lastPlaceToCenter: Place? = null
    private var lastKnownLocation: LatLng? = null
    private var isVisibleToUser = false
    private var lastFocusLocation: LatLng? = null
    private var adapter: PlaceAdapter? = null
    private var lastMapFocus: GeoPoint? = null
    private var nearbyParentFragmentInstanceReadyCallback: NearbyParentFragmentInstanceReadyCallback? =
        null
    private var isAdvancedQueryFragmentVisible = false
    private var nearestPlace: Place? = null

    @Volatile
    private var stopQuery = false
    private var drawableCache: MutableMap<Pair<Context, Int>, Drawable>? = null

    // Explore map data (for if we came from Explore)
    private var prevZoom = 0.0
    private var prevLatitude = 0.0
    private var prevLongitude = 0.0

    private val searchHandler = Handler()
    private val searchRunnable: Runnable? = null

    private val updatedLatLng: LatLng? = null
    private var searchable = false

    private val nearbyLegend: ConstraintLayout? = null

    private var gridLayoutManager: GridLayoutManager? = null
    private var dataList: MutableList<BottomSheetItem>? = null
    private var bottomSheetAdapter: BottomSheetAdapter? = null

    private val galleryPickLauncherForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            controller.handleActivityResultWithCallback(
                requireActivity()
            ) { callbacks -> // Handle the result from the gallery
                controller.onPictureReturnedFromGallery(
                    result,
                    requireActivity(),
                    callbacks
                )
            }
        }


    private val customSelectorLauncherForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            controller.handleActivityResultWithCallback(
                requireActivity()
            ) { callbacks ->
                controller.onPictureReturnedFromCustomSelector(
                    result,
                    requireActivity(),
                    callbacks
                )
            }
        }


    private val cameraPickLauncherForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            controller.handleActivityResultWithCallback(
                requireActivity()
            ) { callbacks ->
                controller.onPictureReturnedFromCamera(
                    result,
                    requireActivity(),
                    callbacks
                )
            }
        }


    private lateinit var inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>

    private val locationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationPermissionGranted()
            } else {
                if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                    showAlertDialog(
                        requireActivity(),
                        getString(R.string.location_permission_title),
                        getString(R.string.location_permission_rationale_nearby),
                        getString(R.string.ok),
                        getString(R.string.cancel),
                        {
                            askForLocationPermission()
                        },
                        null,
                        null
                    )
                } else {
                    if (isPermissionDenied) {
                        locationPermissionsHelper?.showAppSettingsDialog(
                            requireActivity(),
                            R.string.nearby_needs_location
                        )
                    }
                    Timber.d("The user checked 'Don't ask again' or denied the permission twice")
                    isPermissionDenied = true
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        loadExploreMapData()

        binding = FragmentNearbyParentBinding.inflate(inflater, container, false)
        view = binding!!.root

        initNetworkBroadCastReceiver()
        scope = viewLifecycleOwner.lifecycleScope
        presenter = NearbyParentFragmentPresenter(
            bookmarkLocationDao,
            placesRepository, nearbyController
        )
        progressDialog = ProgressDialog(activity)
        progressDialog!!.setCancelable(false)
        progressDialog!!.setMessage("Saving in progress...")
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        return view
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        inflater.inflate(R.menu.nearby_fragment_menu, menu)
        val refreshButton = menu.findItem(R.id.item_refresh)
        val listMenu = menu.findItem(R.id.list_sheet)
        val showInExploreButton = menu.findItem(R.id.list_item_show_in_explore)
        val saveAsGPXButton = menu.findItem(R.id.list_item_gpx)
        val saveAsKMLButton = menu.findItem(R.id.list_item_kml)
        refreshButton.setOnMenuItemClickListener {
            try {
                emptyCache()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            false
        }
        listMenu.setOnMenuItemClickListener {
            listOptionMenuItemClicked()
            false
        }

        showInExploreButton.setOnMenuItemClickListener { _ ->
            (context as MainActivity).loadExploreMapFromNearby(
                binding?.map?.zoomLevelDouble ?: 0.0,  // Using safe calls to avoid NPE
                binding?.map?.mapCenter?.latitude ?: 0.0,
                binding?.map?.mapCenter?.longitude ?: 0.0
            )
            return@setOnMenuItemClickListener true
        }


        saveAsGPXButton.setOnMenuItemClickListener {
            try {
                progressDialog!!.setTitle(getString(R.string.saving_gpx_file))
                progressDialog!!.show()
                savePlacesAsGPX()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            false
        }
        saveAsKMLButton.setOnMenuItemClickListener {
            try {
                progressDialog!!.setTitle(getString(R.string.saving_kml_file))
                progressDialog!!.show()
                savePlacesAsKML()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize the launcher in the appropriate lifecycle method (e.g., onViewCreated)
        inAppCameraLocationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                val areAllGranted = result.values.all { it }

                if (areAllGranted) {
                    controller.locationPermissionCallback?.onLocationPermissionGranted()
                } else {
                    if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                        controller.handleShowRationaleFlowCameraLocation(
                            requireActivity(),
                            inAppCameraLocationPermissionLauncher,  // Reference it directly
                            cameraPickLauncherForResult
                        )
                    } else {
                        controller.locationPermissionCallback?.onLocationPermissionDenied(
                            getString(R.string.in_app_camera_location_permission_denied)
                        )
                    }
                }
            }
        _isDarkTheme = systemThemeUtils?.isDeviceInNightMode() == true
        if (Utils.isMonumentsEnabled(Date())) {
            binding?.rlContainerWlmMonthMessage?.visibility = View.VISIBLE
        } else {
            binding?.rlContainerWlmMonthMessage?.visibility = View.GONE
        }
        locationPermissionsHelper =
            LocationPermissionsHelper(requireActivity(), locationManager, this)

        // Set up the floating activity button to toggle the visibility of the legend
        binding?.fabLegend?.setOnClickListener {
            if (binding?.nearbyLegendLayout?.root?.visibility == View.VISIBLE) {
                binding?.nearbyLegendLayout?.root?.visibility = View.GONE
            } else {
                binding?.nearbyLegendLayout?.root?.visibility = View.VISIBLE
            }
        }

        presenter?.attachView(this)
        isPermissionDenied = false
        recenterToUserLocation = false
        initThemePreferences()
        initViews()
        presenter?.setActionListeners(applicationKvStore)
        org.osmdroid.config.Configuration.getInstance()
            .load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))

        // Use the Wikimedia tile server, rather than OpenStreetMap (Mapnik)
        binding?.map?.setTileSource(TileSourceFactory.WIKIMEDIA)
        binding?.map?.setTilesScaledToDpi(true)

        // Add referer HTTP header because the Wikimedia tile server requires it.
        org.osmdroid.config.Configuration.getInstance()
            .additionalHttpRequestProperties["Referer"] = "http://maps.wikimedia.org/"

        if (applicationKvStore.getString("LastLocation") != null) {
            // Checking for last searched location
            val locationLatLng = applicationKvStore.getString("LastLocation")!!.split(",")
            lastMapFocus = GeoPoint(locationLatLng[0].toDouble(), locationLatLng[1].toDouble())
        } else {
            lastMapFocus = GeoPoint(51.50550, -0.07520)
        }

        val scaleBarOverlay = ScaleBarOverlay(binding?.map)
        scaleBarOverlay.setScaleBarOffset(15, 25)
        val barPaint = Paint().apply {
            setARGB(200, 255, 250, 250)
        }
        scaleBarOverlay.setBackgroundPaint(barPaint)
        scaleBarOverlay.enableScaleBar()
        binding?.map?.overlays?.add(scaleBarOverlay)
        binding?.map?.zoomController
            ?.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        binding?.map?.controller?.setZoom(ZOOM_LEVEL)

        // if we came from Explore map using 'Show in Nearby', load Explore map camera position
        if (isCameFromExploreMap()) {
            moveCameraToPosition(
                GeoPoint(prevLatitude, prevLongitude),
                prevZoom,
                1L
            )
        }
        binding?.map?.overlays?.add(mapEventsOverlay)

        binding?.map?.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                presenter?.handleMapScrolled(scope, !isNetworkErrorOccurred)
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                return false
            }
        })

        binding?.map?.setMultiTouchControls(true)
        nearbyParentFragmentInstanceReadyCallback?.onReady()
        initNearbyFilter()
        addCheckBoxCallback()
        if (!isCameFromExploreMap()) {
            moveCameraToPosition(lastMapFocus!!)
        }
        moveCameraToPosition(lastMapFocus!!)
        initRvNearbyList()
        onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding?.tvAttribution?.text = Html.fromHtml(
                getString(R.string.map_attribution),
                Html.FROM_HTML_MODE_LEGACY
            )
        } else {
            @Suppress("DEPRECATION")
            binding?.tvAttribution?.text =
                Html.fromHtml(getString(R.string.map_attribution))
        }
        binding?.tvAttribution?.movementMethod = LinkMovementMethod.getInstance()

        binding?.nearbyFilterList?.btnAdvancedOptions?.setOnClickListener {
            binding?.nearbyFilter?.searchViewLayout?.searchView?.clearFocus()
            showHideAdvancedQueryFragment(true)
            val fragment = AdvanceQueryFragment()
            val bundle = Bundle()
            try {
                bundle.putString(
                    "query",
                    FileUtils.readFromResource("/queries/radius_query_for_upload_wizard.rq")
                )
            } catch (e: IOException) {
                Timber.e(e)
            }
            fragment.arguments = bundle
            fragment.callback = object : AdvanceQueryFragment.Callback {
                override fun close() {
                    showHideAdvancedQueryFragment(false)
                }

                override fun reset() {
                    presenter?.setAdvancedQuery(null.toString())
                    presenter?.updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
                    showHideAdvancedQueryFragment(false)
                }

                override fun apply(query: String) {
                    presenter?.setAdvancedQuery(query)
                    presenter?.updateMapAndList(LocationChangeType.CUSTOM_QUERY)
                    showHideAdvancedQueryFragment(false)
                }
            }
            childFragmentManager.beginTransaction()
                .replace(R.id.fl_container_nearby_children, fragment)
                .commit()
        }

        binding?.tvLearnMore?.setOnClickListener {
            onLearnMoreClicked()
        }

        if (!locationPermissionsHelper?.checkLocationPermission(requireActivity())!!) {
            askForLocationPermission()
        }
    }

    /**
     * Fetch Explore map camera data from fragment arguments if any.
     */
    private fun loadExploreMapData() {
        // get fragment arguments
        if (arguments != null) {
            prevZoom = requireArguments().getDouble("prev_zoom")
            prevLatitude = requireArguments().getDouble("prev_latitude")
            prevLongitude = requireArguments().getDouble("prev_longitude")
        }
    }

    /**
     * Checks if fragment arguments contain data from Explore map. if present, then the user
     * navigated from Explore using 'Show in Nearby'.
     *
     * @return true if user navigated from Explore map
     */
    private fun isCameFromExploreMap(): Boolean {
        return prevZoom != 0.0 || prevLatitude != 0.0 || prevLongitude != 0.0
    }

    /**
     * Initialise background based on theme, this should be doe ideally via styles, that would need
     * another refactor
     */
    private fun initThemePreferences() {
        if (_isDarkTheme) {
            binding!!.bottomSheetNearby.rvNearbyList.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.contributionListDarkBackground)
            )
            binding!!.nearbyFilterList.checkboxTriStates.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            binding!!.nearbyFilterList.checkboxTriStates.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            binding!!.nearbyFilterList.root.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.contributionListDarkBackground)
            )
            binding!!.map.overlayManager.tilesOverlay
                .setColorFilter(TilesOverlay.INVERT_COLORS)
        } else {
            binding!!.bottomSheetNearby.rvNearbyList.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            binding!!.nearbyFilterList.checkboxTriStates.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.contributionListDarkBackground)
            )
            binding!!.nearbyFilterList.root.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            binding!!.nearbyFilterList.root.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
        }
    }

    private fun initRvNearbyList() {
        binding!!.bottomSheetNearby.rvNearbyList.layoutManager = LinearLayoutManager(context)
        adapter = PlaceAdapter(
            bookmarkLocationsDao = bookmarkLocationDao,
            scope = scope,
            onPlaceClicked = { place: Place ->
                moveCameraToPosition(
                    GeoPoint(
                        place.location.latitude,
                        place.location.longitude
                    )
                )
            },
            onBookmarkClicked = { place: Place?, _: Boolean? ->
                presenter!!.toggleBookmarkedStatus(place, scope)
            },
            commonPlaceClickActions = commonPlaceClickActions,
            inAppCameraLocationPermissionLauncher = inAppCameraLocationPermissionLauncher,
            galleryPickLauncherForResult = galleryPickLauncherForResult,
            cameraPickLauncherForResult = cameraPickLauncherForResult
        )
        binding!!.bottomSheetNearby.rvNearbyList.adapter = adapter
    }

    private fun addCheckBoxCallback() {
        binding!!.nearbyFilterList.checkboxTriStates.setCallback { o, state, b, b1 ->
            presenter!!.filterByMarkerType(
                o,
                state,
                b,
                b1
            )
        }
    }

    private fun performMapReadyActions() {
        if ((activity as MainActivity).activeFragment == ActiveFragment.NEARBY) {
            if (applicationKvStore.getBoolean("doNotAskForLocationPermission", false) &&
                !locationPermissionsHelper!!.checkLocationPermission(requireActivity())
            ) {
                isPermissionDenied = true
            }
        }
        presenter!!.onMapReady()
    }

    override fun askForLocationPermission() {
        Timber.d("Asking for location permission")
        locationPermissionLauncher.launch(permission.ACCESS_FINE_LOCATION)
    }

    private fun locationPermissionGranted() {
        isPermissionDenied = false
        applicationKvStore.putBoolean("doNotAskForLocationPermission", false)
        lastKnownLocation = locationManager.getLastLocation()
        val target = lastKnownLocation

        if (target != null) {
            val targetP = GeoPoint(target.latitude, target.longitude)
            mapCenter = targetP
            binding?.map?.controller?.setCenter(targetP)
            recenterMarkerToPosition(targetP)
            if (!isCameFromExploreMap()) {
                moveCameraToPosition(targetP)
            }
        } else if (locationManager.isGPSProviderEnabled() || locationManager.isNetworkProviderEnabled()) {
            locationManager.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER)
            locationManager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER)
            setProgressBarVisibility(true)
        } else {
            activity?.let {
                locationPermissionsHelper
                ?.showLocationOffDialog(it, R.string.ask_to_turn_location_on_text)
            }
        }

        presenter?.onMapReady()
        registerUnregisterLocationListener(false)
    }

    override fun onResume() {
        super.onResume()
        binding?.map?.onResume()
        presenter?.attachView(this)
        registerNetworkReceiver()

        binding?.coordinatorLayout?.visibility = View.VISIBLE
        binding?.map?.setMultiTouchControls(true)
        binding?.map?.isClickable = true

        if (isResumed && (activity as? MainActivity)?.activeFragment == ActiveFragment.NEARBY) {
            if (activity?.let { locationPermissionsHelper?.checkLocationPermission(it) } == true) {
                locationPermissionGranted()
            } else {
                startMapWithoutPermission()
            }
        }
        drawableCache = HashMap()
    }

    /**
     * Starts the map without GPS and without permission By default it points to 51.50550,-0.07520
     * coordinates, other than that it points to the last known location which can be get by the key
     * "LastLocation" from applicationKvStore
     */
    private fun startMapWithoutPermission() {
        if (applicationKvStore.getString("LastLocation") != null) {
            val locationLatLng =
                applicationKvStore.getString("LastLocation")!!.split(",".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()
            lastKnownLocation = LatLng(
                locationLatLng[0].toDouble(),
                locationLatLng[1].toDouble(), 1f
            )
        } else {
            lastKnownLocation = defaultLatLng
        }
        if (!isCameFromExploreMap()) {
            moveCameraToPosition(
                GeoPoint(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
            )
        }
        presenter!!.onMapReady()
    }

    private fun registerNetworkReceiver() {
        if (activity != null) {
            requireActivity().registerReceiver(broadcastReceiver, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        binding!!.map.onPause()
        compositeDisposable.clear()
        presenter!!.detachView()
        registerUnregisterLocationListener(true)
        try {
            if (broadcastReceiver != null && activity != null) {
                requireContext().unregisterReceiver(broadcastReceiver)
            }

            if (presenter != null) {
                locationManager.removeLocationListener(presenter!!)
                locationManager.unregisterLocationManager()
            }
        } catch (e: Exception) {
            Timber.e(e)
            //Broadcast receivers should always be unregistered inside catch,
            // you never know if they were already registered or not
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let {
            searchHandler.removeCallbacks(it)
        } ?: run {
            Timber.w("NearbyParentFragment: searchRunnable is null")
        }

        if (presenter == null) Timber.w("NearbyParentFragment: presenter is null")

        presenter?.removeNearbyPreferences(applicationKvStore)
    }

    private fun initViews() {
        Timber.d("init views called")
        initBottomSheets()
        loadAnimations()
        setBottomSheetCallbacks()
        addActionToTitle()
        if (!Utils.isMonumentsEnabled(Date())) {
            NearbyFilterState.setWlmSelected(false)
        }
    }

    /**
     * a) Creates bottom sheet behaviours from bottom sheets, sets initial states and visibility b)
     * Gets the touch event on the map to perform following actions: if fab is open then close fab.
     * if bottom sheet details are expanded then collapse bottom sheet details. if bottom sheet
     * details are collapsed then hide the bottom sheet details. if listBottomSheet is open then
     * hide the list bottom sheet.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initBottomSheets() {
        bottomSheetListBehavior =
            BottomSheetBehavior.from<View>(binding!!.bottomSheetNearby.bottomSheet)
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(binding!!.bottomSheetDetails.root)
        bottomSheetDetailsBehavior?.setState(BottomSheetBehavior.STATE_HIDDEN)
        binding!!.bottomSheetDetails.root.visibility = View.VISIBLE
        bottomSheetListBehavior?.setState(BottomSheetBehavior.STATE_HIDDEN)
    }

    private val spanCount: Int
        /**
         * Determines the number of spans (columns) in the RecyclerView based on device orientation
         * and adapter item count.
         *
         * @return The number of spans to be used in the RecyclerView.
         */
        get() {
            val orientation = resources.configuration.orientation
            return if (bottomSheetAdapter != null) {
                if ((orientation == Configuration.ORIENTATION_PORTRAIT))
                    3
                else
                    bottomSheetAdapter!!.itemCount
            } else {
                if ((orientation == Configuration.ORIENTATION_PORTRAIT)) 3 else 6
            }
        }

    private fun initNearbyFilter() {
        binding!!.nearbyFilterList.root.visibility = View.GONE
        hideBottomSheet()
        binding!!.nearbyFilter.searchViewLayout.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            setLayoutHeightAlignedToWidth(
                1.25,
                binding!!.nearbyFilterList.root
            )
            if (hasFocus) {
                binding!!.nearbyFilterList.root.visibility = View.VISIBLE
                presenter!!.searchViewGainedFocus()
            } else {
                binding!!.nearbyFilterList.root.visibility = View.GONE
            }
        }
        binding!!.nearbyFilterList.searchListView.setHasFixedSize(true)
        binding!!.nearbyFilterList.searchListView.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        binding!!.nearbyFilterList.searchListView.layoutManager = linearLayoutManager
        nearbyFilterSearchRecyclerViewAdapter = NearbyFilterSearchRecyclerViewAdapter(
            context, ArrayList(Label.valuesAsList()),
            binding!!.nearbyFilterList.searchListView
        )
        nearbyFilterSearchRecyclerViewAdapter!!.setCallback(
            object : NearbyFilterSearchRecyclerViewAdapter.Callback {
                override fun setCheckboxUnknown() {
                    presenter!!.setCheckboxUnknown()
                }

                override fun filterByMarkerType(
                    selectedLabels: ArrayList<Label>, i: Int,
                    b: Boolean, b1: Boolean
                ) {
                    presenter!!.filterByMarkerType(selectedLabels, i, b, b1)
                }

                override fun isDarkTheme(): Boolean {
                    return _isDarkTheme
                }
            })
        binding!!.nearbyFilterList.root
            .layoutParams.width = getScreenWidth(
            requireActivity(),
            0.75
        ).toInt()
        binding!!.nearbyFilterList.searchListView.adapter = nearbyFilterSearchRecyclerViewAdapter
        setLayoutHeightAlignedToWidth(1.25, binding!!.nearbyFilterList.root)
        compositeDisposable.add(
            binding!!.nearbyFilter.searchViewLayout.searchView.queryTextChanges()
                .takeUntil(RxView.detaches(binding!!.nearbyFilter.searchViewLayout.searchView))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { query: CharSequence ->
                    (binding!!.nearbyFilterList.searchListView.adapter as NearbyFilterSearchRecyclerViewAdapter).filter
                        .filter(query.toString())
                })
    }

    override fun setCheckBoxAction() {
        binding!!.nearbyFilterList.checkboxTriStates.addAction()
        binding!!.nearbyFilterList.checkboxTriStates.state = CheckBoxTriStates.UNKNOWN
    }

    override fun setCheckBoxState(state: Int) {
        binding!!.nearbyFilterList.checkboxTriStates.state = state
    }

    override fun setFilterState() {
        if (NearbyController.currentLocation != null) {
            presenter!!.filterByMarkerType(
                nearbyFilterSearchRecyclerViewAdapter!!.selectedLabels,
                binding!!.nearbyFilterList.checkboxTriStates.state,
                filterForPlaceState = true,
                filterForAllNoneType = false
            )
        }
    }

    /**
     * Defines how bottom sheets will act on click
     */
    private fun setBottomSheetCallbacks() {
        bottomSheetDetailsBehavior!!.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                prepareViewsForSheetPosition(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })

        binding!!.bottomSheetDetails.root.setOnClickListener { _ ->
            if (bottomSheetDetailsBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED)
            } else if (bottomSheetDetailsBehavior!!.state
                == BottomSheetBehavior.STATE_EXPANDED
            ) {
                bottomSheetDetailsBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding!!.bottomSheetNearby.bottomSheet.layoutParams.height =
            getWindowHeight(requireActivity()) / 16 * 9
        bottomSheetListBehavior =
            BottomSheetBehavior.from<View>(binding!!.bottomSheetNearby.bottomSheet)
        bottomSheetListBehavior?.setState(BottomSheetBehavior.STATE_COLLAPSED)
        bottomSheetListBehavior?.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetDetailsBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
    }

    /**
     * Loads animations will be used for FABs
     */
    private fun loadAnimations() {
        fabOpen = AnimationUtils.loadAnimation(activity, R.anim.fab_open)
        fabClose = AnimationUtils.loadAnimation(activity, R.anim.fab_close)
        rotateForward =
            AnimationUtils.loadAnimation(activity, R.anim.rotate_forward)
        rotateBackward =
            AnimationUtils.loadAnimation(activity, R.anim.rotate_backward)
    }

    /**
     *
     */
    private fun addActionToTitle() {
        binding!!.bottomSheetDetails.title.setOnLongClickListener { _ ->
            Utils.copy(
                "place", binding!!.bottomSheetDetails.title.text.toString(),
                context
            )
            Toast.makeText(context, R.string.text_copy, Toast.LENGTH_SHORT)
                .show()
            true
        }

        binding!!.bottomSheetDetails.title.setOnClickListener { _ ->
            bottomSheetListBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
            if (bottomSheetDetailsBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED)
            } else {
                bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        }
    }

    /**
     * Centers the map in nearby fragment to a given place and updates nearestPlace
     *
     * @param place is new center of the map
     */
    override fun centerMapToPlace(place: Place?) {
        Timber.d("Map is centered to place")
        val cameraShift: Double
        if (null != place) {
            lastPlaceToCenter = place
            nearestPlace = place
        }

        if (null != lastPlaceToCenter) {
            val configuration = requireActivity().resources.configuration
            cameraShift = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT
            } else {
                CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE
            }
            recenterMap(
                LatLng(
                    lastPlaceToCenter!!.location.latitude - cameraShift,
                    lastPlaceToCenter!!.getLocation().longitude, 0f
                )
            )
        }
        highlightNearestPlace(place!!)
    }


    override fun updateListFragment(placeList: List<Place>) {
        adapter!!.clear()
        adapter!!.items = placeList
        binding!!.bottomSheetNearby.noResultsMessage.visibility =
            if (placeList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun getLastLocation(): LatLng {
        return lastKnownLocation!!
    }

    override fun getLastMapFocus(): LatLng {
        val latLng = LatLng(
            lastMapFocus!!.latitude, lastMapFocus!!.longitude, 100f
        )
        return latLng
    }

    /**
     * Computes location where map should be centered
     *
     * @return returns the last location, if available, else returns default location
     */
    override fun getMapCenter(): LatLng {
        if (applicationKvStore.getString("LastLocation") != null) {
            val locationLatLng =
                applicationKvStore.getString("LastLocation")!!.split(",".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()
            lastKnownLocation = LatLng(
                locationLatLng[0].toDouble(),
                locationLatLng[1].toDouble(), 1f
            )
        } else {
            lastKnownLocation = LatLng(
                51.50550,
                -0.07520, 1f
            )
        }
        var latLng = lastKnownLocation!!
        if (mapCenter != null) {
            latLng = LatLng(
                mapCenter!!.latitude, mapCenter!!.longitude, 100f
            )
        }
        return latLng
    }

    override fun getMapFocus(): LatLng {
        val mapFocusedLatLng = LatLng(
            binding!!.map.mapCenter.latitude, binding!!.map.mapCenter.longitude,
            100f
        )
        return mapFocusedLatLng
    }

    override fun isAdvancedQueryFragmentVisible(): Boolean {
        return isAdvancedQueryFragmentVisible
    }

    override fun showHideAdvancedQueryFragment(shouldShow: Boolean) {
        setHasOptionsMenu(!shouldShow)
        binding!!.flContainerNearbyChildren.visibility = if (shouldShow) View.VISIBLE else View.GONE
        isAdvancedQueryFragmentVisible = shouldShow
    }

    override fun isNetworkConnectionEstablished(): Boolean {
        return isInternetConnectionEstablished(activity)
    }

    /**
     * Adds network broadcast receiver to recognize connection established
     */
    private fun initNetworkBroadCastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (activity != null) {
                    if (isInternetConnectionEstablished(activity)) {
                        if (isNetworkErrorOccurred) {
                            presenter!!.updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
                            isNetworkErrorOccurred = false
                        }

                        if (snackbar != null) {
                            snackbar!!.dismiss()
                            snackbar = null
                        }
                    } else {
                        if (snackbar == null) {
                            snackbar = Snackbar.make(
                                view!!, R.string.no_internet,
                                Snackbar.LENGTH_INDEFINITE
                            )
                            searchable = false
                            setProgressBarVisibility(false)
                        }

                        isNetworkErrorOccurred = true
                        snackbar!!.show()
                    }
                }
            }
        }
    }

    /**
     * Updates the internet unavailable snackbar to reflect whether cached pins are shown.
     *
     * @param offlinePinsShown Whether there are pins currently being shown on map.
     */
    override fun updateSnackbar(offlinePinsShown: Boolean) {
        if (!isNetworkErrorOccurred || snackbar == null) {
            return
        }
        if (offlinePinsShown) {
            snackbar!!.setText(R.string.nearby_showing_pins_offline)
        } else {
            snackbar!!.setText(R.string.no_internet)
        }
    }

    /**
     * Hide or expand bottom sheet according to states of all sheets
     */
    override fun listOptionMenuItemClicked() {
        bottomSheetDetailsBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
        if (bottomSheetListBehavior!!.state == BottomSheetBehavior.STATE_COLLAPSED
            || bottomSheetListBehavior!!.state == BottomSheetBehavior.STATE_HIDDEN
        ) {
            bottomSheetListBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED)
        } else if (bottomSheetListBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetListBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    /**
     * Returns the location of the top right corner of the map view.
     *
     * @return a `LatLng` object denoting the location of the top right corner of the map.
     */
    override fun getScreenTopRight(): LatLng {
        val screenTopRight = binding!!.map.projection
            .fromPixels(binding!!.map.width, 0)
        return LatLng(
            screenTopRight.latitude, screenTopRight.longitude, 0f
        )
    }

    /**
     * Returns the location of the bottom left corner of the map view.
     *
     * @return a `LatLng` object denoting the location of the bottom left corner of the map.
     */
    override fun getScreenBottomLeft(): LatLng {
        val screenBottomLeft = binding!!.map.projection
            .fromPixels(0, binding!!.map.height)
        return LatLng(
            screenBottomLeft.latitude, screenBottomLeft.longitude, 0f
        )
    }

    override fun populatePlaces(currentLatLng: LatLng) {
        // these two variables have historically been assigned values the opposite of what their
        // names imply, and quite some existing code depends on this fact
        var screenTopRightLatLng = screenBottomLeft
        var screenBottomLeftLatLng = screenTopRight

        // When the nearby fragment is opened immediately upon app launch, the {screenTopRightLatLng}
        // and {screenBottomLeftLatLng} variables return {LatLng(0.0,0.0)} as output.
        // To address this issue, A small delta value {delta = 0.02} is used to adjust the latitude
        // and longitude values for {ZOOM_LEVEL = 15f}.
        // This adjustment helps in calculating the east and west corner LatLng accurately.
        // Note: This only happens when the nearby fragment is opened immediately upon app launch,
        // otherwise {screenTopRightLatLng} and {screenBottomLeftLatLng} are used to determine
        // the east and west corner LatLng.
        if (screenTopRightLatLng.latitude == 0.0 && screenTopRightLatLng.longitude == 0.0 &&
            screenBottomLeftLatLng.latitude == 0.0 && screenBottomLeftLatLng.longitude == 0.0
        ) {
            val delta = 0.009
            val westCornerLat = currentLatLng.latitude - delta
            val westCornerLong = currentLatLng.longitude - delta
            val eastCornerLat = currentLatLng.latitude + delta
            val eastCornerLong = currentLatLng.longitude + delta
            screenTopRightLatLng = LatLng(
                westCornerLat,
                westCornerLong, 0f
            )
            screenBottomLeftLatLng = LatLng(
                eastCornerLat,
                eastCornerLong, 0f
            )
            if (currentLatLng == getLastMapFocus()) {
                // Means we are checking around current location
                populatePlacesForCurrentLocation(
                    mapFocus, screenTopRightLatLng,
                    screenBottomLeftLatLng, currentLatLng, null
                )
            } else {
                populatePlacesForAnotherLocation(
                    mapFocus, screenTopRightLatLng,
                    screenBottomLeftLatLng, currentLatLng, null
                )
            }
        } else {
            if (currentLatLng == getLastMapFocus()) {
                // Means we are checking around current location
                populatePlacesForCurrentLocation(
                    mapFocus, screenTopRightLatLng,
                    screenBottomLeftLatLng, currentLatLng, null
                )
            } else {
                populatePlacesForAnotherLocation(
                    mapFocus, screenTopRightLatLng,
                    screenBottomLeftLatLng, currentLatLng, null
                )
            }
        }

        if (recenterToUserLocation) {
            recenterToUserLocation = false
        }
    }

    override fun populatePlaces(
        currentLatLng: LatLng,
        customQuery: String?
    ) {
        if (customQuery.isNullOrEmpty()) {
            populatePlaces(currentLatLng)
            return
        }
        // these two variables have historically been assigned values the opposite of what their
        // names imply, and quite some existing code depends on this fact
        val screenTopRightLatLng = screenBottomLeft
        val screenBottomLeftLatLng = screenTopRight

        if (currentLatLng == lastFocusLocation || lastFocusLocation == null || recenterToUserLocation) {
            // Means we are checking around current location
            populatePlacesForCurrentLocation(
                lastKnownLocation, screenTopRightLatLng,
                screenBottomLeftLatLng, currentLatLng, customQuery
            )
        } else {
            populatePlacesForAnotherLocation(
                lastKnownLocation, screenTopRightLatLng,
                screenBottomLeftLatLng, currentLatLng, customQuery
            )
        }
        if (recenterToUserLocation) {
            recenterToUserLocation = false
        }
    }

    /**
     * Clears the Nearby local cache and then calls for pin details to be fetched afresh.
     *
     */
    private fun emptyCache() {
        // reload the map once the cache is cleared
        compositeDisposable.add(
            placesRepository.clearCache()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .andThen(Completable.fromAction {
                    // reload only the pin details, by making all loaded pins gray:
                    val newPlaceGroups = ArrayList<MarkerPlaceGroup>(
                        NearbyController.markerLabelList.size
                    )
                    for (placeGroup in NearbyController.markerLabelList) {
                        val place = Place(
                            "", "", placeGroup.place.label, "",
                            placeGroup.place.getLocation(), "",
                            placeGroup.place.siteLinks, "", placeGroup.place.exists,
                            placeGroup.place.entityID
                        )
                        place.setDistance(placeGroup.place.distance)
                        place.isMonument = placeGroup.place.isMonument
                        newPlaceGroups.add(
                            MarkerPlaceGroup(placeGroup.isBookmarked, place)
                        )
                    }
                    presenter!!.loadPlacesDataAsync(newPlaceGroups, scope)
                })
                .subscribe(
                    {
                        Timber.d("Nearby Cache cleared successfully.")
                    },
                    { throwable: Throwable? ->
                        Timber.e(throwable, "Failed to clear the Nearby Cache")
                    }
                )
        )
    }

    private fun savePlacesAsKML() {
        val savePlacesObservable = Observable
            .fromCallable {
                nearbyController.getPlacesAsKML(mapFocus)
            }
        compositeDisposable.add(
            savePlacesObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { kmlString: String? ->
                        if (kmlString != null) {
                            val timeStamp = SimpleDateFormat(
                                "yyyyMMdd_HHmmss",
                                Locale.getDefault()
                            ).format(Date())
                            val fileName =
                                "KML_" + timeStamp + "_" + System.currentTimeMillis() + ".kml"
                            val saved = saveFile(kmlString, fileName)
                            progressDialog!!.hide()
                            if (saved) {
                                showOpenFileDialog(requireContext(), fileName, false)
                            } else {
                                Toast.makeText(
                                    this.context,
                                    getString(R.string.failed_to_save_kml_file),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    { throwable: Throwable ->
                        Timber.d(throwable)
                        showErrorMessage(
                            getString(R.string.error_fetching_nearby_places)
                                + throwable.localizedMessage
                        )
                        setProgressBarVisibility(false)
                        presenter!!.lockUnlockNearby(false)
                        setFilterState()
                    })
        )
    }

    private fun savePlacesAsGPX() {
        val savePlacesObservable = Observable
            .fromCallable {
                nearbyController.getPlacesAsGPX(mapFocus)
            }
        compositeDisposable.add(
            savePlacesObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { gpxString: String? ->
                        if (gpxString != null) {
                            val timeStamp = SimpleDateFormat(
                                "yyyyMMdd_HHmmss",
                                Locale.getDefault()
                            ).format(Date())
                            val fileName =
                                "GPX_" + timeStamp + "_" + System.currentTimeMillis() + ".gpx"
                            val saved = saveFile(gpxString, fileName)
                            progressDialog!!.hide()
                            if (saved) {
                                showOpenFileDialog(requireContext(), fileName, true)
                            } else {
                                Toast.makeText(
                                    this.context,
                                    getString(R.string.failed_to_save_gpx_file),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    { throwable: Throwable ->
                        Timber.d(throwable)
                        showErrorMessage(
                            getString(R.string.error_fetching_nearby_places)
                                + throwable.localizedMessage
                        )
                        setProgressBarVisibility(false)
                        presenter!!.lockUnlockNearby(false)
                        setFilterState()
                    })
        )
    }

    private fun saveFile(string: String, fileName: String?): Boolean {
        if (!isExternalStorageWritable) {
            return false
        }

        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val kmlFile = File(downloadsDir, fileName)

        try {
            val fos = FileOutputStream(kmlFile)
            fos.write(string.toByteArray())
            fos.close()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }


    private fun showOpenFileDialog(context: Context, fileName: String, isGPX: Boolean) {
        val title = getString(R.string.file_saved_successfully)
        val message =
            if ((isGPX))
                getString(R.string.do_you_want_to_open_gpx_file)
            else
                getString(R.string.do_you_want_to_open_kml_file)
        val runnable = Runnable { openFile(context, fileName, isGPX) }
        showAlertDialog(requireActivity(), title, message, runnable) {}
    }

    private fun openFile(context: Context, fileName: String, isGPX: Boolean) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        val uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider", file
        )
        val intent = Intent(Intent.ACTION_VIEW)

        if (isGPX) {
            intent.setDataAndType(uri, "application/gpx")
        } else {
            intent.setDataAndType(uri, "application/kml")
        }

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context, R.string.no_application_available_to_open_gpx_files,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Fetches and updates the data for a specific place, then updates the corresponding marker on the map.
     *
     * @param entity       The entity ID of the place.
     * @param place        The Place object containing the initial place data.
     * @param marker       The Marker object on the map representing the place.
     * @param isBookMarked A boolean indicating if the place is bookmarked.
     */
    private fun getPlaceData(entity: String?, place: Place, marker: Marker, isBookMarked: Boolean) {
        val getPlaceObservable = Observable
            .fromCallable {
                nearbyController.getPlaces(listOf(place))
            }
        compositeDisposable.add(
            getPlaceObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { placeList: List<Place> ->
                        val updatedPlace = placeList[0]
                        updatedPlace.distance = place.distance
                        updatedPlace.location = place.location
                        marker.title = updatedPlace.name
                        marker.snippet = if (containsParentheses(updatedPlace.longDescription))
                            getTextBetweenParentheses(
                                updatedPlace.longDescription
                            )
                        else
                            updatedPlace.longDescription
                        marker.showInfoWindow()
                        presenter!!.handlePinClicked(updatedPlace)
                        savePlaceToDatabase(place)
                        val icon = getDrawable(
                            requireContext(),
                            getIconFor(updatedPlace, isBookMarked)
                        )
                        marker.icon = icon
                        binding!!.map.invalidate()
                        binding!!.bottomSheetDetails.dataCircularProgress.visibility = View.GONE
                        binding!!.bottomSheetDetails.icon.visibility = View.VISIBLE
                        binding!!.bottomSheetDetails.wikiDataLl.visibility = View.VISIBLE
                        passInfoToSheet(updatedPlace)
                        hideBottomSheet()
                    },
                    { throwable: Throwable ->
                        Timber.d(throwable)
                        showErrorMessage(
                            getString(R.string.could_not_load_place_data)
                                + throwable.localizedMessage
                        )
                    })
        )
    }

    private fun populatePlacesForCurrentLocation(
        currentLatLng: LatLng?,
        screenTopRight: LatLng,
        screenBottomLeft: LatLng,
        searchLatLng: LatLng,
        customQuery: String?
    ) {
        val nearbyPlacesInfoObservable = Observable
            .fromCallable {
                nearbyController.loadAttractionsFromLocation(
                    currentLatLng,
                    screenTopRight,
                    screenBottomLeft,
                    searchLatLng,
                    false,
                    true,
                    Utils.isMonumentsEnabled(Date()),
                    customQuery
                )
            }

        compositeDisposable.add(
            nearbyPlacesInfoObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { nearbyPlacesInfo: NearbyPlacesInfo ->
                        if (nearbyPlacesInfo.placeList == null || nearbyPlacesInfo.placeList.isEmpty()) {
                            showErrorMessage(getString(R.string.no_nearby_places_around))
                            setProgressBarVisibility(false)
                            presenter!!.lockUnlockNearby(false)
                        } else {
                            updateMapMarkers(nearbyPlacesInfo.placeList, searchLatLng, true)
                            lastFocusLocation = searchLatLng
                            lastMapFocus = GeoPoint(
                                searchLatLng.latitude,
                                searchLatLng.longitude
                            )
                        }
                    }
                ) { throwable: Throwable ->
                    Timber.d(throwable)
                    showErrorMessage(
                        getString(R.string.error_fetching_nearby_places)
                                + throwable.localizedMessage
                    )
                    setProgressBarVisibility(false)
                    presenter!!.lockUnlockNearby(false)
                    setFilterState()
                }
        )
    }

    private fun populatePlacesForAnotherLocation(
        currentLatLng: LatLng?,
        screenTopRight: LatLng,
        screenBottomLeft: LatLng,
        searchLatLng: LatLng,
        customQuery: String?
    ) {
        val nearbyPlacesInfoObservable = Observable
            .fromCallable {
                nearbyController.loadAttractionsFromLocation(
                    currentLatLng,
                    screenTopRight,
                    screenBottomLeft,
                    searchLatLng,
                    false,
                    true,
                    Utils.isMonumentsEnabled(Date()),
                    customQuery
                )
            }

        compositeDisposable.add(
            nearbyPlacesInfoObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { nearbyPlacesInfo: NearbyPlacesInfo ->
                        if (nearbyPlacesInfo.placeList == null || nearbyPlacesInfo.placeList.isEmpty()) {
                            showErrorMessage(getString(R.string.no_nearby_places_around))
                            setProgressBarVisibility(false)
                            presenter!!.lockUnlockNearby(false)
                        } else {
                            // Updating last searched location
                            applicationKvStore.putString(
                                "LastLocation",
                                searchLatLng.latitude.toString() + "," + searchLatLng.longitude
                            )

                            // curLatLng is used to calculate distance from the current location to the place
                            // and distance is later on populated to the place
                            updateMapMarkers(nearbyPlacesInfo.placeList, searchLatLng, false)
                            lastMapFocus = GeoPoint(
                                searchLatLng.latitude,
                                searchLatLng.longitude
                            )
                            stopQuery()
                        }
                    }
                ) { throwable: Throwable ->
                    Timber.e(throwable)
                    showErrorMessage(
                        getString(R.string.error_fetching_nearby_places)
                                + throwable.localizedMessage
                    )
                    setProgressBarVisibility(false)
                    presenter!!.lockUnlockNearby(false)
                    setFilterState()
                }
        )
    }

    private fun savePlaceToDatabase(place: Place?) {
        placesRepository.save(place)?.subscribeOn(Schedulers.io())?.subscribe()?.let {
            compositeDisposable.add(
                it
            )
        }
    }

    /**
     * Stops any ongoing queries and clears all disposables.
     * This method sets the stopQuery flag to true and clears the compositeDisposable
     * to prevent any further processing.
     */
    override fun stopQuery() {
        stopQuery = true
        compositeDisposable.clear()
    }

    /**
     * Populates places for your location, should be used for finding nearby places around a
     * location where you are.
     *
     * @param nearbyPlaces This variable has place list information and distances.
     */
    private fun updateMapMarkers(
        nearbyPlaces: List<Place>, curLatLng: LatLng,
        shouldUpdateSelectedMarker: Boolean
    ) {
        presenter!!.updateMapMarkers(nearbyPlaces, curLatLng, scope)
    }


    override fun isListBottomSheetExpanded(): Boolean {
        return bottomSheetListBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED
    }

    override fun isDetailsBottomSheetVisible(): Boolean {
        return bottomSheetDetailsBehavior!!.state != BottomSheetBehavior.STATE_HIDDEN
    }

    override fun setBottomSheetDetailsSmaller() {
        if (bottomSheetDetailsBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
        } else {
            bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
        }
    }

    override fun setRecyclerViewAdapterAllSelected() {
        if (nearbyFilterSearchRecyclerViewAdapter != null
            && NearbyController.currentLocation != null
        ) {
            nearbyFilterSearchRecyclerViewAdapter!!.setRecyclerViewAdapterAllSelected()
        }
    }

    override fun setRecyclerViewAdapterItemsGreyedOut() {
        if (nearbyFilterSearchRecyclerViewAdapter != null
            && NearbyController.currentLocation != null
        ) {
            nearbyFilterSearchRecyclerViewAdapter!!.setRecyclerViewAdapterItemsGreyedOut()
        }
    }

    override fun setProgressBarVisibility(isVisible: Boolean) {
        if (isVisible) {
            binding!!.mapProgressBar.visibility = View.VISIBLE
        } else {
            binding!!.mapProgressBar.visibility = View.GONE
        }
    }

    fun setTabItemContributions() {
        (activity as MainActivity).binding?.pager?.currentItem
        // TODO
    }

    /**
     * Starts animation of fab plus (turning on opening) and other FABs
     */
    override fun animateFABs() {
        if (binding!!.fabPlus.isShown) {
            if (isFABsExpanded) {
                collapseFABs(true)
            } else {
                expandFABs(false)
            }
        }
    }

    private fun showFABs() {
        addAnchorToBigFABs(
            binding!!.fabPlus,
            binding!!.bottomSheetDetails.root.id
        )
        binding!!.fabPlus.show()
        addAnchorToSmallFABs(
            binding!!.fabGallery,
            requireView().findViewById<View>(R.id.empty_view).id
        )
        addAnchorToSmallFABs(
            binding!!.fabCamera,
            requireView().findViewById<View>(R.id.empty_view1).id
        )
        addAnchorToSmallFABs(
            binding!!.fabCustomGallery,
            requireView().findViewById<View>(R.id.empty_view2).id
        )
    }

    /**
     * Expands camera and gallery FABs, turn forward plus FAB
     *
     * @param isFABsExpanded true if they are already expanded
     */
    private fun expandFABs(isFABsExpanded: Boolean) {
        if (!isFABsExpanded) {
            showFABs()
            binding!!.fabPlus.startAnimation(rotateForward)
            binding!!.fabCamera.startAnimation(fabOpen)
            binding!!.fabGallery.startAnimation(fabOpen)
            binding!!.fabCustomGallery.startAnimation(fabOpen)
            binding!!.fabCustomGallery.show()
            binding!!.fabCamera.show()
            binding!!.fabGallery.show()
            this.isFABsExpanded = true
        }
    }

    /**
     * Hides all FABs
     */
    private fun hideFABs() {
        removeAnchorFromFAB(binding!!.fabPlus)
        binding!!.fabPlus.hide()
        removeAnchorFromFAB(binding!!.fabCamera)
        binding!!.fabCamera.hide()
        removeAnchorFromFAB(binding!!.fabGallery)
        binding!!.fabGallery.hide()
        removeAnchorFromFAB(binding!!.fabCustomGallery)
        binding!!.fabCustomGallery.hide()
    }

    /**
     * Collapses camera and gallery FABs, turn back plus FAB
     *
     * @param isFABsExpanded
     */
    private fun collapseFABs(isFABsExpanded: Boolean) {
        if (isFABsExpanded) {
            binding!!.fabPlus.startAnimation(rotateBackward)
            binding!!.fabCamera.startAnimation(fabClose)
            binding!!.fabGallery.startAnimation(fabClose)
            binding!!.fabCustomGallery.startAnimation(fabClose)
            binding!!.fabCustomGallery.hide()
            binding!!.fabCamera.hide()
            binding!!.fabGallery.hide()
            this.isFABsExpanded = false
        }
    }

    override fun displayLoginSkippedWarning() {
        if (applicationKvStore.getBoolean("login_skipped", false)) {
            // prompt the user to login
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.login_alert_message)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .setPositiveButton(R.string.login) { _, _ ->
                    // logout of the app
                    val logoutListener =
                        CommonsApplication.BaseLogoutListener(
                            requireActivity()
                        )
                    val app =
                        requireActivity().application as CommonsApplication
                    app.clearApplicationData(requireContext(), logoutListener)
                }
                .show()
        }
    }

    private fun handleLocationUpdate(
        latLng: LatLng,
        locationChangeType: LocationChangeType
    ) {
        lastKnownLocation = latLng
        NearbyController.currentLocation = lastKnownLocation
        presenter!!.updateMapAndList(locationChangeType)
    }

    override fun onLocationChangedSignificantly(latLng: LatLng) {
        Timber.d("Location significantly changed")
        handleLocationUpdate(latLng, LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
    }

    override fun onLocationChangedSlightly(latLng: LatLng) {
        Timber.d("Location slightly changed")
        handleLocationUpdate(latLng, LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
    }

    override fun onLocationChangedMedium(latLng: LatLng) {
        Timber.d("Location changed medium")
        handleLocationUpdate(latLng, LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
    }

    fun backButtonClicked(): Boolean {
        if (::mediaDetails.isInitialized && mediaDetails.isVisible) {
            removeFragment(mediaDetails)

            binding?.coordinatorLayout?.visibility = View.VISIBLE
            binding?.map?.setMultiTouchControls(true)
            binding?.map?.isClickable = true

            val transaction = childFragmentManager.beginTransaction()
            val fragmentContainer = childFragmentManager.findFragmentById(R.id.coordinator_layout)

            if (fragmentContainer != null) {
                transaction.show(fragmentContainer)
            }

            transaction.commit()
            childFragmentManager.executePendingTransactions()

            (activity as? MainActivity)?.showTabs()
            (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            return true
        } else {
            (activity as? MainActivity)?.setSelectedItemId(NavTab.NEARBY.code())
        }

        return presenter?.backButtonClicked() ?: false
    }

    override fun onLocationPermissionDenied(toastMessage: String) {
    }

    override fun onLocationPermissionGranted() {
    }

    /**
     * onLogoutComplete is called after shared preferences and data stored in local database are
     * cleared.
     */
    override fun setFABPlusAction(onClickListener: View.OnClickListener) {
        binding!!.fabPlus.setOnClickListener(onClickListener)
    }

    override fun setFABRecenterAction(onClickListener: View.OnClickListener) {
        binding!!.fabRecenter.setOnClickListener(onClickListener)
    }

    override fun disableFABRecenter() {
        binding!!.fabRecenter.isEnabled = false
    }

    override fun enableFABRecenter() {
        binding!!.fabRecenter.isEnabled = true
    }

    /**
     * Adds a marker for the user's current position. Adds a circle which uses the accuracy * 2, to
     * draw a circle which represents the user's position with an accuracy of 95%.
     *
     *
     * Should be called only on creation of Map, there is other method to update markers location
     * with users move.
     *
     * @param currentLatLng current location
     */
    override fun addCurrentLocationMarker(currentLatLng: LatLng?) {
        if (null != currentLatLng && !isPermissionDenied
            && locationManager.isGPSProviderEnabled()
        ) {
            get().submit {
                Timber.d("Adds current location marker")
                recenterMarkerToPosition(
                    GeoPoint(currentLatLng.latitude, currentLatLng.longitude)
                )
            }
        } else {
            Timber.d("not adding current location marker..current location is null")
        }
    }

    override fun filterOutAllMarkers() {
        clearAllMarkers()
    }

    /**
     * Filters markers based on selectedLabels and chips
     *
     * @param selectedLabels       label list that user clicked
     * @param filterForPlaceState  true if we filter places for place state
     * @param filterForAllNoneType true if we filter places with all none button
     */
    override fun filterMarkersByLabels(
        selectedLabels: List<Label>?,
        filterForPlaceState: Boolean,
        filterForAllNoneType: Boolean
    ) {
        val displayExists = false
        val displayNeedsPhoto = false
        val displayWlm = false
        if (selectedLabels.isNullOrEmpty()) {
            replaceMarkerOverlays(NearbyController.markerLabelList)
            return
        }
        val placeGroupsToShow = ArrayList<MarkerPlaceGroup>()
        for (markerPlaceGroup in NearbyController.markerLabelList) {
            val place = markerPlaceGroup.place
            // When label filter is engaged
            // then compare it against place's label
            if ((selectedLabels.isNotEmpty() || !filterForPlaceState)
                && (!selectedLabels.contains(place.label)
                && !(selectedLabels.contains(Label.BOOKMARKS) && markerPlaceGroup.isBookmarked))
            ) {
                continue
            }

            if (!displayWlm && place.isMonument) {
                continue
            }

            var shouldUpdateMarker = false

            if (displayWlm && place.isMonument) {
                shouldUpdateMarker = true
            } else if (displayExists && displayNeedsPhoto) {
                // Exists and needs photo
                if (place.exists && place.pic.trim { it <= ' ' }.isEmpty()) {
                    shouldUpdateMarker = true
                }
            } else if (displayExists && !displayNeedsPhoto) {
                // Exists and all included needs and doesn't needs photo
                if (place.exists) {
                    shouldUpdateMarker = true
                }
            } else if (!displayExists && displayNeedsPhoto) {
                // All and only needs photo
                if (place.pic.trim { it <= ' ' }.isEmpty()) {
                    shouldUpdateMarker = true
                }
            } else if (!displayExists && !displayNeedsPhoto) {
                // all
                shouldUpdateMarker = true
            }

            if (shouldUpdateMarker) {
                placeGroupsToShow.add(
                    MarkerPlaceGroup(markerPlaceGroup.isBookmarked, place)
                )
            }
        }
        replaceMarkerOverlays(placeGroupsToShow)
    }

    override fun getCameraTarget(): LatLng {
        return mapFocus
    }

    /**
     * Highlights nearest place when user clicks on home nearby banner
     *
     * @param nearestPlace nearest place, which has to be highlighted
     */
    private fun highlightNearestPlace(nearestPlace: Place) {
        binding!!.bottomSheetDetails.icon.visibility = View.VISIBLE
        passInfoToSheet(nearestPlace)
        hideBottomSheet()
        bottomSheetDetailsBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * Returns drawable of marker icon for given place
     *
     * @param place where marker is to be added
     * @param isBookmarked true if place is bookmarked
     * @return returns the drawable of marker according to the place information
     */
    @DrawableRes
    private fun getIconFor(place: Place, isBookmarked: Boolean): Int {
        if (nearestPlace != null && place.name == nearestPlace!!.name) {
            // Highlight nearest place only when user clicks on the home nearby banner
            // highlightNearestPlace(place);
            return if (isBookmarked) {
                R.drawable.ic_custom_map_marker_purple_bookmarked
            } else {
                R.drawable.ic_custom_map_marker_purple
            }
        }

        return when {
            place.isMonument -> R.drawable.ic_custom_map_marker_monuments

            place.pic.trim { it <= ' ' }.isNotEmpty() -> if (isBookmarked) {
                R.drawable.ic_custom_map_marker_green_bookmarked
            } else {
                R.drawable.ic_custom_map_marker_green
            }

            !place.exists -> {
                // Means that the topic of the Wikidata item does not exist in real world anymore
                // for instance it is a past event, or a place that was destroyed
                R.drawable.ic_clear_black_24dp
            }

            place.name.isEmpty() -> if (isBookmarked) {
                R.drawable.ic_custom_map_marker_grey_bookmarked
            } else {
                R.drawable.ic_custom_map_marker_grey
            }

            else -> if (isBookmarked) {
                R.drawable.ic_custom_map_marker_red_bookmarked
            } else {
                R.drawable.ic_custom_map_marker_red
            }
        }
    }

    /**
     * Gets the specified Drawable object. This is a wrapper method for ContextCompat.getDrawable().
     * This method caches results from previous calls for faster retrieval.
     *
     * @param context The context to use to get the Drawable
     * @param id The integer that describes the Drawable resource
     * @return The Drawable object
     */
    private fun getDrawable(context: Context?, id: Int?): Drawable? {
        if (drawableCache == null || context == null || id == null) {
            return null
        }

        val key = Pair(context, id)
        if (!drawableCache!!.containsKey(key)) {
            val drawable = ContextCompat.getDrawable(context, id)

            if (drawable != null) {
                drawableCache!![key] = drawable
            } else {
                return null
            }
        }

        return drawableCache!![key]
    }

    private fun convertToMarker(place: Place, isBookMarked: Boolean): Marker {
        val icon = getDrawable(requireContext(), getIconFor(place, isBookMarked))
        val point = GeoPoint(place.location.latitude, place.location.longitude)
        val marker = Marker(binding!!.map)
        marker.position = point
        marker.icon = icon
        if (place.name != "") {
            marker.title = place.name
            marker.snippet = if (containsParentheses(place.longDescription))
                getTextBetweenParentheses(
                    place.longDescription
                )
            else
                place.longDescription
        }
        marker.textLabelFontSize = 40
        // anchorV is 21.707/28.0 as icon height is 28dp while the pin base is at 21.707dp from top
        marker.setAnchor(Marker.ANCHOR_CENTER, 0.77525f)
        marker.setOnMarkerClickListener { marker1: Marker, _: MapView? ->
            if (clickedMarker != null) {
                clickedMarker!!.closeInfoWindow()
            }
            clickedMarker = marker1
            if (!isNetworkErrorOccurred) {
                binding!!.bottomSheetDetails.dataCircularProgress.visibility =
                    View.VISIBLE
                binding!!.bottomSheetDetails.icon.visibility = View.GONE
                binding!!.bottomSheetDetails.wikiDataLl.visibility = View.GONE
                if (place.name == "") {
                    getPlaceData(place.wikiDataEntityId, place, marker1, isBookMarked)
                } else {
                    marker.showInfoWindow()
                    binding!!.bottomSheetDetails.dataCircularProgress.visibility =
                        View.GONE
                    binding!!.bottomSheetDetails.icon.visibility = View.VISIBLE
                    binding!!.bottomSheetDetails.wikiDataLl.visibility = View.VISIBLE
                    passInfoToSheet(place)
                    hideBottomSheet()
                }
                bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
            } else {
                marker.showInfoWindow()
            }
            true
        }
        return marker
    }

    /**
     * Adds multiple markers representing places to the map and handles item gestures.
     *
     * @param markerPlaceGroups The list of marker place groups containing the places and
     * their bookmarked status
     */
    override fun replaceMarkerOverlays(markerPlaceGroups: List<MarkerPlaceGroup>) {
        val newMarkers = ArrayList<Marker>(markerPlaceGroups.size)
        // iterate in reverse so that the nearest pins get rendered on top
        for (i in markerPlaceGroups.indices.reversed()) {
            newMarkers.add(
                convertToMarker(
                    markerPlaceGroups[i].place,
                    markerPlaceGroups[i].isBookmarked
                )
            )
        }
        clearAllMarkers()
        binding!!.map.overlays.addAll(newMarkers)
    }


    override fun recenterMap(currentLatLng: LatLng?) {
        // if user has denied permission twice, then show dialog
        if (isPermissionDenied) {
            if (locationPermissionsHelper!!.checkLocationPermission(requireActivity())) {
                // this will run when user has given permission by opening app's settings
                isPermissionDenied = false
                locationPermissionGranted()
                return
            } else {
                askForLocationPermission()
            }
        } else {
            if (!locationPermissionsHelper!!.checkLocationPermission(requireActivity())) {
                askForLocationPermission()
            } else {
                locationPermissionGranted()
            }
        }
        if (currentLatLng == null) {
            recenterToUserLocation = true
            return
        }

        /*
         * FIXME: With the revamp of the location permission helper in the MR
         *  #5494[1], there is a doubt that the following code is redundant.
         *  If we could confirm the same, the following code can be removed. If it
         *  turns out to be necessary, we could replace this with a comment
         *  clarifying why it is necessary.
         *
         * Ref: https://github.com/commons-app/apps-android-commons/pull/5494#discussion_r1560404794
         */
        if (lastMapFocus != null) {
            val myLocation = Location("")
            val destLocation = Location("")
            destLocation.latitude = binding!!.map.mapCenter.latitude
            destLocation.longitude = binding!!.map.mapCenter.longitude
            myLocation.latitude = lastMapFocus!!.latitude
            myLocation.longitude = lastMapFocus!!.longitude
            val distance = myLocation.distanceTo(destLocation) //in meters
            if (lastMapFocus != null) {
                if (isNetworkConnectionEstablished) {
                    searchable = if (distance > 2000.0) {
                        true
                    } else {
                        false
                    }
                }
            } else {
                searchable = false
            }
        }
    }

    override fun openLocationSettings() {
        // This method opens the location settings of the device along with a followup toast.
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val packageManager = requireActivity().packageManager

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            Toast.makeText(
                context,
                R.string.recommend_high_accuracy_mode,
                Toast.LENGTH_LONG
            )
                .show()
        } else {
            Toast.makeText(
                context,
                R.string.cannot_open_location_settings,
                Toast.LENGTH_LONG
            )
                .show()
        }
    }

    override fun hideBottomSheet() {
        bottomSheetListBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun hideBottomDetailsSheet() {
        bottomSheetDetailsBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
    }

    /**
     * If nearby details bottom sheet state is collapsed: show fab plus If nearby details bottom
     * sheet state is expanded: show fab plus If nearby details bottom sheet state is hidden: hide
     * all FABs
     *
     * @param bottomSheetState see bottom sheet states
     */
    fun prepareViewsForSheetPosition(bottomSheetState: Int) {
        when (bottomSheetState) {
            (BottomSheetBehavior.STATE_COLLAPSED) -> {
                collapseFABs(isFABsExpanded)
                if (!binding!!.fabPlus.isShown) {
                    showFABs()
                }
            }

            (BottomSheetBehavior.STATE_HIDDEN) -> {
                binding!!.transparentView.isClickable = false
                binding!!.transparentView.alpha = 0F
                collapseFABs(isFABsExpanded)
                hideFABs()
            }
        }
    }

    /**
     * Same bottom sheet carries information for all nearby places, so we need to pass information
     * (title, description, distance and links) to view on nearby marker click
     *
     * @param place Place of clicked nearby marker
     */
    private fun passInfoToSheet(place: Place) {
        selectedPlace = place
        dataList = ArrayList()
        // TODO: Decide button text for fitting in the screen
        (dataList as ArrayList<BottomSheetItem>).add(
            BottomSheetItem(
                R.drawable.ic_round_star_border_24px,
                ""
            )
        )
        (dataList as ArrayList<BottomSheetItem>).add(
            BottomSheetItem(
                R.drawable.ic_directions_black_24dp,
                resources.getString(R.string.nearby_directions)
            )
        )
        if (place.hasWikidataLink()) {
            (dataList as ArrayList<BottomSheetItem>).add(
                BottomSheetItem(
                    R.drawable.ic_wikidata_logo_24dp,
                    resources.getString(R.string.nearby_wikidata)
                )
            )
        }
        (dataList as ArrayList<BottomSheetItem>).add(
            BottomSheetItem(
                R.drawable.ic_feedback_black_24dp,
                resources.getString(R.string.nearby_wikitalk)
            )
        )
        if (place.hasWikipediaLink()) {
            (dataList as ArrayList<BottomSheetItem>).add(
                BottomSheetItem(
                    R.drawable.ic_wikipedia_logo_24dp,
                    resources.getString(R.string.nearby_wikipedia)
                )
            )
        }
        if (selectedPlace!!.hasCommonsLink()) {
            (dataList as ArrayList<BottomSheetItem>).add(
                BottomSheetItem(
                    R.drawable.ic_commons_icon_vector,
                    resources.getString(R.string.nearby_commons)
                )
            )
        }
        val spanCount = spanCount
        gridLayoutManager = GridLayoutManager(this.context, spanCount)
        binding!!.bottomSheetDetails.bottomSheetRecyclerView.layoutManager = gridLayoutManager
        bottomSheetAdapter = BottomSheetAdapter(
            this.context,
            dataList as ArrayList<BottomSheetItem>
        )
        bottomSheetAdapter!!.setClickListener(this)
        binding!!.bottomSheetDetails.bottomSheetRecyclerView.adapter = bottomSheetAdapter
        updateBookmarkButtonImage(selectedPlace!!)

        selectedPlace?.pic?.substringAfterLast("/")?.takeIf { it.isNotEmpty() }?.let { imageName ->
            Glide.with(binding!!.bottomSheetDetails.icon.context)
                .clear(binding!!.bottomSheetDetails.icon)

            val loadingDrawable = ContextCompat.getDrawable(
                binding!!.bottomSheetDetails.icon.context,
                R.drawable.loading_icon
            )
            val animation = AnimationUtils.loadAnimation(
                binding!!.bottomSheetDetails.icon.context,
                R.anim.rotate
            )

            Glide.with(binding!!.bottomSheetDetails.icon.context)
                .load("https://commons.wikimedia.org/wiki/Special:Redirect/file/$imageName?width=25")
                .placeholder(loadingDrawable)
                .error(selectedPlace!!.label.icon)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding!!.bottomSheetDetails.icon.clearAnimation()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding!!.bottomSheetDetails.icon.clearAnimation()
                        return false
                    }
                })
                .into(binding!!.bottomSheetDetails.icon)

            if (binding!!.bottomSheetDetails.icon.drawable != null
                && binding!!.bottomSheetDetails.icon.drawable.constantState == loadingDrawable?.constantState) {
                binding!!.bottomSheetDetails.icon.startAnimation(animation)
            } else {
                binding!!.bottomSheetDetails.icon.clearAnimation()
            }

            binding!!.bottomSheetDetails.icon.setOnClickListener {
                handleMediaClick(imageName)
            }
        } ?: run {
            binding!!.bottomSheetDetails.icon.setImageResource(selectedPlace!!.label.icon)
        }

        binding!!.bottomSheetDetails.title.text = selectedPlace!!.name
        binding!!.bottomSheetDetails.category.text = selectedPlace!!.distance
        // Remove label since it is double information
        var descriptionText = selectedPlace!!.longDescription
            .replace(selectedPlace!!.getName() + " (", "")
        descriptionText = (if (descriptionText == selectedPlace!!.longDescription)
            descriptionText
        else
            descriptionText.replaceFirst(".$".toRegex(), ""))
        // Set the short description after we remove place name from long description
        binding!!.bottomSheetDetails.description.text = descriptionText

        binding!!.fabCamera.setOnClickListener { _ ->
            if (binding!!.fabCamera.isShown) {
                Timber.d("Camera button tapped. Place: %s", selectedPlace.toString())
                storeSharedPrefs(selectedPlace!!)
                activity?.let {
                    controller.initiateCameraPick(
                        it,
                        inAppCameraLocationPermissionLauncher,
                        cameraPickLauncherForResult
                    )
                }
            }
        }

        binding!!.fabGallery.setOnClickListener { _ ->
            if (binding!!.fabGallery.isShown) {
                Timber.d("Gallery button tapped. Place: %s", selectedPlace.toString())
                storeSharedPrefs(selectedPlace!!)
                activity?.let {
                    controller.initiateGalleryPick(
                        it,
                        galleryPickLauncherForResult,
                        false
                    )
                }
            }
        }

        binding!!.fabCustomGallery.setOnClickListener { _ ->
            if (binding!!.fabCustomGallery.isShown) {
                Timber.d("Gallery button tapped. Place: %s", selectedPlace.toString())
                storeSharedPrefs(selectedPlace!!)
                activity?.let {
                    controller.initiateCustomGalleryPickWithPermission(
                        it,
                        customSelectorLauncherForResult
                    )
                }
            }
        }
    }

    private fun handleMediaClick(imageName: String) {
        val decodedImageName = URLDecoder.decode(imageName, StandardCharsets.UTF_8.toString())

        mediaClient.getMedia("File:$decodedImageName")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { mediaResponse ->
                    if (mediaResponse != null) {
                        // Create a Media object from the response
                        media = Media(
                            pageId = mediaResponse.pageId,
                            thumbUrl = mediaResponse.thumbUrl,
                            imageUrl = mediaResponse.imageUrl,
                            filename = mediaResponse.filename,
                            fallbackDescription = mediaResponse.fallbackDescription,
                            dateUploaded = mediaResponse.dateUploaded,
                            license = mediaResponse.license,
                            licenseUrl = mediaResponse.licenseUrl,
                            author = mediaResponse.author,
                            user = mediaResponse.user,
                            categories = mediaResponse.categories,
                            coordinates = mediaResponse.coordinates,
                            captions = mediaResponse.captions,
                            descriptions = mediaResponse.descriptions,
                            depictionIds = mediaResponse.depictionIds,
                            categoriesHiddenStatus = mediaResponse.categoriesHiddenStatus
                        )
                        // Remove existing fragment before showing new details
                        if (::mediaDetails.isInitialized && mediaDetails.isAdded) {
                            removeFragment(mediaDetails)
                        }
                        showMediaDetails()
                    } else {
                        Timber.e("Fetched media is null for image: $decodedImageName")
                    }
                }
            ) { throwable ->
                Timber.e(throwable, "Error fetching media for image: $decodedImageName")
            }
    }

    private fun showMediaDetails() {
        binding?.map?.setMultiTouchControls(false)
        binding?.map?.isClickable = false

        mediaDetails = MediaDetailPagerFragment.newInstance(false, true)


        val transaction = childFragmentManager.beginTransaction()

        val fragmentContainer = childFragmentManager.findFragmentById(R.id.coordinator_layout)
        if (fragmentContainer != null) {
            transaction.hide(fragmentContainer)
        }

        // Replace instead of add to ensure new fragment is used
        transaction.replace(R.id.coordinator_layout, mediaDetails, "MediaDetailFragmentTag")
        transaction.addToBackStack("Nearby_Parent_Fragment_Tag").commit()
        childFragmentManager.executePendingTransactions()

        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (mediaDetails.isAdded) {
            mediaDetails.showImage(0)
        } else {
            Timber.e("Error: MediaDetailPagerFragment is NOT added")
        }
    }

    override fun getMediaAtPosition(i: Int): Media {
        return media
    }

    override fun getTotalMediaCount(): Int {
        return 2
    }

    override fun getContributionStateAt(position: Int): Int? {
        return null
    }

    override fun refreshNominatedMedia(index: Int) {
        if (::mediaDetails.isInitialized && !binding?.map?.isClickable!!) {
            removeFragment(mediaDetails)
            showMediaDetails()
        }
    }

    private fun removeFragment(fragment: Fragment) {
        childFragmentManager
            .beginTransaction()
            .remove(fragment)
            .commit()
        childFragmentManager.executePendingTransactions()
    }

    private fun storeSharedPrefs(selectedPlace: Place) {
        applicationKvStore.putJson(WikidataConstants.PLACE_OBJECT, selectedPlace)
        val place = applicationKvStore.getJson<Place>(WikidataConstants.PLACE_OBJECT)

        Timber.d("Stored place object %s", place.toString())
    }

    private fun updateBookmarkButtonImage(place: Place) {
        NearbyUtil.getBookmarkLocationExists(
            bookmarkLocationDao,
            place.getName(),
            scope,
            bottomSheetAdapter!!
        )
    }

    private fun toggleBookmarkButtonImage() {
        bottomSheetAdapter?.toggleBookmarkIcon()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        wikidataEditListener.authenticationStateListener = this
    }

    override fun onDestroy() {
        super.onDestroy()
        wikidataEditListener.authenticationStateListener = null
    }

    override fun onWikidataEditSuccessful() {
        if (presenter != null) {
            presenter!!.updateMapAndList(LocationChangeType.MAP_UPDATED)
        }
    }

    private fun showErrorMessage(message: String) {
        Timber.e(message)
        showLongToast(requireActivity(), message)
    }

    fun registerUnregisterLocationListener(removeLocationListener: Boolean) {
        try {
            if (removeLocationListener) {
                locationManager.unregisterLocationManager()
                locationManager.removeLocationListener(this)
                Timber.d("Location service manager unregistered and removed")
            } else {
                locationManager.addLocationListener(this)
                locationManager.registerLocationManager()
                Timber.d("Location service manager added and registered")
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        this.isVisibleToUser = isVisibleToUser
        if (isResumed && isVisibleToUser) {
            performMapReadyActions()
        } else {
            if (null != bottomSheetListBehavior) {
                bottomSheetListBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
            }

            if (null != bottomSheetDetailsBehavior) {
                bottomSheetDetailsBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    /**
     * Clears all markers from the map and resets certain map overlays and gestures. After clearing
     * markers, it re-adds a scale bar overlay and rotation gesture overlay to the map.
     */
    override fun clearAllMarkers() {
        binding!!.map.overlayManager.clear()
        binding!!.map.invalidate()
        val geoPoint = mapCenter
        if (geoPoint != null) {
            val diskOverlay =
                ScaleDiskOverlay(
                    this.context,
                    geoPoint, 2000, UnitOfMeasure.foot
                )
            val circlePaint = Paint()
            circlePaint.color = Color.rgb(128, 128, 128)
            circlePaint.style = Paint.Style.STROKE
            circlePaint.strokeWidth = 2f
            diskOverlay.setCirclePaint2(circlePaint)
            val diskPaint = Paint()
            diskPaint.color = Color.argb(40, 128, 128, 128)
            diskPaint.style = Paint.Style.FILL_AND_STROKE
            diskOverlay.setCirclePaint1(diskPaint)
            diskOverlay.setDisplaySizeMin(900)
            diskOverlay.setDisplaySizeMax(1700)
            binding!!.map.overlays.add(diskOverlay)
            val startMarker = Marker(
                binding!!.map
            )
            startMarker.position = geoPoint
            startMarker.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
            )
            startMarker.icon =
                getDrawable(
                    requireContext(),
                    R.drawable.current_location_marker
                )
            startMarker.title = "Your Location"
            startMarker.textLabelFontSize = 24
            binding!!.map.overlays.add(startMarker)
        }
        val scaleBarOverlay = ScaleBarOverlay(binding!!.map)
        scaleBarOverlay.setScaleBarOffset(15, 25)
        val barPaint = Paint()
        barPaint.setARGB(200, 255, 250, 250)
        scaleBarOverlay.setBackgroundPaint(barPaint)
        scaleBarOverlay.enableScaleBar()
        binding!!.map.overlays.add(scaleBarOverlay)
        binding!!.map.overlays.add(mapEventsOverlay)
        binding!!.map.setMultiTouchControls(true)
    }

    /**
     * Re-center the map to the Center and adds a scale disk overlay and a marker at the position.
     *
     * @param geoPoint The GeoPoint representing the new center position of the map.
     */
    private fun recenterMarkerToPosition(geoPoint: GeoPoint?) {
        geoPoint?.let {
            binding?.map?.controller?.setCenter(it)
            val overlays = binding?.map?.overlays ?: return@let

            // Remove markers and disks using index-based removal
            var i = 0
            while (i < overlays.size) {
                when (overlays[i]) {
                    is Marker, is ScaleDiskOverlay -> overlays.removeAt(i)
                    else -> i++
                }
            }

            // Add disk overlay
            ScaleDiskOverlay(context, it, 2000, UnitOfMeasure.foot).apply {
                setCirclePaint2(Paint().apply {
                    color = Color.rgb(128, 128, 128)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                })
                setCirclePaint1(Paint().apply {
                    color = Color.argb(40, 128, 128, 128)
                    style = Paint.Style.FILL_AND_STROKE
                })
                setDisplaySizeMin(900)
                setDisplaySizeMax(1700)
                overlays.add(this)
            }

            // Add marker
            Marker(binding?.map).apply {
                position = it
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = getDrawable(context, R.drawable.current_location_marker)
                title = "Your Location"
                textLabelFontSize = 24
                overlays.add(this)
            }
        }
    }

    private fun moveCameraToPosition(geoPoint: GeoPoint) {
        binding!!.map.controller.animateTo(geoPoint)
    }

    /**
     * Moves the camera of the map view to the specified GeoPoint at specified zoom level and speed
     * using an animation.
     *
     * @param geoPoint The GeoPoint representing the new camera position for the map.
     * @param zoom     Zoom level of the map camera
     * @param speed    Speed of animation
     */
    private fun moveCameraToPosition(geoPoint: GeoPoint, zoom: Double, speed: Long) {
        binding!!.map.controller.animateTo(geoPoint, zoom, speed)
    }


    override fun onBottomSheetItemClick(view: View?, position: Int) {
        val item = dataList?.get(position) ?: return // Null check for dataList
        when (item.imageResourceId) {
            R.drawable.ic_round_star_border_24px -> {
                presenter?.toggleBookmarkedStatus(selectedPlace, scope)
                toggleBookmarkButtonImage()
            }

            R.drawable.ic_round_star_filled_24px -> {
                presenter?.toggleBookmarkedStatus(selectedPlace, scope)
                toggleBookmarkButtonImage()
                selectedPlace?.let { updateBookmarkButtonImage(it) }
            }

            R.drawable.ic_directions_black_24dp -> {
                selectedPlace?.let {
                    Utils.handleGeoCoordinates(this.context, it.getLocation())
                    binding?.map?.zoomLevelDouble ?: 0.0
                }
            }

            R.drawable.ic_wikidata_logo_24dp -> {
                selectedPlace?.siteLinks?.wikidataLink?.let {
                    Utils.handleWebUrl(this.context, it)
                }
            }

            R.drawable.ic_feedback_black_24dp -> {
                selectedPlace?.let {
                    val intent = Intent(this.context, WikidataFeedback::class.java).apply {
                        putExtra("lat", it.location.latitude)
                        putExtra("lng", it.location.longitude)
                        putExtra("place", it.name)
                        putExtra("qid", it.wikiDataEntityId)
                    }
                    startActivity(intent)
                }
            }

            R.drawable.ic_wikipedia_logo_24dp -> {
                selectedPlace?.siteLinks?.wikipediaLink?.let {
                    Utils.handleWebUrl(this.context, it)
                }
            }

            R.drawable.ic_commons_icon_vector -> {
                selectedPlace?.siteLinks?.commonsLink?.let {
                    Utils.handleWebUrl(this.context, it)
                }
            }

            else -> {}
        }
    }


    override fun onBottomSheetItemLongClick(view: View?, position: Int) {
        val item = dataList!![position]
        val message = when (item.imageResourceId) {
            R.drawable.ic_round_star_border_24px -> getString(R.string.menu_bookmark)
            R.drawable.ic_round_star_filled_24px -> getString(R.string.menu_bookmark)
            R.drawable.ic_directions_black_24dp -> getString(R.string.nearby_directions)
            R.drawable.ic_wikidata_logo_24dp -> getString(R.string.nearby_wikidata)
            R.drawable.ic_feedback_black_24dp -> getString(R.string.nearby_wikitalk)
            R.drawable.ic_wikipedia_logo_24dp -> getString(R.string.nearby_wikipedia)
            R.drawable.ic_commons_icon_vector -> getString(R.string.nearby_commons)
            else -> "Long click"
        }
        Toast.makeText(this.context, message, Toast.LENGTH_SHORT).show()
    }

    interface NearbyParentFragmentInstanceReadyCallback {
        fun onReady()
    }

    fun setNearbyParentFragmentInstanceReadyCallback(
        nearbyParentFragmentInstanceReadyCallback: NearbyParentFragmentInstanceReadyCallback?
    ) {
        this.nearbyParentFragmentInstanceReadyCallback = nearbyParentFragmentInstanceReadyCallback
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val rlBottomSheetLayoutParams = binding!!.bottomSheetNearby.bottomSheet.layoutParams
        rlBottomSheetLayoutParams.height = getWindowHeight(requireActivity()) / 16 * 9

        binding!!.bottomSheetNearby.bottomSheet.layoutParams = rlBottomSheetLayoutParams
        val spanCount = spanCount
        if (gridLayoutManager != null) {
            gridLayoutManager!!.spanCount = spanCount
        }
    }

    private fun getWindowHeight(activity: FragmentActivity): Int {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds.height()
        } else {
            activity.windowManager.defaultDisplay.height
        }
    }

    fun onLearnMoreClicked() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setData(Uri.parse(WLM_URL))
        startActivity(intent)
    }

    companion object {
        private const val CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.005
        private const val CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.004
        private const val ZOOM_LEVEL: Double = 15.0

        /**
         * WLM URL
         */
        const val WLM_URL: String =
            "https://commons.wikimedia.org/wiki/Commons:Mobile_app/Contributing_to_WLM_using_the_app"

        @JvmStatic  // This makes it callable as a static method from Java
        fun newInstance(): NearbyParentFragment {
            val fragment = NearbyParentFragment()
            fragment.retainInstance = true
            return fragment
        }


        private val isExternalStorageWritable: Boolean
            get() {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state
            }

        /**
         * Extracts text between the first occurrence of '(' and its corresponding ')' in the input
         * string.
         *
         * @param input The input string from which to extract text between parentheses.
         * @return The text between parentheses if found, or `null` if no parentheses are found.
         */
        fun getTextBetweenParentheses(input: String): String? {
            val startIndex = input.indexOf('(')
            val endIndex = input.indexOf(')', startIndex)
            return if (startIndex != -1 && endIndex != -1) {
                input.substring(startIndex + 1, endIndex)
            } else {
                null
            }
        }

        /**
         * Checks if the given text contains '(' or ')'.
         *
         * @param input The input text to check.
         * @return `true` if '(' or ')' is found, `false` otherwise.
         */
        fun containsParentheses(input: String): Boolean {
            return input.contains("(") || input.contains(")")
        }
    }
}