package com.example.sandboxCode.code.service.strategy;

import org.springframework.stereotype.Component;

@Component
public class CStrategy implements LanguageStrategy{

    @Override
    public String getFileExtension() {
        return ".c";
    }

    @Override
    public String getDockerImage() {
        return "gcc";
    }

    @Override
    public String getLanguageName() {
        return "c";
    }

    @Override
    public String[] getExecutionCommand() {
        return new String[]{"bash", "-c", "gcc /app/prog.c -o /tmp/prog && /tmp/prog"};
    }
}
