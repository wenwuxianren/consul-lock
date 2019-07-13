package com.lyb.consullock;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConsulLockApplicationTests {
    @Autowired
    private ServiceConfig serviceConfig;
    @Test
    public void lockSameResourer() {
        //针对相同资源在同一时刻只有一个线程会获得锁
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        for (int a=0;a<20;a++){
            threadPool.submit(
                    () -> {
                        for (int i = 0;i < 100; i++) {
                            DistributedLock lock = new DistributedLock(
                                    serviceConfig.getConsulRegisterHost(),
                                    serviceConfig.getConsulRegisterPort());

                            DistributedLock.LockContext lockContext = lock.getLock("test lock", 10);
                            if (lockContext.isGetLock()) {
                                System.out.println(Thread.currentThread().getName() + "获得了锁");
                                try {
                                    TimeUnit.SECONDS.sleep(1);
                                    lock.releaseLock(lockContext.getSession());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }else {
                                //System.out.println(Thread.currentThread().getName() + "没有获得锁");
                            }
                        }
                    });
        }

        try {
            TimeUnit.MINUTES.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void lockDiffResource(){
        //针对不通的资源所有线程都应该能获得锁
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        for (int a=0;a<20;a++){
            threadPool.submit(
                    () -> {
                        for (int i = 0;i < 100; i++) {
                            DistributedLock lock = new DistributedLock(
                                    serviceConfig.getConsulRegisterHost(),
                                    serviceConfig.getConsulRegisterPort());

                            DistributedLock.LockContext lockContext = lock.getLock("test lock"+Thread.currentThread().getName(), 10);
                            if (lockContext.isGetLock()) {
                                System.out.println(Thread.currentThread().getName() + "获得了锁");
                                try {
                                    TimeUnit.SECONDS.sleep(1);
                                    lock.releaseLock(lockContext.getSession());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }else {
                                //System.out.println(Thread.currentThread().getName() + "没有获得锁");
                                Assert.assertTrue(lockContext.isGetLock());
                            }
                        }
                    });
        }

        try {
            TimeUnit.MINUTES.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
