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

/** Menor data aceita nos formularios: ninguem nasceu nem se formou antes. */
export const DATA_MINIMA = '1900-01-01';

/**
 * Hoje no fuso de quem usa, no formato do campo de data (aaaa-mm-dd).
 *
 * Nao e new Date().toISOString(): ela da a data em UTC, e as 20h no Acre
 * (UTC-5) ja e o dia seguinte em UTC -- a data de amanha passaria por
 * "nao futura".
 */
export function hoje(): string {
	const agora = new Date();
	const mes = String(agora.getMonth() + 1).padStart(2, '0');
	const dia = String(agora.getDate()).padStart(2, '0');
	return `${agora.getFullYear()}-${mes}-${dia}`;
}

/**
 * Data com ano de 4 digitos, a partir de 1900.
 *
 * O campo de data do navegador aceita anos de ate 6 digitos ("20066").
 * Alem de absurdo, isso quebrava o naoFutura, que compara as datas como
 * texto: "20066-07-08" vem antes de "2026-..." na ordem alfabetica, e
 * passava como data no passado.
 */
export function dataValida(controle: AbstractControl): ValidationErrors | null {
	if (!controle.value) {
		return null;
	}
	const partes = /^(\d{4})-\d{2}-\d{2}$/.exec(controle.value);
	return partes && controle.value >= DATA_MINIMA ? null : { data: true };
}

/**
 * Data que nao pode estar no futuro (nascimento, inicio de atuacao).
 * Inclui a checagem de formato: a comparacao como texto so vale entre
 * datas com ano de 4 digitos.
 */
export function naoFutura(controle: AbstractControl): ValidationErrors | null {
	if (!controle.value) {
		return null;
	}
	if (dataValida(controle)) {
		return { data: true };
	}
	return controle.value > hoje() ? { futura: true } : null;
}
