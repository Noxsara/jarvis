# 可扩展的监控上报组件

### 目前支持功能
* 基于方法和类注解
* SpringEL表达式
* 强弱依赖区分
* SPI插件扩展

<br>

### 一、核心组件介绍

#### @Monitor注解
Monitor注解用于需要进行监控的方法, 被打了该注解的方法会进行监控上报。提供如下属性：<br>

属性|类型|说明
---|---|---
bizType|String|监控的业务类型, 通常对应监控系统中的的metric。如果不指定则默认取'类名.方法名'
expectedExecuteTime|Long|该方法期待的执行时间, 单位ms
monitorExtend|MonitorExtend[]|扩展数组, key-value结构，用于tag上报。支持SpringEL表达式
ignoreException|boolean|是否忽略异常。如果设置为true, 则发生异常后仅仅上报监控, 然后忽略异常。如果设置为false, 则直接抛出异常

<br>

#### @MonitorExtend注解
MonitorExtend注解用于上报tag, 该注解提供如下属性：<br>

属性|类型|说明
---|---|---
key|String|tag的key
value|String|tag的value
byEl|boolean|表示value是否为el表达式

<br>

#### @ClassMonitor注解
ClassMonitor注解用于需要监控的类, 如果类被打上该注解, 则相当于所有的方法被打上了Monitor注解。

<br>

#### MethodMonitor
监控上报的Util类, 用于数据上报, 可以单独拿出来使用, 以便独立打点。

<br>

#### MonitorContext
监控上下文, 以ThreadLocal维护。包含了一些key-value值, 可以手动设置, 实现丰富的tag上报。

<br>

### 二、使用示例

#### @Monitor注解
如果仅仅需要关注方法的QPS以及RT, 直接打上该注解即可：<br>
```
public class MonitorService {

    @Monitor
    public void service() {
        // do something
    }
}
```
该方法被调用时会上报如下内容：

上报内容|metric|tags|备注
---|---|---|---
qps|MonitorService.service.total.qps|无|
rt|MonitorService.service.rt|无|
exception|MonitorService.service.fail.qps|code=xxx, xxx为一个整数值, 表示异常的原因|是否上报异常取决于方法内是否抛出MonitorException, MonitorException包含一个自定义的code

也可以手动指定metric:
```
public class MonitorService {

    @Monitor(bizType = "myBiz")
    public void service() {
        // do something
    }
}
```
这样相应的metric就变为 myBiz.total.qps/myBiz.rt/myBiz.fail.qps。

在Monitor注解中自定义tag:
```
    @Monitor(bizType = "myBiz", monitorExtend = {
        @MonitorExtend(key = "biz", value = "1"),
        @MonitorExtend(key = "id", value = "#param.id", byEl = true)})
    public void service(Param param) {
        // do something
    }
```
该方法还会上报两个tag, 分别是biz = 1, id = "#param.id"。id是一个el表达式, 具体值从param中取。

<br>

#### @ClassMonitor注解
如果一个类的所有方法都需要进行监控, 可以在每一个方法上都使用Monitor注解, 也可以只在类上使用ClassMonitor注解。
```$xslt
@ClassMonitor
public class AnnotationMonitorService {
    public void serviceA() {
    
    }
    
    public void serviceB() {
    
    }
}
```
然后在spring配置文件中指定包扫描
```$xslt
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:jarvis="http://www.avengers.com/schema/jarvis"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/aop
       http://www.springframework.org/schema/aop/spring-aop-3.0.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context-3.0.xsd
       http://www.avengers.com/schema/jarvis
       http://www.avengers.com/schema/jarvis/jarvis.xsd">

    <aop:aspectj-autoproxy/>

    <context:component-scan base-package="com.avengers.jarvis"/>

    <jarvis:annotation-config package-scan="com.avengers.jarvis" id="monitor"/>

    <bean id="monitorTestService" class="com.avengers.jarvis.service.MonitorService"/>
</beans>
```
监控组件会为打了ClassMonitor注解的类的所有方法生成代理，在代理中进行监控上报。上面的类中A方法上报的metric前缀为AnnotationMonitorService.serviceA，B方法上报的metric前缀为AnnotationMonitorService.serviceB。

<br>

#### SPI插件扩展
在jarvis中，没有定义具体使用哪一个监控系统。上报组件定义为一个接口Reporter：包含qps和rt方法。
Reporter接口定义如下:
```$xslt
public interface Reporter {

    /**
     * 上报qps
     * @param metric
     * @param tags
     */
    void qps(String metric, Map<String, String> tags);

    /**
     * 上报rt
     * @param metric
     * @param rt
     * @param tags
     */
    void rt(String metric, Long rt, Map<String, String> tags);
}
```
具体的Reporter实现交给使用jarvis组件的开发者。jarvis会以SPI的机制去加载具体的实现。加载的逻辑为：
```$xslt
 static {
        ServiceLoader<Reporter> loader = ServiceLoader.load(Reporter.class);
        Iterator<Reporter> iterator = loader.iterator();
        while (iterator.hasNext()) {
            Reporter reporter = iterator.next();
            reporters.add(reporter);
        }
        
        if (CollectionUtils.isEmpty(reporters)) {
           throw new ReporterNotFoundException("no report found.");
        }
    }
```


假设我们想使用CAT进行监控上报，则需要实现CAT上报逻辑CatReporter：
```$xslt
public class CatReporter implements Reporter {

    @Override
    public void qps(String metric, Map<String, String> tags) {
        if (MapUtils.isEmpty(tags)) {
            Cat.logMetricForCount(metric, 1);
        } else {
            Cat.logMetricForCount(metric, 1, tags);
        }
    }

    @Override
    public void rt(String metric, Long rt, Map<String, String> tags) {
        if (MapUtils.isEmpty(tags)) {
            Cat.logMetricForDuration(metric, rt);
        } else {
            Cat.logMetricForDuration(metric, rt, tags);
        }
    }
}
```
然后在资源目录resources/META-INF目录下新建com.avengers.jarvis.extension.Reporter文件，文件中的内容为：
```$xslt
com.avengers.javis.cat.CatReporter
```
这样jarvis组件启动时，就能识别到CatReporter，并且使用其进行监控上报。
同理可以定义上报大白监控的DabaiReporter:
```$xslt
public class DabaiReporter implements Reporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DabaiReporter.class);

    @Override
    public void qps(String metric, Map<String, String> tags) {
        MetricsBuilder builder = new MetricsBuilder();
        if (MapUtils.isNotEmpty(tags)) {
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                builder.addTag(entry.getKey(), entry.getValue());
            }
        }

        Metrics metrics = builder.newMetrics();
        metrics.addKeyValue(metric, Lists.newArrayList(1f));

        LOGGER.info(metrics.toString());


    }

    @Override
    public void rt(String metric, Long rt, Map<String, String> tags) {
        MetricsBuilder builder = new MetricsBuilder();
        if (MapUtils.isNotEmpty(tags)) {
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                builder.addTag(entry.getKey(), entry.getValue());
            }
        }

        Metrics metrics = builder.newMetrics();
        metrics.addKeyValue(metric, Lists.newArrayList(rt.floatValue()));

        LOGGER.info(metrics.toString());
    }
}
```
同时将com.avengers.jarvis.extension.Reporter文件修改为：
```$xslt
com.avengers.javis.cat.CatReporter
com.avengers.javis.cat.DabaiReporter
```
这样jarvis将会同时向CAT和Dabai上报监控。