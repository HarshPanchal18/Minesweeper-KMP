import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.gameInteraction(open: () -> Unit, flag: () -> Unit, seek: () -> Unit): Modifier =
    if (!hasRightClick()) {
        combinedClickable(
            onClick = { open() },
            onDoubleClick = { seek() },
            onLongClick = { flag() }
        )
    } else {
        pointerInput(open, flag, seek) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    with(event) {
                        if (type == PointerEventType.Press) {
                            val lmb = buttons.isPrimaryPressed
                            val rmb = buttons.isSecondaryPressed

                            if (lmb && !rmb) {
                                if (keyboardModifiers.isShiftPressed) seek()
                                else open()
                            } else if (rmb && !lmb) {
                                flag()
                            }
                        }
                    }
                }
            }
        }
    }
