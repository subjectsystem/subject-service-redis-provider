package com.miyako.subject.service.redis.provider.api.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.miyako.subject.service.redis.api.RedisService;
import com.miyako.subject.service.redis.key.KeyPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static com.miyako.subject.commons.result.JsonAndBean.beanToString;
import static com.miyako.subject.commons.result.JsonAndBean.stringToBean;

/**
 * ClassName RedisServiceImpl
 * Description //TODO
 * Author weila
 * Date 2019-08-08-0008 15:32
 */
@Service
public class RedisServiceImpl implements RedisService{

    private static Logger logger = LoggerFactory.getLogger(RedisServiceImpl.class);

    @Autowired
    JedisPool jedisPool;	//会出现循环依赖---Circular reference
    //RedisService引用JedisPool--JedisPool在RedisService，只有创建RedisService的实例才可以获取JedisPool的bean
    //所以需要单独拿出JedisPool的bean

    @Override
    public <T> T get(KeyPrefix prefix, String key, Class data){
        logger.info("@RedisService-REDIES-GET!");
        Jedis jedis=null;
        //在JedisPool里面取得Jedis
        try {
            jedis=jedisPool.getResource();
            //生成真正的key  className+":"+prefix;  BasePrefix:id1
            String realKey=prefix.getPrefix()+"->"+key;
            logger.info("@RedisService-get-realKey:"+realKey);
            String sval=jedis.get(realKey);
            logger.info("@RedisService-getvalue:"+sval);
            //将String转换为Bean入后传出
            T t= (T) stringToBean(sval, data);
            return t;
        }finally {
            returnToPool(jedis);
        }
    }

    @Override
    public boolean delete(KeyPrefix prefix,String key){
        Jedis jedis=null;
        try {
            jedis=jedisPool.getResource();
            String realKey=prefix.getPrefix()+key;
            long ret=jedis.del(realKey);
            return ret>0;//删除成功，返回大于0
            //return jedis.decr(realKey);
        }finally {
            returnToPool(jedis);
        }
    }

    @Override
    public <T> boolean set(KeyPrefix prefix,String key,T value){
        logger.info("@RedisService-REDIES-SET!");
        Jedis jedis=null;
        try {//在JedisPool里面取得Jedis
            jedis=jedisPool.getResource();
            String realKey=prefix.getPrefix()+"->"+key;
            logger.info("@RedisService-key:"+key);
            logger.info("@RedisService-getPrefix:"+prefix.getPrefix());
            String s=beanToString(value);//将T类型转换为String类型，json类型？？
            if(s==null||s.length()<=0) {
                return false;
            }
            int seconds=prefix.expireSeconds();
            if(seconds<=0) {//有效期：代表不过期，这样才去设置
                jedis.set(realKey, s);
                //System.out.println("1");
            }else {//没有设置过期时间，即没有设置有效期，那么自己设置。
                jedis.setex(realKey, seconds,s);
                //System.out.println("2");
            }
            return true;
        }finally {
            returnToPool(jedis);
            //System.out.println("3");
        }
    }

    @Override
    public Long decr(KeyPrefix prefix, String key){
        Jedis jedis=null;
        try {
            jedis=jedisPool.getResource();
            String realKey=prefix.getPrefix()+"->"+key;
            return jedis.decr(realKey);
        }finally {
            returnToPool(jedis);
        }
    }

    @Override
    public Long incr(KeyPrefix prefix, String key){
        Jedis jedis=null;
        try {
            jedis=jedisPool.getResource();
            String realKey=prefix.getPrefix()+"->"+key;
            return jedis.incr(realKey);
        }finally {
            returnToPool(jedis);
        }
    }

    @Override
    public boolean exitsKey(KeyPrefix prefix, String key){
        Jedis jedis=null;
        try {
            jedis=jedisPool.getResource();
            String realKey=prefix.getPrefix()+"->"+key;
            return jedis.exists(realKey);
        }finally {
            returnToPool(jedis);
        }
    }

    @Override
    public <T> boolean set(String key,T value){
        Jedis jedis=null;
        //在JedisPool里面取得Jedis
        try {
            jedis=jedisPool.getResource();
            //将T类型转换为String类型
            String s=beanToString(value);
            if(s==null) {
                return false;
            }
            jedis.set(key, s);
            return true;
        }finally {
            returnToPool(jedis);
        }
    }

    @Override
    public <T> T get(String key,Class<T> data){
        Jedis jedis=null;
        //在JedisPool里面取得Jedis
        try {
            jedis=jedisPool.getResource();
            System.out.println("jedis:"+jedis);
            String sval=jedis.get(key);
            System.out.println("sval:"+sval);
            //将String转换为Bean入后传出
            T t=stringToBean(sval,data);
            return t;
        }finally {
            returnToPool(jedis);
        }
    }

    private void returnToPool(Jedis jedis) {
        if(jedis!=null) {
            jedis.close();
        }
    }
}
