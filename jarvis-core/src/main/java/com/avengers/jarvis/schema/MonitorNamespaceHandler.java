package com.avengers.jarvis.schema;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class MonitorNamespaceHandler extends NamespaceHandlerSupport{

    @Override
    public void init() {
        registerBeanDefinitionParser("annotation-config", new MonitorAnnotationParser(MonitorBeanDefinition.class));
    }
}
