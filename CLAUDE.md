# CLAUDE.md — Contexto do Projeto ASTILEAC

> Este arquivo é lido automaticamente pelo Claude Code no início de cada sessão.
> Ele descreve o projeto, as decisões já tomadas e as regras a seguir.
> **Se alguma decisão mudar, atualize este arquivo** — ele é a fonte de verdade do contexto.

---

## 1. O que é este projeto

Sistema web de **cadastro e gestão de profissionais** para a **ASTILEAC** — Associação dos
Profissionais Tradutores e Intérpretes de Língua Brasileira de Sinais no Estado do Acre.

É um **Trabalho de Conclusão de Curso (TCC)** do curso de Tecnologia em Sistemas para Internet
do Instituto Federal do Acre (IFAC), Campus Rio Branco.

- **Autor:** Gabriel Nascimento Nogueira
- **Orientador:** Prof. Dr. Marlon Amaro Coelho Teixeira

### Problema que resolve
Os dados dos associados (formações, certificados, áreas em que atuam, contatos) hoje ficam
dispersos em planilhas e papel. Isso dificulta a atualização, compromete a confiabilidade e
torna lenta a busca por profissionais habilitados quando chega uma demanda de interpretação.

### Contexto legal (importante para as regras de negócio)
A profissão é regulamentada pela Lei 12.319/2010, alterada pela **Lei 14.704/2023**, que
detalhou os requisitos de titulação para o exercício profissional. Por isso o **registro de
formações e certificados é o núcleo do sistema**, não um detalhe secundário: é o que comprova
a habilitação legal do profissional.

### Áreas de atuação previstas
Educacional, jurídica, saúde, artístico-cultural e outras.
Um profissional pode atuar em **mais de uma área ao mesmo tempo**.

---

## 2. Stack e versões

**Não sugira versões diferentes destas sem avisar. Foram escolhidas deliberadamente.**

| Camada         | Tecnologia                                    |
|----------------|-----------------------------------------------|
| Back-end       | Java 21 + **Spring Boot 4.1.0**               |
| Persistência   | Spring Data JPA + Hibernate                   |
| Banco          | PostgreSQL                                    |
| Migrations     | Flyway (`flyway-core` + `flyway-database-postgresql`) |
| Segurança      | Spring Security                               |
| Documentação   | springdoc-openapi 3.1.0 (Swagger UI)          |
| Front-end      | **Angular 22**                                |
| Build back     | Maven                                         |

### ATENÇÃO — Spring Boot 4, não 3.x
Spring Boot 4.1 é uma versão **major recente**, construída sobre Spring Framework 7 e
Spring Security 7. A maior parte do material disponível na internet (tutoriais, respostas de
fórum, exemplos) ainda mostra sintaxe do **Spring Boot 3.x**, que em vários pontos **não se
aplica aqui** — especialmente na configuração do Spring Security.

**Regra:** quando houver dúvida sobre a API correta no Boot 4, **diga que há incerteza e
consulte a documentação oficial** em vez de gerar código do 3.x. Código que "parece certo"
mas usa API antiga custa horas de depuração.

---

## 3. Estrutura do repositório

```
astileac/
├── backend/       API REST em Spring Boot
├── frontend/      Aplicação Angular
├── docs/          Documento do TCC (LaTeX) e diagramas
│   ├── tcc/                    ← fonte LaTeX (tcc.tex, abntspiifac.cls, refs.bib)
│   ├── ASTILEAC-BD.mmd         ← diagrama entidade-relacionamento (Mermaid)
│   ├── ASTILEAC-BDimage.png    ← DER exportado como imagem
│   └── ASTILEAC_Projeto_TCC.pdf ← documento do TCC compilado
├── Postman/      Coleção de demonstração da API (importar no Postman)
├── .gitignore
├── CLAUDE.md
└── README.md
```

Monorepo proposital: um desenvolvedor só, e facilita mostrar a evolução do projeto no Git.

