package fr.free.nrw.commons.delete;

import android.content.Context;

import fr.free.nrw.commons.utils.DateUtil;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.profile.achievements.FeedbackResponse;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.Single;
import timber.log.Timber;

/**
 * This class handles the reason for deleting a Media object
 */
@Singleton
public class ReasonBuilder {

    private SessionManager sessionManager;
    private OkHttpJsonApiClient okHttpJsonApiClient;
    private Context context;
    private ViewUtilWrapper viewUtilWrapper;

    @Inject
    public ReasonBuilder(Context context,
                         SessionManager sessionManager,
                         OkHttpJsonApiClient okHttpJsonApiClient,
                         ViewUtilWrapper viewUtilWrapper) {
        this.context = context;
        this.sessionManager = sessionManager;
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.viewUtilWrapper = viewUtilWrapper;
    }

    /**
     * To process the reason and append the media's upload date and uploaded_by_me string
     * @param media
     * @param reason
     * @return  
     */
    public Single<String> getReason(Media media, String reason) {
        return fetchArticleNumber(media, reason);
    }

    /**
     * get upload date for the passed Media
     */
    private String prettyUploadedDate(Media media) {
        Date date = media.getDateUploaded();
        if (date == null || date.toString() == null || date.toString().isEmpty()) {
            return "Uploaded date not available";
        }
        return DateUtil.getDateStringWithSkeletonPattern(date,"dd MMM yyyy");
    }

    private Single<String> fetchArticleNumber(Media media, String reason) {
        if (checkAccount()) {
            return okHttpJsonApiClient
                    .getAchievements(sessionManager.getUserName())
                    .map(feedbackResponse -> appendArticlesUsed(feedbackResponse, media, reason));
        }
        return Single.just("");
    }

    /**
     * Takes the uploaded_by_me string, the upload date, name of articles using images
     * and appends it to the received reason
     * @param feedBack object
     * @param media whose upload data is to be fetched
     * @param reason 
     */
    private String appendArticlesUsed(FeedbackResponse feedBack, Media media, String reason) {
        String reason1Template = context.getString(R.string.uploaded_by_myself);
        reason += String.format(Locale.getDefault(), reason1Template, prettyUploadedDate(media), feedBack.getArticlesUsingImages());
        Timber.i("New Reason %s", reason);
        return reason;
    }

    /**
     * check to ensure that user is logged in
     * @return
     */
    private boolean checkAccount(){
        if (!sessionManager.doesAccountExist()) {
            Timber.d("Current account is null");
            viewUtilWrapper.showLongToast(context, context.getResources().getString(R.string.user_not_logged_in));
            sessionManager.forceLogin(context);
            return false;
        }
        return true;
    }
}
