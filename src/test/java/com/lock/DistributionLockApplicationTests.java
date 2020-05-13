package com.lock;

import com.lock.etcd.EtcdLock;
import com.lock.redis.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DistributionLockApplicationTests {

    @Autowired
    private RedisUtil redisUtil;

    @Test
    public void redisLock() {
        boolean res1 = redisUtil.tryGetDistributedLock("test", "10010", 100);
        System.out.println(res1);
        boolean res2 = redisUtil.tryGetDistributedLock("test", "10010", 100);
        System.out.println(res2);
        boolean res = redisUtil.releaseDistributedLock("test", "10010");
        System.out.println(res);
    }

    @Test
    public void etcdLock() {
        for (int i = 0; i < 10; i++) {
            new MyThread().start();
        }
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            System.out.println("[error]:" + e);
        }
    }

    public static class MyThread extends Thread {
        @Override
        public void run() {
            String lockName = "/lock";
            EtcdLock.LockResult lockResult = EtcdLock.getInstance().lock(lockName, 30);
            if (lockResult.getIsLockSuccess()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("[error]:" + e);
                }
            }
            EtcdLock.getInstance().unLock(lockName, lockResult);
        }
    }
}
