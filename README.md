# MeasureExecution
The purpose of this feature is to monitor (with logs and metrics) our application's methods execution times, 
and to be able to easily find the slow methods in the logs, and create alerts based on them.
These metrics are being collected by Prometheus and can be displayed in Grafana.


`@MeasureExecution` is a custom annotation and `MeasureExecutionAspect` is an aspect class that together provide functionalities for measuring and logging method execution times in your application.

## MeasureExecution Annotation

This annotation can be added to any method in your application that you wish to measure and log its execution time.

Here are the parameters that can be configured in the `MeasureExecution` annotation:

- `paramNamesToLog`: An array of parameter names that you want to log. The default is an empty array.
- `logReturnValue`: A flag to indicate whether to log the return value of the method. The default value is `false`.
- `timeoutLog`: A flag to indicate whether to log if the execution time of the method is greater than the timeout threshold. The default value is `true`.
- `errorLog`: A flag to indicate whether to log when there is an error during the execution of the method. The default value is `false`.
- `entryLogMessage`: A string to be logged when the method is entered. The default value is an empty string.
- `errorLogLevel`, `logLevel`, `timeoutLogLevel`: These parameters define the log level for error logs, normal logs, and timeout logs respectively. The defaults values are (`errorLogLevel=WARN`, `logLevel=TRACE`, `timeoutLogLevel=WARN`).

## MeasureExecutionAspect Aspect

This aspect class includes the logic for measuring and logging the execution of the methods annotated with `MeasureExecution`.

The execution time of the method is recorded using the `MeterRegistry` from Micrometer, and the logs are written with SLF4J.

If the execution time of a method exceeds the `timeoutForSlowExecutionMs` value (default is 5000 milliseconds), a warning log will be written with the "(SLOW EXECUTION)" message.
So we can easily find the slow methods in the logs, and create alerts based on them.

## Usage Examples

Here are some examples of using the `MeasureExecution` annotation in your application:

### Example 1: Basic usage

```java
@MeasureExecution
public void myMethod() {
    // Your method implementation
}
```
In this example, the execution time of myMethod will be measured and logged at the TRACE level. All parameters will be logged.
### Example 2: Logging specific parameters and return value
```java
@MeasureExecution(paramNamesToLog = {"param1"}, logReturnValue = true)
public String myMethod(String param1, int param2) {
    // Your method implementation
    return "result";
}
```
In this example, the execution time of myMethod will be measured and logged at the TRACE level. The param1 parameter and the return value will be logged.

## Logging
The logs are written in the following format: "Method [{}] executed in {}ms". If the execution time is greater than the timeout threshold, the "(SLOW EXECUTION)" message will be appended to the log.

Here is an example of how the logs message may look like:
```aidl
TRACE - Method [Response com.eldar.examples.Controller.list(UUID,HttpServletRequest)] executed in 150ms
WARN - Method [myMethod] executed in 6000ms (SLOW EXECUTION)
```
Another example is:
```java
@MeasureExecution(entryLogMessage = "Entering myMethod, parameters={} and other stuff ", paramNamesToLog = {"param1", "param3"})
public void myMethod(String param1, int param2, double param3) {
    // method body
}
```
In this example, we are using paramNamesToLog to specify which parameters we want to log.
the output will be:
```aidl
DEBUG - Entering myMethod, parameters={param1=value1, param3=value3} and other stuff
```
In the MeasureExecution annotation, you have the flexibility to adjust the log level for different scenarios through the logLevel, errorLogLevel, and timeoutLogLevel parameters. 
This means you can customize the level of detail in your logs based on the normal execution, error occurrence, and execution time surpassing the timeout threshold, respectively.

## Metrics

The `MeasureExecutionAspect` class records the following metrics:

- `method.execution.time`: The execution time of the method in milliseconds. This metric is tagged with the method signature.
- `method.execution.count`: The number of times the method was executed. This metric is also tagged with the method signature.

You can view these metrics in your Micrometer dashboard.
