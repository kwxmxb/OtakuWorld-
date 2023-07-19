package com.programmersbox.uiviews.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SendTimeExtension
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.models.ApiServicesCatalog
import com.programmersbox.models.RemoteSources
import com.programmersbox.models.SourceInformation
import com.programmersbox.models.sourceFlow
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.all.pagerTabIndicatorOffset
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.DownloadUpdate
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.PreviewTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExtensionList(
    sourceRepository: SourceRepository = LocalSourcesRepository.current,
    viewModel: ExtensionListViewModel = viewModel { ExtensionListViewModel(sourceRepository) }
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { 2 }

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                InsetSmallTopAppBar(
                    title = { Text(stringResource(R.string.extensions)) },
                    navigationIcon = { BackButton() },
                    scrollBehavior = scrollBehavior,
                )
                TabRow(
                    // Our selected tab is our current page
                    selectedTabIndex = pagerState.currentPage,
                    // Override the indicator, using the provided pagerTabIndicatorOffset modifier
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                        )
                    }
                ) {
                    // Add tabs for all of our pages
                    LeadingIconTab(
                        text = { Text(stringResource(R.string.installed)) },
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        icon = { Icon(Icons.Default.Extension, null) }
                    )

                    LeadingIconTab(
                        text = { Text(stringResource(R.string.extensions)) },
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        icon = { Icon(Icons.Default.SendTimeExtension, null) }
                    )
                }
            }
        },
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            contentPadding = paddingValues,
        ) { page ->
            when (page) {
                0 -> InstalledExtensionItems(
                    installedSources = viewModel.installed,
                )

                1 -> RemoteExtensionItems(
                    remoteSources = viewModel.remoteSources,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstalledExtensionItems(
    installedSources: Map<ApiServicesCatalog?, InstalledViewState>,
) {
    val context = LocalContext.current
    fun uninstall(packageName: String) {
        val uri = Uri.fromParts("package", packageName, null)
        val uninstall = Intent(Intent.ACTION_DELETE, uri)
        context.startActivity(uninstall)
    }
    Column {
        ListItem(
            headlineContent = {
                val source by sourceFlow.collectAsState(initial = null)
                Text(stringResource(R.string.currentSource, source?.serviceName.orEmpty()))
            }
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            installedSources.forEach { (t, u) ->
                stickyHeader {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 4.dp,
                        onClick = { u.showItems = !u.showItems },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            modifier = Modifier.padding(4.dp),
                            headlineContent = {
                                Text(t?.name ?: u.sourceInformation.firstOrNull()?.name?.takeIf { t != null } ?: "Single Source")
                            },
                            leadingContent = { Text("(${u.sourceInformation.size})") },
                            trailingContent = t?.let {
                                {
                                    IconButton(
                                        onClick = { uninstall(u.sourceInformation.random().packageName) }
                                    ) { Icon(Icons.Default.Delete, null) }
                                }
                            }
                        )
                    }
                }

                if (u.showItems)
                    items(u.sourceInformation) {
                        ExtensionItem(
                            sourceInformation = it,
                            onClick = { sourceFlow.tryEmit(it.apiService) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { uninstall(it.packageName) }
                                ) { Icon(Icons.Default.Delete, null) }
                            }
                        )
                    }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RemoteExtensionItems(
    remoteSources: Map<String, RemoteViewState>,
) {
    Column {
        var search by remember { mutableStateOf("") }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search Remote Extensions") },
            trailingIcon = {
                IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, null) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            remoteSources.forEach { (t, u) ->
                stickyHeader {
                    InsetSmallTopAppBar(
                        title = { Text(t) },
                        insetPadding = WindowInsets(0.dp),
                        navigationIcon = {

                            Text("(${u.sources.size})")
                        },
                        actions = {
                            IconButton(
                                onClick = { u.showItems = !u.showItems }
                            ) { Icon(Icons.Default.ArrowDropDown, null) }
                        }
                    )
                }

                if (u.showItems)
                    items(u.sources.filter { it.name.contains(search, true) }) {
                        RemoteItem(
                            remoteSource = it,
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionItem(
    sourceInformation: SourceInformation,
    onClick: () -> Unit,
    trailingIcon: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        ListItem(
            headlineContent = { Text(sourceInformation.apiService.serviceName) },
            leadingContent = { Image(rememberDrawablePainter(drawable = sourceInformation.icon), null) },
            trailingContent = trailingIcon
        )
    }
}

@Composable
private fun RemoteItem(
    remoteSource: RemoteSources,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Download and Install ${remoteSource.name}?") },
            icon = { AsyncImage(model = remoteSource.iconUrl, contentDescription = null) },
            text = { Text("Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        println(remoteSource.downloadLink)
                        //Get installing working!
                        DownloadUpdate(context, context.packageName)
                            .downloadUpdate(
                                remoteSource.downloadLink,
                                remoteSource.downloadLink.toUri().lastPathSegment ?: "${remoteSource.name}.apk"
                            )
                        showDialog = false
                    }
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) { Text("No") }
            }
        )
    }
    OutlinedCard(
        modifier = modifier
    ) {
        ListItem(
            headlineContent = { Text(remoteSource.name) },
            leadingContent = { AsyncImage(model = remoteSource.iconUrl, contentDescription = null) },
            trailingContent = {
                IconButton(
                    onClick = { showDialog = true }
                ) { Icon(Icons.Default.InstallMobile, null) }
            }
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun ExtensionListPreview() {
    PreviewTheme {
        ExtensionList()
    }
}