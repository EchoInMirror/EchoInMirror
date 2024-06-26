package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessorDescription
import com.eimsound.audioprocessor.AudioProcessorDescriptionAndFactory
import com.eimsound.audioprocessor.AudioProcessorFactory
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.daw.FAVORITE_AUDIO_PROCESSORS_PATH
import com.eimsound.daw.components.*
import com.eimsound.daw.components.dragdrop.GlobalDraggable
import com.eimsound.daw.components.utils.warning
import com.eimsound.daw.commons.IDisplayName
import com.eimsound.daw.utils.mutableStateSetOf
import com.eimsound.daw.commons.json.toJson
import com.eimsound.daw.language.langs
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Files
import kotlin.io.path.outputStream

private val TOP_TEXTFIELD_HEIGHT = 60.dp
private val BOTTOM_TEXTFIELD_HEIGHT = 60.dp
private val SUB_PADDING = 5.dp
private val KEY = Any()

private var searchText by mutableStateOf("")
private var selectedFactory by mutableStateOf<Any?>(null)
private var selectedManufacturer by mutableStateOf<String?>(null)
private var selectedCategory by mutableStateOf<String?>(null)
private var selectedInstrument by mutableStateOf<Boolean?>(null)
private val favoriteAudioProcessors = mutableStateSetOf<Pair<String, String>>()
private var favoriteAudioProcessorsLoaded = false
private var favoriteAudioProcessorsActuallyLoaded by mutableStateOf(false)

