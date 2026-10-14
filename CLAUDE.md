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
| Back-end       | Java 21 + **Spring Boot 4.1.1**               |
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
│   ├── ASTILEAC-BD-atual.mmd   ← DER do sistema atual, 11 tabelas (Mermaid)
│   ├── ASTILEAC-BD-atual.png   ← DER atual exportado como imagem
│   ├── ASTILEAC-BD.mmd         ← DER inicial (ago/2026), 9 tabelas, registro histórico
│   ├── ASTILEAC-BDimage.png    ← DER inicial exportado como imagem
│   ├── medicao/                ← medição do índice do RF08 (script, saídas, conclusão)
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

O DER atual está em `docs/ASTILEAC-BD-atual.mmd`. São **11 tabelas**: as 9 do modelo
inicial (`docs/ASTILEAC-BD.mmd`, mantido como registro da evolução) mais `LOG_ACESSO` (V4)
e `CONVITE` (V6). As colunas que entraram depois do modelo inicial estão listadas no fim
desta seção.

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

### Colunas acrescentadas depois do modelo inicial
- `profissional.data_exclusao_solicitada` (V4) — pedido de eliminação do titular; bloqueia
  o cadastro durante a janela de 30 dias.
- `profissional.lattes_url` (V8) — opcional, só aceita endereço do `lattes.cnpq.br`.
- `certificado.arquivo_path` passou a ser **opcional** (V5).
- `certificado.validado_em` e `validado_por` (V7) — visto da diretoria no documento.

### LOG_ACESSO (V4) — registro de acesso (RF11)
`id`, `data_hora`, `autor`, `origem`, `acao`, `entidade`, `registro_id`, `detalhe`.
**Sem chave estrangeira, de propósito**: o registro de que alguém acessou um dado precisa
sobreviver à eliminação desse dado.

### CONVITE (V6) — cadastro por convite (RF12)
`id`, `email`, `token_hash` (SHA-256, único), `data_emissao`, `data_expiracao`,
`data_consumo` (nula = ainda não usado), `profissional_id` (único, preenchido no
cadastro), `emitido_por`.

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
| RF12 | Gerenciar contas de acesso: cadastro do associado por convite, conceder e retirar o perfil administrativo, trocar a própria senha |
| RF13 | Anexar o documento digitalizado do certificado e permitir que a diretoria reconheça o documento |

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

### Segunda conversa (out/2026) — fluxo de exclusão confirmado

- **O arranjo de exclusão de dados foi validado como está.** Pedido registrado → cadastro
  bloqueado e fora das consultas → trinta dias de arrependimento → expurgo definitivo, com
  cancelamento possível a qualquer momento dentro do prazo. A diretoria confirmou que é o
  comportamento esperado; **nenhuma alteração foi necessária**.
- Vale registrar a natureza dessa confirmação no texto: não foi requisito que a entidade
  trouxe, foi **decisão de projeto submetida a ela e aprovada**. Os trinta dias, a opção
  por apagar em vez de anonimizar e a mensagem explícita de prazo saíram da análise do
  Art. 18 durante a implementação (RF10) e só depois foram levados à diretoria. No
  capítulo isso se escreve como premissa **validada**, não suposta — do mesmo modo que o
  contato fora do sistema.

### Terceira conversa (out/2026)

- **Excluir × desativar: a diretoria DESATIVA, nunca apaga.** Eram duas coisas confundidas
  numa só. O titular que pede eliminação exerce o Art. 18, e aí apaga mesmo (RF10, com
  janela de arrependimento). A diretoria que tira alguém do quadro — saiu, parou de pagar,
  faleceu — está administrando a associação, não atendendo a um direito; apagar destruiria
  o registro de que a pessoa **foi** associada, que é o que a entidade precisa guardar.
  A exclusão administrativa foi **removida**: `DELETE /profissionais/{id}` deixou de
  existir (405) e deu lugar a `PUT /profissionais/{id}/situacao`.
- **O "reconhecimento" do documento é só um visto.** A diretoria confirmou que quem olha
  reconhece que o documento **confere com o declarado**, e não tem como atestar
  autenticidade junto à instituição emissora. A interface dizia "reconhecido", que se lê
  como atestado — passou a dizer **"documento visto"**, com a ressalva explícita na tela.
  Um sistema que existe para tornar o dado de habilitação confiável não pode afirmar mais
  do que de fato verificou.
- **Toda a diretoria usará o sistema**, cada um com conta própria — sem efeito no código,
  o RF12 já concede perfil a quantas contas forem necessárias.
