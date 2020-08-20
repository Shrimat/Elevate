package com.example.elevate;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskRunner {
    private static final Executor THREAD_POOL_EXECUTOR =
            new ThreadPoolExecutor(5, 128, 1,
                    TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface Callback<R> {
        void onComplete(R result);
    }

    public <R> void executeAsync(Callable<R> callable, Callback<R> callback) {
        THREAD_POOL_EXECUTOR.execute(() -> {
            final R result;
            R temp = null;
            try {
                temp = callable.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            result = temp;
            handler.post(() -> {
                callback.onComplete(result);
            });
        });
    }
}
