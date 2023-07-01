package com.eldar.monitor.measure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class MeasureExecutionAspect {
    public static final String SLOW_EXECUTION_STRING = " (SLOW EXECUTION)";

    // Used for discovering names of method parameters
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Value("${app.metric.timeoutForSlowExecutionMs:5000}")
    private int timeoutForSlowExecutionMs;

    private final MeterRegistry meterRegistry;

    @Around("@annotation(measureExecutionAnnotation)")
    public Object measureAndLogExecution(ProceedingJoinPoint joinPoint, MeasureExecution measureExecutionAnnotation) throws Throwable {
        if (measureExecutionAnnotation == null) {
            return joinPoint.proceed();
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Map<String, Object> methodParams = collectMethodParameters(method, joinPoint.getArgs());

        logEntry(measureExecutionAnnotation, methodParams);
        Object proceed;
        long start = System.currentTimeMillis();
        try {
            // execute the method
            proceed = joinPoint.proceed();
        } catch (Throwable e) {
            if (measureExecutionAnnotation.errorLog()) { // print error log only if errorLog is true
                log(measureExecutionAnnotation.errorLogLevel()
                        , "execution of [{}] failed", joinPoint.getSignature(), e);
            }
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - start;
            handleEndedExecution(joinPoint, measureExecutionAnnotation, executionTime);
        }
        // only on success
        if (measureExecutionAnnotation.logReturnValue()) {
            log(measureExecutionAnnotation.logLevel(), "Return value: {}", joinPoint.proceed());
        }
        // return the result of the method
        return proceed;
    }

    /**
     * Records the execution time of the method in the metrics registry and logs it.
     * if the execution time is above the threshold for slow execution, it will be logged with the `timeoutLogLevel`
     * log level
     * With the `timeoutLog` flag, you can control whether to log slow executions or not.
     * With the `timeoutForSlowExecutionMs` property, you can control the threshold for slow executions.
     * Check the SLOW_EXECUTION_STRING constant for the value that will be appended to the log message,
     * we can search it and creates alerts on it.
     */
    private void handleEndedExecution(ProceedingJoinPoint joinPoint, MeasureExecution measureExecutionAnnotation,
                                      long executionTime) {
        LogLevel logLevel = measureExecutionAnnotation.logLevel();
        StringBuilder logMessage = new StringBuilder("Method [{}] executed in {}ms");
        if (executionTime > timeoutForSlowExecutionMs && measureExecutionAnnotation.timeoutLog()) {
            logLevel = measureExecutionAnnotation.timeoutLogLevel();
            logMessage.append(SLOW_EXECUTION_STRING);
        }
        log(logLevel, logMessage.toString(), joinPoint.getSignature(), executionTime);
        recordExecutionMetrics(joinPoint, executionTime);
    }

    private Map<String, Object> collectMethodParameters(Method method, Object[] paramValues) {
        Map<String, Object> params = new HashMap<>();
        String[] paramNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                params.put(paramNames[i], paramValues[i]);
            }
        }
        return params;
    }

    private void logEntry(MeasureExecution measureExecutionAnnotation, Map<String, Object> methodParams) {
        StringBuilder logMessage = new StringBuilder(measureExecutionAnnotation.entryLogMessage());
        LogLevel logLevel = measureExecutionAnnotation.logLevel();
        List<String> paramNamesToLog = Arrays.asList(measureExecutionAnnotation.paramNamesToLog());
        if (StringUtils.isEmpty(logMessage)) {
            return;
        }
        Map<String, Object> paramsToLog = filterParamsToLog(methodParams, paramNamesToLog);
        log(logLevel, logMessage.toString(), paramsToLog);
    }

    private Map<String, Object> filterParamsToLog(Map<String, Object> methodParams, List<String> paramNamesToLog) {
        Map<String, Object> paramsToLog = new HashMap<>();
        for (Map.Entry<String, Object> entry : methodParams.entrySet()) {
            if (paramNamesToLog.contains(entry.getKey())) {
                paramsToLog.put(entry.getKey(), entry.getValue());
            }
        }
        return paramsToLog;
    }

    private void recordExecutionMetrics(ProceedingJoinPoint joinPoint, long executionTime) {
        String methodSignature = joinPoint.getSignature().toShortString();
        Timer.builder("method.execution.time")
                .tag("method", methodSignature)
                .register(meterRegistry)
                .record(executionTime, TimeUnit.MILLISECONDS);
        Counter.builder("method.execution.count")
                .tag("method", methodSignature)
                .register(meterRegistry)
                .increment();
    }

    private void log(LogLevel logLevel, String format, Object... arguments) {
        switch (logLevel) {
            case TRACE -> log.trace(format, arguments);
            case DEBUG -> log.debug(format, arguments);
            case INFO -> log.info(format, arguments);
            case WARN -> log.warn(format, arguments);
            case ERROR -> log.error(format, arguments);
            default -> log.debug(format, arguments);
        }
    }

}
