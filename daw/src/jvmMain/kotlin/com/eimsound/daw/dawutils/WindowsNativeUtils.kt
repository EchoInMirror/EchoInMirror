@file:Suppress("FunctionName")

package com.eimsound.daw.dawutils

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import com.sun.jna.*
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.*
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.LongByReference
import com.sun.jna.win32.W32APIOptions
import kotlin.math.roundToLong
import com.eimsound.daw.components.app.actionButtonsSize
import com.eimsound.daw.components.app.titleBarContentSize

private interface DwmAPI : Library {
    companion object {
        val INSTANCE: DwmAPI = Native.load("dwmapi", DwmAPI::class.java)
    }

    @Suppress("unused")
    @Structure.FieldOrder("cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight")
    class Margins(
        @JvmField
        var cxLeftWidth: Int,
        @JvmField
        var cxRightWidth: Int,
        @JvmField
        var cyTopHeight: Int,
        @JvmField
        var cyBottomHeight: Int
    ) : Structure(), Structure.ByReference

    fun DwmSetWindowAttribute(hWnd: Long, dwAttribute: Int, pvAttribute: LongByReference, cbAttribute: Int): Int
    fun DwmExtendFrameIntoClientArea(hWnd: Long, pMarInset: Margins): Int
}

internal enum class WindowColorType(val value: Int) {
    CAPTION(35), BORDER(34), TEXT(36);
}
internal fun windowsSetWindowColor(handle: Long, color: Color, type: WindowColorType = WindowColorType.CAPTION) = try {
    DwmAPI.INSTANCE.DwmSetWindowAttribute(
        handle, type.value,
        LongByReference((color.blue * 255).roundToLong() shl 16 or ((color.green * 255).roundToLong() shl 8) or ((color.red * 255).roundToLong() shl 0)),
        4
    ) >= 0
} catch (e: Throwable) {
    e.printStackTrace()
    false
}

private object CustomWindowProc : WinUser.WindowProc {
    var pointer: Pointer? = null
    val rect = RECT()
    override fun callback(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT {
        if (pointer == null) return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam)
        if (uMsg == 0x0083) { // WM_NCCALCSIZE
            if (lParam.toLong() != 0L) {
                val rect2 = Structure.newInstance(RECT::class.java, Pointer(lParam.toLong()))
                rect2.read()
                rect2.left -= rect.left
                rect2.bottom -= rect.bottom
                rect2.right += rect.left
                rect2.write()
            }
            return LRESULT(0)
        } else
            if (uMsg == 0x0084) { // WM_NCHITTEST
                val x = lParam.toInt() and 0xFFFF
                val y = (lParam.toInt() shr 16) - CustomCanvasProc.windowRect.top
                if (y < 5)
                    return if (x - CustomCanvasProc.windowRect.left < 5) MyUser32.HTTOPLEFT
                    else if (CustomCanvasProc.windowRect.right - x < 5) MyUser32.HTTOPRIGHT
                    else MyUser32.HTTOP
                if (y < actionButtonsSize.height) return MyUser32.HTCAPTION
            }
        return User32.INSTANCE.CallWindowProc(pointer, hwnd, uMsg, wParam, lParam)
    }
}