### Arquitetura em camadas (back-end)
```
controller  →  service  →  repository  →  entity
     ↑
    dto
```
- `controller` — expõe REST, recebe/devolve **DTOs**, nunca entidades
- `service` — regras de negócio e transações
- `repository` — interfaces Spring Data
- `entity` — mapeamento JPA
- `dto` — objetos de entrada/saída da API

**Nunca exponha entidades JPA diretamente nos controllers.** Sempre DTO.

---

## 4. Modelo de dados

O DER completo está em `docs/ASTILEAC-BD.mmd`. São **9 tabelas**:

### PROFISSIONAL (entidade central)
`id` PK, `nome`, `cpf` (único), `data_nascimento`, `data_associacao`, `ativo`

### ENDERECO — 1:1 com profissional
`id` PK, `profissional_id` FK, `cep`, `logradouro`, `numero`, `complemento`, `bairro`, `cidade`, `uf`

### CONTATO — 1:N
`id` PK, `profissional_id` FK, `tipo` (EMAIL / TELEFONE), `valor`, `principal`

### AREA_ATUACAO — tabela de domínio
`id` PK, `nome` (único), `descricao`
Carga inicial: educacional, jurídica, saúde, artístico-cultural, outras.

### PROFISSIONAL_AREA — tabela associativa (N:N)
Chave primária **composta**: `profissional_id` + `area_atuacao_id`. Mais `data_inicio`.

> **Ponto central do modelo.** Um profissional atua em várias áreas e cada área tem vários
> profissionais. **Nunca** substitua isto por colunas booleanas (`atua_educacional`, etc.):
> isso exigiria alterar o schema a cada nova área.

### FORMACAO — 1:N
`id` PK, `profissional_id` FK, `nivel`, `curso`, `instituicao`, `ano_conclusao`

### CERTIFICADO — 1:N
`id` PK, `profissional_id` FK, `formacao_id` FK (**opcional**), `titulo`,
`instituicao_emissora`, `carga_horaria`, `data_emissao`, `arquivo_path`

> `arquivo_path` guarda o **caminho** do arquivo, nunca o binário no banco.
> Uploads vão para `./uploads`, que está no `.gitignore`.

### VINCULO_EMPREGATICIO — 1:N
`id` PK, `profissional_id` FK, `instituicao`, `cargo`, `tipo_vinculo`,
`data_inicio`, `data_fim`, `atual`

> `data_fim` nula = vínculo em andamento. Guardamos o histórico, não só o vínculo atual.

### USUARIO — 1:1 opcional com profissional
`id` PK, `profissional_id` FK, `login` (único), `senha_hash`, `perfil`

> **Sempre `senha_hash`, nunca senha em texto.** BCrypt via Spring Security.

---

## 5. Requisitos funcionais

| Código | Descrição |
|--------|-----------|
| RF01 | Cadastrar profissional com nome e dados de identificação |
| RF02 | Registrar endereço |
| RF03 | Registrar contatos (e-mail e telefone) |
| RF04 | Associar uma ou mais áreas de atuação |
| RF05 | Registrar formações já realizadas |
| RF06 | Cadastrar certificados (instituição emissora, carga horária) |
| RF07 | Registrar vínculos empregatícios atuais |
| RF08 | Consultar e filtrar profissionais, **especialmente por área de atuação** |
| RF09 | Autenticar usuários e controlar acesso por perfil |
| RF10 | Atender aos direitos do titular: exportar os próprios dados e pedir a eliminação deles |
| RF11 | Registrar quem consultou ou alterou dado pessoal |
| RF12 | Gerenciar contas de acesso: criar conta de associado, conceder e retirar o perfil administrativo, trocar a própria senha — **a implementar** |

RF08 é o caso de uso mais importante na prática: é como a associação atende a uma demanda
("preciso de um intérprete para audiência judicial").

### Validação com a diretoria da ASTILEAC (out/2026)
Primeira conversa de validação. O que ficou confirmado:

