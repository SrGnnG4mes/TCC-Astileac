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

RF08 é o caso de uso mais importante na prática: é como a associação atende a uma demanda
("preciso de um intérprete para audiência judicial").

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

### Próximos passos (nesta ordem)
1. ~~Migrations Flyway `V1` — criar as 9 tabelas + carga inicial das áreas de atuação~~ ✅
2. ~~Entidades JPA + repositories~~ ✅
3. CRUD de profissional (service, DTOs, controller) — o caminho completo de uma feature
4. Demais cadastros: endereço, contatos, áreas, formações, certificados, vínculos
5. Consulta e filtro por área de atuação (RF08)
6. Autenticação e perfis (RF09)
7. Front-end Angular: formulários e telas de consulta
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
