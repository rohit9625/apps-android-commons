package fr.free.nrw.commons.customselector.ui

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import fr.free.nrw.commons.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSelectorToolBar(
    title: String,
    onNavigateBack: ()-> Unit,
    modifier: Modifier = Modifier,
    showAlert: Boolean = false
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = title)
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        actions = {
            if(showAlert) {
                IconButton(onClick = {  }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_error_red_24dp),
                        contentDescription = stringResource(R.string.custom_selector_limit_error_desc),
                        tint = Color.Unspecified
                    )
                }
            }
        }
    )
}