- **O contato entre contratante e profissional acontece fora do sistema.** Confirmado pela
  diretoria, não mais premissa assumida. O RF08 entrega a localização do profissional; a
  negociação se dá por fora. Isso sustenta o desenho atual e deve ser dito no texto como
  premissa **validada**, não suposta.
- **Cada membro da diretoria terá login próprio**, e não um `admin` compartilhado. Isso é o
  que preserva o sentido do RF11: com login compartilhado o campo `autor` diria sempre a
  mesma coisa e a atribuição do Art. 46 ficaria vazia na prática.
- **Diretor é associado com perfil elevado.** A diretoria descreveu o perfil como "status
  da conta, que dependendo libera ou tira recursos". O modelo **já suporta isso sem
  alteração**: `USUARIO` tem `profissional_id` (1:1 opcional) e `perfil`, então um diretor
  é um `USUARIO` com `perfil = ADMIN` **e** `profissional_id` preenchido apontando para o
  próprio cadastro. Nenhuma migration necessária.
- **Lacuna descoberta na conversa → RF12.** Não existe nenhuma forma de criar ou alterar
  conta: não há `UsuarioController`, e a única conta que o sistema cria é a do
  `BootstrapAdmin`, uma vez, por propriedade local, e só quando não existe usuário algum.
  Sem RF12 a associação não consegue dar acesso a ninguém, ninguém entra ou sai da
  diretoria e ninguém troca a própria senha.
- **Consequência no front-end, ainda aberta:** o login manda `ADMIN` para `/profissionais`,
  e de lá não há caminho para `/dados-pessoais`. Como o diretor agora também é titular, ele
  fica sem acesso aos próprios direitos do Art. 18.

Ainda em aberto com a diretoria: prazo dos 30 dias, excluir × desativar quando é a
diretoria que remove alguém, e se as cinco áreas de atuação da migration `V2` são as
corretas (saíram da literatura, não da prática da entidade).

---

## 6. Regras do projeto

### Segurança e LGPD (inegociável)
- **Nunca** commitar credenciais. `application-local.properties` está no `.gitignore`;
  o que vai para o Git é `application-example.properties` com valores fictícios.
- **Nunca** commitar dados reais de associados nem arquivos de certificados.
- Senhas sempre com hash (BCrypt). Nunca logar senha, CPF completo ou dados pessoais.
- O sistema trata dados pessoais e está sujeito à LGPD (Lei 13.709/2018).

### Banco de dados
- **O Flyway é o dono do schema.** `spring.jpa.hibernate.ddl-auto=validate`.
- **Nunca** use `ddl-auto=update` ou `create`.
- Toda mudança de schema = nova migration `V<n>__descricao.sql`. Migrations já aplicadas
  **nunca** são editadas; corrige-se com uma nova.

### Código
- Validação de entrada com Bean Validation (`@NotBlank`, `@Email`, `@Size`) nos DTOs.
- Lombok nas entidades para evitar boilerplate.
- Nomes de tabelas e colunas em `snake_case` e **sem acentos**.
- Classes, métodos e variáveis em português são aceitáveis (domínio em português),
  mas mantenha consistência com o que já existe no projeto.

### Git
- Commits com prefixo: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`.
- Uma branch por funcionalidade (`feat/cadastro-profissional`); `main` sempre funcionando.
- Commits frequentes e pequenos — o histórico do Git é usado como registro de evolução no TCC.

---

## 7. Como rodar

```bash
# Back-end
cd backend
./mvnw spring-boot:run          # http://localhost:8080
                                # Swagger: http://localhost:8080/swagger-ui.html

