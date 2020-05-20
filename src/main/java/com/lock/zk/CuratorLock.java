package com.lock.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

/**
 * @author zhouxun
 * @ClassName: CuratorLock
 * @Description: curator分布式锁
 * @date - 2020年05月20日 15时48分13秒
 */
public class CuratorLock {

    private final InterProcessMutex processLock;

    public CuratorLock() {
        CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181",
                (retry, sleepTime, retrySleeper) -> true);
        // client操作需要先start
        client.start();
        // zk锁
        processLock = new InterProcessMutex(client, "/lock");
    }

    public boolean lock() {
        try {
            processLock.acquire();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean unlock() {
        try {
            processLock.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}