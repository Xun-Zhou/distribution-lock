package com.lock.zk;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author zhouxun
 * @ClassName: ZkLock
 * @Description: zookeeper分布式锁
 * @date - 2020年05月13日 15时25分03秒
 */
public class ZooLock {

    private ZooKeeper zkClient = null;

    private static final String DEFAULT_LOCK_NODE = "/lock";

    private static final String DEFAULT_COMP_NODE = "/competition";

    private String nodePath = null;

    public ZooLock() {
        try {
            zkClient = new ZooKeeper("127.0.0.1:2181", Integer.MAX_VALUE, watchedEvent -> System.out.println("zookeeper init success..."));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //检查lock路径是否存在 类型为持久化节点
            Stat stat = zkClient.exists(DEFAULT_LOCK_NODE, false);
            if (stat == null) {
                zkClient.create(DEFAULT_LOCK_NODE, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean lock(final long timeout) {
        try {
            //新建一个临时有序节点
            String currentNode = zkClient.create(DEFAULT_LOCK_NODE + DEFAULT_COMP_NODE, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            String[] nodeStr = currentNode.split("/");
            //获取当前线程创建的节点
            String ownNode = nodeStr[nodeStr.length - 1];
            this.nodePath = ownNode;
            System.out.println(Thread.currentThread().getName() + "----" + nodePath);
            //循环获取锁
            while (true) {
                //将该锁路径下的所有节点获取出
                List<String> allSubNode = zkClient.getChildren(DEFAULT_LOCK_NODE, false);
                Collections.sort(allSubNode);
                String minNode = allSubNode.get(0);
                //将当前线程创造节点与最小节点比较 为最小节点成功获得锁
                if (minNode.equals(ownNode)) {
                    return true;
                }
                //如果不是最小节点 对比自己小的节点设置监听
                int ownIndex = allSubNode.indexOf(ownNode);
                setWatcher(allSubNode, ownIndex, timeout);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean unlock() {
        try {
            //删除该节点
            zkClient.delete(DEFAULT_LOCK_NODE + "/" + nodePath, 0);
            return true;
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置监听 只监听删除事件 其实也只有删除事件
     */
    private void setWatcher(List<String> allSubNode, int ownIndex, final long timeout) {
        try {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            zkClient.exists(DEFAULT_LOCK_NODE + "/" + allSubNode.get(ownIndex - 1), watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                    countDownLatch.countDown();
                }
            });
            //阻塞当前线程
            if (timeout == 0) {
                countDownLatch.await();
            } else {
                countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
