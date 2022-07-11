package my_future_thread_pool;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class ThreadPoolFutureUtil {

    private static Integer coreNum = 2;
    private static Integer maxNum = 4;

    private final ThreadPoolExecutor pool;
    private final List<Future<FutureResultDto>> futures = new ArrayList<>();

    public ThreadPoolFutureUtil(String threadName, int poolSize) {
        poolSize = poolSize <= 0 ? 2 : poolSize;

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNamePrefix(threadName).build();

        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        pool = new ThreadPoolExecutor(poolSize, poolSize
                , 10, TimeUnit.SECONDS
                , new LinkedBlockingQueue<>()
                , threadFactory, rejectionHandler);
        pool.allowCoreThreadTimeOut(true);
    }

    public void execute(Callable<FutureResultDto> task) {
        Future<FutureResultDto> submit = pool.submit(() -> {
            try {
                FutureResultDto call = task.call();
                return call;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return null;
        });
        // 防止一直不去拿future结果，导致内存溢出
        if (futures.size() < 2000) {
            futures.add(submit);
        }
    }

    /**
     * 获取结果，future获取结果后才能继续往下执行
     *
     * @return
     */
    public List<FutureResultDto> waitResult() {
        List<FutureResultDto> result = new ArrayList<>();
        for (Future<FutureResultDto> future : futures) {
            try {
                FutureResultDto obj = future.get();
                result.add(obj);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        futures.clear();
        return result;
    }
}