# Front-end
cd frontend
npm install
ng serve                        # http://localhost:4200
```

Banco: `CREATE DATABASE astileac;` e credenciais em `application-local.properties`.

---

## 8. Estado atual e próximos passos

### Já concluído
- Documento do projeto de TCC (LaTeX, no template ABNT do IFAC) — em `docs/tcc/`
- Modelagem do banco: DER validado em Mermaid (`docs/ASTILEAC-BD.mmd`)
- Definição da stack e do `pom.xml` (Spring Boot 4.1.1 — ver nota abaixo)
- Esqueleto do backend: `pom.xml`, `BackendApplication`, pacotes
  `controller/service/repository/entity/dto`, `application.properties` +
  `application-example.properties`
- Migrations Flyway `V1` (schema das 9 tabelas) e `V2` (carga inicial das áreas de atuação)
- Ambiente local validado de ponta a ponta: PostgreSQL 18 instalado, banco `astileac`
  criado, `./mvnw spring-boot:run` sobe a aplicação, Flyway aplica as migrations e o
  endpoint temporário `/hello` responde (protegido pelo login padrão do Spring Security,
  ainda sem autenticação real — isso é o Passo 6)

> Nota: o `pom.xml` usa Spring Boot **4.1.1** (patch mais recente da série 4.1 no momento),
> não 4.1.0 como citado originalmente na seção 2. Mesma API, sem impacto.
> Nomes de starter conferidos no gerador oficial (start.spring.io) porque mudaram do 3.x:
> `spring-boot-starter-webmvc` (não `-web`), `spring-boot-starter-flyway` (starter dedicado,
> além de `flyway-database-postgresql`), e starters de teste por módulo
> (`spring-boot-starter-data-jpa-test`, etc.) em vez de um `spring-boot-starter-test` único.
>
> Maven Wrapper (`./mvnw`, `.mvn/wrapper/`) gerado via start.spring.io. Formato moderno
> (`distributionType=only-script`): não versiona `.jar`, o próprio `./mvnw` baixa o Apache
> Maven na primeira execução.
>
> Entidades JPA para as 9 tabelas, mapeamento **unidirecional** (child → Profissional, sem
> coleções bidirecionais) para simplicidade. `PROFISSIONAL_AREA` usa `@EmbeddedId`
> (`ProfissionalAreaId`) pela chave composta. Único ponto que exigiu atenção: a coluna
> `endereco.uf` é `CHAR(2)` no banco, e por padrão o Hibernate mapeia `String` para
> `VARCHAR` — a validação (`ddl-auto=validate`) só passa com `@JdbcTypeCode(SqlTypes.CHAR)`
> explícito (`columnDefinition` sozinho não afeta a validação, só a geração de DDL).
> Validado rodando a aplicação de verdade contra o banco local antes de commitar.
>
> **CRUD de profissional (Passo 3)** na branch `feat/cadastro-profissional`. DTOs como
> `record` (imutáveis, sem boilerplate). `GlobalExceptionHandler` (`@RestControllerAdvice`)
> centraliza erros: 404 (não encontrado), 409 (CPF duplicado), 400 (Bean Validation).
> `SecurityConfig` com `permitAll` **temporário** — libera os endpoints sem autenticação só
> até o Passo 6 (RF09) implementar login de verdade contra a tabela `USUARIO`; sem isso,
> o Spring Security exigiria login com senha aleatória gerada a cada boot, inviabilizando
> testar no Swagger. Endpoints em `/profissionais` (sem prefixo `/api`, consistente com o
> `/hello` já existente). Testado ponta a ponta via `curl` contra o banco local: criar (201),
> listar/buscar (200), não encontrado (404), CPF duplicado (409), validação (400), atualizar
> (200), remover (204).
>
> **Testes automatizados** para o CRUD de profissional (15 testes, `BUILD SUCCESS`):
> `ProfissionalServiceTest` (unidade, Mockito puro, sem Spring/banco) e
> `ProfissionalControllerTest` (fatia web, `@WebMvcTest` + `MockMvc`, service mockado com
> `@MockitoBean`). Pacotes conferidos direto nos `.jar` do Boot 4.1.1 antes de usar (mudaram
> do 3.x): `@WebMvcTest` agora é `org.springframework.boot.webmvc.test.autoconfigure`,
> `@DataJpaTest` é `org.springframework.boot.data.jpa.test.autoconfigure`, e `@MockBean` foi
> **removido** — usa-se `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`).
>
> **Passo 3.5 — endurecimento** (branch `fix/endurecimento-cadastro`, 21 testes). Decisões que
> valem explicar na banca:
> - **`server.address=127.0.0.1`** no `application.properties`. O Tomcat escuta em todas as
>   interfaces por padrão; com `permitAll` ainda em vigor, qualquer um na mesma rede lia e
>   apagava o cadastro. Expor na rede passa a ser decisão explícita de quem faz o deploy.
> - **`ProfissionalResumoDTO`** (listagem, CPF mascarado `***.***.777-35`) separado do
>   `ProfissionalResponseDTO` (ficha, CPF completo), mais paginação. É o princípio da
>   necessidade da LGPD (Art. 6º, III) aplicado na forma do DTO, não só no discurso.
> - **Mensagem de CPF duplicado não repete o CPF.** Repetir tornava a diferença entre 201 e
>   409 uma consulta de filiação à ASTILEAC.
> - **`@Cpf`** próprio (`validation/`) com cálculo dos dígitos verificadores. O `@Pattern`
>   anterior aceitava `00000000000`.
> - **`GlobalExceptionHandler` herda de `ResponseEntityExceptionHandler`.** Um
>   `@RestControllerAdvice` solto com handler de `Exception` captura também as exceções do
>   Spring e transforma **404, 405 e 415 em 500** — confirmado rodando, não suposto. A classe
>   pai trata cada uma com o status certo; aqui só se padroniza o corpo (`ErroRespostaDTO`).
> - **`WebConfig` com `PageSerializationMode.VIA_DTO`.** Sem isso o Spring Data avisa que o
>   JSON de `PageImpl` não tem estrutura estável — o que quebraria o Angular no Passo 7.
> - **Migration `V3`**: índices nas FKs. O PostgreSQL não os cria sozinho, e a PK composta de
>   `profissional_area` começa em `profissional_id`, então não serve para filtrar só por área
>   — exatamente o RF08.
> - `spring.profiles.active=local` virou **`spring.profiles.default=local`**: fallback em vez
>   de valor fixo brigando com a variável de ambiente.
>
> **Ainda em aberto** (achados da revisão não fechados no 3.5): `DELETE` continua sendo
> exclusão real — virar desativação depende da decisão excluir × desativar; Swagger segue
> ligado, a desligar no perfil do ambiente quando houver deploy; sem `@Version`
> (bloqueio otimista), decisão consciente.

> **RF10 e RF11 — LGPD** (branch `feat/direitos-titular-log-acesso`, migration `V4`).
> - **RF10, direitos do titular (Art. 18).** `GET .../dados-pessoais` exporta tudo sobre a
>   pessoa, com CPF inteiro: o destinatário é o próprio titular, mascarar para ele mesmo não
>   protege ninguém. Inclui formações, certificados e vínculos mesmo antes de esses cadastros
>   terem endpoint — exportação incompleta não cumpre o direito.
> - **Janela de arrependimento de 30 dias.** `DELETE .../dados-pessoais/exclusao` não apaga:
>   marca `data_exclusao_solicitada`, **bloqueia** o cadastro (some das listagens, Art. 18, IV)
>   e devolve **200 com mensagem explícita** do prazo, da data exata e de como desfazer — por
>   isso não é 204 sem corpo. `POST .../exclusao/cancelamento` desfaz. O `ExpurgoAgendado`
>   roda às 3h e apaga em definitivo o que passou do prazo, filhos antes do pai.
>   Optou-se por **apagar e não anonimizar**: numa base pequena, a combinação de áreas,
>   formações e vínculos reidentifica a pessoa.
> - **RF11, registro de acesso (Art. 46).** Capturado num `HandlerInterceptor`, não espalhado
>   pelos services: não há como esquecer de registrar num endpoint novo. Em troca, registra a
>   operação HTTP, não quais campos foram lidos. Grava em transação própria
>   (`REQUIRES_NEW`) — se a operação falhar, o registro de que alguém tentou permanece.
> - **Autor fica `nao-autenticado` até o Passo 6.** Sem RF09 o sistema não tem como saber quem
>   é; é uma linha para trocar quando a autenticação existir.
> - Efeito colateral a saber: o `` carrega os `WebMvcConfigurer` (logo o interceptor)
>   mas não ``, então toda fatia web precisa de ` LogAcessoService`.

> **Passo 4 completo** (branch `feat/formacoes-certificados-vinculos`, migration `V5`, 60 testes).
> - **`V5` tornou `certificado.arquivo_path` opcional.** O certificado é registrado antes de o
>   documento digitalizado ser anexado (upload é o Passo 8); com `NOT NULL`, o cadastro só
>   funcionaria com um caminho inventado — dado falso para satisfazer uma restrição.
> - **`arquivoPath` não é campo do request.** Quem define o caminho de armazenamento é o
>   servidor, no upload. Deixar o cliente escolher é por onde entra *path traversal*.
> - **Certificado só vincula formação do mesmo profissional.** Sem essa checagem bastaria
>   enviar o id da formação de outra pessoa; como formação e certificado comprovam
>   habilitação legal (Lei 14.704/2023), isso corromperia o dado que o sistema existe para
>   tornar confiável.
> - **`atual` do vínculo é derivado, nunca recebido.** `data_fim` nula já significa "em
>   andamento"; aceitar os dois deixaria o cliente gravar uma contradição no banco.
> - Formação valida dois cruzamentos que o Bean Validation não alcança: ano não pode ser
>   futuro nem anterior ao nascimento do profissional.
> - A exportação do RF10 passou a trazer os três cadastros — os DTOs de resposta já tinham
>   sido criados lá, justamente para a exportação não nascer incompleta.

> **RF08 — a consulta que justifica o sistema** (branch `feat/consulta-por-area`).
> `GET /profissionais?area=juridica&nome=maria&ativo=true`, com todos os filtros opcionais.
> - **JPQL explícito, não `Specification`.** É uma consulta só: dá para ler de cima a baixo
>   e defender numa banca. `Specification` seria mais flexível e bem menos legível.
> - **`EXISTS` em vez de `JOIN` no filtro de área.** Com `JOIN`, quem atua em duas áreas
>   apareceria duplicado e estragaria a contagem da paginação. Confirmado rodando.
> - **`CAST(:param AS string)`** é necessário: sem ele o Hibernate não infere o tipo do
>   parâmetro quando ele chega nulo.
> - Filtro nulo ou em branco = "não filtre por isso", então a chamada sem parâmetros é a
>   listagem geral — não existem dois caminhos de código.
> - **O bloqueio da LGPD se propaga**: quem pediu exclusão não aparece na consulta.
>   Verificado nos dois sentidos (some ao pedir, volta ao cancelar).
> - **Pendente de medição:** o índice `idx_profissional_area_area` (V3) só é usado se o
>   PostgreSQL escolher começar pela área; num `EXISTS` correlacionado ele pode preferir a
>   PK composta. Confirmar com `EXPLAIN` sobre volume real antes de afirmar ganho no TCC.

> **RF09 — autenticação e perfis** (branch `feat/autenticacao-e-perfis`, 64 testes).
> - **Sessão com cookie, não JWT.** Logout com JWT exigiria lista de revogação — o estado
>   que o "stateless" prometia evitar. O cookie é `HttpOnly` (XSS não lê); token em
>   `localStorage` seria legível. Escala horizontal, o problema que JWT resolve, não existe
>   num sistema interno com um servidor só.
> - **`GET /autenticacao/csrf` existe por necessidade, não por gosto.** Desde o Security 6 o
>   token é resolvido preguiçosamente: o cookie só é escrito quando alguém **lê** o token.
>   Sem esse endpoint a SPA nunca receberia o cookie e o próprio login seria bloqueado por
>   falta de token. Descoberto rodando — o login dava 403 em vez de 401.
> - **`@PreAuthorize("hasRole(ADMIN) or @autorizacao.eOProprioProfissional(#id)")`.** O perfil
>   fica com o mecanismo padrão do Spring e o bean cuida só da propriedade do registro.
>   Não é estética: com a checagem de perfil dentro do bean, a regra seria intestável com
>   `@WithMockUser`.
> - **O catch-all do `GlobalExceptionHandler` transformava negação de acesso em 500.** Pego
>   pelo teste de autorização, não por leitura. Handler de `AccessDeniedException` agora vem
>   antes do genérico.
> - **Testes de autorização importam o `SecurityConfig` e não usam `addFilters = false`.** Sem
>   isso o method security fica desligado e as anotações passariam despercebidas — o teste
>   diria que está tudo bem sem ter verificado nada.
> - **Bootstrap do primeiro admin por propriedade local**, não por seed em migration: senha
>   padrão em migration iria para o Git. Sem a propriedade, não cria nada e avisa.
> - `/hello` removido: era temporário e cumpriu o papel.

> **Passo 7 — front-end Angular, primeira fatia** (Angular 22.2.0, Node 24.19.0 LTS).
> Projeto criado em `frontend/` com componentes standalone (sem `app.module.ts`), sem SSR,
> CSS puro e rotas por `loadComponent`.
> - **`proxy.conf.json` é a decisão central.** O `ng serve` fica em `:4200` e a API em
>   `:8080`; o proxy faz o navegador ver as duas na **mesma origem**. Sem isso, o
>   interceptor de CSRF do Angular **não age**: ele só anexa `X-XSRF-TOKEN` em URL
>   relativa, e origem cruzada daria 403 sem explicação aparente. Com o proxy também não
>   é preciso `withCredentials` (o cookie viaja sozinho em mesma origem) e o CORS sai da
>   conversa. Por isso toda chamada usa caminho relativo, nunca `http://localhost:8080/...`.
> - **O alvo do proxy é `127.0.0.1:8080`, não `localhost:8080`.** O back-end tem
>   `server.address=127.0.0.1` (só IPv4) e o Node resolve `localhost` preferindo IPv6:
>   `localhost` daria `ECONNREFUSED` em `::1`.
> - **O front-end chama `/api/...`, com o proxy removendo o prefixo (`pathRewrite`).**
>   A primeira versão mapeava os caminhos reais (`/profissionais`, `/autenticacao`,
>   `/areas-atuacao`) e **quebrava link direto**: o proxy repassa ao back-end *toda*
>   requisição naquele caminho, inclusive a navegação do navegador pedindo o HTML, então
>   abrir `localhost:4200/profissionais` devolvia o JSON cru da API em vez da aplicação —
>   recarregar a página na tela de busca, favoritar ou digitar a URL ficava inviável.
>   O prefixo separa os espaços de nome; o back-end **não muda**, continua publicando
>   `/profissionais` sem prefixo. O valor está em `core/api.ts` para virar string vazia
>   quando a aplicação for servida pelo próprio back-end.
>   **Descoberto rodando, não lendo** — compila e funciona em toda navegação interna.
> - **`Pagina<T>` traz os metadados aninhados em `page`**, não no topo:
>   `{"content":[...],"page":{"size","number","totalElements","totalPages"}}`. É o formato
>   do `PageSerializationMode.VIA_DTO` configurado no `WebConfig`. Declarar os campos no
>   topo compila sem erro e falha em silêncio — a tela mostra `NaN`.
> - **`AutenticacaoService.entrar()` renova o CSRF dentro da própria operação**, em vez de
>   depender de alguém ter chamado antes. É a mesma dependência de ordem que quebrou a
>   coleção do Postman três vezes.
> - **Login vai como `application/x-www-form-urlencoded`**, não JSON: o `formLogin` do
>   Spring lê parâmetros de requisição (`login` e `senha`, nomeados no `SecurityConfig`).
> - **Nenhum token é guardado no cliente.** O estado local (`signal`) é só cópia de quem
>   está logado, para a interface decidir o que mostrar; a fonte da verdade é
>   `GET /autenticacao/eu`. Recarregar a página não desloga.
> - A fatia é login + painel mínimo que faz **uma consulta autenticada** — o menor caminho
>   que exercita proxy, cookie, CSRF e form login juntos. As telas do RF08 entram depois
>   que isso estiver verificado contra o back-end rodando.
> - **`java` não está no PATH do Windows**, mas há JDKs em `~/.jdks` (o `jbr-21.0.11`
>   serve, é Java 21). Com `JAVA_HOME` apontado para lá, `./mvnw spring-boot:run`
>   funciona pelo terminal — foi assim que os dois defeitos acima apareceram.

