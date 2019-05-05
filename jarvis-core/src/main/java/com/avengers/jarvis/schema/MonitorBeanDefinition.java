package com.avengers.jarvis.schema;

import com.avengers.jarvis.annotation.ClassMonitor;
import com.avengers.jarvis.exception.MonitorException;
import com.avengers.jarvis.proxy.MonitorProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

public class MonitorBeanDefinition implements BeanPostProcessor, BeanFactoryPostProcessor {

    private String[] packages;

    private String id;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                ClassPathBeanDefinitionScanner scanner =
                        new ClassPathBeanDefinitionScanner((BeanDefinitionRegistry) beanFactory, true);
                scanner.addIncludeFilter(new AnnotationTypeFilter(ClassMonitor.class));
                scanner.scan(getPackages());
            } catch (Throwable e) {
                throw new MonitorException(e.getMessage());
            }
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = AopUtils.getTargetClass(bean);
        if (clazz == null) {
            return bean;
        }
        ClassMonitor monitor = clazz.getAnnotation(ClassMonitor.class);
        if (monitor == null) {
            return bean;
        }
        return MonitorProxy.newProxy(clazz, bean);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getPackages() {
        return packages;
    }

    public void setPackages(String[] packages) {
        this.packages = packages;
    }

}
