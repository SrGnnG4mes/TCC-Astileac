package br.edu.ifac.astileac.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.edu.ifac.astileac.backend.dto.EnderecoRequestDTO;
import br.edu.ifac.astileac.backend.dto.EnderecoResponseDTO;
import br.edu.ifac.astileac.backend.entity.Endereco;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.EnderecoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Teste de unidade do EnderecoService. O comportamento que importa
 * aqui e a relacao 1:1: salvar duas vezes substitui, nao duplica.
 */
@ExtendWith(MockitoExtension.class)
class EnderecoServiceTest {

	@Mock
	private EnderecoRepository enderecoRepository;

	@Mock
	private ProfissionalService profissionalService;

	@InjectMocks
	private EnderecoService enderecoService;

	private Profissional profissional() {
		Profissional profissional = new Profissional();
		profissional.setId(1L);
		return profissional;
	}

	private EnderecoRequestDTO dto() {
		return new EnderecoRequestDTO(
				"69900000", "Rua das Palmeiras", "120", "Apto 3", "Centro", "Rio Branco", "AC");
	}

	@Test
	void deveCriarEnderecoQuandoProfissionalAindaNaoTem() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(enderecoRepository.findByProfissionalId(1L)).thenReturn(Optional.empty());
		when(enderecoRepository.save(any(Endereco.class))).thenAnswer(invocation -> {
			Endereco salvo = invocation.getArgument(0);
			salvo.setId(30L);
			return salvo;
		});

		EnderecoResponseDTO resultado = enderecoService.salvar(1L, dto());

		assertEquals(30L, resultado.id());
		assertEquals("Rio Branco", resultado.cidade());
		assertEquals("AC", resultado.uf());
	}

	@Test
	void deveSubstituirOEnderecoExistenteEmVezDeCriarOutro() {
		Endereco existente = new Endereco();
		existente.setId(30L);
		existente.setProfissional(profissional());
		existente.setCidade("Cruzeiro do Sul");

		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(enderecoRepository.findByProfissionalId(1L)).thenReturn(Optional.of(existente));
		when(enderecoRepository.save(any(Endereco.class))).thenAnswer(invocation -> invocation.getArgument(0));

		EnderecoResponseDTO resultado = enderecoService.salvar(1L, dto());

		// Mesmo id: substituiu o registro, nao criou um segundo.
		assertEquals(30L, resultado.id());
		assertEquals("Rio Branco", resultado.cidade());
	}

	@Test
	void deveLancarExcecaoAoBuscarEnderecoInexistente() {
		when(profissionalService.buscarEntidade(1L)).thenReturn(profissional());
		when(enderecoRepository.findByProfissionalId(1L)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> enderecoService.buscar(1L));
	}

	@Test
	void deveRepassarErroQuandoProfissionalNaoExiste() {
		when(profissionalService.buscarEntidade(99L))
				.thenThrow(new RecursoNaoEncontradoException("Profissional nao encontrado: id 99"));

		assertThrows(RecursoNaoEncontradoException.class, () -> enderecoService.salvar(99L, dto()));
	}

}
