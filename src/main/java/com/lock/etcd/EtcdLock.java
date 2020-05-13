package com.lock.etcd;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.Lock;
import com.coreos.jetcd.data.ByteSequence;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author zhouxun
 * @ClassName: EtcdLock
 * @Description: etcd分布式锁
 * @date - 2020年05月13日 14时11分49秒
 */
public class EtcdLock {

    private static EtcdLock etcdLock = null;

    private static final Object mutex = new Object();

    // 分布式锁客户端
    private final Lock lock;

    // 租约客户端
    private final Lease lease;

    private EtcdLock() {
        // etcd客户端
        Client client = Client.builder().endpoints("http://localhost:2379").build();
        this.lock = client.getLockClient();
        this.lease = client.getLeaseClient();
    }

    public static EtcdLock getInstance() {
        synchronized (mutex) {
            if (null == etcdLock) {
                etcdLock = new EtcdLock();
            }
        }
        return etcdLock;
    }

    /**
     * 加锁
     *
     * @param lockName: 锁
     * @param ttl:      租约有效期
     * @return LockResult
     */
    public LockResult lock(String lockName, long ttl) {
        LockResult lockResult = new LockResult();
        //创建租期续约服务
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        // 返回
        lockResult.setIsLockSuccess(false);
        lockResult.setService(service);
        // 租约
        long leaseId;
        // 创建租约
        try {
            leaseId = lease.grant(ttl).get().getID();
            lockResult.setLeaseId(leaseId);
            // 启动定时任务续约，心跳周期和初次启动延时计算公式如下，可根据实际业务制定。
            long period = ttl - ttl / 5;
            service.scheduleAtFixedRate(new KeepAliveTask(lease, leaseId), period, period, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("[error]: create lease failed:" + e);
            return lockResult;
        }
        // 加锁
        try {
            System.out.println("[lock]: get lock." + Thread.currentThread().getName());
            lock.lock(ByteSequence.fromString(lockName), leaseId).get();
        } catch (InterruptedException | ExecutionException e1) {
            System.out.println("[error]: lock failed:" + e1);
            return lockResult;
        }
        System.out.println("[lock]: lock success." + Thread.currentThread().getName());
        lockResult.setIsLockSuccess(true);
        return lockResult;
    }

    /**
     * 解锁
     *
     * @param lockName:锁名
     * @param lockResult:加锁操作返回的结果
     */
    public void unLock(String lockName, LockResult lockResult) {
        System.out.println("[unlock]: release lock." + Thread.currentThread().getName());
        try {
            // 释放锁
            lock.unlock(ByteSequence.fromString(lockName)).get();
            // 关闭定时任务
            lockResult.getService().shutdown();
            // 删除租约
            if (lockResult.getLeaseId() != 0L) {
                lease.revoke(lockResult.getLeaseId());
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("[error]: unlock failed: " + e);
        }
        System.out.println("[unlock]: unlock success." + Thread.currentThread().getName());
    }

    // 续约线程
    public static class KeepAliveTask implements Runnable {

        private final Lease lease;

        private final long leaseId;

        KeepAliveTask(Lease lease, long leaseId) {
            this.lease = lease;
            this.leaseId = leaseId;
        }

        @Override
        public void run() {
            // 续约一次
            lease.keepAliveOnce(leaseId);
        }
    }

    // 加锁返回
    public static class LockResult {

        // 是否加锁成功
        private boolean isLockSuccess;

        // 租约
        private long leaseId;

        // 租约续期定时服务
        private ScheduledExecutorService service;

        public void setIsLockSuccess(boolean isLockSuccess) {
            this.isLockSuccess = isLockSuccess;
        }

        public void setLeaseId(long leaseId) {
            this.leaseId = leaseId;
        }

        public void setService(ScheduledExecutorService service) {
            this.service = service;
        }

        public boolean getIsLockSuccess() {
            return this.isLockSuccess;
        }

        public long getLeaseId() {
            return this.leaseId;
        }

        public ScheduledExecutorService getService() {
            return this.service;
        }
    }
}
