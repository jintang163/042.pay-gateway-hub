package com.payhub.common.context;

public class SandboxContext {

    private static final ThreadLocal<Boolean> SANDBOX_HOLDER = new ThreadLocal<>();

    private static final ThreadLocal<String> SCENE_HOLDER = new ThreadLocal<>();

    public static void setSandboxMode(boolean sandbox) {
        SANDBOX_HOLDER.set(sandbox);
    }

    public static boolean isSandboxMode() {
        Boolean sandbox = SANDBOX_HOLDER.get();
        return sandbox != null && sandbox;
    }

    public static void setScene(String scene) {
        SCENE_HOLDER.set(scene);
    }

    public static String getScene() {
        return SCENE_HOLDER.get();
    }

    public static void clear() {
        SANDBOX_HOLDER.remove();
        SCENE_HOLDER.remove();
    }
}
