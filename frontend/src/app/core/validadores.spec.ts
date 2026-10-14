import { FormControl } from '@angular/forms';
import { cpfValido, naoFutura } from './validadores';

/**
 * Validadores do formulario.
 *
 * O de CPF espelha a anotacao @Cpf do back-end. Os casos abaixo sao os
 * mesmos que justificaram trocar o @Pattern de formato por um calculo de
 * digito verificador: o padrao antigo aceitava onze digitos iguais.
 */
describe('cpfValido', () => {
	const validar = (valor: string) => cpfValido(new FormControl(valor));

	it('aceita CPF com digitos verificadores corretos', () => {
		expect(validar('11144477735')).toBeNull();
	});

	it('aceita o CPF digitado com mascara', () => {
		expect(validar('111.444.777-35')).toBeNull();
	});

	it('recusa digito verificador errado', () => {
		expect(validar('11144477736')).toEqual({ cpf: true });
	});

	it('recusa onze digitos iguais, que passariam num teste so de formato', () => {
		expect(validar('00000000000')).toEqual({ cpf: true });
		expect(validar('11111111111')).toEqual({ cpf: true });
	});

	it('recusa quantidade errada de digitos', () => {
		expect(validar('1114447773')).toEqual({ cpf: true });
		expect(validar('111444777355')).toEqual({ cpf: true });
	});

	it('deixa o campo vazio para o Validators.required', () => {
		expect(validar('')).toBeNull();
	});
});

describe('naoFutura', () => {
	const validar = (valor: string) => naoFutura(new FormControl(valor));

	it('aceita data passada', () => {
		expect(validar('2000-01-01')).toBeNull();
	});

	it('recusa data futura', () => {
		expect(validar('2999-01-01')).toEqual({ futura: true });
	});

	it('deixa o campo vazio para o Validators.required', () => {
		expect(validar('')).toBeNull();
	});
});
