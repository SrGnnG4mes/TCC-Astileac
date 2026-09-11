package br.edu.ifac.astileac.backend.config;

import br.edu.ifac.astileac.backend.entity.Usuario;
import br.edu.ifac.astileac.backend.repository.UsuarioRepository;
import br.edu.ifac.astileac.backend.security.Perfil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria a primeira conta administrativa, e so ela (RF09).
 *
 * Ha um problema de partida: so um ADMIN pode cadastrar usuarios, mas
 * no banco vazio nao existe nenhum ADMIN. Tres saidas eram possiveis:
 *
 * 1. Semear um admin na migration com senha conhecida. Descartada: a
 *    senha padrao iria para o Git e valeria em qualquer instalacao que
 *    esquecesse de troca-la.
 * 2. Criar a conta por SQL na mao. Funciona, mas exige gerar o hash
 *    BCrypt fora do sistema.
 * 3. Ler a senha de uma propriedade local (esta aqui). O
 *    application-local.properties nao vai para o Git, entao a senha
 *    nao e versionada, e o hash e gerado pelo proprio sistema.
 *
 * So roda com o banco sem nenhum usuario. Se a propriedade nao estiver
 * definida, nao cria nada e avisa no log -- e melhor subir sem admin
 * do que subir com um admin de senha previsivel.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdmin implements ApplicationRunner {

	@Value("${app.admin.login:}")
	private String login;

	@Value("${app.admin.senha-inicial:}")
	private String senhaInicial;

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (usuarioRepository.count() > 0) {
			return;
		}

		if (login.isBlank() || senhaInicial.isBlank()) {
			log.warn("Nenhum usuario cadastrado e app.admin.login/app.admin.senha-inicial nao definidos. "
					+ "Defina os dois em application-local.properties para criar a conta administrativa inicial.");
			return;
		}

		Usuario admin = new Usuario();
		admin.setLogin(login.trim());
		// Hash gerado aqui: a senha em texto so existe na propriedade
		// local e nunca chega ao banco.
		admin.setSenhaHash(passwordEncoder.encode(senhaInicial));
		admin.setPerfil(Perfil.ADMIN.name());
		// Conta administrativa nao corresponde a um associado.
		admin.setProfissional(null);

		usuarioRepository.save(admin);
		log.info("Conta administrativa inicial criada: {}. Troque a senha e remova "
				+ "app.admin.senha-inicial do application-local.properties.", admin.getLogin());
	}

}
