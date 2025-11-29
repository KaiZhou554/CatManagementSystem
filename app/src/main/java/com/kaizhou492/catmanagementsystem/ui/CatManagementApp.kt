package com.kaizhou492.catmanagementsystem.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kaizhou492.catmanagementsystem.data.CatDataManager
import com.kaizhou492.catmanagementsystem.models.Cat
import com.kaizhou492.catmanagementsystem.models.CatteryState
import com.kaizhou492.catmanagementsystem.svg.CatAvatar
import kotlinx.coroutines.launch
import com.kaizhou492.catmanagementsystem.data.WEEKLY_ADOPTION_LIMIT

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                    },
                    onExitGiftMode = {
                        showGiftMode = false
                        selectedForGift = setOf()
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

// 设置页面（将原侧边栏改为二级页面），带进入/退出动画
    AnimatedVisibility(
        visible = showSettings,
        enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
    ) {
        SettingsScreen(
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
            onBack = { showSettings = false }
        )
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
    onConfirmGift: () -> Unit,
    onExitGiftMode: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
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
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
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

                // 添加底部间距
                if (showGiftMode) {
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // 浮动按钮区域
        if (showGiftMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExitGiftMode,
                    modifier = Modifier.weight(1f),
                    // 使用 ButtonDefaults 来设置自定义颜色
                    colors = ButtonDefaults.buttonColors(
                        // 背景色使用次要容器颜色
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        // 内容（文字和图标）颜色使用在次要容器上的颜色
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(strings.exitGift)
                }
                Button(
                    onClick = onConfirmGift,
                    modifier = Modifier.weight(1f),
                    enabled = selectedForGift.isNotEmpty()
                ) {
                    Text(strings.confirm)
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
                MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 使用 SVG 渲染的猫咪头像
            CatAvatar(
                cat = cat,
                size = 64.dp
            )

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
        // 喂养区域：将按钮改为可点击卡片，增加小字描述，禁用时变淡
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (!state.autoFeederEnabled && !state.foodClickedToday) 1f else 0.5f)
                    .clickable(enabled = !state.autoFeederEnabled && !state.foodClickedToday) {
                        onFillFood()
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val enabledFood = !state.autoFeederEnabled && !state.foodClickedToday
                    Icon(
                        Icons.Default.SetMeal,
                        contentDescription = null,
                        tint = if (enabledFood) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.fillFood, style = MaterialTheme.typography.titleMedium)
                        val foodDesc = when {
                            state.autoFeederEnabled -> strings.foodDescAuto
                            state.foodClickedToday -> strings.foodDescAlready
                            else -> strings.foodDescTap
                        }
                        Text(
                            foodDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 点击指示（可选）
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
        }

        // 喂水卡片：增加小字描述，禁用时变淡
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (!state.autoFeederEnabled && !state.waterClickedToday) 1f else 0.5f)
                    .clickable(enabled = !state.autoFeederEnabled && !state.waterClickedToday) {
                        onFillWater()
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val enabledWater = !state.autoFeederEnabled && !state.waterClickedToday
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = if (enabledWater) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.fillWater, style = MaterialTheme.typography.titleMedium)
                        val waterDesc = when {
                            state.autoFeederEnabled -> strings.waterDescAuto
                            state.waterClickedToday -> strings.waterDescAlready
                            else -> strings.waterDescTap
                        }
                        Text(
                            waterDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
        }

        // 收养区域：改为可点击卡片，显示剩余次数提示，禁用时变淡
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (state.adoptionsThisWeek < WEEKLY_ADOPTION_LIMIT) 1f else 0.5f)
                        .clickable(enabled = state.adoptionsThisWeek < WEEKLY_ADOPTION_LIMIT) { onAdoptClick() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val enabledAdopt = state.adoptionsThisWeek < WEEKLY_ADOPTION_LIMIT
                    Icon(Icons.Default.Pets, contentDescription = null, tint = if (enabledAdopt) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.adoptCat, style = MaterialTheme.typography.titleMedium)
                        val left = WEEKLY_ADOPTION_LIMIT - state.adoptionsThisWeek
                        val adoptDesc = if (left > 0) String.format(strings.adoptLeftTemplate, left) else strings.adoptNone
                        Text(
                            text = adoptDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
        }

        // 危险操作区：使用 error 配色的卡片，保持较强视觉提示
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                // 赠送猫（错误色调卡片）: 卡片内包含小字描述，描述放在图标右侧
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGiftClick() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.giftCat, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.giftDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                }

                // 转让猫舍（错误色调卡片）: 卡片内包含小字描述，描述放在图标右侧
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTransferDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.transferCattery, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.transferDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: CatteryState,
    strings: Strings,
    onAutoFeederToggle: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {

    var showLanguageDialog by remember { mutableStateOf(false) }

    // 当 showLanguageDialog 为 true 时，显示 AlertDialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Language / 语言") },
            text = {
                // 对话框内容，提供语言选项
                Column {
                    // 中文选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageChange("zh")
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.language == "zh",
                            onClick = null // 点击事件在 Row 上处理
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("简体中文")
                    }
                    // 英文选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageChange("en")
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.language == "en",
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("English")
                    }
                }
            },
            confirmButton = {
                // 对话框通常只需要一个关闭按钮
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = strings.cancel)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 自动喂养器卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(strings.autoFeeder, style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(
                        checked = state.autoFeederEnabled,
                        onCheckedChange = onAutoFeederToggle
                    )
                }
            }

            // 语言卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true }, // 点击卡片以显示对话框
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Language / 语言", style = MaterialTheme.typography.titleMedium)
                        // 显示当前选择的语言
                        val currentLanguage = if (state.language == "zh") "简体中文" else "English"
                        Text(
                            currentLanguage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Language"
                    )
                }
            }


            // 关于卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.about, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(strings.version, style = MaterialTheme.typography.bodyMedium)
                    Text(strings.developer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}