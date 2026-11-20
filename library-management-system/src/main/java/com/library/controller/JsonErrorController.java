package com.library.controller;

import com.library.payload.response.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Errors that happen before a request reaches our controllers (for example the security firewall
 * rejecting a URL like /api/fines//pay) are forwarded by the server to /error.
 * GlobalException can't see those, so this replaces Spring's default /error page with our
 * standard body: { "message": "...", "success": false }.
 */
@RestController
public class JsonErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiResponse> handleError(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (code instanceof Integer c) {
            HttpStatus resolved = HttpStatus.resolve(c);
            if (resolved != null) {
                status = resolved;
            }
        }

        String message = switch (status) {
            case BAD_REQUEST -> "The request is not valid. Check the URL and parameters.";
            case UNAUTHORIZED -> "Authentication required";
            case FORBIDDEN -> "Access denied";
            case NOT_FOUND -> "Endpoint not found";
            case METHOD_NOT_ALLOWED -> "Method not allowed here";
            default -> status.is5xxServerError()
                    ? "Something went wrong. Please try again later."
                    : status.getReasonPhrase();
        };
        return ResponseEntity.status(status).body(new ApiResponse(message, false));
    }
}