> **Cadastro e direitos do titular na interface** (mesma branch).
> - **`ativo` não entra no formulário de cadastro**, espelhando o `ProfissionalRequestDTO`:
>   profissional novo nasce ativo e desativar é ação própria, não campo livre.
> - **A seção de áreas só aparece na edição.** Associar área exige profissional já
>   criado; por isso, ao cadastrar, a tela passa sozinha para o modo edição.
> - **Validação de CPF replicada no cliente** (`core/validadores.ts`), espelhando a
>   anotação `@Cpf`. É resposta imediata a quem digita, **não** substitui a do servidor.
> - **Achado 13 fechado de ponta a ponta.** O back-end já atendia ao Art. 18 desde a
>   branch `feat/direitos-titular-log-acesso`, mas nenhuma tela dava acesso — e um
>   direito que só o titular pode exercer não se cumpre se ele não alcança. A tela
>   `/dados-pessoais` expõe exportação (com download do JSON **sem recorte**: filtrar ali
>   transformaria "todos os seus dados" no que a tela achou conveniente), o registro de
>   quem acessou, e o pedido de eliminação com confirmação em dois passos exibindo a
>   resposta inteira do servidor — prazo, data da exclusão definitiva e como cancelar.
>   É por isso que aquele endpoint devolve corpo e não 204.
> - Dois tipos corrigidos pelo teste contra a API: o enum `AcaoLog` tem **`EXPORTACAO`**
>   (exportar é ação distinta de consultar), e `origem` vem **nula** quando a ação não
>   veio de requisição HTTP.

