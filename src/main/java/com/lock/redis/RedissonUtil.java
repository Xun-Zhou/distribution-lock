package com.lock.redis;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * @author zhouxun
 * @ClassName: RedissonUtil
 * @Description: redisson分布式锁
 * @date - 2020年05月26日 14时30分11秒
 */
public class RedissonUtil {

    private static final RedissonClient redissonClient = Redisson.create();

    private RedissonUtil() {
    }

    public static RedissonClient getRedissonClient() {
        return redissonClient;
    }

    /**
     * 加锁 lock
     * 一直循环获取锁
     */
    public static Boolean lock(RLock rLock, Long leaseTime) {
        rLock.lock(leaseTime, TimeUnit.SECONDS);
        return true;
    }

    /**
     * 加锁 tryLock
     * 循环获取锁 超出等待时间返回false
     */
    public static Boolean tryLock(RLock rLock, Long waitTime, Long leaseTime) throws InterruptedException {
        return rLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
    }

    /**
     * 解锁
     */
    public static Boolean unLock(RLock rLock) {
        rLock.unlock();
        return true;
    }
}
