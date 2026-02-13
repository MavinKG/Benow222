package dev.confusedalex.thegoldeconomy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public final class SchedulerBridge {
    private static final Method ASYNC_SCHEDULER_RUN_NOW = findAsyncRunNowMethod();
    private static final Method GLOBAL_SCHEDULER_EXECUTE = findGlobalExecuteMethod();

    private SchedulerBridge() {
    }

    public static void runAsync(JavaPlugin plugin, Runnable task) {
        if (ASYNC_SCHEDULER_RUN_NOW != null) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                ASYNC_SCHEDULER_RUN_NOW.invoke(asyncScheduler, plugin, task);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Fall back to Bukkit scheduler below.
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static void runGlobal(JavaPlugin plugin, Runnable task) {
        if (GLOBAL_SCHEDULER_EXECUTE != null) {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                GLOBAL_SCHEDULER_EXECUTE.invoke(globalScheduler, plugin, task);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Fall back to Bukkit scheduler below.
            }
        }

        Bukkit.getScheduler().runTask(plugin, task);
    }

    private static Method findAsyncRunNowMethod() {
        try {
            Class<?> asyncSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            return asyncSchedulerClass.getMethod("runNow", org.bukkit.plugin.Plugin.class, Runnable.class);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method findGlobalExecuteMethod() {
        try {
            Class<?> globalRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            return globalRegionSchedulerClass.getMethod("execute", org.bukkit.plugin.Plugin.class, Runnable.class);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
