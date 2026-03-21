package com.example.sandboxCode.code.exception;

import com.example.sandboxCode.code.model.CodeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CodeExceptionHandler {

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<CodeResponse> handleException(ClientException ex) {
        CodeResponse codeResponse = new CodeResponse(
                ex.getMessage(),
                "",
                "error"
        );

        return new ResponseEntity<>(codeResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServerException.class)
    public ResponseEntity<CodeResponse> handleException(ServerException ex){
        CodeResponse codeResponse = new CodeResponse(
                ex.getMessage(),
                "",
                "error"
        );
        return new ResponseEntity<>(codeResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CodeResponse> handleException(Exception ex){
        CodeResponse codeResponse = new CodeResponse(
                "An internal unexpected error occurred \n",
                "",
                "error"
        );
        return new ResponseEntity<>(codeResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
