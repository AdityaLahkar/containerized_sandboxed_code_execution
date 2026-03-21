package com.example.sandboxCode.code.service.strategy;

public interface LanguageStrategy {
    public String getFileExtension();
    public String getDockerImage();
    public String[] getExecutionCommand();
    public String getLanguageName();
}
