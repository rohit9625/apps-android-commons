package fr.free.nrw.commons.customselector.ui.screens

import android.graphics.Paint.Align
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.CustomSelectorToolBar
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorViewModel

@Composable
fun ImageGridScreen(
    viewModel: CustomSelectorViewModel,
    bucketName: String = "None",
    onNavigateBack: ()-> Unit = {}
) {
    val images by viewModel.filteredImages.collectAsState()
    var actionedPicturesChecked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { CustomSelectorToolBar(
            title = bucketName,
            onNavigateBack = onNavigateBack
        ) }
    ) {innerPadding ->
        Column(
            modifier = Modifier
                .padding(
                    start = 2.dp,
                    end = 2.dp,
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.show_already_actioned_pictures),
                    fontSize = 16.sp
                )

                Switch(
                    checked = actionedPicturesChecked,
                    onCheckedChange = { actionedPicturesChecked = it },
                    modifier = Modifier.scale(0.75f),
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colorResource(id = R.color.primaryColor),
                        uncheckedThumbColor = colorResource(id = R.color.primaryColor),
                        uncheckedBorderColor = colorResource(id = R.color.primaryColor)
                    )
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(images, key = { it.image.id }) {
                    ImageGridItem(
                        item = it,
                        onClick = { viewModel.toggleImageSelection(it.image.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ImageGridItem(
    item: SelectableImage,
    modifier: Modifier = Modifier,
    onClick: ()-> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box {
            AsyncImage(
                model = item.image.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            if(item.isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Checked",
                    modifier = Modifier
                        .padding(4.dp)
                        .background(color = Color.White, shape = CircleShape)
                        .align(Alignment.TopStart),
                    tint = Color.Black
                )
            }
            if(item.alreadyActioned) {
                Icon(
                    painter = painterResource(R.drawable.commons),
                    contentDescription = "Checked",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
            }
        }
    }
}

data class SelectableImage(
    val image: Image,
    var isSelected: Boolean = false,
    var alreadyActioned: Boolean = false
)

@Preview
@Composable
private fun ImageGridScreenPreview() {
    val isSelected by remember {
        mutableStateOf(false)
    }

    Box(modifier = Modifier) {
        Image(
            painter = painterResource(id = R.drawable.image_placeholder),
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1f),
            contentScale = ContentScale.Crop,
            alpha = 0.8f,
            colorFilter = ColorFilter.tint(color = Color.Black, blendMode = BlendMode.Color)
        )
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Checked",
            modifier = Modifier
                .padding(4.dp)
                .background(color = Color.White, shape = CircleShape)
                .align(Alignment.TopStart),
            tint = Color.Black
        )
    }
}