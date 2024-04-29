package com.eimsound.daw.language.langs

open class AudioClipLangs(
    val noAudio: String = "没有音频",
    val sampleTimeTooShort: String = "采样时间过短!",
    val fileBPM: String = "文件速度",
    val detectBPM: String = "检测速度",
    val changeSpeed: String = "变速",
    val changePitch: String = "变调",
    val pitchTo: String = "变调到...",
    val timeStretcher: String = "变速算法",
    val previewNote: String = "试听音符",
    val analyzeChords: String = "分析和弦",
    val selectCCEvents: String = "选择 CC 事件",
)

open class CCEvents(
    val modulation: String = "调制",
    val volume: String = "音量",
    val pan: String = "声像",
    val expression: String = "表情",
    val sustain: String = "延音踏板",
)

open class AudioProcessorLangs(
    val openControlPanel: String = "打开控制面板",
    val native: String = "原生",
    val nativeAudioProcessor: String = "原生音频处理器",
    val searchPath: String = "搜索路径",
    val excludePath: String = "排除路径",
    val searching: String = "搜索中...",

    val favorite: String = "已收藏",
    val addFavorite: String = "收藏",
    val instrument: String = "乐器",
    val effect: String = "效果器",
    val allKind: String = "所有类型",
    val kind: String = "类型",
    val allCategory: String = "所有类别",
    val category: String = "类别",
    val allManufacturer: String = "所有厂商",
    val manufacturer: String = "厂商",
)

open class EditorToolsLangs(
    val cursor: String = "光标工具",
    val pencil: String = "铅笔工具",
    val eraser: String = "橡皮擦工具",
    val mute: String = "静音工具",
    val cut: String = "刀片工具",
)

open class AudioSettingsLang(
    val sampleRateMismatch: String = "采样率不匹配",
    val name: String = "设备与音频设置",
    val audioFactory: String = "音频工厂",
    val audioDevice: String = "音频设备",
    val bufferSize: String = "缓冲区大小",
    val sharedAudioDevice: String = "后台共享音频设备",
    val clipAudio: String = "对超过 0db 的音频进行削波",
    val inputLatency: String = "输入延迟",
    val outputLatency: String = "输出延迟",
)

open class RenderLangs(
    val render: String = "渲染",
    val length: String = "长度",
    val totalTime: String = "总时间",
    val fileName: String = "文件名",
    val format: String = "格式",
    val bitDepth: String = "位深",
    val bits: String = "位",
    val bitRate: String = "比特率",
    val flacCompression: String = "FLAC 压缩",
    val timeUsed: String = "已用时",
    val timeRemaining: String = "剩余时间",
    val estimatedTime: String = "已渲染",
    val fasterThanRealTime: String = "%.1f 倍快于实时",
    val exportTo: String = "导出到",
)

open class StringsBase(
    val none: String = "无",
    val time: String = "时间",
    val beats: String = "节拍",
    val x86Bits: String = "32 位",
    val all: String = "全部",
    val cancel: String = "取消",
    val ok: String = "确定",
    val enabled: String = "启用",
    val value: String = "值",
    val addPath: String = "添加路径",
    val add: String = "添加",
    val delete: String = "删除",
    val replace: String = "替换",
    val reset: String = "重置",
    val expand: String = "展开",
    val collapse: String = "折叠",
    val unselected: String = "未选择",
    val builtin: String = "内置",
    val default: String = "默认",
    val file: String = "文件",
    val about: String = "关于",
    val search: String = "搜索",
    val ms: String = "毫秒",
    val sample: String = "采样",
    val sampleRate: String = "采样率",
    val errorOccurred: String = "发生错误",
    val dontSave: String = "不保存",
    val saveProjectPrompt: String = "当前还没有保存项目, 是否需要保存项目并退出?",
    val saveProjectTitle: String = "是否保存项目并退出?",
    val saveAndExit: String = "保存并退出",
    val pleaseSelectFile: String = "请选择文件...",

    val copy: String = "复制",
    val paste: String = "粘贴",
    val cut: String = "剪切",
    val selectAll: String = "全选",
    val copyToClipboard: String = "复制到剪辑版",
    val pasteFromClipboard: String = "粘贴",
    val save: String = "保存",
    val undo: String = "撤销",
    val redo: String = "重做",
    val duplicate: String = "拷贝",

    val midiClip: String = "MIDI 片段",
    val createEnvelopeClip: String = "创建包络片段",
    val mergeClips: String = "合并片段",
    val rootPath: String = "根目录",
    val untitled: String = "未命名",
    val track: String = "轨道",
    val createTrack: String = "创建轨道",
    val envelope: String = "包络",
    val controller: String = "控制器",
    val velocity: String = "力度",
    val pleaseSelectClip: String = "请先选择一个片段.",
    val initialState: String = "初始状态",
    val audio: String = "音频",

    val editor: String = "编辑器",
    val mixer: String = "混音台",
    val trackView: String = "轨道视图",
    val fileBrowser: String = "文件浏览",
    val history: String = "历史操作",
    val quickLoad: String = "快速加载",
    val unknownAudioProcessor: String = "未知的音频处理器",
    val autoPlay: String = "自动播放",
    val shortcutKey: String = "快捷键",

    val openSetting: String = "打开设置",
    val openQuickLoad: String = "打开快速加载窗口",
    val pausePlay: String = "暂停/播放",

    val audioClipLangs: AudioClipLangs = AudioClipLangs(),
    val ccEvents: CCEvents = CCEvents(),
    val audioProcessorLangs: AudioProcessorLangs = AudioProcessorLangs(),
    val editorToolsLangs: EditorToolsLangs = EditorToolsLangs(),
    val audioSettingsLang: AudioSettingsLang = AudioSettingsLang(),
    val renderLangs: RenderLangs = RenderLangs(),

    val quantificationUnits: Array<String> = arrayOf("小节", "节拍", "1/2 拍", "1/3 拍", "1/4 拍", "1/6 拍", "步进", "1/2 步", "1/3 步", "1/4 步", "1/6 步", "无")
)

val ZhCnStrings = StringsBase()
