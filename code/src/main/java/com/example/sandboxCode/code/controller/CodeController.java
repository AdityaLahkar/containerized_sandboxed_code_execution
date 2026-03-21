package com.example.sandboxCode.code.controller;


import com.example.sandboxCode.code.model.CodeRequest;
import com.example.sandboxCode.code.model.CodeResponse;
import com.example.sandboxCode.code.service.ExecutorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CodeController {

    private final ExecutorService executorService;

    public CodeController(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @PostMapping("/run")
    public CodeResponse runCode(@Valid @RequestBody CodeRequest codeRequest){
        return executorService.execute(codeRequest);
    }
}
