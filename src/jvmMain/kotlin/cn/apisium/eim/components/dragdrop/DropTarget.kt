package cn.apisium.eim.components.dragdrop

/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.debugInspectorInfo
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File

internal typealias Uri = File

interface DropTarget {
    fun onDragStarted(uris: DropTargetDragEvent, position: Offset): Boolean
    fun onDragEntered()
    fun onDragMoved(position: Offset) {}
    fun onDragExited()
    fun onDropped(uris: DropTargetDropEvent, position: Offset): Boolean
    fun onDragEnded()
}

interface DropTargetModifier : DropTarget, Modifier.Element

internal fun dropTargetModifier(): DropTargetModifier = DropTargetContainer(
    onDragStarted = { _, _ -> DragAction.Reject }
)

fun Modifier.dropTarget(
    onDragStarted: (uris: List<Uri>, Offset) -> Boolean,
    onDragEntered: () -> Unit = { },
    onDragMoved: (position: Offset) -> Unit = {},
    onDragExited: () -> Unit = { },
    onDragEnded: () -> Unit = {},
    onDropped: (uris: DropTargetDropEvent, position: Offset) -> Boolean,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "dropTarget"
        properties["onDragStarted"] = onDragStarted
    },
    factory = {
        val node = remember {
            DropTargetContainer { uris, offset ->
                when (onDragStarted(uris, offset)) {
                    false -> DragAction.Reject
                    true -> DragAction.Accept(
                        object : DropTarget {
                            override fun onDragStarted(uris: DropTargetDragEvent, position: Offset): Boolean = onDragStarted(
                                uris,
                                position
                            )

                            override fun onDragEntered() = onDragEntered()

                            override fun onDragMoved(position: Offset) = onDragMoved(position)

                            override fun onDragExited() = onDragExited()

                            override fun onDropped(uris: DropTargetDropEvent, position: Offset): Boolean = onDropped(
                                uris,
                                position
                            )

                            override fun onDragEnded() = onDragEnded()
                        }
                    )
                }
            }
        }
        this.then(node)
    })
