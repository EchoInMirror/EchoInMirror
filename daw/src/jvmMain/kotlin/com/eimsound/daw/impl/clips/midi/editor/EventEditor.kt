package com.eimsound.daw.impl.clips.midi.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import com.eimsound.dsp.data.DefaultEnvelopePointList
import com.eimsound.dsp.data.EnvelopePointList
import com.eimsound.dsp.data.MIDI_CC_RANGE
import com.eimsound.dsp.data.midi.NoteMessage
import com.eimsound.daw.actions.GlobalEnvelopeEditorEventHandler
import com.eimsound.daw.actions.doAddOrRemoveMidiCCEventAction
import com.eimsound.daw.actions.doNoteVelocityAction
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.clips.MidiClipEditor
import com.eimsound.daw.api.clips.MutableMidiCCEvents
import com.eimsound.daw.components.*
import com.eimsound.daw.components.IconButton
import com.eimsound.daw.components.utils.EditAction
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.utils.FloatRange
import com.eimsound.daw.commons.MultiSelectableEditor
import com.eimsound.daw.commons.SerializableEditor
import com.eimsound.daw.language.langs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

interface EventType : MultiSelectableEditor {
    val range: FloatRange
    val name: String
    val isInteger: Boolean
    @Composable
    fun Editor()
}

class VelocityEvent(private val editor: MidiClipEditor) : EventType {
    override val range get() = MIDI_CC_RANGE
    override val name get() = langs.velocity
    override val isInteger = true

    @Composable
    override fun Editor() {
        if (editor !is DefaultMidiClipEditor) return
        val primaryColor = MaterialTheme.colorScheme.primary
        var delta by remember { mutableStateOf(0) }
        var selectedNote by remember { mutableStateOf<NoteMessage?>(null) }
        editor.apply {
            val trackColor = clip.track?.color ?: MaterialTheme.colorScheme.primary
            Spacer(Modifier.fillMaxSize().drawBehind {
                if (size.height < 1) return@drawBehind
                val noteWidthPx = noteWidth.value.toPx()
                val offsetOfDelta = -delta / 127F * size.height
                val offsetX = if (action == EditAction.MOVE) deltaX * noteWidthPx else 0f
                val scrollX = horizontalScrollState.value
                val startTime = clip.time - clip.start
                val circleRadius = 4 * density
                notesInView.fastForEach {
                    val isSelected = selectedNotes.contains(it)
                    val x = (startTime + it.time) * noteWidthPx - scrollX + (if (isSelected) offsetX else 0f)
                    val y = (size.height * (1 - it.velocity / 127F) + (if (isSelected || selectedNote == it) offsetOfDelta else 0f))
                        .coerceIn(0f, size.height - 1)
                    var color = if (isSelected) primaryColor else trackColor
                    if (it.isDisabled) color = color.copy(alpha = 0.5F)
                    drawLine(color, Offset(x, y + circleRadius), Offset(x, size.height), 2 * density)
                    drawCircle(color, circleRadius, Offset(x, y))
                }
            }.pointerInput(editor) {
                detectDragGestures({
                    val x = it.x + horizontalScrollState.value
                    val noteWidthPx = noteWidth.value.toPx()
                    val startTime = clip.time - clip.start
                    for (i in startNoteIndex until clip.clip.notes.size) {
                        val note = clip.clip.notes[i]
                        if (((startTime + note.time) * noteWidthPx - x).absoluteValue <= 2 * density) {
                            if (selectedNotes.isNotEmpty() && !selectedNotes.contains(note)) continue
                            selectedNote = note
                            break
                        }
                    }
                }, {
                    val cur = selectedNote
                    if (cur != null) {
                        selectedNote = null
                        clip.clip.doNoteVelocityAction(
                            if (selectedNotes.isEmpty()) arrayOf(cur) else selectedNotes.toTypedArray(), delta)
                    }
                    delta = 0
                }) { it, _ ->
                    val cur = selectedNote ?: return@detectDragGestures
                    delta = ((1 - it.position.y / size.height) * 127 - cur.velocity).roundToInt().coerceIn(-127, 127)
                }
            })
        }
    }
}

