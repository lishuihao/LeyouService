package com.leyou.common.advice;

import com.leyou.common.enums.ExceptionEnums;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.ExceptionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(LyException.class)
    public ResponseEntity<ExceptionResult> handleExcpetion(LyException e){
        ExceptionEnums exceptionEnums = e.getExceptionEnums();
        return ResponseEntity.status(exceptionEnums.getCode()).body(new ExceptionResult(e.getExceptionEnums()));
    }

}
