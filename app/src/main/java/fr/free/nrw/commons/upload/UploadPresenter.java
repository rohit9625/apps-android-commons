package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailsContract;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.lang.reflect.Proxy;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * The MVP pattern presenter of Upload GUI
 */
@Singleton
public class UploadPresenter implements UploadContract.UserActionListener {

    private static final UploadContract.View DUMMY = (UploadContract.View) Proxy.newProxyInstance(
            UploadContract.View.class.getClassLoader(),
            new Class[]{UploadContract.View.class}, (proxy, method, methodArgs) -> null);
    private final UploadRepository repository;
    private final JsonKvStore defaultKvStore;
    private UploadContract.View view = DUMMY;
    @Inject
    UploadMediaDetailsContract.UserActionListener presenter;

    private CompositeDisposable compositeDisposable;
    public static final String COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES
        = "number_of_consecutive_uploads_without_coordinates";

    public static final int CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES_REMINDER_THRESHOLD = 10;


    @Inject
    UploadPresenter(UploadRepository uploadRepository,
        @Named("default_preferences") JsonKvStore defaultKvStore) {
        this.repository = uploadRepository;
        this.defaultKvStore = defaultKvStore;
        compositeDisposable = new CompositeDisposable();
    }


    /**
     * Called by the submit button in {@link UploadActivity}
     */
    @SuppressLint("CheckResult")
    @Override
    public void handleSubmit() {
        boolean hasLocationProvidedForNewUploads = false;
        for (UploadItem item : repository.getUploads()) {
            if (item.getGpsCoords().getImageCoordsExists()) {
                hasLocationProvidedForNewUploads = true;
            }
        }
        boolean hasManyConsecutiveUploadsWithoutLocation = defaultKvStore.getInt(
            COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0) >=
            CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES_REMINDER_THRESHOLD;

        if (hasManyConsecutiveUploadsWithoutLocation && !hasLocationProvidedForNewUploads) {
            defaultKvStore.putInt(COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0);
            view.showAlertDialog(
                R.string.location_message,
                () -> {defaultKvStore.putInt(
                    COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES,
                    0);
                    processContributionsForSubmission();
                });
        } else {
            processContributionsForSubmission();
        }
    }

    private void processContributionsForSubmission() {
        if (view.isLoggedIn()) {
            view.showProgress(true);
            repository.buildContributions()
                    .observeOn(Schedulers.io())
                    .subscribe(new Observer<Contribution>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            view.showProgress(false);
                            if (defaultKvStore
                                .getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED,
                                    false)) {
                                view.showMessage(R.string.uploading_queued);
                            } else {
                                view.showMessage(R.string.uploading_started);
                            }

                            compositeDisposable.add(d);
                        }

                        @Override
                        public void onNext(Contribution contribution) {
                            if (contribution.getDecimalCoords() == null) {
                                final int recentCount = defaultKvStore.getInt(
                                    COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0);
                                defaultKvStore.putInt(
                                    COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, recentCount + 1);
                            } else {
                                defaultKvStore.putInt(
                                    COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0);
                            }
                            repository.prepareMedia(contribution);
                            contribution.setState(Contribution.STATE_QUEUED);
                            repository.saveContribution(contribution);
                        }

                        @Override
                        public void onError(Throwable e) {
                            view.showMessage(R.string.upload_failed);
                            repository.cleanup();
                            view.returnToMainActivity();
                            compositeDisposable.clear();
                            Timber.e("failed to upload: " + e.getMessage());

                            //is submission error, not need to go to the uploadActivity
                            //not start the uploading progress
                        }

                        @Override
                        public void onComplete() {
                            view.makeUploadRequest();
                            repository.cleanup();
                            view.returnToMainActivity();
                            compositeDisposable.clear();

                            //after finish the uploadActivity, if successful,
                            //directly go to the upload progress activity
                            view.goToUploadProgressActivity();
                        }
                    });
        } else {
            view.askUserToLogIn();
        }
    }

    /**
     * Calls checkImageQuality of UploadMediaPresenter to check image quality of next image
     *
     * @param uploadItemIndex Index of next image, whose quality is to be checked
     */
    @Override
    public void checkImageQuality(int uploadItemIndex) {
        UploadItem uploadItem = repository.getUploadItem(uploadItemIndex);
        presenter.checkImageQuality(uploadItem, uploadItemIndex);
    }


    @Override
    public void deletePictureAtIndex(int index) {
        List<UploadableFile> uploadableFiles = view.getUploadableFiles();
        if (index == uploadableFiles.size() - 1) {
            // If the next fragment to be shown is not one of the MediaDetailsFragment
            // lets hide the top card so that it doesn't appear on the other fragments
            view.showHideTopCard(false);
        }
        view.setImageCancelled(true);
        repository.deletePicture(uploadableFiles.get(index).getFilePath());
        if (uploadableFiles.size() == 1) {
            view.showMessage(R.string.upload_cancelled);
            view.finish();
            return;
        } else {
            if (presenter != null) {
                presenter.updateImageQualitiesJSON(uploadableFiles.size(), index);
            }
            view.onUploadMediaDeleted(index);
            if (!(index == uploadableFiles.size()) && index != 0) {
                // if the deleted image was not the last item to be uploaded, check quality of next
                UploadItem uploadItem = repository.getUploadItem(index);
                presenter.checkImageQuality(uploadItem, index);
            }
        }
        if (uploadableFiles.size() < 2) {
            view.showHideTopCard(false);
        }

        //In case lets update the number of uploadable media
        view.updateTopCardTitle();

    }

    @Override
    public void onAttachView(UploadContract.View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
        compositeDisposable.clear();
        repository.cleanup();
    }

}
