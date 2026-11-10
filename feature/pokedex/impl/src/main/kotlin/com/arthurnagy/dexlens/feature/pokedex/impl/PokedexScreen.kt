package com.arthurnagy.dexlens.feature.pokedex.impl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.window.core.layout.WindowSizeClass
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.designsystem.CameraColors
import com.arthurnagy.dexlens.core.designsystem.SkeletonBlock
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.ui.LoadingSkeleton
import com.arthurnagy.dexlens.core.ui.LocalSharedTransitionScope
import com.arthurnagy.dexlens.core.ui.MessageState
import com.arthurnagy.dexlens.core.ui.SpeciesArt
import com.arthurnagy.dexlens.core.ui.TypeChipRow
import com.arthurnagy.dexlens.core.ui.labelRes
import com.arthurnagy.dexlens.core.ui.listText
import com.arthurnagy.dexlens.core.ui.speciesNumberLabel
import com.arthurnagy.dexlens.core.ui.toMessage
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.AimBox
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.CameraPermission
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.CameraStatus
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.CameraViewfinder
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.MlKitLineRecognizer
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.ScannerArea
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.ScannerButtons
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.TextFrameAnalyzer
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.scannerAreaHeight
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.topBarAlpha
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import com.arthurnagy.dexlens.core.ui.R as CoreUiR