- **Pagamento (Sicredi) fica FORA do escopo do TCC.** A associação tem conta no Sicredi,
  mas usar API de banco exige contrato, credenciais e homologação — processo de terceiros,
  e integração de pagamento não está em nenhum objetivo específico. O desenho do RF12 já
  separou o gatilho da emissão do convite justamente para isso entrar depois sem
  reescrever nada. Hoje a diretoria confirma o pagamento como já confirma e emite o convite.
- **Hospedagem em aberto, e não bloqueia a defesa.** Nenhum objetivo específico pede
  implantação; a validação pode ser feita na máquina do autor ou pela rede local. Se a
  entidade quiser no ar depois, entram TLS, gestão de credenciais e alguém responsável por
  manter — compromisso contínuo da associação. Vale perguntar ao orientador se o IFAC
  hospeda projeto de aluno.

### Quarta conversa (out/2026) — sistema demonstrado à diretoria

- **Retorno geral positivo**, com uma sugestão de cores adiada pelo autor.
- **Campo Lattes** (migration `V8`). Quem trabalha na área normalmente tem Lattes.
  Coluna no `PROFISSIONAL`, **não** um `CONTATO` de tipo novo: não é meio de contato, é
  referência a currículo público, e forçá-lo em `CONTATO` faria `EMAIL/TELEFONE` perder o
  sentido. Opcional, vazio gravado como nulo, e o endereço é conferido contra o próprio
  `lattes.cnpq.br` — sem isso o campo aceitaria qualquer link e deixaria de significar
  "Lattes".
- **O documento do certificado é opcional, e agora a tela diz isso.** A diretoria decidiu
  confiar na boa-fé de quem declara. Tecnicamente nada mudou — o arquivo já era opcional
  desde a `V5` —, mas o texto fazia a ausência parecer pendência: o selo
  "declarado, sem documento" virou **"sem documento (opcional)"**, "aguardando visto" virou
  **"documento anexado"**, e há aviso em destaque dizendo que um certificado sem arquivo
  vale do mesmo jeito.
  **O registro do certificado continua existindo (RF06)** — dispensou-se a comprovação, não
  o dado. Isso importa para o texto: a justificativa do trabalho apoia-se no registro de
  formações e certificados como núcleo do sistema.

Ainda em aberto com a diretoria: se as cinco áreas de atuação da migration `V2` são as
corretas (saíram da literatura, não da prática da entidade); e **o que fazer com os
cadastros que hoje estão em planilhas e papel** — se ninguém migrar o que já existe, o
sistema entra em operação vazio e a associação continua consultando a planilha.

O ajuste de cores sugerido pela diretoria foi feito com a paleta da logo da associação
(ver "Identidade visual e acessibilidade", seção 8).

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
./mvnw spring-boot:run          # API em http://localhost:8080/api
                                # Swagger: http://localhost:8080/swagger-ui.html
                                # (só no perfil local)

# Front-end em desenvolvimento
cd frontend
npm install
npm start                       # http://localhost:4200, proxy de /api para o back-end

# Tudo num processo só (o back-end serve o Angular compilado)
cd frontend && npm run build
cd ../backend && ./mvnw spring-boot:run      # http://localhost:8080
./mvnw package                  # um .jar com tudo, SEM o application-local.properties