class CCEvent(private val editor: MidiClipEditor, eventId: Int, points: EnvelopePointList) :
    EventType, SerializableEditor, MultiSelectableEditor {
    override val range get() = MIDI_CC_RANGE
    override val name = "CC:${eventId}"
    override val isInteger = true
    private val envEditor = EnvelopeEditor(
        points, range, horizontalScrollState = (editor as? DefaultMidiClipEditor)?.horizontalScrollState,
        eventHandler = GlobalEnvelopeEditorEventHandler
    )

    @Composable
    override fun Editor() {
        if (editor !is DefaultMidiClipEditor) return
        editor.apply {
            envEditor.Editor(
                clip.start - clip.time + with (LocalDensity.current) { horizontalScrollState.value / noteWidth.value.toPx() },
                clip.track?.color ?: MaterialTheme.colorScheme.primary,
                noteWidth, editUnit = EchoInMirror.editUnit,
                clipStartTime = clip.time
            )
        }
    }

    override fun copy() { envEditor.copy() }
    override fun paste() { envEditor.paste() }
    override fun cut() { envEditor.cut() }
    override fun copyAsString() = envEditor.copyAsString()
    override fun pasteFromString(value: String) { envEditor.pasteFromString(value) }
    override fun delete() { envEditor.delete() }
    override fun selectAll() { envEditor.selectAll() }
    override fun duplicate() { envEditor.duplicate() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun FloatingLayerProvider.openEventSelectorDialog(events: MutableMidiCCEvents) {
    val key = Any()
    val close: () -> Unit = { closeFloatingLayer(key) }

    val defaultCCEvents = sortedMapOf(
        1 to langs.ccEvents.modulation,
        7 to langs.ccEvents.volume,
        10 to langs.ccEvents.pan,
        11 to langs.ccEvents.expression,
        64 to langs.ccEvents.sustain,
    )
    openFloatingLayer(::closeFloatingLayer, key = key, hasOverlay = true) {
        Dialog(close, modifier = Modifier.widthIn(max = 460.dp)) {
            Text(langs.audioClipLangs.selectCCEvents, style = MaterialTheme.typography.titleMedium)
            val keys = events.keys
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.sorted().forEach {
                    val name = defaultCCEvents[it]
                    InputChip(true, {
                        events.doAddOrRemoveMidiCCEventAction(it)
                    }, { Text(if (name == null) "CC $it" else "CC $it ($name)") },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = langs.delete) },
                    )
                }
            }
            Divider(Modifier.padding(vertical = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                defaultCCEvents.forEach { (id, name) ->
                    if (id in keys) return@forEach
                    InputChip(false, {
                        events.doAddOrRemoveMidiCCEventAction(id, DefaultEnvelopePointList())
                    }, { Text("CC $id ($name)") },
                        trailingIcon = { Icon(Icons.Filled.Add, contentDescription = langs.add) },
                    )
                }
                Box {
                    var cc by remember { mutableStateOf(1) }
                    TextField(
                        cc.toString(),
                        { cc = it.toIntOrNull()?.coerceIn(1, 127) ?: 1 },
                        Modifier.width(140.dp),
                        singleLine = true,
                        leadingIcon = { Text("CC") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            IconButton({
                                events.doAddOrRemoveMidiCCEventAction(cc, DefaultEnvelopePointList())
                            }, enabled = cc !in keys) {
                                Icon(Icons.Filled.Add, contentDescription = langs.add)
                            }
                        }
                    )
                }
            }
        }
    }
}

internal val eventSelectorHeight = 26.dp

@Composable
private fun EventHints(editor: DefaultMidiClipEditor) {
    Surface(Modifier.width(KEYBOARD_DEFAULT_WIDTH).fillMaxHeight().zIndex(2f), shadowElevation = 5.dp) {
        val lineColor = MaterialTheme.colorScheme.outlineVariant
        val style = MaterialTheme.typography.labelMedium
        val measurer = rememberTextMeasurer()
        Canvas(Modifier.fillMaxSize()) {
            val lines = (size.height / 50).toInt().coerceAtMost(5)
            val lineSize = size.height / lines
            val isInteger = editor.selectedEvent?.isInteger ?: false
            val last = editor.selectedEvent?.range?.endInclusive ?: 127F
            for (i in 1..lines) {
                drawLine(
                    lineColor,
                    Offset(size.width - 10, lineSize * i),
                    Offset(size.width, lineSize * i),
                    1F
                )
                val value = (last / lines) * (lines - i)
                val result = measurer.measure(AnnotatedString(if (isInteger) value.roundToInt().toString() else "%.2F".format(value)), style)
                drawText(result, lineColor,
                    Offset(size.width - 14 - result.size.width, (lineSize * i - result.size.height / 2)
                        .coerceAtMost(size.height - result.size.height)))
            }
            val result = measurer.measure(AnnotatedString(if (isInteger) last.roundToInt().toString() else "$last.00"), style)
            drawText(result, lineColor, Offset(size.width - 14 - result.size.width, 0F))
        }
    }
}

@Composable
private fun EventsList(editor: DefaultMidiClipEditor) {
    Surface(Modifier.fillMaxWidth().height(eventSelectorHeight).zIndex(2f), shadowElevation = 5.dp) {
        Row {
            Text(
                langs.velocity, (
                    if (editor.selectedEvent is VelocityEvent) Modifier.background(MaterialTheme.colorScheme.primary.copy(0.2F))
                    else Modifier)
                .height(eventSelectorHeight)
                .clickableWithIcon {
                    editor.selectedEvent = VelocityEvent(editor)
                }.padding(4.dp, 2.dp), style = MaterialTheme.typography.labelLarge)
            editor.clip.clip.events.forEach { (id, points) ->
                key(id) {
                    Text("CC:${id}", (
                            if (editor.selectedEvent?.name == "CC:${id}")
                                Modifier.background(MaterialTheme.colorScheme.primary.copy(0.2F))
                            else Modifier).height(eventSelectorHeight).clickableWithIcon {
                        editor.selectedEvent = CCEvent(editor, id, points)
                    }.padding(4.dp, 2.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            val floatingLayerProvider = LocalFloatingLayerProvider.current
            Box(Modifier.size(eventSelectorHeight).clickableWithIcon {
                floatingLayerProvider.openEventSelectorDialog(editor.clip.clip.events)
            }) {
                Icon(Icons.Default.Add, langs.add, Modifier.padding(2.dp).align(Alignment.Center))
            }
        }
    }
}

// Focusable
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun EventEditor(editor: DefaultMidiClipEditor) {
    editor.apply {
        val focusManager = LocalFocusManager.current
        Column(Modifier.fillMaxSize().onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) {
            isEventPanelActive = true
            focusManager.clearFocus(true)
        }) {
            EventsList(editor)
            Row(Modifier.fillMaxSize()) {
                EventHints(editor)
                Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.background)
                    .scrollable(horizontalScrollState, Orientation.Horizontal, reverseDirection = true)
                    .scalableNoteWidth(noteWidth, horizontalScrollState)
                ) {
                    val range = remember(clip.time, clip.duration) { clip.time..(clip.time + clip.duration) }
                    EchoInMirror.currentPosition.apply {
                        EditorGrid(noteWidth, horizontalScrollState, range, ppq, timeSigDenominator, timeSigNumerator)
                    }
                    selectedEvent?.Editor()
                }
            }
        }
    }
}
