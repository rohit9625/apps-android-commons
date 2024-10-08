package fr.free.nrw.commons.explore.depictions.media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.depictions.search.LoadFunction
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.media.WikidataMediaClient
import javax.inject.Inject

class PageableDepictedMediaDataSource
    @Inject
    constructor(
        liveDataConverter: LiveDataConverter,
        private val wikiMediaClient: WikidataMediaClient,
    ) : PageableBaseDataSource<Media>(liveDataConverter) {
        override val loadFunction: LoadFunction<Media> = { loadSize: Int, startPosition: Int ->
            wikiMediaClient.fetchImagesForDepictedItem(query, loadSize, startPosition).blockingGet()
        }
    }
