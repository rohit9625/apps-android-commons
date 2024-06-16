package fr.free.nrw.commons.customselector.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Response
import fr.free.nrw.commons.customselector.ui.CustomSelectorToolBar
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorViewModel

@Composable
fun CustomSelectorScreen(
    viewModel: CustomSelectorViewModel,
    onBack: () -> Unit = {},
    onFolderClick: (String) -> Unit,
) {
    val uiState by viewModel.folderState.collectAsState()

    Scaffold(
        topBar = { CustomSelectorToolBar(
            title = stringResource(R.string.custom_selector_title),
            onNavigateBack = onBack
        ) }
    ) {innerPadding->
        Box(
            modifier = Modifier
                .padding(
                    start = 2.dp,
                    end = 2.dp,
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when(uiState) {
                is Response.Success -> {
                    uiState.data?.let {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(it, key = { it.bucketId }) {
                                FolderItem(
                                    folder = it,
                                    preview = it.images.first().uri,
                                    onClick = {
                                        onFolderClick(it.name)
                                        viewModel.filterImagesByBucket(it.bucketId)
                                    }
                                )
                            }
                        }
                    }
                }

                is Response.Error -> {
                    uiState.error?.let {
                        Text(text = it)
                    }
                }
                is Response.Loading -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun FolderItem(
    folder: Folder,
    preview: Uri,
    onClick: ()-> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box {
            AsyncImage(
                model = preview,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.25f))
            ) {
                Text(
                    text = folder.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun SelectorGridItemPreview() {
    val folder = Folder(bucketId = 1235L, name = "My Folder 1")
//    FolderItem(folder = folder, onClick = {})
}

@Preview
@Composable
private fun CustomSelectorScreenPreview() {
//    CustomSelectorScreen()
}