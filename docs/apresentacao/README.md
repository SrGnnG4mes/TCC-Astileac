# Apresentação do sistema (Laboratório de Desenvolvimento de Software)

| O quê | Onde |
|---|---|
| Slides | `apresentacao.html` (abrir no navegador: setas passam, **F** tela cheia, **N** notas) |
| Banco da demonstração | `astileac_apresentacao` (PostgreSQL local), **só dados fictícios** |
| PDFs dos certificados | `backend/uploads/apresentacao/` (fora do Git) |
| Login e senha da demonstração | `docs/apresentacao/contas-da-apresentacao.txt` (fora do Git) |

O banco da apresentação é separado de propósito: a demonstração altera dados
(dá o visto no documento do Bruno, pode emitir convite e criar cadastro), e
isso não pode mexer no banco de uso da associação (`astileac`) nem no da
sessão com a diretoria (`astileac_validacao`).

## O que tem no banco

Os mesmos dez associados fictícios da validação (ver `docs/validacao/README.md`),
criados pelo mesmo script, e uma conta da diretoria, `diretoria1`, que também é
associada ("Membro da Diretoria 1"). Os que aparecem no roteiro da demonstração:

| Associado | Na demonstração |
|---|---|
| Ana Paula Ribeiro | Jurídica e educacional: aparece **uma vez** na consulta jurídica; certificado com visto |
| Bruno Cavalcante Lima | Jurídica; certificado com documento anexado, sem visto: é nele que se dá o visto |
| Isabela Pinto Queiroz | Jurídica e saúde; terceira pessoa da consulta jurídica |
| Carla Menezes Duarte | Jurídica, **inativa**: só aparece com a situação "todas" |

## No dia

O PostgreSQL já roda sozinho como serviço do Windows. São dois terminais (Git Bash).

Back-end, apontado para o banco da apresentação:

```bash
cd backend
export JAVA_HOME=~/.jdks/jbr-21.0.11
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/astileac_apresentacao --app.uploads.dir=uploads/apresentacao"
```

Pronto quando aparecer `Started BackendApplication`. Depois, o front-end:

```bash
cd frontend
npm start
```

Abra `http://localhost:4200/login` e entre com `diretoria1`. Digite a senha
com o projetor desligado ou a tela espelhada desativada.

Ao terminar, Ctrl+C nos dois terminais. Para voltar ao sistema normal, suba o
back-end **sem** os argumentos (`./mvnw spring-boot:run`).

## Para recomeçar do zero

Depois de um ensaio, para a demonstração voltar ao estado inicial (Bruno sem visto,
nenhum convite emitido):

1. Desligue o back-end. No `psql`: `DROP DATABASE astileac_apresentacao;` e
   `CREATE DATABASE astileac_apresentacao;`. Apague `backend/uploads/apresentacao/`.
2. Suba o back-end uma vez com a conta de instalação, que só é criada com o banco vazio:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/astileac_apresentacao --app.uploads.dir=uploads/apresentacao --app.admin.login=instalacao --app.admin.senha-inicial=<uma senha>"
   ```
3. Na raiz do repositório:
   ```bash
   ASTILEAC_ADMIN_LOGIN=instalacao ASTILEAC_ADMIN_SENHA=<a mesma senha> node docs/validacao/preparar.mjs --contas 1 --saida docs/apresentacao/contas-da-apresentacao.txt
   ```
   O `--saida` é o que impede de sobrescrever as senhas da sessão de validação.
4. A conta pessoal do apresentador (`gabriel.nogueira`, diretoria) some junto com o
   banco. Para recriá-la, pelo próprio sistema: entre como `diretoria1`, emita um
   convite em **Convites**, faça o cadastro pelo link numa janela anônima (CPF
   fictício, e-mail `@example.org`) e conceda o perfil em **Contas de acesso**. A senha
   dessa conta não fica registrada em nenhum arquivo do repositório.
