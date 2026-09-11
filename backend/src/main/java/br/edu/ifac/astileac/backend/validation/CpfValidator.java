package br.edu.ifac.astileac.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a verificacao dos digitos verificadores do CPF.
 *
 * Cada um dos dois digitos finais e calculado sobre os digitos
 * anteriores, com pesos decrescentes: soma-se digito x peso, tira-se o
 * resto da divisao por 11 e, se o resto for menor que 2, o digito e 0;
 * caso contrario, e 11 menos o resto.
 *
 * Sequencias de digitos repetidos (00000000000, 11111111111, ...)
 * satisfazem o calculo por coincidencia, entao sao rejeitadas a parte.
 */
public class CpfValidator implements ConstraintValidator<Cpf, String> {

	private static final int TAMANHO = 11;

	@Override
	public boolean isValid(String valor, ConstraintValidatorContext context) {
		// Campo obrigatorio e responsabilidade do @NotBlank, nao daqui.
		if (valor == null) {
			return true;
		}

		if (!valor.matches("\\d{" + TAMANHO + "}")) {
			return false;
		}

		if (valor.chars().distinct().count() == 1) {
			return false;
		}

		int primeiroDigito = calcularDigito(valor, 9);
		int segundoDigito = calcularDigito(valor, 10);

		return primeiroDigito == Character.getNumericValue(valor.charAt(9))
				&& segundoDigito == Character.getNumericValue(valor.charAt(10));
	}

	/**
	 * Calcula o digito verificador da posicao informada usando os
	 * digitos que vem antes dela.
	 */
	private int calcularDigito(String cpf, int posicao) {
		int soma = 0;
		int peso = posicao + 1;

		for (int i = 0; i < posicao; i++) {
			soma += Character.getNumericValue(cpf.charAt(i)) * peso;
			peso--;
		}

		int resto = soma % TAMANHO;
		return resto < 2 ? 0 : TAMANHO - resto;
	}

}
