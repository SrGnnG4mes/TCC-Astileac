package br.edu.ifac.astileac.backend.dto;

import br.edu.ifac.astileac.backend.validation.Cpf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Dados de entrada para criar/atualizar um profissional (RF01).
 * "ativo" nao entra aqui de proposito: todo profissional novo comeca
 * ativo, e a desativacao e uma acao propria, nao um campo livre no
 * formulario de cadastro.
 */
public record ProfissionalRequestDTO(
		@NotBlank(message = "nome e obrigatorio")
		@Size(max = 150, message = "nome deve ter no maximo 150 caracteres")
		String nome,

		@NotBlank(message = "cpf e obrigatorio")
		@Cpf(message = "cpf invalido: informe 11 digitos numericos com digitos verificadores corretos")
		String cpf,

		@NotNull(message = "data de nascimento e obrigatoria")
		@Past(message = "data de nascimento deve estar no passado")
		LocalDate dataNascimento,

		@NotNull(message = "data de associacao e obrigatoria")
		LocalDate dataAssociacao,

		/**
		 * Endereco do curriculo Lattes. Opcional -- nem todo associado
		 * tem, e exigir impediria o cadastro de quem nao tem.
		 *
		 * O padrao aceita vazio OU um endereco do proprio lattes.cnpq.br.
		 * Conferir o dominio nao e preciosismo: sem isso o campo aceita
		 * qualquer link e deixa de significar "Lattes" -- passa a ser um
		 * campo de URL qualquer com nome enganoso.
		 */
		@Pattern(
				regexp = "^$|^https?://(www\\.)?lattes\\.cnpq\\.br/.+$",
				message = "informe o endereco do Lattes (http://lattes.cnpq.br/...)")
		@Size(max = 255, message = "lattes deve ter no maximo 255 caracteres")
		String lattesUrl
) {
}
