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
import androidx.compose.ui.window.Dialog
import com.eimsound.audioprocessor.AudioProcessorDescription
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.components.MenuItem
import com.eimsound.daw.components.Scrollable
import com.eimsound.daw.utils.clickableWithIcon
import java.awt.Dimension

private fun closeQuickLoadWindow() {
    EchoInMirror.windowManager.dialogs[QuickLoadDialog] = false
}

private fun selectDescription(desc: AudioProcessorDescription?) {
    if (desc == null) return
    closeQuickLoadWindow()

//    EchoInMirror.audioProcessorManager.load(desc) | (不存在的API(
}

val TOP_TEXTFIELD_HEIGHT = 60.dp
val BOTTOM_TEXTFIELD_HEIGHT = 60.dp
val SUB_PADDING = 5.dp

@OptIn(ExperimentalMaterial3Api::class)
val QuickLoadDialog = @Composable {
    val searchText = remember { mutableStateOf("") }

    // 所有插件
    val descriptions = EchoInMirror.audioProcessorManager.factories.values.flatMap { it.descriptions }.sortedBy { it.name }.distinctBy { it.name }

    // 插件收藏表
    val favorites = descriptions.groupBy { it.name }.mapValues { mutableStateOf(false) }.toMutableMap()

    val selectedFactory = remember {  mutableStateOf<String?>(null) }
    val selectedCategory = remember { mutableStateOf<String?>(null) }
    val selectedInstrument = remember { mutableStateOf<Boolean?>(null) }
    var selectedDescription by mutableStateOf<AudioProcessorDescription?>(null)

    // 根据已选的厂商、类别、乐器过滤插件
    val descList = descriptions.filter {
        (selectedFactory.value == null || it.manufacturerName == selectedFactory.value) &&
        (selectedCategory.value == null || it.category?.contains(selectedCategory.value!!) == true) && // 或者用正则 it.category?.contains(Regex("(^|\\|)"+selectedCategory.value!!+"(\\||$)")) == true 可以解决子字符串包含的问题，一般遇不到
        (selectedInstrument.value == null || it.isInstrument == selectedInstrument.value) &&
        (searchText.value == "" || it.name.contains(searchText.value, true))
    }
    // 根据已选的厂商、乐器过滤类别
    val categoryList = descriptions.filter {
        (selectedFactory.value == null || it.manufacturerName == selectedFactory.value) &&
        (selectedInstrument.value == null || it.isInstrument == selectedInstrument.value)
    }.mapNotNull { it.category }.flatMap { it.split("|") }.distinct().sorted()
    // 根据已选的乐器、类别过滤厂商
    val factoryList = descriptions.filter {
        (selectedCategory.value == null || it.category?.contains(selectedCategory.value!!) == true) &&
        (selectedInstrument.value == null || it.isInstrument == selectedInstrument.value)
    }.mapNotNull { it.manufacturerName }.distinct().sorted()

    // 计算每个类别的插件数量
    val categoryCount: Map<String, Int> = categoryList.groupBy { it }.mapValues { category ->
        descriptions.count {
            (selectedFactory.value == null || it.manufacturerName == selectedFactory.value) &&
            (selectedInstrument.value == null || it.isInstrument == selectedInstrument.value) &&
            (it.category?.contains(category.key) == true) }}
    // 计算每个厂商的插件数量
    val factoryCount: Map<String, Int> = factoryList.groupBy { it }.mapValues { factory ->
        descriptions.count {
            (selectedCategory.value == null || it.category?.contains(selectedCategory.value!!) == true) &&
            (selectedInstrument.value == null || it.isInstrument == selectedInstrument.value) &&
            (it.manufacturerName == factory.key) }}
    // 计算乐器和效果器的插件数量
    val instrumentCount: Map<String, Int> = listOf("乐器", "效果器").groupBy { it }.mapValues { instrument ->
        descriptions.count {
            (selectedCategory.value == null || it.category?.contains(selectedCategory.value!!) == true) &&
            (selectedFactory.value == null || it.manufacturerName == selectedFactory.value) &&
            (it.isInstrument == (instrument.key == "乐器")) }}

    Dialog(::closeQuickLoadWindow, title = "快速加载") {
        window.minimumSize = Dimension(860, 700)
        window.isModal = false

        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.surface,
            LocalAbsoluteTonalElevation provides 0.dp
        ) {
            Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Surface(Modifier.fillMaxWidth().height(TOP_TEXTFIELD_HEIGHT).padding(10.dp, 10.dp, 10.dp, 0.dp)) {
                            TextField(
                                value = searchText.value,
                                onValueChange = { searchText.value = it },
                                modifier = Modifier.fillMaxSize(),
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
                                    DescLister(
                                        modifier = Modifier.weight(7f).padding(SUB_PADDING),
                                        descList = listOf("已收藏", "内置插件"),
                                        onClick = { },
                                        selectedDesc = selectedDescription?.name,
                                        defaultText = "所有插件",
                                        favIcon = true
                                    )
                                    DescLister(
                                        modifier = Modifier.weight(3f).padding(SUB_PADDING),
                                        descList = listOf("乐器", "效果器"),
                                        onClick = { selectedInstrument.value = if (it == null) null else it == "乐器" },
                                        selectedDesc = if (selectedInstrument.value == true) "乐器" else if (selectedInstrument.value == false) "效果器" else null,
                                        defaultText = "所有类型",
                                        countMap = instrumentCount
                                    )
                                }
                            }
                            DescLister(
                                modifier = Modifier.weight(1f).padding(SUB_PADDING),
                                descList = categoryList,
                                onClick = { selectedCategory.value = it },
                                selectedDesc = selectedCategory.value,
                                defaultText = "所有类别",
                                countMap = categoryCount
                            )
                            DescLister(
                                modifier = Modifier.weight(1f).padding(SUB_PADDING),
                                descList = factoryList,
                                onClick = { selectedFactory.value = it },
                                selectedDesc = selectedFactory.value,
                                defaultText = "所有厂商",
                                countMap = factoryCount
                            )
                            DescLister(
                                modifier = Modifier.weight(1f).padding(SUB_PADDING),
                                descList = descList.map { it.name }.distinct().sorted(),
                                onClick = { desc ->
                                    if (selectedDescription?.name == desc) {
                                        selectDescription(selectedDescription)
                                    } else {
                                        selectedDescription = descList.find { it.name == desc }
                                    }
                                },
                                selectedDesc = selectedDescription?.name,
                                defaultText = null,
                                favIcon = true,
                                favOnClick = { desc ->
                                    favorites[desc!!] = !favorites[desc!!]?.value!!
                                },
                                favMap = favorites
                            )
                        }
                    },
                    bottomBar = {
                        Row(Modifier.fillMaxWidth().height(BOTTOM_TEXTFIELD_HEIGHT).padding(10.dp, 0.dp, 10.dp, 10.dp)) {
                            Text(
                                if (selectedDescription != null) selectedDescription?.name + ": 插件介绍 json键名\"descriptiveName\"" else "",
                                modifier = Modifier.weight(8f).align(Alignment.CenterVertically)
                            )
                            Button(
                                modifier = Modifier.weight(1f).padding(end = 5.dp),
                                enabled = selectedDescription != null,
                                onClick = {
                                    selectDescription(selectedDescription)
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("确定")
                            }
                            Button(modifier = Modifier.weight(1f).padding(start = 5.dp), onClick = {
                                closeQuickLoadWindow()
                            }, contentPadding = PaddingValues(0.dp)) {
                                Text("取消")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DescLister(
    modifier: Modifier = Modifier,
    descList: List<String>,
    onClick: (it: String?) -> Unit,
    selectedDesc: String?,
    defaultText: String?,
    favIcon: Boolean = false,
    favOnClick: (it: String?) -> Unit = {},
    favMap: Map<String, MutableState<Boolean>> = mapOf(),
    countMap: Map<String, Int> = mapOf()
) {
    Card(colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation((-4).dp), modifier = modifier) {
        Scrollable(vertical = true, horizontal = false) {
            Column {
                if (defaultText != null){
                    MenuItem(selectedDesc == null,
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                            onClick(null)
                        },
                        minHeight = 30.dp
                    ){
                        Text(defaultText, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start, maxLines = 1, modifier = Modifier.weight(9f).align(Alignment.CenterVertically), style = MaterialTheme.typography.labelSmall)
                        if (countMap.isNotEmpty()){
                            val count = countMap.values.sum()
                            Text(if (count < 1000) count.toString() else "99+", modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                descList.forEach {description ->
                    MenuItem( selectedDesc == description,
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                            onClick(description)
                        },
                        minHeight = 30.dp
                    ){
                        Text(description, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start, maxLines = 1, modifier = Modifier.weight(9f).align(Alignment.CenterVertically), style = MaterialTheme.typography.labelSmall)
                        if (favIcon) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = if (favMap[description]?.value  == true) Color.Yellow else Color.Gray, modifier = Modifier.size(15.dp).weight(1f).align(Alignment.CenterVertically).clickableWithIcon {
                                favOnClick(description)
                            })
                        } else {
                            Text(if (countMap[description]!! < 1000) countMap[description].toString() else "99+", modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
