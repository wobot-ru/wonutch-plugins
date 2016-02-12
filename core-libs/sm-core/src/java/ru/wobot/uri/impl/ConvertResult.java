package ru.wobot.uri.impl;

public class ConvertResult {
    private final boolean isConvertSuccess;
    private final Object result;

    ConvertResult(Object result) {
        this.result = result;
        this.isConvertSuccess = true;
    }

    private ConvertResult() {
        isConvertSuccess = false;
        result = null;
    }

    public static ConvertResult getFailedConvertResult() {
        return new ConvertResult();
    }

    public Object getResult() {
        return result;
    }

    public boolean isConvertSuccess() {
        return isConvertSuccess;
    }
}
