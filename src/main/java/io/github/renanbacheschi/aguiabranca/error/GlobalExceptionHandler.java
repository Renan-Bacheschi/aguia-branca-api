package io.github.renanbacheschi.aguiabranca.error;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problemDetail = createProblem(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Um ou mais campos são inválidos.",
                request);
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler({
            InvalidRequestException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    ProblemDetail handleBadRequest(Exception exception, HttpServletRequest request) {
        String detail = exception instanceof InvalidRequestException
                ? exception.getMessage()
                : "A requisição contém valores inválidos.";
        return createProblem(HttpStatus.BAD_REQUEST, "Requisição inválida", detail, request);
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidAuthenticatedUserException.class})
    ProblemDetail handleUnauthorized(RuntimeException exception, HttpServletRequest request) {
        return createProblem(
                HttpStatus.UNAUTHORIZED,
                "Não autorizado",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return createProblem(
                HttpStatus.FORBIDDEN,
                "Acesso negado",
                "Você não possui permissão para acessar este recurso.",
                request);
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    ProblemDetail handleNotFound(Exception exception, HttpServletRequest request) {
        return createProblem(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(EmailConflictException.class)
    ProblemDetail handleEmailConflict(EmailConflictException exception, HttpServletRequest request) {
        return createProblem(
                HttpStatus.CONFLICT,
                "Conflito de e-mail",
                "O e-mail informado já está cadastrado.",
                request);
    }

    @ExceptionHandler({ConflictException.class, DuplicateKeyException.class, OptimisticLockingFailureException.class})
    ProblemDetail handleConflict(RuntimeException exception, HttpServletRequest request) {
        String detail = exception instanceof ConflictException
                ? exception.getMessage()
                : "O recurso foi alterado ou já existe com os mesmos dados únicos.";
        return createProblem(HttpStatus.CONFLICT, "Conflito", detail, request);
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}
