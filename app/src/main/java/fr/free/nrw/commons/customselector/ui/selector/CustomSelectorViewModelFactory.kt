package fr.free.nrw.commons.customselector.ui.selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.contributions.ContributionDao
import javax.inject.Inject

/**
 * View Model Factory.
 */
class CustomSelectorViewModelFactory @Inject constructor(
    private val imageFileLoader: ImageFileLoader,
    private val contributionDao: ContributionDao
) : ViewModelProvider.Factory {
    override fun<CustomSelectorViewModel: ViewModel> create(
        modelClass: Class<CustomSelectorViewModel>
    ): CustomSelectorViewModel {
        return CustomSelectorViewModel(imageFileLoader, contributionDao) as CustomSelectorViewModel
    }

}