package com.example.sandboxCode.code.model;

public class CodeResponse {
    private String error;
    private String output;
    private String status;

    public CodeResponse(String error, String output, String status) {
        this.error = error;
        this.output = output;
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
