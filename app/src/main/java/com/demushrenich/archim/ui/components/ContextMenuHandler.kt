package com.demushrenich.archim.ui.components


import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun rememberContextMenuState(): ContextMenuState {
    return remember { ContextMenuState() }
}


class ContextMenuState {
    private var _isVisible by mutableStateOf(false)
    val isVisible: Boolean get() = _isVisible

    private var _offset by mutableStateOf(DpOffset.Zero)
    val offset: DpOffset get() = _offset

    private var _selectedItem by mutableStateOf<Any?>(null)
    val selectedItem: Any? get() = _selectedItem

    fun show(offset: DpOffset, item: Any? = null) {
        _offset = offset
        _selectedItem = item
        _isVisible = true
    }

    fun hide() {
        _isVisible = false
        _selectedItem = null
    }
}

@Composable
fun Modifier.contextMenuHandler(
    item: Any,
    contextMenuState: ContextMenuState,
    containerCoords: LayoutCoordinates?,
    onTap: (() -> Unit)? = null,
    onLongPress: ((Any) -> Unit)? = null
): Modifier {
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    var itemCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    return this
        .onGloballyPositioned { itemCoords = it }
        .pointerInput(item) {
            detectTapGestures(
                onTap = {
                    if (contextMenuState.isVisible) {
                        contextMenuState.hide()
                    } else {
                        onTap?.invoke()
                    }
                },
                onLongPress = { pressInItem: Offset ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                    val itemTopLeft = itemCoords?.positionInRoot() ?: Offset.Zero
                    val containerTopLeft = containerCoords?.positionInRoot() ?: Offset.Zero

                    val pressInContainer = itemTopLeft + pressInItem - containerTopLeft

                    val menuOffset = with(density) {
                        DpOffset(pressInContainer.x.toDp(), pressInContainer.y.toDp())
                    }

                    contextMenuState.show(menuOffset, item)
                    onLongPress?.invoke(item)
                }
            )
        }
}

@Composable
fun ContextMenu(
    state: ContextMenuState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(item: Any?) -> Unit
) {
    if (state.isVisible) {
        val density = LocalDensity.current

        Popup(
            offset = IntOffset(
                x = with(density) { state.offset.x.roundToPx() },
                y = with(density) { state.offset.y.roundToPx() }
            ),
            onDismissRequest = { state.hide() },
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = modifier
                    .wrapContentSize()
                    .widthIn(min = 160.dp, max = 280.dp)
                    .shadow(8.dp, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(vertical = 4.dp)
                ) {
                    content(state.selectedItem)
                }
            }
        }
    }
}

@Composable
fun ContextMenuItem(
    text: String,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (enabled) onClick()
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        leadingIcon?.let { icon ->
            icon()
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.wrapContentWidth()
        )
    }
}