@Composable
internal fun PokedexScreen(
    onOpenSpecies: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onShowOffer: () -> Unit,
    canNavigateBack: Boolean,
    modifier: Modifier = Modifier,
    viewModel: PokedexViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val textFieldState = rememberTextFieldState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collect(viewModel::onQueryChanged)
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val currentShowOffer by rememberUpdatedState(onShowOffer)
    LaunchedEffect(state.pendingOffer) {
        if (state.pendingOffer) {
            currentShowOffer()
            viewModel.onOfferConsumed()
        }
    }
    LaunchedEffect(state.scan.pendingHaptic) {
        if (state.scan.pendingHaptic) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            viewModel.onHapticConsumed()
        }
    }

    val permission = rememberCameraPermission()
    var torchAvailable by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }

    PokedexContent(
        state = state,
        textFieldState = textFieldState,
        camera = CameraStatus(permission.status, cameraFailed, torchAvailable),
        canNavigateBack = canNavigateBack,
        onOpenSpecies = { id ->
            keyboard?.hide()
            onOpenSpecies(id)
        },
        onOpenSettings = onOpenSettings,
        onRetry = viewModel::onRetry,
        onClearMatch = viewModel::onMatchCleared,
        onToggleTorch = viewModel::onTorchToggled,
        onRequestPermission = permission.request,
        onOpenAppSettings = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        },
        modifier = modifier,
        debugEnabled = BuildConfig.DEBUG,
        viewfinder = { aimBox ->
            val recognizer = remember { MlKitLineRecognizer() }
            DisposableEffect(recognizer) { onDispose { recognizer.close() } }
            val currentBox by rememberUpdatedState(aimBox)
            val analyzer = remember(recognizer) {
                TextFrameAnalyzer(
                    recognizer = recognizer,
                    aimBox = { currentBox },
                    onFrame = { frame -> viewModel.onFrame(frame, debugEnabled = BuildConfig.DEBUG) },
                )
            }
            CameraViewfinder(
                analyzer = analyzer,
                torchOn = state.scan.torchOn,
                onTorchAvailabilityChange = { torchAvailable = it },
                onCameraUnavailable = { cameraFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

/** From this width the status bar also spans the detail pane, so the home screen starts below it. */
@Composable
private fun isWideWindow(): Boolean =
    currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/** The camera area is dark in every theme, so the status bar icons stay light while it is showing. */
@Composable
private fun LightStatusBarIcons() {
    val activity = LocalActivity.current
    val view = LocalView.current
    DisposableEffect(activity, view) {
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { previous?.let { controller.isAppearanceLightStatusBars = it } }
    }
}

private class CameraPermissionAccess(
    val status: CameraPermission,
    val request: () -> Unit,
)

/** Reads the camera permission and refreshes it on every resume. The system dialog appears only when [request] is called. */
@Composable
private fun rememberCameraPermission(): CameraPermissionAccess {
    val context = LocalContext.current
    val activity = LocalActivity.current

    fun hasCamera() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasCamera()) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        permanentlyDenied =
            !ok && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }
    LifecycleResumeEffect(Unit) {
        granted = hasCamera()
        if (granted) permanentlyDenied = false
        onPauseOrDispose {}
    }
    val status = when {
        granted -> CameraPermission.GRANTED
        permanentlyDenied -> CameraPermission.PERMANENTLY_DENIED
        else -> CameraPermission.NEEDS_REQUEST
    }
    return CameraPermissionAccess(status) { launcher.launch(Manifest.permission.CAMERA) }
}

/** Stateless screen body. [viewfinder] is a slot so tests and goldens can run without camera hardware. */
@Composable
internal fun PokedexContent(
    state: PokedexState,
    textFieldState: TextFieldState,
    camera: CameraStatus,
    canNavigateBack: Boolean,
    onOpenSpecies: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onClearMatch: () -> Unit,
    onToggleTorch: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
    debugEnabled: Boolean = false,
    viewfinder: @Composable (AimBox) -> Unit,
) {
    val wide = isWideWindow()
    val topInset = if (wide) AppSpacing.none else WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listActions = ListActions(onOpenSpecies, onRetry, onClearMatch)
    // Nothing but the surface until the switch has been read, so neither layout appears and then jumps.
    val background = if (state.scannerEnabled == true) CameraColors.background else MaterialTheme.colorScheme.surface
    BoxWithConstraints((if (wide) modifier.statusBarsPadding() else modifier).fillMaxSize().background(background)) {
        when (state.scannerEnabled) {
            null -> Unit

            true -> ScannerHome(
                state = state,
                textFieldState = textFieldState,
                camera = camera,
                canNavigateBack = canNavigateBack,
                windowHeight = maxHeight,
                areaHeight = scannerAreaHeight(maxWidth, maxHeight, topInset),
                topInset = topInset,
                listActions = listActions,
                onOpenSettings = onOpenSettings,
                onToggleTorch = onToggleTorch,
                onRequestPermission = onRequestPermission,
                onOpenAppSettings = onOpenAppSettings,
                debugEnabled = debugEnabled,
                viewfinder = viewfinder,
            )

            false -> ListOnlyHome(state, textFieldState, canNavigateBack, topInset, listActions, onOpenSettings)
        }
    }
}

/** What the list and its search field can do, in both layouts. */
private class ListActions(
    val onOpenSpecies: (Int) -> Unit,
    val onRetry: () -> Unit,
    val onClearMatch: () -> Unit,
)

/** The scanner on top and the list in a sheet over it. The sheet's position decides whether the camera runs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannerHome(
    state: PokedexState,
    textFieldState: TextFieldState,
    camera: CameraStatus,
    canNavigateBack: Boolean,
    windowHeight: Dp,
    areaHeight: Dp,
    topInset: Dp,
    listActions: ListActions,
    onOpenSettings: () -> Unit,
    onToggleTorch: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    debugEnabled: Boolean,
    viewfinder: @Composable (AimBox) -> Unit,
) {
    // Text can already be there when the switch is turned on mid-search, and collapsed always means scanning.
    val sheetState = rememberStandardBottomSheetState(
        initialValue = if (textFieldState.text.isEmpty()) SheetValue.PartiallyExpanded else SheetValue.Expanded,
    )
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val collapsed = sheetState.currentValue == SheetValue.PartiallyExpanded && sheetState.targetValue == SheetValue.PartiallyExpanded

    // Every way down ends a search the same way: a drag, a fling at the top of the list, or Back.
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue == SheetValue.PartiallyExpanded && sheetState.targetValue == SheetValue.PartiallyExpanded }
            .drop(1)
            .filter { it }
            .collect {
                textFieldState.clearText()
                focusManager.clearFocus()
                keyboard?.hide()
            }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        // With an entry open beside the list, Back belongs to the entry.
        isBackEnabled = !canNavigateBack && (!collapsed || state.matchListed),
        onBackCompleted = {
            if (collapsed) listActions.onClearMatch() else scope.launch { sheetState.partialExpand() }
        },
    )

    // Runs until the sheet has settled, so the camera fades into the top bar instead of cutting to black first.
    val cameraRunning = sheetState.currentValue == SheetValue.PartiallyExpanded &&
        camera.permission == CameraPermission.GRANTED &&
        !camera.failed &&
        state.scan.error == null
    val stripHeight = topInset + AppDimens.scannerButtonRowHeight
    val collapsedOffset = with(LocalDensity.current) { areaHeight.toPx() }
    val expandedOffset = with(LocalDensity.current) { stripHeight.toPx() }

    // The status bar icons and the buttons change at the fade's midpoint, when the top bar has become the brighter of the two.
    var overTopBar by remember { mutableStateOf(sheetState.currentValue == SheetValue.Expanded) }
    LaunchedEffect(sheetState, collapsedOffset, expandedOffset) {
        withFrameNanos {} // The sheet has no offset until it has been laid out once.
        snapshotFlow { topBarAlpha(sheetState.requireOffset(), collapsedOffset, expandedOffset) > TOP_BAR_MIDPOINT }
            .collect { overTopBar = it }
    }
    if (!isWideWindow() && !overTopBar) LightStatusBarIcons()
    Box(Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState),
            sheetPeekHeight = windowHeight - areaHeight,
            sheetMaxWidth = Dp.Unspecified,
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetShadowElevation = AppSpacing.none,
            containerColor = Color.Transparent,
            sheetContent = {
                // Sized here rather than left to fill, because this is what stops the expanded sheet below the buttons.
                SearchAndList(
                    state = state,
                    textFieldState = textFieldState,
                    actions = listActions,
                    onFocus = { scope.launch { sheetState.expand() } },
                    modifier = Modifier.height(windowHeight - stripHeight - AppDimens.sheetDragHandleHeight),
                )
            },
        ) {
            ScannerArea(
                scan = state.scan,
                camera = camera,
                cameraRunning = cameraRunning,
                topBarAlpha = { topBarAlpha(sheetState.requireOffset(), collapsedOffset, expandedOffset) },
                height = areaHeight,
                topInset = topInset,
                debugEnabled = debugEnabled,
                onRequestPermission = onRequestPermission,
                onOpenAppSettings = onOpenAppSettings,
                viewfinder = viewfinder,
            )
        }
        ScannerButtons(
            showTorch = collapsed && cameraRunning && camera.torchAvailable,
            torchOn = state.scan.torchOn,
            onTopBar = overTopBar,
            topInset = topInset,
            onToggleTorch = onToggleTorch,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

/** The switched-off layout: no scanner and no sheet, so there is nothing to drag, and the camera can't start. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListOnlyHome(
    state: PokedexState,
    textFieldState: TextFieldState,
    canNavigateBack: Boolean,
    topInset: Dp,
    listActions: ListActions,
    onOpenSettings: () -> Unit,
) {
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        // With an entry open beside the list, Back belongs to the entry.
        isBackEnabled = textFieldState.text.isNotEmpty() && !canNavigateBack,
        onBackCompleted = { textFieldState.clearText() },
    )
    Box(Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize().padding(top = topInset + AppDimens.scannerButtonRowHeight),
        ) {
            SearchAndList(
                state = state,
                textFieldState = textFieldState,
                actions = listActions,
                onFocus = {},
                modifier = Modifier.padding(top = AppSpacing.md),
            )
        }
        ScannerButtons(
            showTorch = false,
            torchOn = false,
            onTopBar = true,
            topInset = topInset,
            onToggleTorch = {},
            onOpenSettings = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun SearchAndList(
    state: PokedexState,
    textFieldState: TextFieldState,
    actions: ListActions,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        SearchField(
            textFieldState = textFieldState,
            matchListed = state.matchListed,
            onFocus = onFocus,
            onClearMatch = actions.onClearMatch,
        )
        PokedexBody(state, actions.onOpenSpecies, actions.onRetry)
    }
}

/** The collapsed form of Material's search bar: the results are always the list below it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    textFieldState: TextFieldState,
    matchListed: Boolean,
    onFocus: () -> Unit,
    onClearMatch: () -> Unit,
) {
    SearchBar(
        state = rememberSearchBarState(),
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md).padding(bottom = AppSpacing.sm),
        inputField = {
            SearchBarDefaults.InputField(
                state = textFieldState,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.testTag("pokedex:search").onFocusChanged { if (it.isFocused) onFocus() },
                placeholder = { Text(stringResource(R.string.pokedex_search_hint)) },
                leadingIcon = {
                    Icon(painterResource(CoreUiR.drawable.core_ui_ic_search), contentDescription = null)
                },
                trailingIcon = {
                    if (textFieldState.text.isNotEmpty() || matchListed) {
                        IconButton(
                            onClick = {
                                textFieldState.clearText()
                                onClearMatch()
                            },
                            modifier = Modifier.testTag("pokedex:clear"),
                        ) {
                            Icon(
                                painterResource(CoreUiR.drawable.core_ui_ic_close),
                                contentDescription = stringResource(R.string.pokedex_clear_search),
                            )
                        }
                    }
                },
            )
        },
    )
}

@Composable
private fun PokedexBody(
    state: PokedexState,
    onOpenSpecies: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    val bodyModifier = Modifier.windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
    val listed = state.listed
    when {
        state.loading && listed.isEmpty() -> SpeciesListSkeleton(bodyModifier)

        state.error != null && !state.matchListed -> MessageState(
            title = state.error.toMessage(),
            actionLabel = stringResource(CoreUiR.string.core_ui_retry),
            onAction = onRetry,
            modifier = bodyModifier,
        )

        listed.isEmpty() -> MessageState(
            title = stringResource(R.string.pokedex_empty_title, state.query),
            message = stringResource(R.string.pokedex_empty_message),
            icon = ImageVector.vectorResource(CoreUiR.drawable.core_ui_ic_search),
            modifier = bodyModifier,
        )

        else -> LazyColumn(
            modifier = bodyModifier.fillMaxSize().testTag("pokedex:list"),
            contentPadding = PaddingValues(
                top = AppSpacing.xs,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + AppSpacing.md,
            ),
        ) {
            items(listed, key = { it.summary.id.value }, contentType = { "species" }) { result ->
                SpeciesRow(result, state.artwork, onClick = { onOpenSpecies(result.summary.id.value) }, Modifier.animateItem())
            }
        }
    }
}

/** The list's shape while its data loads: rows laid out like [SpeciesRow]. */
@Composable
private fun SpeciesListSkeleton(modifier: Modifier = Modifier) {
    LoadingSkeleton(modifier) {
        // Taller than most windows on purpose: the rows run off the bottom, like the real list.
        Column(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true).padding(top = AppSpacing.xs)) {
            repeat(SKELETON_ROWS) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    SkeletonBlock(Modifier.size(AppDimens.listArt), RoundedCornerShape(AppDimens.artCorner))
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        SkeletonBlock(Modifier.size(AppDimens.skeletonLineLong, AppDimens.skeletonLineHeight))
                        SkeletonBlock(Modifier.size(AppDimens.skeletonLineShort, AppDimens.typeChipHeight))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SpeciesRow(
    result: SpeciesSearchResult,
    artwork: ArtworkLocation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = result.summary
    val number = speciesNumberLabel(summary.id.value)
    val typeNames = listText(summary.types.map { stringResource(it.labelRes()) })
    val description = stringResource(R.string.pokedex_row_description, number, summary.name, typeNames)
    val sharedScope = LocalSharedTransitionScope.current
    val artModifier = if (sharedScope != null) {
        val animatedScope = LocalNavAnimatedContentScope.current
        with(sharedScope) {
            Modifier.sharedBounds(rememberSharedContentState(key = "species-art-${summary.id.value}"), animatedScope)
        }
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pokedex:row:${summary.id.value}")
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        SpeciesArt(
            name = summary.name,
            types = summary.types,
            imagePath = artwork.artwork(summary.id),
            contentDescription = null,
            letterStyle = MaterialTheme.typography.headlineSmall,
            modifier = artModifier.size(AppDimens.listArt),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(number, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(summary.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            result.matchedName?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TypeChipRow(summary.types)
        }
    }
}

private const val SKELETON_ROWS = 20
private const val TOP_BAR_MIDPOINT = 0.5f
