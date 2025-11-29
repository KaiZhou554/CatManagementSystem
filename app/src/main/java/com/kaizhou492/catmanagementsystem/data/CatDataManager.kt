package com.kaizhou492.catmanagementsystem.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kaizhou492.catmanagementsystem.models.Cat
import com.kaizhou492.catmanagementsystem.models.CatteryState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cat_management")

class CatDataManager(private val context: Context) {

    private val STATE_KEY = stringPreferencesKey("cattery_state")

    // 猫咪品种定义
    data class BreedInfo(
        val name: String,
        val colors: List<String>,
        val rareEyeColor: String
    )

    private val breeds = listOf(
        BreedInfo("橘猫", listOf("#FFA500", "#FF8C00", "#CD5700"), "#8B4513"),
        BreedInfo("布偶猫", listOf("#F5E6D3", "#E8D5C4"), "#B0C4DE"),
        BreedInfo("暹罗猫", listOf("#F5DEB3", "#D2B48C"), "#9370DB"),
        BreedInfo("蓝猫", listOf("#708090", "#778899"), "#FF8C00"),
        BreedInfo("三花猫", listOf("#FFFFFF", "#000000", "#FFA500"), "#00CED1"),
        BreedInfo("无毛猫", listOf("#FFE4C4", "#F5DEB3"), "#FFD700"),
        BreedInfo("奶牛猫", listOf("#000000", "#FFFFFF"), "#87CEEB"),
        BreedInfo("狸花猫", listOf("#8B7355", "#A0826D"), "#DAA520"),
        BreedInfo("缅因猫", listOf("#8B4513", "#A0522D"), "#87CEEB")
    )

    private val defaultColors = listOf(
        "#90EE90", "#D2B48C", "#FFD700", "#FFFFE0",
        "#DAA520", "#FFA500", "#CD853F"
    )

    private val defaultRareColors = listOf("#4169E1", "#DDA0DD", "#FFB6C1")

    private val emojis = listOf(
        "😊", "😺", "😸", "😻", "🥰", "😽", "🤗", "💖", "✨", "🌟"
    )

    val stateFlow: Flow<CatteryState> = context.dataStore.data.map { prefs ->
        val json = prefs[STATE_KEY]
        if (json != null) {
            try {
                CatteryState.fromJson(json)
            } catch (e: Exception) {
                CatteryState()
            }
        } else {
            CatteryState()
        }
    }

    suspend fun getState(): CatteryState {
        return stateFlow.first()
    }

    suspend fun saveState(state: CatteryState) {
        context.dataStore.edit { prefs ->
            prefs[STATE_KEY] = state.toJson()
        }
    }

    // 生成随机名字
    fun generateRandomName(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        return (1..6).map { chars.random() }.joinToString("")
    }