# Testes
./mvnw test                     # unidade
./mvnw verify -Pintegracao      # unidade + integração (banco astileac_teste)
cd frontend && npx ng test --watch=false     # front-end (Vitest)
```

Banco: `CREATE DATABASE astileac;` e credenciais em `application-local.properties`.
Testes de integração: `CREATE DATABASE astileac_teste;` (o Flyway aplica as migrations).

**`java` não está no PATH do Windows** desta máquina: use
`JAVA_HOME=~/.jdks/jbr-21.0.11`.

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
> **Achados da revisão não fechados no 3.5:** `DELETE` como exclusão real — **fechado** na
> terceira conversa com a diretoria (virou desativação); Swagger ligado — **fechado**
> (desligado por padrão, ligado só no perfil local); sem `@Version` (bloqueio otimista) —
> continua, decisão consciente.

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
> - **Índice medido** (`docs/medicao/rf08-indice.md`): com 300 profissionais o PostgreSQL
>   **não usa** o índice `idx_profissional_area_area`, e a consulta leva menos de 0,5 ms com
>   ou sem ele. Passa a usá-lo por volta de 10 mil cadastros (área rara 2,9 vezes mais
>   rápida). O receio de ele preferir a PK composta não se confirmou: o `EXISTS` vira
>   *hashed SubPlan*, que busca pelo índice da área. **Não afirmar ganho de desempenho no
>   volume real**; a justificativa honesta é preparo para crescimento.

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
>   O prefixo separa os espaços de nome. Nesta primeira versão o proxy removia o
>   prefixo e o back-end seguia sem ele; **depois o back-end passou a publicar a API sob
>   `/api` também** (ver "Processo único" mais abaixo), porque o plano de trocar o prefixo
>   por string vazia traria a mesma colisão de volta na implantação.
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

> **RF12 — contas de acesso por convite** (branch `feat/contas-de-acesso-por-convite`,
> migration `V6`, 84 testes). Back-end pronto; falta a interface.
> - **Fluxo definido pela entidade:** a pessoa paga para se associar, a diretoria confirma
>   e emite um convite, o convite chega por e-mail e serve uma vez só. Todo o
>   preenchimento fica com quem se associa — a diretoria só emite.
> - **O convite é a prova de identidade.** Autocadastro puro deixaria qualquer um que
>   soubesse um CPF abrir conta em nome de outra pessoa e alcançar os dados dela.
> - **Token de 256 bits guardado como SHA-256**, nunca em texto. SHA-256 e **não** BCrypt:
>   o BCrypt é lento para proteger segredo fraco escolhido por humano, e o sal por registro
>   obrigaria a varrer a tabela para achar um convite. O token aparece **uma vez**, na
>   emissão; a listagem não o expõe e não há como recuperá-lo.
> - **As duas rotas públicas são POST, inclusive a validação** (que logicamente seria GET):
>   assim o token não vai na URL, que aparece no log do servidor, em proxy e no `Referer`.
> - Convite inválido é **indistinguível** de convite já usado — dizer "já foi usado"
>   confirmaria a um estranho que aquele código existe. Tentativa falhada **não** queima o
>   convite.
> - `dataAssociacao` é definida pelo servidor, e `perfil` nunca vem do formulário.
> - **Nenhuma operação deixa zero administradores** (409 explícito).
> - **Defeito latente corrigido junto:** o expurgo não apagava `USUARIO` nem `CONVITE`, e a
>   FK não tem `ON DELETE CASCADE`. A primeira conta de associado transformaria a rotina das
>   3h em erro; como o laço roda numa transação só, reverteria o expurgo de **todos** os
>   cadastros vencidos — o Art. 18 deixaria de ser cumprido em silêncio.
> - **Defeito encontrado rodando:** o convite de uso único valia para sempre. O código
>   carregava a entidade, fazia o `UPDATE` em massa (que altera o banco e **não** a
>   entidade) e no fim chamava `save()`, gravando o estado obsoleto por cima e zerando o
>   consumo. Os 11 testes de unidade passavam e **não podiam** pegar: com repositório
>   simulado não existe contexto de persistência para ficar obsoleto.
> - **Registro de acesso da criação fica como `nao-autenticado`**, e está certo: quem se
>   cadastra ainda não tem sessão. A atribuição se faz pela cadeia — o registro aponta o
>   convite, e o convite guarda quem o emitiu.

> **Formações, certificados e manutenção das áreas** (front-end).
> - **Formações e certificados entram na própria ficha do profissional**, junto com as
>   áreas, e não em telas separadas: é o registro completo do associado num lugar só, que
>   é o que a diretoria quer ver.
> - **O seletor "formação relacionada" do certificado lista apenas as formações daquele
>   profissional.** Torna visível na tela a regra que o back-end já impunha. Confirmado
>   rodando: certificado apontando para formação de outra pessoa devolve **404**, e não
>   403 — a mensagem não confirma que aquela formação existe pertencendo a terceiro.
> - **Nível da formação usa `datalist`**, não `select`: sugere os níveis comuns sem
>   impedir um valor novo. Fechar a lista repetiria, em miniatura, o erro das colunas
>   booleanas por área.
> - **Tela de manutenção das áreas (`/areas-atuacao`, ADMIN).** É o que torna real a
>   decisão da tabela associativa: sem ela, a vantagem de "área nova não exige alterar o
>   schema" continuaria dependendo de alguém rodando SQL. Remover área vinculada devolve
>   409, com mensagem que diz o que fazer.
>
> **Dois defeitos encontrados rodando, ambos de CSRF:**
> - **A primeira escrita após qualquer login falhava com 403.** O Spring troca o token CSRF
>   ao autenticar (proteção contra fixação de sessão) e descarta o anterior; como a
>   resolução é preguiçosa, o cookie só é reescrito quando alguém **lê** o token. O
>   `entrar()` renovava o CSRF **antes** do login, e nada renovava depois — então o cookie
>   ficava com o valor pré-login. A própria falha fazia o servidor reescrever o cookie, e
>   por isso a segunda tentativa funcionava, o que mascarava o problema. Corrigido
>   renovando o token também **depois** do POST de login.
> - **As telas trocavam a mensagem do 403 por um texto genérico de permissão**, jogando
>   fora a distinção entre falta de permissão e falha de CSRF que o `SecurityConfig` faz de
>   propósito. Foi o que me fez ler a falha acima como problema de perfil. O 403 agora
>   repassa a mensagem do servidor.

> **Autoatendimento do associado** (`/meu-cadastro`).
> - **A diretoria confirmou que quem envia a documentação é o associado.** Isso exigia uma
>   tela que não existia: a rota de edição está atrás do `adminGuard`, porque é a da
>   diretoria administrando qualquer cadastro, e as únicas telas do associado eram o painel
>   e `/dados-pessoais`, só de leitura.
> - **O back-end já autorizava desde o RF09.** Os controllers de profissional, áreas,
>   formações e certificados são
>   `hasRole('ADMIN') or @autorizacao.eOProprioProfissional(...)`. Faltava só o caminho —
>   mesma forma do achado 13: a permissão existia, a tela não. **Nenhuma mudança no
>   back-end.**
> - **A rota não recebe id.** O profissional é o que a sessão diz, nunca o que a URL pede;
>   assim não existe a forma de digitar o id de outra pessoa nesta tela.
> - **CPF e data de associação não são editáveis no autoatendimento.** O CPF é a âncora de
>   identidade, conferida pela entidade no pagamento e usada para impedir cadastro
>   duplicado; a data de associação é definida pela entidade ao reconhecer o pagamento.
>   Correção de CPF errado continua sendo direito do titular (Art. 18, III) e se faz pela
>   diretoria. **É restrição de interface**: a API permite ao titular, e quem autoriza
>   continua sendo o servidor.
> - **`FichaVinculos` é compartilhado** entre a ficha da diretoria e o autoatendimento — as
>   três seções são as mesmas, muda só quem chega nelas. As três ficam juntas num
>   componente, e não em três, porque compartilham estado: o seletor de formação do
>   certificado depende da lista de formações, que muda quando uma é adicionada ou
>   removida.
> - Verificado rodando com uma conta `PROFISSIONAL` real, criada pelo fluxo de convite: ela
>   cria formação, área e certificado próprios e altera o próprio cadastro (201/200), e
>   recebe **403** em tudo que é de outra pessoa ou da diretoria — formações alheias, ficha
>   alheia, listagem do quadro e manutenção do domínio de áreas.

> **Passo 8 e RF13 — documento do certificado e reconhecimento** (migration `V7`, 89 testes).
>
> Terceira decisão vinda da validação com a entidade, depois do RF12. A diretoria definiu:
> quem envia o documento é o associado; ninguém confere de forma rotineira; e **quando a
> diretoria procura quem pode atender uma demanda, reconhece ali o documento ainda não
> visto**. Isso evita uma fila de conferência que talvez nunca fosse trabalhada e amarra o
> esforço ao momento em que ele tem consequência.
> - **Três estados, sem campo de situação:** declarado (`arquivo_path` nulo), enviado
>   (com arquivo, `validado_em` nulo) e reconhecido. A situação é **derivada** na tela; um
>   campo a mais seria estado duplicado podendo divergir.
> - **Substituir o arquivo derruba o reconhecimento.** Sem isso: anexa-se o documento A,
>   espera-se o reconhecimento e troca-se pelo B, que ficaria validado sem nunca ter sido
>   visto. É a regra central do RF13 e tem teste próprio.
> - **O nome do arquivo é gerado (UUID), nunca aproveitado do cliente.** Nome vindo do
>   cliente é o vetor clássico de *path traversal*. Verificado rodando: upload com
>   `filename=../../../invadido.pdf` gravou um UUID na pasta certa e nada fora dela.
> - **Aceita só PDF, conferindo a assinatura `%PDF-` do conteúdo** — content-type e extensão
>   vêm do cliente e são declaração, não prova. Limite de 5 MB no Tomcat e no service.
> - **O caminho do arquivo não sai da API.** O DTO devolve `temDocumento`, e o download tem
>   endpoint próprio: URL estática seria alcançável por quem adivinhasse o caminho, sem
>   passar por autorização.
> - **Reconhecer é só da diretoria.** A anotação de classe permitiria também o titular, e
>   deixar alguém validar o próprio documento esvaziaria o reconhecimento — um
>   `@PreAuthorize` de método sobrepõe o da classe. Verificado: associado anexa e baixa o
>   próprio documento (200) e recebe **403** ao tentar reconhecê-lo.
> - Quem reconheceu e quando ficam **no próprio certificado**, não só no log: é propriedade
>   durável do registro, não linha de auditoria.
> - **Defeito de anotação corrigido junto:** `Certificado.arquivoPath` ainda declarava
>   `nullable = false` depois da `V5`. Não quebrava — com Bean Validation presente o
>   Hibernate desliga a própria checagem de nulidade, e `ddl-auto=validate` não confere
>   nulidade de coluna — mas era anotação mentindo sobre o banco.

> **Interface do RF12** — convites, cadastro público e contas.
> - **`/convites` (ADMIN).** A diretoria emite depois de confirmar o pagamento. O token
>   aparece **uma vez**, com link pronto para copiar; recarregar a página o faz sumir,
>   porque ele só existe em memória. A listagem mostra pendente/utilizado/expirado e
>   **nunca** o código.
>   Emitir à mão é o substituto do gatilho enquanto a integração com o banco não existe —
>   quando ela chegar, chamará o mesmo ponto de emissão. A tela continua útil de todo modo,
>   para a exceção: quem pagou em dinheiro, e-mail que voltou, associado fundador.
> - **`/cadastro?convite=...` (pública).** Única rota sem guarda além do login — quem usa o
>   convite ainda não tem conta. O token chega na URL porque é assim que link de e-mail
>   funciona; dali em diante vai **no corpo**, nunca em outra URL.
> - **`/contas` (ADMIN).** Concede e retira o perfil. Não cria conta: contas nascem pelo
>   convite. A guarda do último administrador aparece com a mensagem do servidor.
> - **Troca de senha no autoatendimento**, exigindo a senha atual.
>
> **Defeito encontrado rodando — o mesmo padrão de CSRF, terceira vez:**
> quem chega por um link de e-mail **nunca leu o token CSRF**, então não tem o cookie — e a
> validação do convite é um POST. A primeira visita à tela de cadastro falhava com 403
> antes de mostrar qualquer campo. Corrigido fazendo as duas chamadas públicas renovarem o
> CSRF **dentro da própria operação**, como o `entrar()` já fazia. A lição que se repete:
> toda operação de escrita precisa ser autossuficiente quanto ao token, nunca depender de
> alguém ter chamado algo antes.
>
> Verificado rodando o fluxo inteiro: emissão → link → validação sem sessão alguma →
> tentativa falhada **sem queimar o convite** → cadastro → convite invalidado → login da
> conta nova caindo no painel como `PROFISSIONAL` → concessão e retirada de perfil →
> **409 ao tentar retirar o último administrador** → troca de senha recusada com a senha
> atual errada e aceita com a correta.

> **Endereço, contatos e vínculos na interface** — fecha o front-end.
> - As três seções entram no `FichaVinculos`, então valem para a ficha da diretoria e para o
>   autoatendimento ao mesmo tempo. Ordem na tela segue a leitura de uma ficha: quem é e
>   como achar (contatos, endereço), o que faz (áreas), o que comprova (formações,
>   certificados), onde trabalhou (vínculos).
> - **Endereço sem cadastro devolve 404, e a tela trata como "não informado"**, não como
>   erro — é o estado normal de uma relação 1:1 opcional. Salvar usa `PUT` tanto para criar
>   quanto para substituir, porque cabe um endereço só.
> - **Contato principal:** marcar um novo desmarca o anterior **do mesmo tipo**, no
>   servidor. O e-mail do convite já aparece como principal — o RF12 alimentando o RF03 sem
>   a pessoa redigitar.
> - **Vínculo em andamento é término em branco.** `atual` é derivado no servidor; a tela
>   não pergunta.
> - O formulário de vincular **área** se chamava `vinculo`, nome que colidia com os vínculos
>   empregatícios; virou `novaArea`.
>
> **Defeito encontrado preenchendo a tela como uma pessoa preencheria:** o CEP digitado com
> hífen (`69900-000`) nunca salvava. A remoção do hífen acontecia na hora de salvar, mas o
> validador do formulário exigia 8 dígitos **antes** — e roda primeiro. O mesmo valia para
> a UF em minúscula. Os dois validadores passaram a aceitar a forma que a pessoa digita, e a
> normalização continua no salvar. Lição: validar no cliente o formato **de entrada**, não
> o formato **de armazenamento**.

> **Pendências do levantamento do capítulo (out/2026)** — testes de integração, processo
> único, Postman, medição do índice e DER.
> - **Testes de integração contra PostgreSQL** (`./mvnw verify -Pintegracao`, banco
>   `astileac_teste`, pacote `integracao`, 11 testes). Ficam fora do `./mvnw test` para quem
>   clona o repositório rodar a suíte de unidade sem banco. **Não são `@Transactional`**: um
>   teste transacional desfaria tudo no fim e esconderia exatamente o que se quer ver (o que
>   o service grava ao confirmar e o que desfaz ao falhar). A base **confere o nome do banco
>   antes do `TRUNCATE`** e se recusa a rodar fora do `astileac_teste`.
>   Cobrem: convite de uso único (inclusive duas tentativas simultâneas), falha no cadastro
>   que não queima o convite (rollback), expurgo completo e o prazo mínimo de 30 dias,
>   documento do certificado acompanhando o registro, consulta do RF08 (EXISTS sem
>   duplicar, filtros, bloqueio da LGPD) e registro de acesso de ponta a ponta.
>   **Conferido que pegam os defeitos:** sem a correção do convite, 2 dos 3 testes falham —
>   inclusive o de concorrência, em que o mesmo convite chegou a criar **dois** cadastros;
>   sem o ajuste do prefixo no interceptor, o do registro de acesso falha.
> - **Defeito encontrado pelos testes de integração: o PDF sobrevivia à exclusão.**
>   Remover um certificado ou expurgar um cadastro apagava as linhas e **deixava o PDF no
>   disco** — dado pessoal órfão, sem registro nenhum que apontasse para ele. O Art. 18 era
>   cumprido no banco e não no disco. Agora o arquivo é apagado **depois que a transação
>   confirma** (`TransactionSynchronization.afterCommit`): arquivo não participa da
>   transação, e apagar antes faria um rollback deixar o registro apontando para nada. A
>   troca de documento passou a usar o mesmo caminho — o comentário antigo dizia "só
>   depois de a troca estar gravada", mas `save()` não é confirmação.
> - **`@WithMockUser` não serve para testar o RF11.** O autor do registro sai do principal,
>   e o usuário simulado do Spring não é `UsuarioAutenticado`: o registro sairia como
>   `nao-autenticado`. O teste usa uma conta de verdade, carregada do banco.
> - **Processo único: o back-end serve o Angular compilado.** `./mvnw package` empacota
>   `frontend/dist/frontend/browser` como conteúdo estático. Exigiu três coisas:
>   - **A API foi para `/api` também no back-end** (`WebConfig.PREFIXO_API`, via
>     `addPathPrefix` restrito ao pacote `controller`, para não mexer no springdoc). Sem
>     prefixo, as rotas do Angular colidem com as da API (`/profissionais`, `/convites`,
>     `/areas-atuacao`). O proxy do `ng serve` deixou de reescrever o caminho: o mesmo
>     `/api` vale nos dois ambientes. **O interceptor do RF11 teve de tirar o prefixo**
>     antes de deduzir entidade e id — sem isso gravaria tudo como entidade `API`, e o
>     titular veria o próprio registro vazio, em silêncio.
>   - **Volta para o `index.html`** em caminho que não é arquivo, para recarga e link
>     direto (inclusive o do convite) funcionarem. Exceções: o que está sob `/api` e o que
>     tem extensão continuam dando 404 de verdade. Na segurança, fora de `/api` só `GET` é
>     público (é o Angular, sem dado nenhum); o resto é `denyAll`.
>   - A página de teste antiga (`static/index.html`) foi removida: o front-end a substituiu.
>   Verificado rodando: login, escrita com CSRF, link do convite aberto sem sessão e
>   registro de acesso gravando a entidade certa.
> - **O `application-local.properties` ia para dentro do `.jar`**, com a senha do banco e a
>   do admin inicial — quem recebesse o pacote extrairia com um unzip. O `maven-jar-plugin`
>   agora o exclui; ele continua em `target/classes` para `spring-boot:run` e testes. Na
>   implantação, fica ao lado do `.jar` ou vem de variável de ambiente.
> - **Swagger desligado por padrão** (`application.properties`), ligado só no perfil local.
>   Conferido no `.jar` sem perfil local: a documentação não existe.
> - **Coleção do Postman atualizada**: `baseUrl` com `/api`; pastas novas para o RF13
>   (anexar, baixar, visto, troca derruba o visto), o RF12 (emissão, validação, cadastro,
>   uso único, login do associado, 403 nos dados alheios e no visto do próprio documento,
>   concessão de perfil) e a situação no quadro (desativar, `DELETE` → 405). CPFs gerados a
>   cada execução e asserções pelos ids da própria execução: **roda várias vezes seguidas**
>   — antes dependia da limpeza por `DELETE`, que não existe mais. Já havia uma asserção
>   quebrada desde o RF13 (`arquivoPath` nulo, campo que saiu da API). Verificada com o
>   newman: 139 asserções, nenhuma falha, em duas execuções seguidas.
> - **DER atualizado** (`docs/ASTILEAC-BD-atual.*`). O inicial tinha três erros de
>   cardinalidade, corrigidos no atual: faltava a relação `PROFISSIONAL`–`CERTIFICADO`;
>   `FORMACAO`–`CERTIFICADO` dizia "no máximo um certificado por formação", quando podem
>   ser vários e a formação é opcional; e `USUARIO` exigia profissional, mas a conta de
>   instalação não tem.

> **Testes do front-end** (Vitest, pelo builder `@angular/build:unit-test`, 35 testes).
> Antes havia só o teste gerado pelo Angular. Escolhidos pelo que já deu errado, não por
> cobertura: cada defeito encontrado rodando o front-end virou um caso.
> - **Ordem das requisições do login:** CSRF antes, POST como formulário, CSRF de novo
>   depois, e só então `/autenticacao/eu`. Login recusado não segue adiante.
> - **Visitante do convite:** validação e cadastro renovam o CSRF dentro da própria
>   operação, e o token vai no corpo, nunca na URL.
> - **CEP com hífen e UF minúscula** passam no formulário e chegam ao servidor
>   normalizados; CEP incompleto não sai do cliente.
> - **403 mostra a mensagem do servidor**, não um texto genérico.
> - **Paginação aninhada em `page`**: a consulta mostra a contagem, sem `NaN`.
> - Os três selos do certificado; botão de visto só para a diretoria e só em documento
>   ainda não visto; endereço 404 como "não informado"; `ativo=false` enviado como
>   filtro (é *falsy*, e um `if` descuidado o descartaria); validador de CPF; guardas.
> - **Conferido que pegam os defeitos:** reintroduzindo cada um — sem o CSRF depois do
>   login, sem o CSRF do convite, o CEP no formato do banco e a mensagem genérica no
>   403 — o teste correspondente falha.
> - Os testes usam `HttpTestingController`: conferem as requisições que o front-end
>   faria, sem servidor. O que só o servidor de verdade mostra continua com os testes de
>   integração do back-end e com a coleção do Postman.

> **Identidade visual e acessibilidade** (paleta da logo da ASTILEAC).
> - **Cores extraídas dos pixels da logo**, não escolhidas a olho: verde `#04A75A`, amarelo
>   `#FEF013` e vermelho `#EC3238` — as cores da bandeira do Acre.
> - **As cores puras da logo reprovam no contraste.** Branco sobre o verde da logo dá 3,1:1,
>   e sobre o vermelho, 4,1:1 — abaixo dos 4,5:1 da WCAG 2.1 AA para texto. Por isso há
>   dois grupos de cor em `styles.css`: as da **marca**, exatas, só em faixas e detalhes
>   sem texto em cima; e as de **interface**, tons mais escuros do mesmo verde e do mesmo
>   vermelho, conferidos (primária `#02703D`, 6,2:1; erro `#AA2428`, 7,0:1). O amarelo
>   entra como fundo claro com texto escuro, e como faixa e contorno de foco sobre o verde
>   escuro (7,4:1).
> - **Cores viraram variáveis.** Eram 275 cores escritas direto em 12 arquivos, sem
>   nenhuma variável; agora cada componente usa `var(--cor-...)` e a paleta mora num lugar só.
> - **Bordas de campo** passaram de 1,6:1 para 3,6:1 (WCAG 1.4.11 pede 3:1 para o contorno
>   de um controle). **Contorno de foco** visível em tudo que recebe foco pelo teclado, não só
>   nos campos.
> - **`lang="pt-BR"`** — estava `en`, e o leitor de tela lia o português com pronúncia de
>   inglês. **Título por página** (WCAG 2.4.2) — toda tela se chamava "Frontend".
> - **Tabelas com rolagem própria** no celular: em cinco telas a página inteira rolava para o
>   lado (WCAG 1.4.10). A área de rolagem recebe foco, para quem usa teclado.
> - **Sem fonte externa:** carregar fonte do Google mandaria o IP de cada usuário a terceiro,
>   num sistema que existe para cumprir a LGPD.
> - **Logo** na barra, no login e no convite, com `alt` vazio porque o nome vem escrito ao
>   lado — o leitor de tela não precisa anunciar a associação duas vezes.
> - **Verificado com o axe-core** (regras WCAG 2.0/2.1 A e AA) em todas as telas, nos três
>   papéis — diretoria, associado e visitante do convite —: **nenhuma violação**. O axe
>   automatiza parte da WCAG, não toda; leitor de tela e navegação completa por teclado
>   ficam para teste manual.

