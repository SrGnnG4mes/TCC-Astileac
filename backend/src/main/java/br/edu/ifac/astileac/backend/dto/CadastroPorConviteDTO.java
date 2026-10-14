package br.edu.ifac.astileac.backend.dto;

import br.edu.ifac.astileac.backend.validation.Cpf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cadastro feito pelo proprio associado, com o convite em maos (RF12).
 *
 * Cria o PROFISSIONAL e o USUARIO de uma vez: no fluxo da entidade a
 * pessoa paga antes de ser associada, entao nao existe cadastro algum
 * quando o convite chega.
 *
 * "dataAssociacao" NAO entra aqui: quem define a data em que a pessoa
 * passou a ser associada e a associacao, no momento em que reconhece o
 * pagamento -- nao o formulario. O servidor grava o dia do consumo do
 * convite.
 *
 * "perfil" tambem nao entra, pelo motivo obvio: ninguem se promove a
 * administrador preenchendo um campo.
 */
public record CadastroPorConviteDTO(
		@NotBlank(message = "O código do convite é obrigatório.")
		String token,

		@NotBlank(message = "Informe o nome.")
		@Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
		String nome,

		@NotBlank(message = "Informe o CPF.")
		@Cpf(message = "CPF inválido: informe os 11 dígitos, com os dígitos verificadores corretos.")
		String cpf,

		@NotNull(message = "Informe a data de nascimento.")
		@Past(message = "A data de nascimento deve estar no passado.")
		LocalDate dataNascimento,

		@NotBlank(message = "Informe o login.")
		@Size(min = 4, max = 100, message = "O login deve ter entre 4 e 100 caracteres.")
		String login,

		@NotBlank(message = "Informe a senha.")
		@Size(min = 8, max = 100, message = "A senha deve ter no mínimo 8 caracteres.")
		String senha
) {
}
