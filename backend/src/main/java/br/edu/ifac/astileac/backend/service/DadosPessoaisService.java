package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.config.WebConfig;
import br.edu.ifac.astileac.backend.dto.CertificadoResponseDTO;
import br.edu.ifac.astileac.backend.dto.ContatoResponseDTO;
import br.edu.ifac.astileac.backend.dto.DadosPessoaisDTO;
import br.edu.ifac.astileac.backend.dto.EnderecoResponseDTO;
import br.edu.ifac.astileac.backend.dto.ExclusaoRespostaDTO;
import br.edu.ifac.astileac.backend.dto.FormacaoResponseDTO;
import br.edu.ifac.astileac.backend.dto.ProfissionalAreaResponseDTO;
import br.edu.ifac.astileac.backend.dto.VinculoEmpregaticioResponseDTO;
import br.edu.ifac.astileac.backend.entity.AcaoLog;
import br.edu.ifac.astileac.backend.entity.Certificado;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.RecursoNaoEncontradoException;
import br.edu.ifac.astileac.backend.repository.CertificadoRepository;
import br.edu.ifac.astileac.backend.repository.ContatoRepository;
import br.edu.ifac.astileac.backend.repository.EnderecoRepository;
import br.edu.ifac.astileac.backend.repository.FormacaoRepository;
import br.edu.ifac.astileac.backend.repository.ConviteRepository;
import br.edu.ifac.astileac.backend.repository.ProfissionalAreaRepository;
import br.edu.ifac.astileac.backend.repository.ProfissionalRepository;
import br.edu.ifac.astileac.backend.repository.UsuarioRepository;
import br.edu.ifac.astileac.backend.repository.VinculoEmpregaticioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direitos do titular dos dados (RF10, LGPD Art. 18): obter os
 * proprios dados, pedir a eliminacao deles e desistir do pedido.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DadosPessoaisService {

	/**
	 * Janela de arrependimento. Nao e um prazo tecnico: e uma decisao
	 * da associacao, informada a pessoa no momento do pedido.
	 */
	public static final int DIAS_ATE_EXCLUSAO_DEFINITIVA = 30;

	private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final ProfissionalRepository profissionalRepository;
	private final EnderecoRepository enderecoRepository;
	private final ContatoRepository contatoRepository;
	private final ProfissionalAreaRepository profissionalAreaRepository;
	private final FormacaoRepository formacaoRepository;
	private final CertificadoRepository certificadoRepository;
	private final VinculoEmpregaticioRepository vinculoEmpregaticioRepository;
	private final UsuarioRepository usuarioRepository;
	private final ConviteRepository conviteRepository;
	private final ProfissionalService profissionalService;
	private final LogAcessoService logAcessoService;
	private final ArquivoCertificadoService arquivoCertificadoService;

	/**
	 * Reune tudo que o sistema guarda sobre a pessoa (Art. 18, II e V).
	 *
	 * Registra a exportacao no log de acesso explicitamente, em vez de
	 * deixar o interceptor classifica-la como consulta comum: entregar
	 * a ficha inteira de alguem e um evento que a auditoria precisa
	 * distinguir de abrir uma tela.
	 */
	@Transactional(readOnly = true)
	public DadosPessoaisDTO exportar(Long profissionalId) {
		Profissional profissional = profissionalService.buscarEntidade(profissionalId);

		logAcessoService.registrar(AcaoLog.EXPORTACAO, LogAcessoService.ENTIDADE_PROFISSIONAL, profissionalId, null,
				"Exportacao de dados pessoais (LGPD Art. 18)");

		return new DadosPessoaisDTO(
				LocalDate.now(),
				new DadosPessoaisDTO.IdentificacaoDTO(
						profissional.getId(),
						profissional.getNome(),
						profissional.getCpf(),
						profissional.getDataNascimento(),
						profissional.getDataAssociacao(),
						profissional.getAtivo()),
				enderecoRepository.findByProfissionalId(profissionalId)
						.map(endereco -> new EnderecoResponseDTO(
								endereco.getId(), endereco.getCep(), endereco.getLogradouro(),
								endereco.getNumero(), endereco.getComplemento(), endereco.getBairro(),
								endereco.getCidade(), endereco.getUf()))
						.orElse(null),
				contatoRepository.findByProfissionalId(profissionalId).stream()
						.map(contato -> new ContatoResponseDTO(
								contato.getId(), contato.getTipo(), contato.getValor(), contato.getPrincipal()))
						.toList(),
				profissionalAreaRepository.findByIdProfissionalId(profissionalId).stream()
						.map(vinculo -> new ProfissionalAreaResponseDTO(
								vinculo.getAreaAtuacao().getId(), vinculo.getAreaAtuacao().getNome(),
								vinculo.getDataInicio()))
						.toList(),
				formacaoRepository.findByProfissionalId(profissionalId).stream()
						.map(formacao -> new FormacaoResponseDTO(
								formacao.getId(), formacao.getNivel(), formacao.getCurso(),
								formacao.getInstituicao(), formacao.getAnoConclusao()))
						.toList(),
				certificadoRepository.findByProfissionalId(profissionalId).stream()
						.map(certificado -> new CertificadoResponseDTO(
								certificado.getId(),
								certificado.getFormacao() != null ? certificado.getFormacao().getId() : null,
								certificado.getTitulo(), certificado.getInstituicaoEmissora(),
								certificado.getCargaHoraria(), certificado.getDataEmissao(),
								// A exportacao diz se ha documento e se ele foi
								// reconhecido, nao onde o servidor o guardou: o
								// caminho interno nao e dado do titular, e
								// quem exporta tem direito aos proprios dados,
								// nao a estrutura de pastas da aplicacao.
								certificado.temDocumento(),
								certificado.getValidadoEm(),
								certificado.getValidadoPor()))
						.toList(),
				vinculoEmpregaticioRepository.findByProfissionalId(profissionalId).stream()
						.map(vinculo -> new VinculoEmpregaticioResponseDTO(
								vinculo.getId(), vinculo.getInstituicao(), vinculo.getCargo(),
								vinculo.getTipoVinculo(), vinculo.getDataInicio(), vinculo.getDataFim(),
								vinculo.getAtual()))
						.toList());
	}

	/**
	 * Registra o pedido de eliminacao (Art. 18, VI) e devolve, de forma
	 * explicita, o que vai acontecer com os dados.
	 *
	 * Nao apaga na hora: durante a janela de arrependimento os dados
	 * ficam BLOQUEADOS -- somem das listagens e das buscas, mas ainda
	 * existem, para que o pedido possa ser desfeito. Bloqueio e uma das
	 * formas de atendimento previstas no proprio Art. 18, IV.
	 */
	@Transactional
	public ExclusaoRespostaDTO solicitarExclusao(Long profissionalId) {
		Profissional profissional = profissionalService.buscarEntidade(profissionalId);

		if (profissional.getDataExclusaoSolicitada() != null) {
			throw new ConflitoException("Ja existe um pedido de exclusao em andamento para este cadastro");
		}

		LocalDateTime agora = LocalDateTime.now();
		profissional.setDataExclusaoSolicitada(agora);
		profissional.setAtivo(false);
		profissionalRepository.save(profissional);

		LocalDate dataDoPedido = agora.toLocalDate();
		LocalDate dataDaExclusao = dataDoPedido.plusDays(DIAS_ATE_EXCLUSAO_DEFINITIVA);

		String mensagem = ("Pedido de exclusao registrado. Os seus dados serao mantidos por %d dias, "
				+ "ate %s, e nesse periodo ficam bloqueados: nao aparecem em consultas nem em "
				+ "listagens da associacao. O prazo existe para o caso de voce mudar de ideia. "
				+ "Passada essa data, os dados serao apagados definitivamente e nao havera como "
				+ "recupera-los.")
				.formatted(DIAS_ATE_EXCLUSAO_DEFINITIVA, dataDaExclusao.format(DATA_BR));

		return new ExclusaoRespostaDTO(
				mensagem,
				dataDoPedido,
				dataDaExclusao,
				DIAS_ATE_EXCLUSAO_DEFINITIVA,
				"Para desfazer, use POST " + WebConfig.PREFIXO_API + "/profissionais/" + profissionalId
						+ "/dados-pessoais/exclusao/cancelamento");
	}

	/**
	 * Desfaz o pedido dentro da janela: o caso de mudar de ideia.
	 */
	@Transactional
	public ExclusaoRespostaDTO cancelarExclusao(Long profissionalId) {
		Profissional profissional = profissionalService.buscarEntidade(profissionalId);

		if (profissional.getDataExclusaoSolicitada() == null) {
			throw new RecursoNaoEncontradoException(
					"Nao ha pedido de exclusao em andamento para este cadastro");
		}

		profissional.setDataExclusaoSolicitada(null);
		profissional.setAtivo(true);
		profissionalRepository.save(profissional);

		return new ExclusaoRespostaDTO(
				"Pedido de exclusao cancelado. O cadastro voltou a ficar ativo e os dados nao serao apagados.",
				LocalDate.now(),
				null,
				0,
				null);
	}

	/**
	 * Apaga em definitivo quem pediu exclusao ha mais de 30 dias.
	 *
	 * Os filhos saem antes do pai por causa das chaves estrangeiras --
	 * a mesma restricao que devolve 409 numa exclusao comum. Aqui ela
	 * nao e obstaculo: o pedido do titular alcanca todos os dados dele.
	 *
	 * Optou-se por apagar, e nao anonimizar. Anonimizar preservaria
	 * estatisticas, mas numa base pequena a combinacao de areas,
	 * formacoes e vinculos pode reidentificar a pessoa: o dado
	 * supostamente anonimo nao seria anonimo de fato.
	 *
	 * @return quantos cadastros foram apagados
	 */
	@Transactional
	public int expurgarPrazoVencido() {
		LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_ATE_EXCLUSAO_DEFINITIVA);
		List<Profissional> vencidos = profissionalRepository.findByDataExclusaoSolicitadaBefore(limite);

		for (Profissional profissional : vencidos) {
			Long id = profissional.getId();

			List<Certificado> certificados = certificadoRepository.findByProfissionalId(id);
			certificadoRepository.deleteAll(certificados);
			// O PDF do certificado e dado do titular como o resto. Sem
			// isto ele sobrevivia a exclusao definitiva, orfao na pasta de
			// uploads. Achado pelo teste de integracao.
			certificados.forEach(certificado ->
					arquivoCertificadoService.apagarAoConfirmar(certificado.getArquivoPath()));
			formacaoRepository.deleteAll(formacaoRepository.findByProfissionalId(id));
			vinculoEmpregaticioRepository.deleteAll(vinculoEmpregaticioRepository.findByProfissionalId(id));
			profissionalAreaRepository.deleteAll(profissionalAreaRepository.findByIdProfissionalId(id));
			contatoRepository.deleteAll(contatoRepository.findByProfissionalId(id));
			enderecoRepository.findByProfissionalId(id).ifPresent(enderecoRepository::delete);

			// A conta de acesso e o convite que a originou tambem
			// apontam para o profissional, e as chaves estrangeiras nao
			// tem ON DELETE CASCADE: sem apagar os dois antes, o delete
			// abaixo viola a restricao.
			//
			// Nao e detalhe de implementacao. O laco inteiro roda numa
			// transacao so, as 3h da manha: uma violacao reverteria o
			// expurgo de TODOS os cadastros vencidos, e a unica pista
			// seria uma linha de log. O sistema deixaria de cumprir a
			// eliminacao prometida pelo Art. 18 em silencio.
			//
			// Apagar a conta tambem e o comportamento certo em si:
			// eliminar os dados da pessoa e manter o acesso dela seria
			// contraditorio.
			conviteRepository.findByProfissionalId(id).ifPresent(conviteRepository::delete);
			usuarioRepository.findByProfissionalId(id).ifPresent(usuarioRepository::delete);

			profissionalRepository.delete(profissional);

			// Sem CPF nem nome: o log registra que a eliminacao
			// ocorreu, nao quem era a pessoa.
			logAcessoService.registrar(AcaoLog.EXCLUSAO, LogAcessoService.ENTIDADE_PROFISSIONAL, id, null,
					"Exclusao definitiva apos a janela de " + DIAS_ATE_EXCLUSAO_DEFINITIVA + " dias");
			log.info("Cadastro {} apagado definitivamente apos a janela de arrependimento", id);
		}

		return vencidos.size();
	}

}
