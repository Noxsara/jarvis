package com.avengers.jarvis.proxy;

import com.avengers.jarvis.AbstractMonitorAspect;
import com.avengers.jarvis.MethodMonitor;
import com.avengers.jarvis.exception.MonitorException;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class MonitorProxy {

    private static Map<String, Object> proxyMap = new HashMap<>();

    public static Object newProxy(Class<?> clazz, Object bean) {
        String name = clazz.getCanonicalName();
        Object proxy = proxyMap.get(name);
        if (proxy == null) {
            synchronized (MonitorProxy.class) {
                proxy = proxyMap.get(name);
                if (proxy == null) {
                    proxy = createProxy(clazz, bean);
                    proxyMap.put(name, proxy);
                }
            }
        }
        return proxy;
    }

    private static Object createProxy(Class<?> clazz, Object bean) {
        return clazz.getInterfaces().length > 0 ? jdkProxy(clazz, bean) : cglibProxy(clazz, bean);
    }

    private static Object jdkProxy(Class<?> clazz, Object bean) {
        return Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), new MonitorInvocationHandler(clazz, bean));
    }

    private static Object cglibProxy(Class<?> clazz, Object bean) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(new MonitorInvocationHandler(clazz, bean));
        enhancer.setClassLoader(clazz.getClassLoader());
        return enhancer.create();
    }


    static class MonitorInvocationHandler implements InvocationHandler, MethodInterceptor {

        private Object bean;
        private Class<?> clazz;

        MonitorInvocationHandler(Class<?> clazz, Object bean) {
            this.clazz = clazz;
            this.bean = bean;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return doInvoke(proxy, method, args);
        }

        @Override
        public Object intercept(Object o, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            return doInvoke(proxy, method, args);
        }

        private Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
            //处理Object中的方法
            String methodName = method.getName();
            if (args == null || args.length == 0) {
                if ("toString".equals(methodName)) {
                    return clazz.getCanonicalName();
                } else if ("hashCode".equals(methodName)) {
                    return proxy.hashCode();
                }
            } else if (args.length == 1 && "equals".equals(methodName)) {
                return proxy == args[0];
            }

            String bizType = clazz.getSimpleName() + AbstractMonitorAspect.DOT + methodName;
            MethodMonitor.start(bizType);
            Object re;
            try {
                re = method.invoke(bean, args);
            } catch (MonitorException e) {
                MethodMonitor.exception(bizType, e.getCode());
                throw e;
            } catch (Exception e) {
                MethodMonitor.exception(bizType);
                throw e;
            } finally {
                MethodMonitor.end(bizType);
            }

            return re;
        }
    }
}