> **Site público** (`frontend/src/app/site/`) — pedido do autor, tomando como referência
> a estrutura do site do SINTRA, sem copiar. Três páginas: **Início**, **Valores de
> referência** e **Associe-se**, com cabeçalho próprio e "Área do associado" levando ao login.
> - **Mesmo projeto, não site à parte:** o "Associe-se" desemboca no fluxo de convite do
>   RF12, e a área do associado é o próprio login. Separado, duplicaria identidade e hospedagem.
> - **Sem diretório público de intérpretes**, ao contrário do SINTRA. A consulta ao quadro é
>   da diretoria (RF08) e o contato acontece fora do sistema — premissa validada. Expor nome
>   e área de cada associado a qualquer visitante iria contra o princípio da necessidade.
> - **Sem formulário de inscrição:** o "Associe-se" descreve os passos reais (falar com a
>   diretoria e pagar → convite por e-mail, 7 dias, uso único → cadastro → completar a ficha).
>   O pagamento ainda é confirmado fora do sistema.
> - **Conteúdo da associação num arquivo só** (`site/conteudo.ts`): valores de referência,
>   vigência e canais de contato. Enquanto estiverem vazios, as páginas dizem que serão
>   publicados — **nunca um valor ou contato inventado**. Há teste para isso.
> - A raiz `/` passou a ser o início do site (antes redirecionava ao login), e caminho
>   inexistente volta para lá. Testado que o site não captura `/login` nem as rotas do sistema.
> - "Pular para o conteúdo" (WCAG 2.4.1), feito com foco programático: no Angular, um link
>   `href="#conteudo"` trocaria a rota para a raiz. axe-core: nenhuma violação nas três páginas.
> - **Em aberto:** os valores de referência e os contatos (o autor vai passar); os textos de
>   "Quem somos" precisam da revisão da diretoria; decidir com o orientador se o site entra
>   como requisito (seria o RF14) ou como complemento.