@OptIn(DelicateCoroutinesApi::class)
private fun loadFavoriteAudioProcessors() {
    if (favoriteAudioProcessorsLoaded) return
    favoriteAudioProcessorsLoaded = true
    GlobalScope.launch {
        withContext(Dispatchers.IO) {
            try {
                if (Files.exists(FAVORITE_AUDIO_PROCESSORS_PATH)) {
                    favoriteAudioProcessors.addAll(FAVORITE_AUDIO_PROCESSORS_PATH.toFile()
                        .toJson<List<Pair<String, String>>>())
                }
                favoriteAudioProcessorsActuallyLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
                Files.delete(FAVORITE_AUDIO_PROCESSORS_PATH)
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalSerializationApi::class)
private fun saveFavoriteAudioProcessors() {
    GlobalScope.launch {
        withContext(Dispatchers.IO) {
            try {
                Json.encodeToStream(favoriteAudioProcessors.toList(), FAVORITE_AUDIO_PROCESSORS_PATH.outputStream())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun FloatingLayerProvider.openQuickLoadDialog(onClose: ((AudioProcessorDescriptionAndFactory?) -> Unit)? = null) {
    if (closeFloatingLayer(KEY)) return
    loadFavoriteAudioProcessors()
    openFloatingLayer({
        closeFloatingLayer(KEY)
        onClose?.invoke(null)
    }, key = KEY) {
        val favoriteIconColors = IconButtonDefaults.iconToggleButtonColors(
            contentColor = MaterialTheme.colorScheme.outline,
            checkedContentColor = MaterialTheme.colorScheme.warning
        )
        Dialog(Modifier.size(700.dp, 450.dp)) {
            // 所有插件
            val descriptions = arrayListOf<AudioProcessorDescription>()
            val descriptionsToFactory = hashMapOf<AudioProcessorDescription, AudioProcessorFactory<*>>()
            val favorites = hashSetOf<AudioProcessorDescription>()
            val factories = arrayListOf<Any>(langs.audioProcessorLangs.favorite)
            val factoriesCountMap = hashMapOf<Any, Int>()
            factoriesCountMap[langs.audioProcessorLangs.favorite] = favoriteAudioProcessors.size
            AudioProcessorManager.instance.factories.values.forEach {
                factories.add(it)
                factoriesCountMap[it] = it.descriptions.size
                it.descriptions.forEach { desc ->
                    descriptions.add(desc)
                    descriptionsToFactory[desc] = it
                    val pair = desc.identifier to it.name
                    if (favoriteAudioProcessors.contains(pair)) favorites.add(desc)
                }
            }
            descriptions.sort()
            val descList = arrayListOf<AudioProcessorDescription>()
            val notFavoriteDescList = arrayListOf<AudioProcessorDescription>()
            val categoryList = hashSetOf<String>()
            val factoryList = hashSetOf<String>()
            var categoryAllCount = 0
            var manufacturerAllCount = 0
            var instrumentAllCount = 0
            val categoryCountMap = hashMapOf<String, Int>()
            val manufacturerCountMap = hashMapOf<String, Int>()
            val instrumentCountMap = hashMapOf<String, Int>()

            val selectedManufacturer0 = selectedManufacturer
            val selectedCategory0 = selectedCategory
            val selectedInstrument0 = selectedInstrument
            val selectedFactory0 = selectedFactory
            val isFavorite = selectedFactory0 == langs.audioProcessorLangs.favorite
            descriptions.forEach {
                if (!it.name.contains(searchText, true)) return@forEach
                val isCurrentManufacturer = selectedManufacturer0 == null || it.manufacturerName == selectedManufacturer0
                val isCurrentCategory = selectedCategory0 == null || it.category?.contains(selectedCategory0) == true
                val isCurrentInstrument = selectedInstrument0 == null || it.isInstrument == selectedInstrument0
                val isCurrentFavorite = favorites.contains(it)
                val isCurrentFactory = selectedFactory0 == null || (isFavorite && isCurrentFavorite) ||
                        descriptionsToFactory[it] == selectedFactory0
                // 根据已选的厂商、类别、乐器过滤插件
                if (isCurrentManufacturer && isCurrentCategory && isCurrentInstrument && isCurrentFactory) {
                    (if (isCurrentFavorite) descList else notFavoriteDescList).add(it)
                }
                // 根据已选的厂商、乐器过滤类别
                if (isCurrentManufacturer && isCurrentInstrument && isCurrentFactory && it.category != null) {
                    val list = it.category!!.split("|")
                    list.forEach { c ->
                        val category = c.trim()
                        categoryList.add(category)
                        categoryCountMap[category] = (categoryCountMap[category] ?: 0) + 1
                        categoryAllCount++
                    }
                }
                // 根据已选的类别、乐器过滤厂商
                if (isCurrentCategory && isCurrentInstrument && isCurrentFactory) it.manufacturerName?.let { manufacturer ->
                    factoryList.add(manufacturer)
                    manufacturerCountMap[manufacturer] = (manufacturerCountMap[manufacturer] ?: 0) + 1
                    manufacturerAllCount++
                }
                // 计算乐器和效果器的插件数量
                if (isCurrentManufacturer && isCurrentCategory && isCurrentFactory) {
                    val key = if (it.isInstrument) langs.audioProcessorLangs.instrument else langs.audioProcessorLangs.effect
                    instrumentCountMap[key] = (instrumentCountMap[key] ?: 0) + 1
                    instrumentAllCount++
                }
            }
            descList.addAll(notFavoriteDescList)

            var selectedDescription by mutableStateOf<AudioProcessorDescription?>(null)
            Scaffold(Modifier.fillMaxSize(),
                topBar = {
                    Surface(Modifier.fillMaxWidth().height(TOP_TEXTFIELD_HEIGHT).padding(10.dp, 10.dp, 10.dp, 0.dp)) {
                        TextField(searchText, { searchText = it }, Modifier.fillMaxSize(),
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        )
                    }
                },
                content = {
                    Row(
                        Modifier.fillMaxSize().padding(
                            top = TOP_TEXTFIELD_HEIGHT + SUB_PADDING,
                            bottom = BOTTOM_TEXTFIELD_HEIGHT + SUB_PADDING,
                            start = SUB_PADDING,
                            end = SUB_PADDING
                        ), horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AbsoluteElevation {
                            Column(Modifier.weight(1f).fillMaxHeight()) {
                                Column(Modifier.weight(1f).fillMaxHeight()) {
                                    DescList(
                                        Modifier.weight(5f).padding(SUB_PADDING),
                                        listOf(langs.audioProcessorLangs.instrument, langs.audioProcessorLangs.effect),
                                        when (selectedInstrument0) {
                                            true -> langs.audioProcessorLangs.instrument
                                            false -> langs.audioProcessorLangs.effect
                                            else -> null
                                        },
                                        langs.audioProcessorLangs.allKind, langs.audioProcessorLangs.kind,
                                        countMap = instrumentCountMap,
                                        allCount = instrumentAllCount
                                    ) { selectedInstrument = if (it == null) null else it == langs.audioProcessorLangs.instrument }
                                    DescList(
                                        Modifier.weight(5f).padding(SUB_PADDING),
                                        factories,
                                        selectedFactory0,
                                        langs.all,
                                        countMap = factoriesCountMap
                                    ) { selectedFactory = it }
                                }
                            }
                            DescList(
                                Modifier.weight(1f).padding(SUB_PADDING), categoryList.sorted(),
                                selectedCategory0, langs.audioProcessorLangs.allCategory, langs.audioProcessorLangs.category,
                                countMap = categoryCountMap,
                                allCount = categoryAllCount
                            ) { selectedCategory = it }
                            DescList(
                                Modifier.weight(1f).padding(SUB_PADDING), factoryList.sorted(),
                                selectedManufacturer0, langs.audioProcessorLangs.allManufacturer, langs.audioProcessorLangs.manufacturer,
                                countMap = manufacturerCountMap,
                                allCount = manufacturerAllCount
                            ) { selectedManufacturer = it }
                            DescList( // AudioProcessor list
                                Modifier.weight(1f).padding(SUB_PADDING), descList,
                                selectedDescription, ellipsis = false,
                                tailContent = {
                                    descriptionsToFactory[it]?.name?.let { factoryName ->
                                        val pair = it.identifier to factoryName
                                        IconToggleButton(favoriteAudioProcessors.contains(pair), {
                                            if (!favoriteAudioProcessors.remove(pair)) favoriteAudioProcessors.add(pair)
                                            saveFavoriteAudioProcessors()
                                        }, 20.dp, colors = favoriteIconColors) {
                                            Icon(Icons.Filled.Star, langs.audioProcessorLangs.addFavorite, Modifier.size(16.dp))
                                        }
                                    }
                                }, onDragStart = {
                                    val factory = descriptionsToFactory[it] ?: return@DescList null
                                    setFloatingLayerShow(KEY, false)
                                    AudioProcessorDescriptionAndFactory(it, factory)
                                }, onDragEnd = { closeFloatingLayer(KEY) }
                            ) { desc ->
                                if (desc == null) return@DescList
                                if (onClose != null && selectedDescription == desc) {
                                    closeFloatingLayer(KEY)
                                    onClose(AudioProcessorDescriptionAndFactory(desc, descriptionsToFactory[desc] ?: return@DescList))
                                } else selectedDescription = descList.find { it == desc }
                            }
                        }
                    }
                },
                bottomBar = {
                    Row(Modifier.fillMaxWidth().height(BOTTOM_TEXTFIELD_HEIGHT).padding(10.dp, 0.dp, 10.dp, 10.dp)) {
                        var descriptiveName = selectedDescription?.descriptiveName ?: ""
                        if (descriptiveName.isNotEmpty()) descriptiveName = ": $descriptiveName"
                        Text(
                            if (selectedDescription != null) "${selectedDescription?.name}${descriptiveName}" else "",
                            Modifier.weight(1f).align(Alignment.CenterVertically),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        TextButton({
                            closeFloatingLayer(KEY)
                            onClose?.invoke(null)
                        }) {
                            Text(langs.cancel)
                        }
                        if (onClose != null) Button({
                            closeFloatingLayer(KEY)
                            val desc = selectedDescription
                            val factory = descriptionsToFactory[desc]
                            onClose(if (desc == null || factory == null) null
                                    else AudioProcessorDescriptionAndFactory(desc, factory))
                        }, Modifier.padding(horizontal = 5.dp), selectedDescription != null) {
                            Text(langs.ok)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun <T: Any> RowScope.ItemText(it: T, ellipsis: Boolean) {
    val isDeprecated = it is AudioProcessorDescription && it.isDeprecated == true
    val color = LocalContentColor.current
    Text(
        if (it is IDisplayName) it.displayName else it.toString(),
        if (ellipsis) Modifier.weight(1F) else Modifier,
        overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
        textAlign = TextAlign.Start,
        maxLines = 1,
        style = MaterialTheme.typography.labelMedium,
        fontStyle = if (isDeprecated) FontStyle.Italic else null,
        color = if (isDeprecated) color.copy(0.7F) else color
    )
}

@Composable
private fun <T: Any> DescListItem(
    it: T,
    selectedDesc: T?,
    onClick: (it: T?) -> Unit,
    ellipsis: Boolean,
    countMap: Map<T, Int> = mapOf(),
    tailContent: (@Composable RowScope.(T) -> Unit)? = null
) {
    MenuItem(
        { onClick(it) },
        selectedDesc == it,
        modifier = Modifier.fillMaxWidth().height(30.dp),
        minHeight = 30.dp
    ) {
        if (ellipsis) ItemText(it, true) else Marquee(Modifier.weight(1F)) { ItemText(it, false) }
        if (tailContent != null) tailContent(it)
        else {
            val count = countMap.getOrDefault(it, 0)
            if (count > 0) Text(
                if (count < 100) count.toString() else "99+",
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun <T: Any> DescList(
    modifier: Modifier = Modifier,
    descList: List<T>,
    selectedDesc: T?,
    defaultText: String? = null,
    title: String? = null,
    countMap: Map<T, Int> = mapOf(),
    allCount: Int = countMap.values.sum(),
    ellipsis: Boolean = true,
    tailContent: (@Composable RowScope.(T) -> Unit)? = null,
    onDragStart: ((T) -> Any?)? = null,
    onDragEnd: (() -> Unit)? = null,
    onClick: (T?) -> Unit
) {
    Column(modifier) {
        if (title != null) Text(title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        AbsoluteElevationCard(Modifier.weight(1F)) {
            Box(Modifier.fillMaxSize()) {
                val state = rememberLazyListState()
                remember(favoriteAudioProcessorsActuallyLoaded) {
                    if (favoriteAudioProcessorsActuallyLoaded) state.dispatchRawDelta(Float.NEGATIVE_INFINITY)
                }
                LazyColumn(state = state) {
                    if (defaultText != null) item(defaultText) {
                        MenuItem(
                            { onClick(null) },
                            selectedDesc == null,
                            modifier = Modifier.fillMaxWidth(),
                            minHeight = 30.dp
                        ) {
                            Text(
                                defaultText,
                                Modifier.weight(1F),
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (allCount > 0) Text(
                                if (allCount < 100) allCount.toString() else "99+",
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    items(descList, { it }) {
                        if (onDragEnd == null || onDragStart == null)
                            DescListItem(it, selectedDesc, onClick, ellipsis, countMap, tailContent)
                        else GlobalDraggable({ onDragStart(it) }, onDragEnd, draggingComponent = {
                            Text(if (it is IDisplayName) it.displayName else it.toString())
                        }) {
                            DescListItem(it, selectedDesc, onClick, ellipsis, countMap, tailContent)
                        }
                    }
                }
                VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }
}
