package com.avengers.jarvis.schema;

import com.avengers.jarvis.exception.MonitorException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import java.util.regex.Pattern;

public class MonitorAnnotationParser extends AbstractSingleBeanDefinitionParser {

    private static final Pattern COMMA = Pattern.compile("\\s*[,]+\\s*");

    private final Class<?> beanClass;

    public MonitorAnnotationParser(Class beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String id = element.getAttribute("id");
        if (!StringUtils.hasText(id)) {
            throw new MonitorException("an id must be specified.");
        }
        builder.addPropertyValue("id", id);


        String packageScan = element.getAttribute("package-scan");
        if (!StringUtils.hasText(packageScan)) {
            throw new MonitorException("scan package must be specified.");
        }
        String[] packages = COMMA.split(packageScan);
        if (packages == null || packages.length == 0) {
            throw new MonitorException("error parse package path.");
        }
        builder.addPropertyValue("packages", packages);
    }


    @Override
    protected Class<?> getBeanClass(Element element) {
        return beanClass;
    }
}
