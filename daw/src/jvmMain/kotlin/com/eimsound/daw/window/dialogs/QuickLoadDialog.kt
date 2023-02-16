package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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

val TOP_TEXTFIELD_HEIGHT = 40.dp
val BOTTOM_TEXTFIELD_HEIGHT = 40.dp
val SUB_PADDING = 5.dp

@OptIn(ExperimentalMaterial3Api::class)
val QuickLoadDialog = @Composable {
    val descriptions = EchoInMirror.audioProcessorManager.factories.values.flatMap { it.descriptions }.sortedBy { it.name }.distinctBy { it.name } // 所有插件

    val selectedFactory = remember {  mutableStateOf<String?>(null) }
    val selectedCategory = remember { mutableStateOf<String?>(null) }
    val selectedInstrument = remember { mutableStateOf<Boolean?>(null) }
    var selectedDescription by mutableStateOf<AudioProcessorDescription?>(null)

    // 根据已选的厂商、类别、乐器过滤插件，显示在最后一列
    val descList = descriptions.filter {
        (selectedFactory.value == null || it.manufacturerName == selectedFactory.value) &&
        (selectedCategory.value == null || it.category?.contains(selectedCategory.value!!) == true) && // 或者用正则 it.category?.contains(Regex("(^|\\|)"+selectedCategory.value!!+"(\\||$)")) == true 可以解决子字符串包含的问题，一般遇不到
        (selectedInstrument.value == null || it.isInstrument == selectedInstrument.value)
    }

    Dialog(::closeQuickLoadWindow, title = "快速加载") {
        window.minimumSize = Dimension(860, 700)
        window.isModal = false

        Scaffold (
            Modifier.fillMaxSize(),
            topBar = {
                Surface(Modifier.fillMaxWidth().height(TOP_TEXTFIELD_HEIGHT)) {

                }
            },
            content = {
                Row(Modifier.fillMaxSize().padding(top= TOP_TEXTFIELD_HEIGHT + SUB_PADDING, bottom = BOTTOM_TEXTFIELD_HEIGHT + SUB_PADDING, start = SUB_PADDING, end = SUB_PADDING), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        DescLister(
                            modifier = Modifier.weight(1f).padding(SUB_PADDING),
                            descList = listOf("乐器", "效果器"),
                            onClick = { selectedInstrument.value = if (it == null) null else it == "乐器" },
                            selectedDesc = if (selectedInstrument.value == true) "乐器" else if (selectedInstrument.value == false) "效果器" else null,
                            defaultText = "所有类型"
                        )
                    }
                    DescLister(
                        modifier = Modifier.weight(1f).padding(SUB_PADDING),
                        descList = descriptions.mapNotNull { it.category?.split("|") }.flatten().distinct().sorted(), // 把类别按|展开
                        onClick = { selectedCategory.value = it },
                        selectedDesc = selectedCategory.value,
                        defaultText = "所有类别"
                    )
                    DescLister(
                        modifier = Modifier.weight(1f).padding(SUB_PADDING),
                        descList = descriptions.mapNotNull { it.manufacturerName }.distinct(),
                        onClick = { selectedFactory.value = it },
                        selectedDesc = selectedFactory.value,
                        defaultText = "所有厂商"
                    )
                    DescLister(
                        modifier = Modifier.weight(1f).padding(SUB_PADDING),
                        descList = descList.map { it.name }.distinct().sorted(),
                        onClick = { desc ->
                            if (selectedDescription?.name == desc) {
                                selectedDescription = null // 这行后续可以删除
                                // 已选中时再点击触发插件添加请求
                                // EchoInMirror.Track.addAudioProcessor(selectedDescription!!) （不存在的API（
                            } else {
                                selectedDescription = descList.find { it.name == desc }
                            }},
                        selectedDesc = selectedDescription?.name,
                        defaultText = null,
                        favIcon = true,
                        favOnClick = { desc ->

                        }
                    )
                }
            },
            bottomBar = {
                Row(Modifier.fillMaxWidth().height(BOTTOM_TEXTFIELD_HEIGHT).padding(10.dp, 0.dp, 10.dp, 10.dp)) {
                    Text(if (selectedDescription != null) selectedDescription?.name + ": 插件介绍，json键名\"descriptiveName\"" else "", modifier = Modifier.weight(8f))
                    Button(modifier = Modifier.weight(1f), onClick = {
                        closeQuickLoadWindow()
                    }) {
                        Text("确定")
                    }
                    Button(modifier = Modifier.weight(1f), onClick = {
                        closeQuickLoadWindow()
                    }) {
                        Text("取消")
                    }
                }
            }
        )
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
    favOnClick: (it: String?) -> Unit = {}
) {
    Surface(modifier = modifier) {
        Scrollable(vertical = true, horizontal = false) {
            Column {
                if (defaultText != null){
                    MenuItem(selectedDesc == null,
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                            onClick(null)
                        }
                    ){
                        Text(defaultText, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start, maxLines = 1)
                    }
                }
                descList.forEach {description ->
                    MenuItem( selectedDesc == description,
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                                onClick(description)
                        }
                    ){
                        Text(description, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start, maxLines = 1, modifier = Modifier.weight(9f).align(Alignment.CenterVertically))
                        if (favIcon) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(10.dp).weight(1f).align(Alignment.CenterVertically).clickableWithIcon {
                                favOnClick(description)
                            })
                        }
                    }
                }
            }
        }
    }
}
