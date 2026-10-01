import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * Validacao de CPF com digitos verificadores.
 *
 * Espelha a anotacao @Cpf do back-end, que substituiu um @Pattern de
 * formato porque aquele aceitava sequencias estruturalmente validas e
 * inexistentes, como onze digitos iguais.
 *
 * Esta copia no cliente NAO substitui a do servidor -- serve para dar
 * resposta imediata a quem digita, sem ida e volta. Quem decide se o
 * dado entra no banco continua sendo o back-end.
 */
export function cpfValido(controle: AbstractControl): ValidationErrors | null {
	const valor = (controle.value ?? '').toString().replace(/\D/g, '');

	if (!valor) {
		return null; // obrigatoriedade e responsabilidade do Validators.required
	}

	if (valor.length !== 11 || /^(\d)\1{10}$/.test(valor)) {
		return { cpf: true };
	}

	const digito = (ate: number): number => {
		let soma = 0;
		for (let i = 0; i < ate; i++) {
			soma += Number(valor[i]) * (ate + 1 - i);
		}
		const resto = soma % 11;
		return resto < 2 ? 0 : 11 - resto;
	};

	if (digito(9) !== Number(valor[9]) || digito(10) !== Number(valor[10])) {
		return { cpf: true };
	}

	return null;
}

/** Data que nao pode estar no futuro (nascimento, inicio de atuacao). */
export function naoFutura(controle: AbstractControl): ValidationErrors | null {
	if (!controle.value) {
		return null;
	}
	const hoje = new Date().toISOString().slice(0, 10);
	return controle.value > hoje ? { futura: true } : null;
}
