package com.example.sandboxCode.code.service.strategy;

import com.example.sandboxCode.code.exception.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StrategyFactory {
    private final Map<String, LanguageStrategy> strategies;

    @Autowired
    StrategyFactory(List<LanguageStrategy> strategies){
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(strategy -> strategy.getLanguageName().toLowerCase(), strategy ->strategy));
    }

    public LanguageStrategy getStrategy(String languageName){
        LanguageStrategy strategy = strategies.get(languageName.toLowerCase());
        if (strategy == null) {
            throw new ClientException(languageName + " Language not supported");
        }
        return strategy;
    }
}
