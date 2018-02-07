package com.kitchen.rpc.server.thread;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

/**
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2017-06-29
 */
@Ignore
public class BusinessThreadTest {
    @Before
    public void setUp() throws Exception {

    }
    @After
    public void tearDown() throws Exception {

    }
    @Test
    public void test() throws InterruptedException, ExecutionException {
        Future<String> result1 = null;
        Future<String> result2 = null;
        try {
            System.out.println("任务1开始");
            result1 = BusinessThread.submitBusinessTask(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Thread.sleep(2000);
                    return "Result1";
                }
            });

            System.out.println("任务2开始");
            result2 = BusinessThread.submitBusinessTask(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Thread.sleep(1000);
                    return "Result2";
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("等待输出");
        System.out.println(result1.get());
        System.out.println(result2.get());
    }

}