    // 收养猫咪
    suspend fun adoptCat(): Result<Cat> {
        val state = getState()
        val currentTime = System.currentTimeMillis()

        // 检查是否需要重置周计数（简化版：7天一周）
        // 仅重置收养相关的每周计数，不再把每日的食物/水标记放在这里重置
        val newState = if (currentTime - state.weekStartTime > 7 * 24 * 60 * 60 * 1000L) {
            state.copy(
                adoptionsThisWeek = 0,
                weekStartTime = currentTime
            )
        } else {
            state
        }

        // 检查收养次数
        if (newState.adoptionsThisWeek >= 93) {
            return Result.failure(Exception("adoption_limit_reached"))
        }

        // 计算是否抽到品种猫
        var breedProbability = 0.15

        // 小保底：连续5只纯色猫后，品种猫概率提升至30%
        if (newState.pityCounter >= 5) {
            breedProbability = 0.30
        }

        // 大保底：第10只必出品种猫
        val isBreedCat = if (newState.guaranteeCounter >= 9) {
            true
        } else {
            Random.nextDouble() < breedProbability
        }

        // 计算稀有瞳色概率
        val rareEyesProbability = if (isBreedCat) 0.20 else 0.10
        val isRareEyes = Random.nextDouble() < rareEyesProbability

        val (breed, skinColor, eyeColor) = if (isBreedCat) {
            val breedInfo = breeds.random()
            val skin = breedInfo.colors.random()
            val eye = if (isRareEyes) breedInfo.rareEyeColor else breedInfo.colors.first()
            Triple(breedInfo.name, skin, eye)
        } else {
            val skin = defaultColors.random()
            val eye = if (isRareEyes) defaultRareColors.random() else defaultColors.random()
            Triple("默认猫咪", skin, eye)
        }

        val newCat = Cat(
            id = currentTime,
            name = generateRandomName(),
            breed = breed,
            skinColor = skinColor,
            eyeColor = eyeColor,
            lastFedTime = currentTime
        )

        // 更新保底计数
        val newPityCounter = if (isBreedCat) 0 else newState.pityCounter + 1
        val newGuaranteeCounter = if (isBreedCat) 0 else newState.guaranteeCounter + 1

        val updatedState = newState.copy(
            cats = newState.cats + newCat,
            adoptionsThisWeek = newState.adoptionsThisWeek + 1,
            pityCounter = newPityCounter,
            guaranteeCounter = newGuaranteeCounter
        )

        saveState(updatedState)
        return Result.success(newCat)
    }

    // 互动猫咪
    suspend fun interactWithCat(catId: Long): Result<Unit> {
        val state = getState()
        val currentTime = System.currentTimeMillis()

        val updatedCats = state.cats.map { cat ->
            if (cat.id == catId && !cat.interacted) {
                cat.copy(
                    interacted = true,
                    emoji = emojis.random(),
                    interactionResetTime = currentTime + 24 * 60 * 60 * 1000L // 次日重置
                )
            } else {
                cat
            }
        }

        saveState(state.copy(cats = updatedCats))
        return Result.success(Unit)
    }

    // 更新猫咪名字
    suspend fun updateCatName(catId: Long, newName: String): Result<Unit> {
        val state = getState()

        // 校验名字
        if (newName.isBlank()) {
            return Result.failure(Exception("name_empty"))
        }

        if (newName.contains(Regex("[!@#\$%^&*()_+\\-=\\[\\]{}|;:,.<>?]"))) {
            return Result.failure(Exception("name_invalid"))
        }

        if (state.cats.any { it.id != catId && it.name == newName }) {
            return Result.failure(Exception("name_exists"))
        }

        val updatedCats = state.cats.map { cat ->
            if (cat.id == catId) cat.copy(name = newName) else cat
        }

        saveState(state.copy(cats = updatedCats))
        return Result.success(Unit)
    }

    // 填猫粮碗
    suspend fun fillFoodBowl(): Result<Unit> {
        val state = getState()
        val currentTime = System.currentTimeMillis()

        // 检查并按天重置每天的标记（24小时）
        val baseState = if (currentTime - state.dayStartTime > 24 * 60 * 60 * 1000L) {
            state.copy(
                dayStartTime = currentTime,
                foodClickedToday = false,
                waterClickedToday = false
            )
        } else {
            state
        }

        val newState = baseState.copy(foodClickedToday = true)

        // 如果水盆也在同一天被点击了，重置所有猫咪状态
        if (newState.waterClickedToday) {
            val updatedCats = newState.cats.map { cat ->
                cat.copy(
                    lastFedTime = currentTime,
                    brightness = 1f,
                    saturation = 1f
                )
            }
            saveState(newState.copy(cats = updatedCats))
        } else {
            saveState(newState)
        }

        return Result.success(Unit)
    }

