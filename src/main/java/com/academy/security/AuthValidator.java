package com.academy.security;

import org.springframework.stereotype.Component;

//Clase S10
/**
 * Bean-based method security: referenced as {@code @PreAuthorize("@authValidator.isValid()")},
 * so an authorization rule can live in plain Java instead of a SpEL expression.
 *
 * <p>The bean name {@code authValidator} is what the expression resolves, so renaming the class
 * breaks the annotation.
 */
@Component
public class AuthValidator {

    /**
     * Placeholder rule. Flip to {@code false} to watch the annotated endpoint answer 403 while
     * the rest of the controller keeps working.
     */
    public boolean isValid() {
        return true;
    }
}