### Próximos passos (nesta ordem)
1. ~~Migrations Flyway `V1` — criar as 9 tabelas + carga inicial das áreas de atuação~~ ✅
2. ~~Entidades JPA + repositories~~ ✅
3. ~~CRUD de profissional (service, DTOs, controller) — o caminho completo de uma feature~~ ✅
3.5. ~~Endurecimento: exposição de rede, CPF mascarado + paginação, validação de CPF,
   tratamento de erros, índices~~ ✅
3.7. ~~RF10 (direitos do titular) e RF11 (registro de acesso) — exigências da LGPD~~ ✅

4. ~~Demais cadastros: endereço, contatos, áreas, formações, certificados, vínculos~~ ✅
5. ~~Consulta e filtro por área de atuação (RF08)~~ ✅
6. ~~Autenticação e perfis (RF09)~~ ✅ (falta verificar as regras de perfil com um admin real)
7. Front-end Angular — **em andamento**: projeto criado, login e consulta autenticada
   prontos (a verificar contra o back-end rodando). Falta a busca do RF08, a ficha, os
   formulários de cadastro e as telas da LGPD.
8. Upload de certificados
9. Testes e validação com a ASTILEAC

---

## 9. Como me ajudar melhor

- Este é um **projeto acadêmico de um estudante**. Ao propor algo não trivial,
  **explique o porquê** — o autor precisa defender cada decisão perante uma banca.
- **Prefira simplicidade a sofisticação.** Não introduza padrões, bibliotecas ou
  abstrações extras sem necessidade clara; código que o autor não sabe explicar é um
  problema, não uma vantagem.
- Se eu pedir algo que conflite com as regras acima (ex.: `ddl-auto=update`,
  senha em texto puro, entidade exposta no controller), **aponte o conflito**
  em vez de simplesmente atender.
- Antes de criar arquivos novos, verifique o que já existe no projeto e siga o padrão.