    // 填水盆
    suspend fun fillWaterBowl(): Result<Unit> {
        val state = getState()
        val currentTime = System.currentTimeMillis()

        // 检查并按天重置每天的标记（24小时）
        val baseState = if (currentTime - state.dayStartTime > 24 * 60 * 60 * 1000L) {
            state.copy(
                dayStartTime = currentTime,
                foodClickedToday = false,
                waterClickedToday = false
            )
        } else {
            state
        }

        val newState = baseState.copy(waterClickedToday = true)

        // 如果猫粮碗也在同一天被点击了，重置所有猫咪状态
        if (newState.foodClickedToday) {
            val updatedCats = newState.cats.map { cat ->
                cat.copy(
                    lastFedTime = currentTime,
                    brightness = 1f,
                    saturation = 1f
                )
            }
            saveState(newState.copy(cats = updatedCats))
        } else {
            saveState(newState)
        }

        return Result.success(Unit)
    }

    // 赠送猫咪
    suspend fun giftCats(catIds: List<Long>): Result<Unit> {
        val state = getState()
        val updatedCats = state.cats.filter { it.id !in catIds }
        saveState(state.copy(cats = updatedCats))
        return Result.success(Unit)
    }

    // 转让猫舍
    suspend fun transferCattery(): Result<Unit> {
        val currentTime = System.currentTimeMillis()
        // 转让时重置周计数和当天标记
        val old = getState()
        saveState(CatteryState(
            weekStartTime = currentTime,
            dayStartTime = currentTime,
            language = old.language // 保留语言设置
        ))
        return Result.success(Unit)
    }

    // 切换自动喂养器
    suspend fun toggleAutoFeeder(enabled: Boolean): Result<Unit> {
        val state = getState()
        val currentTime = System.currentTimeMillis()

        // 如果开启自动喂养器，重置所有猫咪状态
        val updatedCats = if (enabled) {
            state.cats.map { cat ->
                cat.copy(
                    lastFedTime = currentTime,
                    brightness = 1f,
                    saturation = 1f
                )
            }
        } else {
            state.cats
        }

        saveState(state.copy(
            autoFeederEnabled = enabled,
            cats = updatedCats
        ))
        return Result.success(Unit)
    }

    // 切换语言
    suspend fun setLanguage(language: String): Result<Unit> {
        val state = getState()
        saveState(state.copy(language = language))
        return Result.success(Unit)
    }

    // 更新猫咪状态（检查饥饿和状态降低）
    suspend fun updateCatStates(): Result<Unit> {
        val state = getState()

        // 如果开启自动喂养器，不更新状态
        if (state.autoFeederEnabled) {
            return Result.success(Unit)
        }

        val currentTime = System.currentTimeMillis()
        var needsUpdate = false

        val updatedCats = state.cats.map { cat ->
            val timeSinceLastFed = currentTime - cat.lastFedTime

            // 异常处理：如果时间戳在未来，重置为当前时间
            if (timeSinceLastFed < 0) {
                needsUpdate = true
                return@map cat.copy(lastFedTime = currentTime)
            }

            // 超过7天，降低饱和度
            val newSaturation = if (timeSinceLastFed > 7 * 24 * 60 * 60 * 1000L) {
                0.5f
            } else {
                cat.saturation
            }

            // 超过14天，降低亮度
            val newBrightness = if (timeSinceLastFed > 14 * 24 * 60 * 60 * 1000L) {
                0.7f
            } else {
                cat.brightness
            }

            // 检查互动重置时间
            val (interacted, emoji, resetTime) = if (cat.interactionResetTime != null) {
                if (currentTime >= cat.interactionResetTime!!) {
                    Triple(false, null, null)
                } else {
                    Triple(cat.interacted, cat.emoji, cat.interactionResetTime)
                }
            } else {
                Triple(cat.interacted, cat.emoji, cat.interactionResetTime)
            }

            if (newSaturation != cat.saturation || newBrightness != cat.brightness ||
                interacted != cat.interacted) {
                needsUpdate = true
            }

            cat.copy(
                saturation = newSaturation,
                brightness = newBrightness,
                interacted = interacted,
                emoji = emoji,
                interactionResetTime = resetTime
            )
        }

        if (needsUpdate) {
            saveState(state.copy(cats = updatedCats))
        }

        return Result.success(Unit)
    }
}