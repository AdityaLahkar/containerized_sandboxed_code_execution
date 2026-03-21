package com.example.sandboxCode.code.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CodeRequest {

    @NotBlank(message = "Invalid language")
    private String language;

    @NotBlank(message = "Code cannot be null or empty")
    private String code;

    public CodeRequest(String language, String code) {
        this.language = language;
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
