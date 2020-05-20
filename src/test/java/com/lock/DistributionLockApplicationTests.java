package com.lock;

import com.lock.etcd.EtcdLock;
import com.lock.redis.RedisUtil;
import com.lock.zk.CuratorLock;
import com.lock.zk.ZooLock;
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
            new MyRedisThread().start();
        }
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class MyRedisThread extends Thread {
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

    @Test
    public void zooLock() {
        for (int i = 0; i < 10; i++) {
            new MyZooThread().start();
        }
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class MyZooThread extends Thread {
        @Override
        public void run() {
            ZooLock zooLock = new ZooLock();
            boolean lockRes = zooLock.lock(0);
            System.out.println(Thread.currentThread().getName() + "加锁:" + lockRes);
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            boolean unlockRes = zooLock.unlock();
            System.out.println(Thread.currentThread().getName() + "解锁:" + unlockRes);
        }
    }

    @Test
    public void curatorLock() {
        for (int i = 0; i < 10; i++) {
            new MyCuratorThread().start();
        }
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class MyCuratorThread extends Thread {
        @Override
        public void run() {
            CuratorLock curatorLock = new CuratorLock();
            boolean lockRes = curatorLock.lock();
            System.out.println(Thread.currentThread().getName() + "加锁:" + lockRes);
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            boolean unlockRes = curatorLock.unlock();
            System.out.println(Thread.currentThread().getName() + "解锁:" + unlockRes);
        }
    }
}
