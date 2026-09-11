package br.edu.ifac.astileac.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que o valor e um CPF real: 11 digitos cujos dois digitos
 * verificadores conferem. Ver CpfValidator para o algoritmo.
 */
@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cpf {

	String message() default "cpf invalido";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