private object CustomCanvasProc : WinUser.WindowProc {
    var pointer: Pointer? = null
    val windowRect = RECT()
    override fun callback(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT {
        if (pointer == null) return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam)
        if (uMsg == 0x0084) { // WM_NCHITTEST
            val x = lParam.toInt() and 0xFFFF
            val y = lParam.toInt() shr 16
            User32.INSTANCE.GetWindowRect(hwnd, windowRect)
            if (y - windowRect.top < 5) return MyUser32.HTTRANSPARENT
            if (
                x in (windowRect.right - actionButtonsSize.width)..windowRect.right &&
                y in windowRect.top..(windowRect.top + actionButtonsSize.height)
            ) return MyUser32.HTCLIENT
//            if (
//                    x in (windowRect.right - actionButtonsSize.width + (actionButtonsSize.width - titleBarContentSize.width) / 2)..(windowRect.right - actionButtonsSize.width + (actionButtonsSize.width + titleBarContentSize.width) / 2) &&
//                    y in windowRect.top..windowRect.top + actionButtonsSize.height
//                ) {
//                    User32.INSTANCE.CallWindowProc(pointer, hwnd, uMsg, wParam, lParam)
//                    MyUser32.HTMAXBUTTON
//                } else MyUser32.HTCLIENT
            if (
                x in (windowRect.left + (windowRect.right - windowRect.left - titleBarContentSize.width) / 2)..(windowRect.left + (windowRect.right - windowRect.left + titleBarContentSize.width) / 2) &&
                y in (windowRect.top + 4)..(windowRect.top + actionButtonsSize.height - 4)
            ) return MyUser32.HTCLIENT
            if (
                y in windowRect.top..windowRect.top + actionButtonsSize.height
            ) return MyUser32.HTTRANSPARENT
        }
        return User32.INSTANCE.CallWindowProc(pointer, hwnd, uMsg, wParam, lParam)
    }
}

private interface MyUser32 : User32 {
    fun SetWindowLongPtr(hWnd: HWND?, nIndex: Int, callback: Callback?): Int

    companion object {
        val INSTANCE = Native.load(
            "user32",
            MyUser32::class.java, W32APIOptions.UNICODE_OPTIONS
        ) as MyUser32

        val HTTRANSPARENT = LRESULT(-1)
        val HTCLIENT = LRESULT(1)
        val HTCAPTION = LRESULT(2)
//        val HTMAXBUTTON = LRESULT(9)
        val HTTOP = LRESULT(12)
        val HTTOPLEFT = LRESULT(13)
        val HTTOPRIGHT = LRESULT(14)
    }
}

internal fun windowsDecorateWindow(handle: Long) = try {
    User32.INSTANCE.AdjustWindowRectEx(CustomWindowProc.rect, DWORD(User32.WS_OVERLAPPEDWINDOW.toLong()), BOOL(false), null)
    DwmAPI.INSTANCE.DwmSetWindowAttribute(handle, 2, LongByReference(2), 4)
    DwmAPI.INSTANCE.DwmExtendFrameIntoClientArea(handle, DwmAPI.Margins(-1, -1, -1, -1))

    val h = HWND(Pointer(handle))
    CustomWindowProc.pointer = User32.INSTANCE.GetWindowLongPtr(h, -4).toPointer()
    MyUser32.INSTANCE.SetWindowLongPtr(h, -4, CustomWindowProc)
    User32.INSTANCE.SetWindowPos(h, null, 0, 0, 0, 0, User32.SWP_NOMOVE or User32.SWP_NOSIZE or User32.SWP_NOZORDER or User32.SWP_FRAMECHANGED)

    User32.INSTANCE.FindWindowEx(h, null, "SunAwtCanvas", null)?.let { canvasHandle ->
        CustomCanvasProc.pointer = User32.INSTANCE.GetWindowLongPtr(canvasHandle, -4).toPointer()
        MyUser32.INSTANCE.SetWindowLongPtr(canvasHandle, -4, CustomCanvasProc)
    }
    true
} catch (e: Throwable) {
    e.printStackTrace()
    false
}

fun ComposeWindow.maximize() { User32.INSTANCE.ShowWindow(HWND(Pointer(windowHandle)), User32.SW_MAXIMIZE) }
fun ComposeWindow.restore() { User32.INSTANCE.ShowWindow(HWND(Pointer(windowHandle)), User32.SW_RESTORE) }
fun ComposeWindow.minimize() { User32.INSTANCE.ShowWindow(HWND(Pointer(windowHandle)), User32.SW_MINIMIZE) }
