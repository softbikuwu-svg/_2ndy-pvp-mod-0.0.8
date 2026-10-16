package com.secondy.client.util;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Прямой опрос клавиатуры/мыши через GLFW (не зависит от версии Minecraft). */
public final class Input {
    private Input() {}

    /** Key codes >= MOUSE_BASE are mouse buttons (MOUSE_BASE + GLFW button). */
    public static final int MOUSE_BASE = 1000;

    private static long window() {
        return MinecraftClient.getInstance().getWindow().getHandle();
    }

    public static boolean key(int glfwKey) {
        return GLFW.glfwGetKey(window(), glfwKey) == GLFW.GLFW_PRESS;
    }

    public static boolean mouse(int button) {
        return GLFW.glfwGetMouseButton(window(), button) == GLFW.GLFW_PRESS;
    }

    /** Keyboard key or mouse button (see MOUSE_BASE). */
    public static boolean down(int code) {
        if (code < 0) return false;
        return code >= MOUSE_BASE ? mouse(code - MOUSE_BASE) : key(code);
    }

    /** Human-readable name of a key code. */
    public static String keyName(int code) {
        if (code < 0) return "None";
        if (code >= MOUSE_BASE) {
            int b = code - MOUSE_BASE;
            return switch (b) {
                case 0 -> "Mouse Left";
                case 1 -> "Mouse Right";
                case 2 -> "Mouse Middle";
                default -> "Mouse " + (b + 1);
            };
        }
        switch (code) {
            case GLFW.GLFW_KEY_SPACE: return "Space";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "L Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "R Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "L Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "R Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT: return "L Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "R Alt";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_DELETE: return "Delete";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            case GLFW.GLFW_KEY_UP: return "Up";
            case GLFW.GLFW_KEY_DOWN: return "Down";
            case GLFW.GLFW_KEY_LEFT: return "Left";
            case GLFW.GLFW_KEY_RIGHT: return "Right";
            default: break;
        }
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25) return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        if (code >= GLFW.GLFW_KEY_KP_0 && code <= GLFW.GLFW_KEY_KP_9) return "Num " + (code - GLFW.GLFW_KEY_KP_0);
        String n = GLFW.glfwGetKeyName(code, 0);
        if (n != null && !n.isEmpty()) return n.toUpperCase(java.util.Locale.ROOT);
        return "Key " + code;
    }
}
