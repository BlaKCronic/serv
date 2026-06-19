package me.julionxn.nobaitc.util;

import javafx.concurrent.Task;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AppExecutor {
//    AppExecutor.execute(
//            this::generateFractions,
//        );
    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(CORES);

    public static ExecutorService getInstance() {
        return executor;
    }

    public static void shutdown() {
        executor.shutdown();
    }

    public static <T> void execute(Callable<T> backgroundTask, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return backgroundTask.call();
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccess != null) onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            if (onError != null) onError.accept(task.getException());
        });

        executor.submit(task);
    }


    // Version A: "Fire and Forget" (Only background task, no success or error handling)
    public static void executeVoid(Runnable backgroundTask) {
        executeVoid(backgroundTask, null, null);
    }

    // Version B: Background + Success (Assumes errors are handled internally or ignored)
    public static void executeVoid(Runnable backgroundTask, Runnable onSuccess) {
        executeVoid(backgroundTask, onSuccess, null);
    }

    // Version C: Full version (The one you already have)
    public static void executeVoid(Runnable backgroundTask, Runnable onSuccess, Consumer<Throwable> onError) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                backgroundTask.run();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccess != null) onSuccess.run();
        });
        task.setOnFailed(e -> {
            if (onError != null) onError.accept(task.getException());
        });

        executor.submit(task);
    }
}