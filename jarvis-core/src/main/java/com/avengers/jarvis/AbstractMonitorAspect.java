package com.avengers.jarvis;


import com.avengers.jarvis.annotation.Monitor;
import com.avengers.jarvis.annotation.MonitorExtend;
import com.avengers.jarvis.exception.MonitorException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AbstractMonitorAspect {

    public static final String DOT = ".";

    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private static final Logger logger = LoggerFactory.getLogger(MonitorAspect.class);

    protected Map<String, String> buildTags(Monitor monitor, ProceedingJoinPoint joinPoint, MonitorException exception) {
        Map<String, String> tags = new HashMap<>();

        if (exception != null) {
            tags.put("code", String.valueOf(exception.getCode()));
        }

        //手动埋入的context值作为tag上报
        tags.putAll(MonitorContext.getAllContext());

        EvaluationContext context = buildContext(joinPoint);
        if (context == null) {
            return tags;
        }

        MonitorExtend[] monitorExtends = monitor.monitorExtend();
        for (MonitorExtend monitorExtend : monitorExtends) {
            String key = monitorExtend.key();
            String value = monitorExtend.value();
            if (monitorExtend.byEl()) {
                value = parse(value, context);
            }
            tags.put(key, value);
        }

        return tags;

    }

    @SuppressWarnings("unchecked")
    private EvaluationContext buildContext(ProceedingJoinPoint pjp) {
        try {
            Object[] args = pjp.getArgs();
            if (args == null || args.length <= 0) {
                return null;
            }
            Method method = ((MethodSignature) pjp.getSignature()).getMethod();
            Class originalClass = pjp.getTarget().getClass();
            Method originalMethod = originalClass.getMethod(method.getName(), method.getParameterTypes());
            String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(originalMethod);
            if (parameterNames == null || parameterNames.length <= 0) {
                return null;
            }

            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
            return context;
        } catch (Exception e) {
            logger.warn("[AbstractMonitorAspect] cannot get SpEL context. msg:{}", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected String parseBizType(ProceedingJoinPoint pjp, Monitor monitor) {
        String bizType = monitor.bizType();
        if (StringUtils.isEmpty(bizType)) {
            try {
                Method method = ((MethodSignature) pjp.getSignature()).getMethod();
                Class originalClass = pjp.getTarget().getClass();
                Method originalMethod = originalClass.getMethod(method.getName(), method.getParameterTypes());
                bizType = originalClass.getSimpleName() + DOT + originalMethod.getName();
            } catch (Exception e) {
                logger.warn("[AbstractMonitorAspect] build default biz exception.", e);
            }
        }
        return bizType;
    }

    private String parse(String value, EvaluationContext context) {
        Object ret = EXPRESSION_PARSER.parseExpression(value).getValue(context);
        return ret == null ? "UNKNOWN" : String.valueOf(ret);
    }
}