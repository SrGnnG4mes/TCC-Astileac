package br.edu.ifac.astileac.backend.service;

import br.edu.ifac.astileac.backend.dto.CadastroPorConviteDTO;
import br.edu.ifac.astileac.backend.dto.ConviteEmitidoDTO;
import br.edu.ifac.astileac.backend.dto.ConviteResponseDTO;
import br.edu.ifac.astileac.backend.dto.ConviteValidacaoDTO;
import br.edu.ifac.astileac.backend.entity.Contato;
import br.edu.ifac.astileac.backend.entity.Convite;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.entity.TipoContato;
import br.edu.ifac.astileac.backend.entity.Usuario;
import br.edu.ifac.astileac.backend.exception.ConflitoException;
import br.edu.ifac.astileac.backend.exception.CpfJaCadastradoException;
import br.edu.ifac.astileac.backend.exception.RegraDeNegocioException;
import br.edu.ifac.astileac.backend.repository.ContatoRepository;
import br.edu.ifac.astileac.backend.repository.ConviteRepository;
import br.edu.ifac.astileac.backend.repository.ProfissionalRepository;
import br.edu.ifac.astileac.backend.repository.UsuarioRepository;
import br.edu.ifac.astileac.backend.security.Perfil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convites de cadastro (RF12).
 *
 * O fluxo da entidade: a pessoa paga para se associar, a diretoria
 * confirma o pagamento e emite um convite, o convite chega por e-mail e
 * serve uma vez so. E ele que liga "quem esta se cadastrando" a "quem a
 * associacao reconheceu como associado" -- sem essa ligacao, qualquer
 * um que soubesse um CPF abriria conta em nome de outra pessoa e
 * alcancaria os dados pessoais dela.
 *
 * A diretoria so emite. Todo o preenchimento fica com quem se associa.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConviteService {

	/** Prazo de validade do convite. */
	public static final int DIAS_DE_VALIDADE = 7;

	/** 32 bytes = 256 bits de aleatoriedade. Nao se adivinha. */
	private static final int BYTES_DO_TOKEN = 32;

	private static final SecureRandom ALEATORIO = new SecureRandom();

	private final ConviteRepository conviteRepository;
	private final ProfissionalRepository profissionalRepository;
	private final UsuarioRepository usuarioRepository;
	private final ContatoRepository contatoRepository;
	private final PasswordEncoder passwordEncoder;
	private final LogAcessoService logAcessoService;

	/**
	 * Emite um convite. O token em texto e devolvido UMA vez: o banco
	 * guarda so o hash, entao nao ha como recupera-lo depois.
	 */
	@Transactional
	public ConviteEmitidoDTO emitir(String email, String emitidoPor) {
		String token = gerarToken();

		Convite convite = new Convite();
		convite.setEmail(email.trim().toLowerCase());
		convite.setTokenHash(hashDe(token));
		convite.setDataEmissao(LocalDateTime.now());
		convite.setDataExpiracao(LocalDateTime.now().plusDays(DIAS_DE_VALIDADE));
		convite.setEmitidoPor(emitidoPor);

		Convite salvo = conviteRepository.save(convite);

		// Nao loga o token, obviamente -- so que houve emissao.
		log.info("Convite {} emitido por {}", salvo.getId(), emitidoPor);

		return new ConviteEmitidoDTO(
				salvo.getId(),
				salvo.getEmail(),
				token,
				salvo.getDataExpiracao(),
				"Entregue este codigo a pessoa convidada. Ele serve uma vez so e expira em "
						+ DIAS_DE_VALIDADE + " dias. Nao e possivel consulta-lo depois: "
						+ "se perder, emita um novo convite.");
	}

	@Transactional(readOnly = true)
	public List<ConviteResponseDTO> listar() {
		return conviteRepository.findAll().stream().map(this::paraResposta).toList();
	}

	@Transactional(readOnly = true)
	public List<ConviteResponseDTO> listarPendentes() {
		return conviteRepository.findByDataConsumoIsNullOrderByDataEmissaoDesc()
				.stream().map(this::paraResposta).toList();
	}

	/**
	 * Consulta publica, para a tela de cadastro saber se vale mostrar o
	 * formulario.
	 *
	 * Um convite invalido nao revela se chegou a existir: a resposta e a
	 * mesma para token inventado e para token ja usado. Dizer "este
	 * convite ja foi usado" confirmaria a um estranho que aquele codigo
	 * existe.
	 */
	@Transactional(readOnly = true)
	public ConviteValidacaoDTO validar(String token) {
		Optional<Convite> encontrado = conviteRepository.findByTokenHash(hashDe(token));

		if (encontrado.isEmpty() || !encontrado.get().estaValido()) {
			return new ConviteValidacaoDTO(false, null,
					"Convite invalido, ja utilizado ou expirado. Procure a diretoria da associacao.");
		}

		return new ConviteValidacaoDTO(true, encontrado.get().getEmail(), null);
	}

	/**
	 * Consome o convite e cria o cadastro e a conta de uma vez.
	 *
	 * Tudo numa transacao so: se qualquer passo falhar -- CPF ja
	 * existente, login em uso -- o convite volta a valer e a pessoa pode
	 * tentar de novo. Seria ruim queimar o convite por um erro de
	 * digitacao.
	 */
	@Transactional
	public Long cadastrar(CadastroPorConviteDTO dados) {
		Convite convite = conviteRepository.findByTokenHash(hashDe(dados.token()))
				.filter(Convite::estaValido)
				.orElseThrow(() -> new RegraDeNegocioException(
						"Convite invalido, ja utilizado ou expirado. Procure a diretoria da associacao."));

		// Antes de criar qualquer coisa: o banco decide quem ganhou a
		// corrida por este convite. Zero linhas = outra requisicao
		// chegou primeiro.
		LocalDateTime agora = LocalDateTime.now();
		if (conviteRepository.marcarConsumido(convite.getId(), agora) == 0) {
			throw new ConflitoException("Este convite ja foi utilizado.");
		}

		// O UPDATE acima e em massa: altera a linha no banco mas NAO a
		// entidade ja carregada nesta transacao, que continua com
		// dataConsumo nula. Sem esta linha, o save() do fim deste metodo
		// gravaria o estado obsoleto por cima e ZERARIA o consumo --
		// deixando o convite valido para sempre.
		//
		// Descoberto rodando contra o banco: o teste de unidade nao
		// alcanca isso, porque com repositorio simulado nao existe
		// contexto de persistencia para ficar obsoleto.
		convite.setDataConsumo(agora);

		if (profissionalRepository.existsByCpf(dados.cpf())) {
			// Mensagem sem repetir o CPF, pelo mesmo motivo do RF01.
			throw new CpfJaCadastradoException();
		}

		if (usuarioRepository.existsByLogin(dados.login())) {
			throw new ConflitoException("Este login ja esta em uso. Escolha outro.");
		}

		Profissional profissional = new Profissional();
		profissional.setNome(dados.nome().trim());
		profissional.setCpf(dados.cpf());
		profissional.setDataNascimento(dados.dataNascimento());
		// Quem define quando a pessoa passou a ser associada e a
		// associacao, ao reconhecer o pagamento -- nao o formulario.
		profissional.setDataAssociacao(LocalDate.now());
		profissional.setAtivo(true);
		Profissional criado = profissionalRepository.save(profissional);

		// O e-mail do convite ja e conhecido: vira o contato principal,
		// para a pessoa nao redigitar o que a associacao ja sabe.
		Contato contato = new Contato();
		contato.setProfissional(criado);
		contato.setTipo(TipoContato.EMAIL);
		contato.setValor(convite.getEmail());
		contato.setPrincipal(true);
		contatoRepository.save(contato);

		Usuario usuario = new Usuario();
		usuario.setProfissional(criado);
		usuario.setLogin(dados.login().trim());
		usuario.setSenhaHash(passwordEncoder.encode(dados.senha()));
		// Nunca ADMIN por este caminho: ninguem se promove preenchendo
		// formulario. O perfil administrativo so se concede (RF12).
		usuario.setPerfil(Perfil.PROFISSIONAL.name());
		usuarioRepository.save(usuario);

		convite.setProfissional(criado);
		conviteRepository.save(convite);

		// Sem nome nem CPF no registro: anota que houve cadastro, nao
		// quem e a pessoa.
		logAcessoService.registrar(
				br.edu.ifac.astileac.backend.entity.AcaoLog.CRIACAO,
				LogAcessoService.ENTIDADE_PROFISSIONAL, criado.getId(), null,
				"Cadastro criado pelo proprio associado via convite " + convite.getId());

		return criado.getId();
	}

	// ---------- token ----------

	private String gerarToken() {
		byte[] bytes = new byte[BYTES_DO_TOKEN];
		ALEATORIO.nextBytes(bytes);
		// Sem padding e seguro para URL: o token viaja em link de e-mail.
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * SHA-256, e nao BCrypt, de proposito.
	 *
	 * O BCrypt e lento para proteger segredo de baixa entropia escolhido
	 * por humano. Este token tem 256 bits de aleatoriedade e nao se
	 * quebra por forca bruta. Alem disso o BCrypt usa sal por registro,
	 * o que obrigaria a varrer a tabela comparando hash a hash para
	 * achar um convite; com SHA-256 a busca e direta, pelo indice.
	 */
	private String hashDe(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			// SHA-256 e obrigatorio em toda JVM; se faltar, nao ha
			// recuperacao sensata.
			throw new IllegalStateException("SHA-256 indisponivel nesta JVM", e);
		}
	}

	private ConviteResponseDTO paraResposta(Convite convite) {
		String situacao;
		if (convite.estaConsumido()) {
			situacao = "utilizado";
		} else if (convite.estaExpirado()) {
			situacao = "expirado";
		} else {
			situacao = "pendente";
		}

		return new ConviteResponseDTO(
				convite.getId(),
				convite.getEmail(),
				convite.getDataEmissao(),
				convite.getDataExpiracao(),
				convite.getDataConsumo(),
				situacao,
				convite.getProfissional() != null ? convite.getProfissional().getId() : null);
	}
}
