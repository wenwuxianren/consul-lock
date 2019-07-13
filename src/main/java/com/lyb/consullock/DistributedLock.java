package com.lyb.consullock;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewCheck;
import com.ecwid.consul.v1.kv.model.PutParams;
import com.ecwid.consul.v1.session.model.NewSession;
import com.ecwid.consul.v1.session.model.Session;
import lombok.Data;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class DistributedLock{
    private ConsulClient consulClient;

    /**
     * 构造函数
     * @param consulHost 注册consul的client或服务端的Ip或主机名，或域名
     * @param consulPort 端口号
     */
    public DistributedLock(String consulHost,int consulPort){
        consulClient = new ConsulClient(consulHost,consulPort);
    }

    /**
     * 获得锁的方法
     * @param lockName 竞争的资源名
     * @param ttlSeconds 锁的超时时间，超过该时间自动释放
     * @return
     */
    public LockContext getLock(String lockName,int ttlSeconds){
        LockContext lockContext = new LockContext();
        if(ttlSeconds<10 || ttlSeconds > 86400) ttlSeconds = 60;
        String sessionId = createSession(lockName,ttlSeconds);
        boolean success = lock(lockName,sessionId);
        if(success == false){
            consulClient.sessionDestroy(sessionId,null);
            lockContext.setGetLock(false);

            return lockContext;
        }

        lockContext.setSession(sessionId);
        lockContext.setGetLock(true);

        return lockContext;
    }

    /**
     * 释放锁
     * @param sessionID
     */
    public void releaseLock(String sessionID){
        consulClient.sessionDestroy(sessionID,null);
    }

    private String createSession(String lockName,int ttlSeconds){
        NewCheck check = new NewCheck();
        check.setId("check "+lockName);
        check.setName(check.getId());
        check.setTtl(ttlSeconds+"s"); //该值和session ttl共同决定决定锁定时长
        check.setTimeout("10s");
        consulClient.agentCheckRegister(check);
        consulClient.agentCheckPass(check.getId());

        NewSession session = new NewSession();
        session.setBehavior(Session.Behavior.RELEASE);
        session.setName("session "+lockName);
        session.setLockDelay(1);
        session.setTtl(ttlSeconds + "s"); //和check ttl共同决定锁时长
        List<String> checks = new ArrayList<>();
        checks.add(check.getId());
        session.setChecks(checks);
        String sessionId = consulClient.sessionCreate(session,null).getValue();

        return sessionId;
    }

    private boolean lock(String lockName,String sessionId){
        PutParams putParams = new PutParams();
        putParams.setAcquireSession(sessionId);

        boolean isSuccess = consulClient.setKVValue(lockName,"lock:"+ LocalDateTime.now(),putParams).getValue();

        return isSuccess;
    }

    /**
     * 竞争锁时返回的对象
     */
    @Data
    public class LockContext{
        /**
         * 获得锁成功返回该值，比便后面用该值来释放锁
         */
        private String session;
        /**
         * 是否获得到锁
         */
        private boolean isGetLock;
    }
}
