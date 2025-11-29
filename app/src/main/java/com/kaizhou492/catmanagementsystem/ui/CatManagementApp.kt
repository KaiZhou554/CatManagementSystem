package com.kaizhou492.catmanagementsystem.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kaizhou492.catmanagementsystem.data.CatDataManager
import com.kaizhou492.catmanagementsystem.models.Cat
import com.kaizhou492.catmanagementsystem.models.CatteryState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatManagementApp(dataManager: CatDataManager) {
    val scope = rememberCoroutineScope()
    val state by dataManager.stateFlow.collectAsState(initial = CatteryState())

    var activeTab by remember { mutableStateOf("cattery") }
    var showSettings by remember { mutableStateOf(false) }
    var showAdoptDialog by remember { mutableStateOf(false) }
    var showGiftMode by remember { mutableStateOf(false) }
    var selectedForGift by remember { mutableStateOf(setOf<Long>()) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var editingCatId by remember { mutableStateOf<Long?>(null) }
    var editingName by remember { mutableStateOf("") }

    val strings = if (state.language == "zh") StringsZh else StringsEn

    val snackbarHostState = remember { SnackbarHostState() }

    // 显示提示信息
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.appTitle) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = strings.settings)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == "cattery",
                    onClick = {
                        activeTab = "cattery"
                        showGiftMode = false
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(strings.cattery) }
                )
                NavigationBarItem(
                    selected = activeTab == "office",
                    onClick = {
                        activeTab = "office"
                        showGiftMode = false
                    },
                    icon = { Icon(Icons.Default.Work, contentDescription = null) },
                    label = { Text(strings.office) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                "cattery" -> CatteryScreen(
                    state = state,
                    strings = strings,
                    showGiftMode = showGiftMode,
                    selectedForGift = selectedForGift,
                    editingCatId = editingCatId,
                    editingName = editingName,
                    onCatClick = { cat ->
                        if (!cat.interacted) {
                            scope.launch {
                                dataManager.interactWithCat(cat.id)
                            }
                        }
                    },
                    onNameClick = { cat ->
                        editingCatId = cat.id
                        editingName = cat.name
                    },
                    onNameChange = { editingName = it },
                    onNameSave = {
                        editingCatId?.let { catId ->
                            scope.launch {
                                val result = dataManager.updateCatName(catId, editingName)
                                result.fold(
                                    onSuccess = {
                                        editingCatId = null
                                        editingName = ""
                                    },
                                    onFailure = { e ->
                                        snackbarMessage = when (e.message) {
                                            "name_empty" -> strings.emptyName
                                            "name_invalid" -> strings.invalidName
                                            "name_exists" -> strings.nameExists
                                            else -> e.message
                                        }
                                    }
                                )
                            }
                        }
                    },
                    onGiftSelect = { catId ->
                        selectedForGift = if (catId in selectedForGift) {
                            selectedForGift - catId
                        } else {
                            selectedForGift + catId
                        }
                    },
                    onConfirmGift = {
                        scope.launch {
                            dataManager.giftCats(selectedForGift.toList())
                            selectedForGift = setOf()
                            showGiftMode = false
                            snackbarMessage = strings.giftSuccess
                        }
                    }
                )

                "office" -> OfficeScreen(
                    state = state,
                    strings = strings,
                    onFillFood = {
                        scope.launch {
                            dataManager.fillFoodBowl()
                        }
                    },
                    onFillWater = {
                        scope.launch {
                            dataManager.fillWaterBowl()
                        }
                    },
                    onAdoptClick = { showAdoptDialog = true },
                    onGiftClick = { showGiftMode = true; activeTab = "cattery" },
                    onTransferClick = {
                        scope.launch {
                            dataManager.transferCattery()
                            snackbarMessage = strings.transferSuccess
                        }
                    }
                )
            }
        }
    }

