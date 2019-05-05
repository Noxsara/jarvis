package com.avengers.jarvis;


import com.avengers.jarvis.annotation.Monitor;
import com.avengers.jarvis.exception.MonitorException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class MonitorAspect extends AbstractMonitorAspect {

    @Around(value = "@annotation(monitor)")
    public Object monitorStart(ProceedingJoinPoint joinPoint, Monitor monitor) throws Throwable {
        String bizType = parseBizType(joinPoint, monitor);
        MethodMonitor.start(bizType);
        Object re = null;
        try {
            re = joinPoint.proceed();
        } catch (MonitorException e) {
            MethodMonitor.exception(bizType, buildTags(monitor, joinPoint, e));
            if (!monitor.ignoreException()) {
                throw e;
            }
        } finally {
            MethodMonitor.end(bizType, monitor.expectedExecuteTime(), buildTags(monitor, joinPoint, null));
        }
        return re;
    }
}
