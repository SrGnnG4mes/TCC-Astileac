# Sessão de validação com a diretoria

Ambiente da sessão descrita no roteiro de validação: um banco **só com dados
fictícios**, separado do banco de uso da associação (`astileac`), que fica
intocado.

| O quê | Onde |
|---|---|
| Banco da sessão | `astileac_validacao` (PostgreSQL local) |
| PDFs anexados aos certificados | `backend/uploads/validacao/` (fora do Git) |
| Logins e senhas provisórias | `docs/validacao/contas-da-sessao.txt` (fora do Git) |
| PDF de exemplo para a tarefa B10 | `Postman/certificado-exemplo.pdf` |
| Script que monta tudo | `docs/validacao/preparar.mjs` |

## O que já está no banco

Dez associados fictícios, pensados para as tarefas do roteiro, e quatro contas
da diretoria (`diretoria1` a `diretoria4`), uma para cada membro presente. Cada
conta da diretoria é também um associado ("Membro da Diretoria N"), como na
prática: é ela que o membro usa na tarefa C10, ao trocar para "Minha área".

| Associado | Áreas | Para que serve no roteiro |
|---|---|---|
| Ana Paula Ribeiro | jurídica e educacional | Aparece **uma vez** na consulta da área jurídica, mesmo atuando em duas (C1); certificado com visto |
| Bruno Cavalcante Lima | jurídica | Consulta jurídica; certificado com documento anexado e ainda sem visto, que serve para dar o visto; vínculo encerrado |
| Carla Menezes Duarte | jurídica | **Inativa**: some da consulta de ativos e aparece com o filtro "todas" |
| Isabela Pinto Queiroz | saúde e jurídica | Consulta jurídica; certificado sem documento |
| Daniel Freitas Moura | saúde | Consulta da área da saúde |
| Elisa Martins Barros | educacional | Duas formações, Lattes, certificado com visto |
| Gabriela Rocha Prado | educacional e saúde | Sem certificado: a ficha mostra "Nenhum certificado registrado" |
| Fábio Teixeira Sales | artístico-cultural | Sem endereço: a ficha mostra "Nenhum endereço informado" |
| Hugo Almeida Castro | outras | Guia-interpretação; documento anexado |
| João Victor Farias | educacional | Documento anexado |

Nomes inventados, CPFs gerados (com dígitos verificadores válidos, para passar
na validação) e e-mails em `example.org`, domínio reservado para exemplos.

## No dia da sessão

O sistema de uso normal e o da sessão usam a mesma porta. Desligue o primeiro e
suba o segundo apontando para o banco da validação (`java` não está no PATH
desta máquina: `JAVA_HOME=~/.jdks/jbr-21.0.11`):

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/astileac_validacao --app.uploads.dir=uploads/validacao"
```

Em outro terminal, o front-end. Só neste computador:

```bash
cd frontend
npm start
```

Ou pela rede local, para a diretoria usar os próprios computadores (abre em
`http://<IP desta máquina>:4200`; o Node tem permissão no firewall, o Java não,
por isso o acesso passa pelo `ng serve`):

```bash
cd frontend
npx ng serve --host 0.0.0.0
```

Ao terminar, desligue os dois e volte a subir o sistema normal. Com o sistema
aberto na rede, qualquer computador da mesma rede alcança a tela de login.

## Para recomeçar do zero

Depois da sessão, ou para um ensaio antes dela:

1. Apague e recrie o banco: `DROP DATABASE astileac_validacao;` e
   `CREATE DATABASE astileac_validacao;`. Apague também `backend/uploads/validacao/`.
2. Suba o back-end uma vez com a conta de instalação, que só é criada com o banco
   vazio:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/astileac_validacao --app.uploads.dir=uploads/validacao --app.admin.login=instalacao --app.admin.senha-inicial=<uma senha>"
   ```
3. Rode o script, na raiz do repositório:
   ```bash
   ASTILEAC_ADMIN_LOGIN=instalacao ASTILEAC_ADMIN_SENHA=<a mesma senha> node docs/validacao/preparar.mjs --contas 4
   ```
   `--contas` é o número de membros da diretoria presentes. O script se recusa a
   rodar num banco que já tenha cadastros.