### Próximos passos (nesta ordem)
1. ~~Migrations Flyway `V1` — criar as 9 tabelas + carga inicial das áreas de atuação~~ ✅
2. ~~Entidades JPA + repositories~~ ✅
3. ~~CRUD de profissional (service, DTOs, controller) — o caminho completo de uma feature~~ ✅
3.5. ~~Endurecimento: exposição de rede, CPF mascarado + paginação, validação de CPF,
   tratamento de erros, índices~~ ✅
3.7. ~~RF10 (direitos do titular) e RF11 (registro de acesso) — exigências da LGPD~~ ✅

4. ~~Demais cadastros: endereço, contatos, áreas, formações, certificados, vínculos~~ ✅
5. ~~Consulta e filtro por área de atuação (RF08)~~ ✅
6. ~~Autenticação e perfis (RF09)~~ ✅ — regras de perfil verificadas com contas reais de
   diretoria e de associado
7. ~~Front-end Angular~~ ✅ — todas as telas: login, cadastro público por convite,
   consulta do RF08, ficha completa (contatos, endereço, áreas, formações, certificados,
   vínculos), telas da LGPD, manutenção das áreas, autoatendimento, convites e contas.
   Identidade visual com a paleta da logo e revisão de acessibilidade feitas.
8. ~~Upload de certificados~~ ✅ — com o reconhecimento pela diretoria (RF13)
9. Testes e validação com a ASTILEAC — **a primeira conversa aconteceu** (ver seção 5);
   faltam os três pontos ainda em aberto e o registro formal para citar no TCC

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
