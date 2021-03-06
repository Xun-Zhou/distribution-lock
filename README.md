# distribution-lock 分布式锁

## etcd

    Etcd 是一个高可用、强一致的分布式键值（key-value）数据库，主要用途是共享配置和服务发现，其内部采用 Raft 算法作为分布式一致性协议，因此，Etcd 集群作为一个分布式系统 “天然” 就是强一致性的。而副本机制（一个 Leader，多个 Follower）又保证了其高可用性
    
    Lease 机制：即租约机制（TTL，Time To Live），Etcd 可以为存储的 key-value 对设置租约，当租约到期，key-value 将失效删除；同时也支持续约，通过客户端可以在租约到期之前续约，以避免 key-value 对过期失效；此外，还支持解约，一旦解约，与该租约绑定的 key-value 将失效删除；
    Prefix 机制：即前缀机制，也称目录机制，如两个 key 命名如下：key1=“/mykey/key1" , key2="/mykey/key2"，那么，可以通过前缀-"/mykey"查询，返回包含两个 key-value 对的列表；
    Watch 机制：即监听机制，Watch 机制支持 Watch 某个固定的key，也支持 Watch 一个范围（前缀机制），当被 Watch 的 key 或范围发生变化，客户端将收到通知；
    Revision 机制：每个key带有一个 Revision 号，每进行一次事务加一，因此它是全局唯一的，如初始值为 0，进行一次 put 操作，key 的 Revision 变为 1，同样的操作，再进行一次，Revision 变为 2；换成 key1 进行 put 操作，Revision 将变为 3；这种机制有一个作用：通过 Revision 的大小就可以知道进行写操作的顺序，这对于实现公平锁，队列十分有益。
    
 Etcd分布式锁示意图
 
 ![Etcd分布式锁示意图](https://github.com/Xun-Zhou/distribution-lock/blob/master/etcd.jpg "Etcd分布式锁示意图")
 
 [自实现分布式锁](https://github.com/Xun-Zhou/distribution-lock/blob/master/src/main/java/com/lock/etcd/EtcdClient.java "自实现分布式锁")
 
 Etcd Java客户端Jetcd提供的Lock客户端
 
 [Lock客户端](https://github.com/Xun-Zhou/distribution-lock/blob/master/src/main/java/com/lock/etcd/EtcdLock.java "Lock客户端")

## redis

 [redis分布式锁](https://github.com/Xun-Zhou/distribution-lock/blob/master/src/main/java/com/lock/redis/RedisUtil.java "redis分布式锁")
 
 redis推荐使用Redisson客户端
 
 ![Redisson分布式锁示意图](https://github.com/Xun-Zhou/distribution-lock/blob/master/redission.jpg "Redisson分布式锁示意图")
 
 [Redisson分布式锁](https://github.com/Xun-Zhou/distribution-lock/blob/master/src/main/java/com/lock/redis/RedissonUtil.java "redis分布式锁")

## zookeeper

 [自实现分布式锁](https://github.com/Xun-Zhou/distribution-lock/blob/master/src/main/java/com/lock/zk/ZooLock.java "自实现分布式锁")
 
 Curator客户端实现分布式锁，简化开发
 
 [Curator客户端](https://github.com/Xun-Zhou/distribution-lock/blob/master/src/main/java/com/lock/zk/CuratorLock.java "CuratorL客户端")

## zookerper读写锁

    实现步骤
        1.所有客户端在/sharelock创建自己的锁节点(顺序临时节点)
        2.获取/sharelock下所有节点
        3.读锁客户端判断自己前面有没有写锁，没有则获得锁，有则监听写锁；写锁客户端判断自己前面有没有节点，无节点则获得锁，有则监听前一个节点
        4.持有锁的客户端删除节点，某个客户端获得通知，得到锁
        5.重复步骤4
