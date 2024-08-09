package searchengine.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@Slf4j
@RestControllerAdvice
public class ExceptionApiHandler {
    @ExceptionHandler({MissingServletRequestParameterException.class, ServiceException.class})
    public ResponseEntity<ErrorMessage> notFoundException(MissingServletRequestParameterException exception) {
        log.error(exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessage(false, exception.getMessage()));
    }

}
