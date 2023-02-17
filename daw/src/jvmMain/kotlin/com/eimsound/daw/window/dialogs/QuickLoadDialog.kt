@file:Suppress("PrivatePropertyName")

package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessorDescription
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.daw.Configuration
import com.eimsound.daw.components.*
import com.eimsound.daw.components.utils.clickableWithIcon

private val TOP_TEXTFIELD_HEIGHT = 60.dp
private val BOTTOM_TEXTFIELD_HEIGHT = 60.dp
private val SUB_PADDING = 5.dp
private val KEY = Any()

var searchText by mutableStateOf("")
var selectedFactory by mutableStateOf<String?>(null)
var selectedCategory by mutableStateOf<String?>(null)
var selectedInstrument by mutableStateOf<Boolean?>(null)

@OptIn(ExperimentalMaterial3Api::class)
fun FloatingDialogProvider.openQuickLoadDialog() {
    openFloatingDialog(::closeFloatingDialog, key = KEY) {
        Dialog(true, Modifier.size(600.dp, 450.dp)) {
            // 所有插件
            val descriptions = AudioProcessorManager.instance.factories.values.flatMap { it.descriptions }.sortedBy { it.name }
                .distinctBy { it.name }

            // 根据已选的厂商、类别、乐器过滤插件
            val descList = descriptions.filter {
                (selectedFactory == null || it.manufacturerName == selectedFactory) &&
                        (selectedCategory == null || it.category?.contains(selectedCategory!!) == true) && // 或者用正则 it.category?.contains(Regex("(^|\\|)"+selectedCategory.value!!+"(\\||$)")) == true 可以解决子字符串包含的问题，一般遇不到
                        (selectedInstrument == null || it.isInstrument == selectedInstrument) &&
                        (searchText.isEmpty() || it.name.contains(searchText, true))
            }
            // 根据已选的厂商、乐器过滤类别
            val categoryList = descriptions.filter {
                (selectedFactory == null || it.manufacturerName == selectedFactory) &&
                        (selectedInstrument == null || it.isInstrument == selectedInstrument)
            }.mapNotNull { it.category }.flatMap { it.split("|") }.distinct().sorted()
            // 根据已选的乐器、类别过滤厂商
            val factoryList = descriptions.filter {
                (selectedCategory == null || it.category?.contains(selectedCategory!!) == true) &&
                        (selectedInstrument == null || it.isInstrument == selectedInstrument)
            }.mapNotNull { it.manufacturerName }.distinct().sorted()

            // 计算每个类别的插件数量
            val categoryCount: Map<String, Int> = categoryList.groupBy { it }.mapValues { category ->
                descriptions.count {
                    (selectedFactory == null || it.manufacturerName == selectedFactory) &&
                            (selectedInstrument == null || it.isInstrument == selectedInstrument) &&
                            (it.category?.contains(category.key) == true)
                }
            }
            // 计算每个厂商的插件数量
            val factoryCount: Map<String, Int> = factoryList.groupBy { it }.mapValues { factory ->
                descriptions.count {
                    (selectedCategory == null || it.category?.contains(selectedCategory!!) == true) &&
                            (selectedInstrument == null || it.isInstrument == selectedInstrument) &&
                            (it.manufacturerName == factory.key)
                }
            }
            // 计算乐器和效果器的插件数量
            val instrumentCount: Map<String, Int> = listOf("乐器", "效果器").groupBy { it }.mapValues { instrument ->
                descriptions.count {
                    (selectedCategory == null || it.category?.contains(selectedCategory!!) == true) &&
                            (selectedFactory == null || it.manufacturerName == selectedFactory) &&
                            (it.isInstrument == (instrument.key == "乐器"))
                }
            }

            var selectedDescription by mutableStateOf<AudioProcessorDescription?>(null)
            Scaffold(
                modifier = Modifier.fillMaxSize(),
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
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            Column(Modifier.weight(1f).fillMaxHeight()) {
                                DescList(
                                    Modifier.weight(5f).padding(SUB_PADDING),
                                    listOf("乐器", "效果器"),
                                    if (selectedInstrument == true) "乐器" else if (selectedInstrument == false) "效果器" else null,
                                    "所有类型", "类型",
                                    countMap = instrumentCount
                                ) { selectedInstrument = if (it == null) null else it == "乐器" }
                                DescList(
                                    modifier = Modifier.weight(5f).padding(SUB_PADDING),
                                    descList = listOf("已收藏", "内置"),
                                    selectedDesc = selectedDescription?.name,
                                    defaultText = "所有",
                                    favIcon = true
                                ) { }
                            }
                        }
                        DescList(
                            Modifier.weight(1f).padding(SUB_PADDING), categoryList,
                            selectedCategory, "所有类别", "类别",
                            countMap = categoryCount
                        ) { selectedCategory = it }
                        DescList(
                            Modifier.weight(1f).padding(SUB_PADDING), factoryList,
                            selectedFactory, "所有厂商", "厂商",
                            countMap = factoryCount
                        ) { selectedFactory = it }
                        DescList(
                            Modifier.weight(1f).padding(SUB_PADDING), descList.map { it.name }.distinct().sorted(),
                            selectedDescription?.name, favIcon = true,
                            favOnClick = { desc ->
                                if (desc != null && !Configuration.favoriteAudioProcessors.remove(desc))
                                    Configuration.favoriteAudioProcessors.add(desc)
                                // TODO: Configuration.save()
                            },
                            favMap = Configuration.favoriteAudioProcessors
                        ) { desc ->
                            if (selectedDescription?.name == desc) {
    //                              TODO: selectDescription(selectedDescription)
                                closeFloatingDialog(KEY)
                            } else {
                                selectedDescription = descList.find { it.name == desc }
                            }
                        }
                    }
                },
                bottomBar = {
                    Row(
                        Modifier.fillMaxWidth().height(BOTTOM_TEXTFIELD_HEIGHT).padding(10.dp, 0.dp, 10.dp, 10.dp)
                    ) {
                        var descriptiveName = selectedDescription?.descriptiveName ?: ""
                        if (descriptiveName.isNotEmpty()) descriptiveName = ": $descriptiveName"
                        Text(
                            if (selectedDescription != null) "${selectedDescription?.name}${descriptiveName}" else "",
                            Modifier.weight(1f).align(Alignment.CenterVertically),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Button(
                            {
                                // TODO: selectDescription(selectedDescription)
                                closeFloatingDialog(KEY)
                            },
                            Modifier.padding(end = 5.dp),
                            selectedDescription != null
                        ) {
                            Text("确定")
                        }
                        Button({ closeFloatingDialog(KEY) }, Modifier.padding(start = 5.dp)) {
                            Text("取消")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DescList(
    modifier: Modifier = Modifier,
    descList: List<String>,
    selectedDesc: String?,
    defaultText: String? = null,
    title: String? = null,
    favIcon: Boolean = false,
    favOnClick: (it: String?) -> Unit = {},
    favMap: MutableSet<String>? = null,
    countMap: Map<String, Int> = mapOf(),
    onClick: (it: String?) -> Unit
) {
    Column(modifier) {
        if (title != null) Text(title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Card(
            Modifier.weight(1F), MaterialTheme.shapes.extraSmall,
            CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surface),
            CardDefaults.cardElevation((-4).dp),
        ) {
            Scrollable(vertical = true, horizontal = false) {
                Column {
                    if (defaultText != null) {
                        MenuItem(
                            { onClick(null) },
                            selectedDesc == null,
                            modifier = Modifier.fillMaxSize(),
                            minHeight = 30.dp
                        ) {
                            Text(
                                defaultText, Modifier.weight(9f).align(Alignment.CenterVertically),
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (countMap.isNotEmpty()) {
                                val count = countMap.values.sum()
                                Text(
                                    if (count < 1000) count.toString() else "99+",
                                    Modifier.weight(1.5f),
                                    textAlign = TextAlign.End,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    descList.forEach { description ->
                        MenuItem(
                            { onClick(description) },
                            selectedDesc == description,
                            modifier = Modifier.fillMaxSize(),
                            minHeight = 30.dp
                        ) {
                            Text(
                                description, Modifier.weight(9f).align(Alignment.CenterVertically),
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (favIcon) {
                                Icon(
                                    Icons.Filled.Star,
                                    "收藏",
                                    Modifier.size(15.dp).weight(1f).align(Alignment.CenterVertically)
                                        .clickableWithIcon { favOnClick(description) },
                                    if (favMap != null && favMap.contains(description)) Color.Yellow else Color.Gray
                                )
                            } else {
                                Text(
                                    if (countMap[description]!! < 1000) countMap[description].toString() else "99+",
                                    Modifier.weight(1.5f),
                                    textAlign = TextAlign.End,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
