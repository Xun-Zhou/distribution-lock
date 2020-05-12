package com.lock;

import com.lock.redis.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DistributionLockApplicationTests {

    @Autowired
    private RedisUtil redisUtil;

    @Test
    public void lock() {
        boolean res1 = redisUtil.tryGetDistributedLock("test", "10010", 100);
        System.out.println(res1);
        boolean res2 = redisUtil.tryGetDistributedLock("test", "10010", 100);
        System.out.println(res2);
    }

    @Test
    public void releaseLock() {
        boolean res = redisUtil.releaseDistributedLock("test", "10010");
        System.out.println(res);
    }
}
