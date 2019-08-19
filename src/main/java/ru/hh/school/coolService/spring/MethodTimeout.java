package ru.hh.school.coolService.spring;

public class MethodTimeout {

    String methodName;
    String timeoutSupName;
    String value;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getTimeoutSupName() {
        return timeoutSupName;
    }

    public void setTimeoutSupName(String timeoutSupName) {
        this.timeoutSupName = timeoutSupName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return methodName + "/" + timeoutSupName + "/" + value;
    }
}
