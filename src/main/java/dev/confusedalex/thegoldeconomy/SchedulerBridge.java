package dev.confusedalex.thegoldeconomy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

public final class SchedulerBridge {
    private static final Method ASYNC_SCHEDULER_RUN_NOW = findAsyncRunNowMethod();
    private static final Method GLOBAL_SCHEDULER_EXECUTE = findGlobalExecuteMethod();

    private SchedulerBridge() {
    }

    public static void runAsync(JavaPlugin plugin, Runnable task) {
        if (ASYNC_SCHEDULER_RUN_NOW != null) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Class<?> callbackType = ASYNC_SCHEDULER_RUN_NOW.getParameterTypes()[1];
                Object callback = buildAsyncCallback(callbackType, task);
                ASYNC_SCHEDULER_RUN_NOW.invoke(asyncScheduler, plugin, callback);
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

    private static Object buildAsyncCallback(Class<?> callbackType, Runnable task) {
        if (callbackType == Runnable.class) {
            return task;
        }

        if (Consumer.class.isAssignableFrom(callbackType)) {
            Consumer<Object> consumer = ignored -> task.run();
            return callbackType.cast(consumer);
        }

        return Proxy.newProxyInstance(
                callbackType.getClassLoader(),
                new Class<?>[]{callbackType},
                (proxy, method, args) -> {
                    if ("accept".equals(method.getName()) || "run".equals(method.getName())) {
                        task.run();
                        return null;
                    }

                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(task, args);
                    }

                    return null;
                }
        );
    }

    private static Method findAsyncRunNowMethod() {
        try {
            Class<?> asyncSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            for (Method method : asyncSchedulerClass.getMethods()) {
                if (method.getName().equals("runNow")
                        && method.getParameterCount() == 2
                        && Plugin.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    return method;
                }
            }
            return null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method findGlobalExecuteMethod() {
        try {
            Class<?> globalRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            return globalRegionSchedulerClass.getMethod("execute", Plugin.class, Runnable.class);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
