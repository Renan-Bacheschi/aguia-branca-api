package io.github.renanbacheschi.aguiabranca.error;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiProblemWriter problemWriter;

    public ApiAuthenticationEntryPoint(ApiProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException, ServletException {
        problemWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Não autorizado",
                "Token ausente, inválido ou expirado.");
    }
}
