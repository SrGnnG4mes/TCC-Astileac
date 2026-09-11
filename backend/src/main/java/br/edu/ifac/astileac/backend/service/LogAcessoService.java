package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.LogAcessoResponseDTO;
import br.edu.ifac.astileac.backend.entity.AcaoLog;
import br.edu.ifac.astileac.backend.entity.LogAcesso;
import br.edu.ifac.astileac.backend.security.UsuarioLogado;
import br.edu.ifac.astileac.backend.repository.LogAcessoRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de acesso a dado pessoal (RF11, LGPD Art. 46).
 */
@Service
@RequiredArgsConstructor
public class LogAcessoService {

	/**
	 * Enquanto o RF09 nao existe, nao ha usuario autenticado para
	 * registrar. Trocar por quem esta logado e o trabalho do Passo 6.
	 */
	/**
	 * Registrado quando nao ha ninguem autenticado -- o que, depois
	 * do RF09, so acontece nas rotas abertas. Os registros antigos
	 * mantem este valor, e isso esta correto: eles foram feitos
	 * mesmo sem autenticacao, e reescrever historico e justamente o
	 * que um log nao pode permitir.
	 */
	public static final String SEM_AUTENTICACAO = "nao-autenticado";

	/**
	 * Nome da entidade raiz no log. Igual ao primeiro segmento da URL,
	 * que e como o interceptor a deriva -- interceptor e services
	 * precisam gravar o mesmo valor, senao a consulta por registro nao
	 * encontra nada.
	 */
	public static final String ENTIDADE_PROFISSIONAL = "PROFISSIONAIS";

	private final LogAcessoRepository logAcessoRepository;
	private final UsuarioLogado usuarioLogado;

	/**
	 * Grava em transacao PROPRIA (REQUIRES_NEW). Se a operacao que
	 * gerou o log falhar e sofrer rollback, o registro de que alguem
	 * tentou permanece -- que e justamente o que interessa numa
	 * auditoria.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrar(AcaoLog acao, String entidade, Long registroId, String origem, String detalhe) {
		LogAcesso log = new LogAcesso();
		log.setDataHora(LocalDateTime.now());
		log.setAutor(usuarioLogado.loginAtual());
		log.setOrigem(origem);
		log.setAcao(acao);
		log.setEntidade(entidade);
		log.setRegistroId(registroId);
		log.setDetalhe(detalhe);

		logAcessoRepository.save(log);
	}

	@Transactional(readOnly = true)
	public Page<LogAcessoResponseDTO> consultarPorRegistro(String entidade, Long registroId, Pageable pageable) {
		return logAcessoRepository.findByEntidadeAndRegistroId(entidade, registroId, pageable)
				.map(this::toResponseDTO);
	}

	private LogAcessoResponseDTO toResponseDTO(LogAcesso log) {
		return new LogAcessoResponseDTO(
				log.getId(),
				log.getDataHora(),
				log.getAutor(),
				log.getOrigem(),
				log.getAcao(),
				log.getEntidade(),
				log.getRegistroId(),
				log.getDetalhe()
		);
	}

}