// 设置侧边栏
    if (showSettings) {
        // 1. 将 drawerState 提取出来，以便在 LaunchedEffect 中访问
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)

        // 2. 使用 LaunchedEffect 监听抽屉状态
        //    当 drawerState.isClosed 变为 true 时，将 showSettings 设为 false
        LaunchedEffect(drawerState.isClosed) {
            if (drawerState.isClosed) {
                showSettings = false
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState, // 使用我们上面定义的 state
            drawerContent = {
                SettingsDrawer(
                    state = state,
                    strings = strings,
                    onAutoFeederToggle = { enabled ->
                        scope.launch {
                            dataManager.toggleAutoFeeder(enabled)
                        }
                    },
                    onLanguageChange = { lang ->
                        scope.launch {
                            dataManager.setLanguage(lang)
                        }
                    },
                    // 当在侧边栏内部点击关闭时，主动将 showSettings 设为 false
                    onDismiss = { showSettings = false }
                )
            },
            // gesturesEnabled 默认就是 true，可以不写
            // gesturesEnabled = true
        ) {
            // 这个 content lambda 是为主屏幕内容准备的，
            // 但在你的结构中，主屏幕内容由 Scaffold 管理。
            // 所以这里保持为空是正确的。
        }
    }

    // 收养确认对话框
    if (showAdoptDialog) {
        AlertDialog(
            onDismissRequest = { showAdoptDialog = false },
            title = { Text(strings.confirmAdopt) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val result = dataManager.adoptCat()
                            result.fold(
                                onSuccess = {
                                    snackbarMessage = strings.adoptSuccess
                                    showAdoptDialog = false
                                },
                                onFailure = { e ->
                                    snackbarMessage = if (e.message == "adoption_limit_reached") {
                                        strings.adoptLimitReached
                                    } else {
                                        e.message
                                    }
                                    showAdoptDialog = false
                                }
                            )
                        }
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdoptDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun CatteryScreen(
    state: CatteryState,
    strings: Strings,
    showGiftMode: Boolean,
    selectedForGift: Set<Long>,
    editingCatId: Long?,
    editingName: String,
    onCatClick: (Cat) -> Unit,
    onNameClick: (Cat) -> Unit,
    onNameChange: (String) -> Unit,
    onNameSave: () -> Unit,
    onGiftSelect: (Long) -> Unit,
    onConfirmGift: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (state.cats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐱", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        strings.noCats,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.cats, key = { it.id }) { cat ->
                    CatCard(
                        cat = cat,
                        showGiftMode = showGiftMode,
                        isSelected = cat.id in selectedForGift,
                        isEditing = editingCatId == cat.id,
                        editingName = editingName,
                        onCatClick = onCatClick,
                        onNameClick = onNameClick,
                        onNameChange = onNameChange,
                        onNameSave = onNameSave,
                        onGiftSelect = onGiftSelect
                    )
                }

                if (showGiftMode) {
                    item {
                        Button(
                            onClick = onConfirmGift,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedForGift.isNotEmpty()
                        ) {
                            Text(strings.confirm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CatCard(
    cat: Cat,
    showGiftMode: Boolean,
    isSelected: Boolean,
    isEditing: Boolean,
    editingName: String,
    onCatClick: (Cat) -> Unit,
    onNameClick: (Cat) -> Unit,
    onNameChange: (String) -> Unit,
    onNameSave: () -> Unit,
    onGiftSelect: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 猫咪图标
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Color(android.graphics.Color.parseColor(cat.skinColor))
                            .copy(
                                alpha = cat.saturation,
                                red = cat.brightness
                            )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🐱", style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 名字和品种
            Column(modifier = Modifier.weight(1f)) {
                if (isEditing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = editingName,
                            onValueChange = onNameChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = onNameSave) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                } else {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onNameClick(cat) }
                    )
                }
                Text(
                    text = cat.breed,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 互动按钮或选择框
            if (showGiftMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onGiftSelect(cat.id) }
                )
            } else {
                IconButton(
                    onClick = { onCatClick(cat) },
                    enabled = !cat.interacted
                ) {
                    if (cat.interacted && cat.emoji != null) {
                        Text(cat.emoji!!, style = MaterialTheme.typography.headlineSmall)
                    } else {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Interact",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfficeScreen(
    state: CatteryState,
    strings: Strings,
    onFillFood: () -> Unit,
    onFillWater: () -> Unit,
    onAdoptClick: () -> Unit,
    onGiftClick: () -> Unit,
    onTransferClick: () -> Unit
) {
    var showTransferDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 喂养区域
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onFillFood,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.autoFeederEnabled && !state.foodClickedThisWeek
                    ) {
                        Icon(Icons.Default.SetMeal, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.fillFood)
                    }

                    Button(
                        onClick = onFillWater,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.autoFeederEnabled && !state.waterClickedThisWeek
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.fillWater)
                    }
                }
            }
        }

        // 收养区域
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAdoptClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.adoptionsThisWeek < 3
                    ) {
                        Icon(Icons.Default.Pets, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.adoptCat)
                    }

                    Text(
                        text = "${strings.adoptionsLeft}: ${3 - state.adoptionsThisWeek}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        // 危险操作区
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            strings.dangerZone,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedButton(
                        onClick = onGiftClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.giftCat)
                    }

                    OutlinedButton(
                        onClick = { showTransferDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.transferCattery)
                    }
                }
            }
        }
    }

    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text(strings.confirmTransfer) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTransferClick()
                        showTransferDialog = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun SettingsDrawer(
    state: CatteryState,
    strings: Strings,
    onAutoFeederToggle: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                strings.settings,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Divider()

            // 自动喂养器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.autoFeeder)
                Switch(
                    checked = state.autoFeederEnabled,
                    onCheckedChange = onAutoFeederToggle
                )
            }

            Divider()

            // 语言选择
            Text(
                "Language / 语言",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.language == "zh",
                    onClick = { onLanguageChange("zh") },
                    label = { Text("简体中文") }
                )
                FilterChip(
                    selected = state.language == "en",
                    onClick = { onLanguageChange("en") },
                    label = { Text("English") }
                )
            }

            Divider()

            // 关于
            Column {
                Text(
                    strings.about,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    strings.version,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    strings.developer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}