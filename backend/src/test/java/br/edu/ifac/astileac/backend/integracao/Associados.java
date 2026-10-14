package br.edu.ifac.astileac.backend.integracao;

import br.edu.ifac.astileac.backend.dto.CadastroPorConviteDTO;
import br.edu.ifac.astileac.backend.entity.Certificado;
import br.edu.ifac.astileac.backend.entity.Endereco;
import br.edu.ifac.astileac.backend.entity.Formacao;
import br.edu.ifac.astileac.backend.entity.Profissional;
import br.edu.ifac.astileac.backend.entity.ProfissionalArea;
import br.edu.ifac.astileac.backend.entity.VinculoEmpregaticio;
import br.edu.ifac.astileac.backend.repository.AreaAtuacaoRepository;
import br.edu.ifac.astileac.backend.repository.CertificadoRepository;
import br.edu.ifac.astileac.backend.repository.EnderecoRepository;
import br.edu.ifac.astileac.backend.repository.FormacaoRepository;
import br.edu.ifac.astileac.backend.repository.ProfissionalAreaRepository;
import br.edu.ifac.astileac.backend.repository.ProfissionalRepository;
import br.edu.ifac.astileac.backend.repository.VinculoEmpregaticioRepository;
import br.edu.ifac.astileac.backend.service.CertificadoService;
import br.edu.ifac.astileac.backend.service.ConviteService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.context.TestComponent;

/**
 * Monta associados de teste, do jeito que eles nascem no sistema.
 *
 * O cadastro passa pelo convite, como na vida real: e isso que cria a
 * conta de acesso e o contato principal, e e o que deixa o convite
 * apontando para o profissional -- as tres ligacoes que o expurgo
 * precisa desfazer. Os demais cadastros entram direto pelos
 * repositorios, porque o que se testa aqui e o que acontece com eles
 * depois, nao as regras de cada um.
 *
 * Todos os dados sao ficticios.
 */
@TestComponent
@RequiredArgsConstructor
class Associados {

	private final ConviteService conviteService;
	private final CertificadoService certificadoService;
	private final ProfissionalRepository profissionalRepository;
	private final AreaAtuacaoRepository areaAtuacaoRepository;
	private final ProfissionalAreaRepository profissionalAreaRepository;
	private final EnderecoRepository enderecoRepository;
	private final FormacaoRepository formacaoRepository;
	private final CertificadoRepository certificadoRepository;
	private final VinculoEmpregaticioRepository vinculoEmpregaticioRepository;

	/** Associado so com o que o convite cria: cadastro, conta e e-mail. */
	Long porConvite(String nome, String cpf, String login) {
		String token = conviteService.emitir(login + "@exemplo.com", "diretora").token();
		return conviteService.cadastrar(new CadastroPorConviteDTO(
				token, nome, cpf, LocalDate.of(1990, 5, 20), login, "senha-de-teste-123"));
	}

	/**
	 * Associado com uma linha em cada tabela que aponta para ele --
	 * inclusive o documento do certificado gravado em disco.
	 */
	Long completo(String nome, String cpf, String login) {
		Long id = porConvite(nome, cpf, login);
		Profissional profissional = profissionalRepository.findById(id).orElseThrow();

		Endereco endereco = new Endereco();
		endereco.setProfissional(profissional);
		endereco.setCep("69900000");
		endereco.setLogradouro("Rua de Teste");
		endereco.setNumero("100");
		endereco.setBairro("Centro");
		endereco.setCidade("Rio Branco");
		endereco.setUf("AC");
		enderecoRepository.save(endereco);

		atuaEm(id, "juridica");

		Formacao formacao = new Formacao();
		formacao.setProfissional(profissional);
		formacao.setNivel("Graduacao");
		formacao.setCurso("Letras Libras");
		formacao.setInstituicao("Instituicao de Teste");
		formacao.setAnoConclusao(2015);
		formacaoRepository.save(formacao);

		// Certificado apontando para a formacao: obriga o expurgo a
		// apagar o certificado antes dela.
		Certificado certificado = new Certificado();
		certificado.setProfissional(profissional);
		certificado.setFormacao(formacao);
		certificado.setTitulo("Proficiencia em Libras");
		certificado.setInstituicaoEmissora("Instituicao de Teste");
		certificado.setCargaHoraria(120);
		certificado.setDataEmissao(LocalDate.of(2016, 8, 1));
		certificadoRepository.save(certificado);
		certificadoService.anexarDocumento(id, certificado.getId(), pdf());

		VinculoEmpregaticio vinculo = new VinculoEmpregaticio();
		vinculo.setProfissional(profissional);
		vinculo.setInstituicao("Escola de Teste");
		vinculo.setCargo("Interprete");
		vinculo.setTipoVinculo("CLT");
		vinculo.setDataInicio(LocalDate.of(2018, 2, 1));
		vinculoEmpregaticioRepository.save(vinculo);

		return id;
	}

	void atuaEm(Long profissionalId, String... areas) {
		Profissional profissional = profissionalRepository.findById(profissionalId).orElseThrow();
		for (String area : areas) {
			ProfissionalArea vinculo = new ProfissionalArea();
			vinculo.setProfissional(profissional);
			vinculo.setAreaAtuacao(areaAtuacaoRepository.findByNome(area).orElseThrow());
			vinculo.setDataInicio(LocalDate.of(2021, 1, 1));
			profissionalAreaRepository.save(vinculo);
		}
	}

	/** Comeca com %PDF-, que e o que o servidor confere. */
	static MockMultipartFile pdf() {
		return new MockMultipartFile("arquivo", "certificado.pdf", "application/pdf",
				"%PDF-1.4\n% documento ficticio de teste\n".getBytes(StandardCharsets.US_ASCII));
	}

}
