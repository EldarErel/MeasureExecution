package com.eldar.monitor.measure;

import org.springframework.boot.logging.LogLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The MeasureExecution annotation is used to annotate methods where execution metrics are to be captured.
 * The annotation has several options to customize the logging and measurement of the execution.
 * It can be set to log all method parameters, log specific method parameters, log the return value,
 * log errors, set custom log messages, and set log levels for general, error, and timeout scenarios.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MeasureExecution {

    /**
     * Array of parameter names that should be logged.
     * This is used when logAllParams is set to false, to selectively log certain parameters.
     */
    String[] paramNamesToLog() default {};

    /**
     * If set to true, the return value of the annotated method will be logged. Default is false.
     */
    boolean logReturnValue() default false;

    /**
     * If set to true, any exception thrown by the annotated method will be logged.
     * Default is false.
     */
    boolean errorLog() default false;

    /**
     * Message to be logged when the annotated method is entered. Default is an empty string for no message on entry.
     */
    String entryLogMessage() default "";

    /**
     * Specifies the log level to use when an error occurs.
     * Default is LogLevel.WARN.
     */
    LogLevel errorLogLevel() default LogLevel.WARN;

    /**
     * Specifies the general log level to use for logging the execution of the annotated method.
     * Default is LogLevel.TRACE.
     */
    LogLevel logLevel() default LogLevel.TRACE;

    /**
     * Specifies the log level to use when a timeout occurs.
     * Default is LogLevel.WARN.
     */
    LogLevel timeoutLogLevel() default LogLevel.WARN;

    /**
     * If set to true, a log will be created when the method execution exceeds a certain threshold.
     * Default is true.
     * Please refer to the `MeasureExecutionAspect.timeoutForSlowExecutionMs` property for the threshold.
     */
    boolean timeoutLog() default true;
}
