package fr.free.nrw.commons.LocationPicker;

import static fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_LOCATION;
import static fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_ZOOM;
import static fr.free.nrw.commons.utils.MapUtils.ZOOM_LEVEL;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import fr.free.nrw.commons.CameraPosition;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient;
import fr.free.nrw.commons.coordinates.CoordinateEditHelper;
import fr.free.nrw.commons.filepicker.Constants;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationPermissionsHelper;
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.constants.GeoConstants;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleDiskOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import timber.log.Timber;

/**
 * Helps to pick location and return the result with an intent
 */
public class LocationPickerActivity extends BaseActivity implements
    LocationPermissionCallback {
    /**
     * coordinateEditHelper: helps to edit coordinates
     */
    @Inject
    CoordinateEditHelper coordinateEditHelper;
    /**
     * media : Media object
     */
    private Media media;
    /**
     * cameraPosition : position of picker
     */
    private CameraPosition cameraPosition;
    /**
     * markerImage : picker image
     */
    private ImageView markerImage;
    /**
     * mapView : OSM Map
     */
    private org.osmdroid.views.MapView mapView;
    /**
     * tvAttribution : credit
     */
    private AppCompatTextView tvAttribution;
    /**
     * activity : activity key
     */
    private String activity;
    /**
     * modifyLocationButton : button for start editing location
     */
    Button modifyLocationButton;
    /**
     * removeLocationButton : button to remove location metadata
     */
    Button removeLocationButton;
    /**
     * showInMapButton : button for showing in map
     */
    TextView showInMapButton;
    /**
     * placeSelectedButton : fab for selecting location
     */
    FloatingActionButton placeSelectedButton;
    /**
     * fabCenterOnLocation: button for center on location;
     */
    FloatingActionButton fabCenterOnLocation;
    /**
     * shadow : imageview of shadow
     */
    private ImageView shadow;
    /**
     * largeToolbarText : textView of shadow
     */
    private TextView largeToolbarText;
    /**
     * smallToolbarText : textView of shadow
     */
    private TextView smallToolbarText;
    /**
     * applicationKvStore : for storing values
     */
    @Inject
    @Named("default_preferences")
    public
    JsonKvStore applicationKvStore;
    BasicKvStore store;
    /**
     * isDarkTheme: for keeping a track of the device theme and modifying the map theme accordingly
     */
    @Inject
    SystemThemeUtils systemThemeUtils;
    private boolean isDarkTheme;
    private boolean moveToCurrentLocation;

    @Inject
    LocationServiceManager locationManager;
    LocationPermissionsHelper locationPermissionsHelper;

    @Inject
    SessionManager sessionManager;

    /**
     * Constants
     */
    private static final String CAMERA_POS = "cameraPosition";
    private static final String ACTIVITY = "activity";


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);

        isDarkTheme = systemThemeUtils.isDeviceInNightMode();
        moveToCurrentLocation = false;
        store = new BasicKvStore(this, "LocationPermissions");

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_location_picker);

        if (savedInstanceState == null) {
            cameraPosition = getIntent()
                .getParcelableExtra(LocationPickerConstants.MAP_CAMERA_POSITION);
            activity = getIntent().getStringExtra(LocationPickerConstants.ACTIVITY_KEY);
            media = getIntent().getParcelableExtra(LocationPickerConstants.MEDIA);
        }else{
            cameraPosition = savedInstanceState.getParcelable(CAMERA_POS);
            activity = savedInstanceState.getString(ACTIVITY);
            media = savedInstanceState.getParcelable("sMedia");
        }
        bindViews();
        addBackButtonListener();
        addPlaceSelectedButton();
        addCredits();
        getToolbarUI();
        addCenterOnGPSButton();

        org.osmdroid.config.Configuration.getInstance().load(getApplicationContext(),
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        mapView.setTileSource(TileSourceFactory.WIKIMEDIA);
        mapView.setTilesScaledToDpi(true);
        mapView.setMultiTouchControls(true);

        org.osmdroid.config.Configuration.getInstance().getAdditionalHttpRequestProperties().put(
            "Referer", "http://maps.wikimedia.org/"
        );
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.getController().setZoom(ZOOM_LEVEL);
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (markerImage.getTranslationY() == 0) {
                    markerImage.animate().translationY(-75)
                        .setInterpolator(new OvershootInterpolator()).setDuration(250).start();
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                markerImage.animate().translationY(0)
                    .setInterpolator(new OvershootInterpolator()).setDuration(250).start();
            }
            return false;
        });

        if ("UploadActivity".equals(activity)) {
            placeSelectedButton.setVisibility(View.GONE);
            modifyLocationButton.setVisibility(View.VISIBLE);
            removeLocationButton.setVisibility(View.VISIBLE);
            showInMapButton.setVisibility(View.VISIBLE);
            largeToolbarText.setText(getResources().getString(R.string.image_location));
            smallToolbarText.setText(getResources().
                getString(R.string.check_whether_location_is_correct));
            fabCenterOnLocation.setVisibility(View.GONE);
            markerImage.setVisibility(View.GONE);
            shadow.setVisibility(View.GONE);
            assert cameraPosition != null;
            showSelectedLocationMarker(new GeoPoint(cameraPosition.getLatitude(),
                cameraPosition.getLongitude()));
        }
        setupMapView();
    }

    /**
     * Moves the center of the map to the specified coordinates
     *
     */
    private void moveMapTo(double latitude, double longitude){
        if(mapView != null && mapView.getController() != null){
            GeoPoint point = new GeoPoint(latitude, longitude);

            mapView.getController().setCenter(point);
            mapView.getController().animateTo(point);
        }
    }

    /**
     * Moves the center of the map to the specified coordinates
     * @param point The GeoPoint object which contains the coordinates to move to
     */
    private void moveMapTo(GeoPoint point){
        if(point != null){
            moveMapTo(point.getLatitude(), point.getLongitude());
        }
    }

    /**
     * For showing credits
     */
    private void addCredits() {
        tvAttribution.setText(Html.fromHtml(getString(R.string.map_attribution)));
        tvAttribution.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * For setting up Dark Theme
     */
    private void darkThemeSetup() {
        if (isDarkTheme) {
            shadow.setColorFilter(Color.argb(255, 255, 255, 255));
            mapView.getOverlayManager().getTilesOverlay()
                .setColorFilter(TilesOverlay.INVERT_COLORS);
        }
    }

    /**
     * Clicking back button destroy locationPickerActivity
     */
    private void addBackButtonListener() {
        final ImageView backButton = findViewById(R.id.maplibre_place_picker_toolbar_back_button);
        backButton.setOnClickListener(v -> {
            finish();
        });

    }

    /**
     * Binds mapView and location picker icon
     */
    private void bindViews() {
        mapView = findViewById(R.id.map_view);
        markerImage = findViewById(R.id.location_picker_image_view_marker);
        tvAttribution = findViewById(R.id.tv_attribution);
        modifyLocationButton = findViewById(R.id.modify_location);
        removeLocationButton = findViewById(R.id.remove_location);
        showInMapButton = findViewById(R.id.show_in_map);
        showInMapButton.setText(getResources().getString(R.string.show_in_map_app).toUpperCase(
            Locale.ROOT));
        shadow = findViewById(R.id.location_picker_image_view_shadow);
    }

    /**
     * Gets toolbar color
     */
    private void getToolbarUI() {
        final ConstraintLayout toolbar = findViewById(R.id.location_picker_toolbar);
        largeToolbarText = findViewById(R.id.location_picker_toolbar_primary_text_view);
        smallToolbarText = findViewById(R.id.location_picker_toolbar_secondary_text_view);
        toolbar.setBackgroundColor(getResources().getColor(R.color.primaryColor));
    }

    private void setupMapView() {
        requestLocationPermissions();

        //If location metadata is available, move map to that location.
        if(activity.equals("UploadActivity") || activity.equals("MediaActivity")){
            moveMapToMediaLocation();
        } else {
            //If location metadata is not available, move map to device GPS location.
            moveMapToGPSLocation();
        }

        modifyLocationButton.setOnClickListener(v -> onClickModifyLocation());
        removeLocationButton.setOnClickListener(v -> onClickRemoveLocation());
        showInMapButton.setOnClickListener(v -> showInMapApp());
        darkThemeSetup();
    }

    /**
     * Handles onclick event of modifyLocationButton
     */
    private void onClickModifyLocation() {
        placeSelectedButton.setVisibility(View.VISIBLE);
        modifyLocationButton.setVisibility(View.GONE);
        removeLocationButton.setVisibility(View.GONE);
        showInMapButton.setVisibility(View.GONE);
        markerImage.setVisibility(View.VISIBLE);
        shadow.setVisibility(View.VISIBLE);
        largeToolbarText.setText(getResources().getString(R.string.choose_a_location));
        smallToolbarText.setText(getResources().getString(R.string.pan_and_zoom_to_adjust));
        fabCenterOnLocation.setVisibility(View.VISIBLE);
        removeSelectedLocationMarker();
        moveMapToMediaLocation();
    }

    /**
     * Handles onclick event of removeLocationButton
     */
    private void onClickRemoveLocation() {
        DialogUtil.showAlertDialog(this,
            getString(R.string.remove_location_warning_title),
            getString(R.string.remove_location_warning_desc),
            getString(R.string.continue_message),
            getString(R.string.cancel), () -> removeLocationFromImage(), null);
    }

    /**
     * Method to remove the location from the picture
     */
    private void removeLocationFromImage() {
        if (media != null) {
            getCompositeDisposable().add(coordinateEditHelper.makeCoordinatesEdit(getApplicationContext()
                    , media, "0.0", "0.0", "0.0f")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    Timber.d("Coordinates are removed from the image");
                }));
        }
        final Intent returningIntent = new Intent();
        setResult(AppCompatActivity.RESULT_OK, returningIntent);
        finish();
    }

    /**
     * Show the location in map app. Map will center on the location metadata, if available.
     * If there is no location metadata, the map will center on the commons app map center.
     */
    private void showInMapApp() {
        fr.free.nrw.commons.location.LatLng position = null;

        if(activity.equals("UploadActivity") && cameraPosition != null){
            //location metadata is available
            position = new fr.free.nrw.commons.location.LatLng(cameraPosition.getLatitude(),
                cameraPosition.getLongitude(), 0.0f);
        } else if(mapView != null){
            //location metadata is not available
            position = new fr.free.nrw.commons.location.LatLng(mapView.getMapCenter().getLatitude(),
                mapView.getMapCenter().getLongitude(), 0.0f);
        }

        if(position != null){
            Utils.handleGeoCoordinates(this, position);
        }
    }

    /**
     * Moves the center of the map to the media's location, if that data
     * is available.
     */
    private void moveMapToMediaLocation() {
        if (cameraPosition != null) {

            GeoPoint point = new GeoPoint(cameraPosition.getLatitude(),
                cameraPosition.getLongitude());

            moveMapTo(point);
        }
    }

    /**
     * Moves the center of the map to the device's GPS location, if that data is available.
     */
    private void moveMapToGPSLocation(){
        if(locationManager != null){
            fr.free.nrw.commons.location.LatLng location = locationManager.getLastLocation();

            if(location != null){
                GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());

                moveMapTo(point);
            }
        }
    }

    /**
     * Select the preferable location
     */
    private void addPlaceSelectedButton() {
        placeSelectedButton = findViewById(R.id.location_chosen_button);
        placeSelectedButton.setOnClickListener(view -> placeSelected());
    }

    /**
     * Return the intent with required data
     */
    void placeSelected() {
        if (activity.equals("NoLocationUploadActivity")) {
            applicationKvStore.putString(LAST_LOCATION,
                mapView.getMapCenter().getLatitude()
                    + ","
                    + mapView.getMapCenter().getLongitude());
            applicationKvStore.putString(LAST_ZOOM, mapView.getZoomLevel() + "");
        }

        if (media == null) {
            final Intent returningIntent = new Intent();
            returningIntent.putExtra(LocationPickerConstants.MAP_CAMERA_POSITION,
                new CameraPosition(mapView.getMapCenter().getLatitude(),
                    mapView.getMapCenter().getLongitude(), 14.0));
            setResult(AppCompatActivity.RESULT_OK, returningIntent);
        } else {
            updateCoordinates(String.valueOf(mapView.getMapCenter().getLatitude()),
                String.valueOf(mapView.getMapCenter().getLongitude()),
                String.valueOf(0.0f));
        }

        finish();
    }

    /**
     * Fetched coordinates are replaced with existing coordinates by a POST API call.
     * @param Latitude to be added
     * @param Longitude to be added
     * @param Accuracy to be added
     */
    public void updateCoordinates(final String Latitude, final String Longitude,
        final String Accuracy) {
        if (media == null) {
            return;
        }

        try {
            getCompositeDisposable().add(
                coordinateEditHelper.makeCoordinatesEdit(getApplicationContext(), media,
                        Latitude, Longitude, Accuracy)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> {
                            Timber.d("Coordinates are added.");
                        }));
        } catch (Exception e) {
            if (e.getLocalizedMessage().equals(CsrfTokenClient.ANONYMOUS_TOKEN_MESSAGE)) {
                final String username = sessionManager.getUserName();
                final CommonsApplication.BaseLogoutListener logoutListener = new CommonsApplication.BaseLogoutListener(
                    this,
                    getString(R.string.invalid_login_message),
                    username
                );

                CommonsApplication.getInstance().clearApplicationData(
                    this, logoutListener);
            }
        }
    }

    /**
     * Center the camera on the last saved location
     */
    private void addCenterOnGPSButton() {
        fabCenterOnLocation = findViewById(R.id.center_on_gps);
        fabCenterOnLocation.setOnClickListener(view -> {
            moveToCurrentLocation = true;
            requestLocationPermissions();
        });
    }

    /**
     * Adds selected location marker on the map
     */
    private void showSelectedLocationMarker(GeoPoint point) {
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.map_default_map_marker);
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(icon);
        marker.setInfoWindow(null);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    /**
     * Removes selected location marker from the map
     */
    private void removeSelectedLocationMarker() {
        List<Overlay> overlays = mapView.getOverlays();
        for (int i = 0; i < overlays.size(); i++) {
            if (overlays.get(i) instanceof Marker) {
                Marker item = (Marker) overlays.get(i);
                if (cameraPosition.getLatitude() == item.getPosition().getLatitude()
                    && cameraPosition.getLongitude() == item.getPosition().getLongitude()) {
                    mapView.getOverlays().remove(i);
                    mapView.invalidate();
                    break;
                }
            }
        }
    }

    /**
     * Center the map at user's current location
     */
    private void requestLocationPermissions() {
        locationPermissionsHelper = new LocationPermissionsHelper(
            this, locationManager, this);
        locationPermissionsHelper.requestForLocationAccess(R.string.location_permission_title,
            R.string.upload_map_location_access);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
        @NonNull final String[] permissions,
        @NonNull final int[] grantResults) {
        if (requestCode == Constants.RequestCodes.LOCATION
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onLocationPermissionGranted();
        } else {
            onLocationPermissionDenied(getString(R.string.upload_map_location_access));
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLocationPermissionDenied(String toastMessage) {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
            permission.ACCESS_FINE_LOCATION)) {
            if (!locationPermissionsHelper.checkLocationPermission(this)) {
                if (store.getBoolean("isPermissionDenied", false)) {
                    // means user has denied location permission twice or checked the "Don't show again"
                    locationPermissionsHelper.showAppSettingsDialog(this,
                        R.string.upload_map_location_access);
                } else {
                    Toast.makeText(getBaseContext(), toastMessage, Toast.LENGTH_LONG).show();
                }
                store.putBoolean("isPermissionDenied", true);
            }
        } else {
            Toast.makeText(getBaseContext(), toastMessage, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationPermissionGranted() {
        if (moveToCurrentLocation || !(activity.equals("MediaActivity"))) {
            if (locationPermissionsHelper.isLocationAccessToAppsTurnedOn()) {
                locationManager.requestLocationUpdatesFromProvider(
                    LocationManager.NETWORK_PROVIDER);
                locationManager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER);
                addMarkerAtGPSLocation();
            } else {
                addMarkerAtGPSLocation();
                locationPermissionsHelper.showLocationOffDialog(this,
                    R.string.ask_to_turn_location_on_text);
            }
        }
    }

    /**
     * Adds a marker to the map at the most recent GPS location
     * (which may be the current GPS location).
     */
    private void addMarkerAtGPSLocation() {
        fr.free.nrw.commons.location.LatLng currLocation = locationManager.getLastLocation();
        if (currLocation != null) {
            GeoPoint currLocationGeopoint = new GeoPoint(currLocation.getLatitude(),
                currLocation.getLongitude());
            addLocationMarker(currLocationGeopoint);
            markerImage.setTranslationY(0);
        }
    }

    private void addLocationMarker(GeoPoint geoPoint) {
        if (moveToCurrentLocation) {
            mapView.getOverlays().clear();
        }
        ScaleDiskOverlay diskOverlay =
            new ScaleDiskOverlay(this,
                geoPoint, 2000, GeoConstants.UnitOfMeasure.foot);
        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.rgb(128, 128, 128));
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(2f);
        diskOverlay.setCirclePaint2(circlePaint);
        Paint diskPaint = new Paint();
        diskPaint.setColor(Color.argb(40, 128, 128, 128));
        diskPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        diskOverlay.setCirclePaint1(diskPaint);
        diskOverlay.setDisplaySizeMin(900);
        diskOverlay.setDisplaySizeMax(1700);
        mapView.getOverlays().add(diskOverlay);
        org.osmdroid.views.overlay.Marker startMarker = new org.osmdroid.views.overlay.Marker(
            mapView);
        startMarker.setPosition(geoPoint);
        startMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
            org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        startMarker.setIcon(
            ContextCompat.getDrawable(this, R.drawable.current_location_marker));
        startMarker.setTitle("Your Location");
        startMarker.setTextLabelFontSize(24);
        mapView.getOverlays().add(startMarker);
    }

    /**
     * Saves the state of the activity
     * @param outState Bundle
     */
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        if(cameraPosition!=null){
            outState.putParcelable(CAMERA_POS, cameraPosition);
        }
        if(activity!=null){
            outState.putString(ACTIVITY, activity);
        }

        if(media!=null){
            outState.putParcelable("sMedia", media);
        }
    }
}
