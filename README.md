# Sistema ASTILEAC

Sistema web de cadastro e gestão de profissionais para a **Associação dos Profissionais
Tradutores e Intérpretes de Língua Brasileira de Sinais no Estado do Acre (ASTILEAC)**.

Trabalho de Conclusão de Curso — Tecnologia em Sistemas para Internet
Instituto Federal do Acre (IFAC)

- **Autor:** Gabriel Nascimento Nogueira
- **Orientador:** Prof. Dr. Marlon Amaro Coelho Teixeira

---

## Sobre

O sistema centraliza, em uma base única e consultável, os dados profissionais dos
associados: dados pessoais, endereço, contatos, áreas de atuação, formações,
certificados e vínculos empregatícios.

## Tecnologias

| Camada        | Tecnologia                          |
|---------------|-------------------------------------|
| Back-end      | Java 21 + Spring Boot 4.1           |
| Persistência  | Spring Data JPA + PostgreSQL        |
| Migrations    | Flyway                              |
| Segurança     | Spring Security                     |
| Documentação  | springdoc-openapi (Swagger UI)      |
| Front-end     | Angular 22                          |

## Estrutura do repositório

```
astileac/
├── backend/       API REST em Spring Boot
├── frontend/      Aplicação Angular
├── docs/          Documentação do TCC (LaTeX), diagramas e medições
│   ├── tcc/       Fonte LaTeX do TCC
│   └── medicao/   Medição do índice da consulta por área (RF08)
├── Postman/       Coleção de demonstração da API
└── README.md
```

## Como executar

### Pré-requisitos
- JDK 21
- Node.js 24 LTS
- PostgreSQL 16+

### Banco de dados
```sql
CREATE DATABASE astileac;
```

### Back-end
```bash
cd backend
cp src/main/resources/application-example.properties src/main/resources/application-local.properties
# edite application-local.properties com as suas credenciais
./mvnw spring-boot:run
```
A API fica sob `http://localhost:8080/api`.
Swagger UI: `http://localhost:8080/swagger-ui.html` (ligado só no perfil local).

### Front-end, em desenvolvimento
```bash
cd frontend
npm install
npm start
```
A aplicação sobe em `http://localhost:4200`, e o proxy do `ng serve` repassa
`/api` ao back-end.

### Tudo num processo só
O back-end também serve o Angular compilado: uma porta, um processo, sem
`ng serve`.
```bash
cd frontend && npm run build
cd ../backend && ./mvnw spring-boot:run
```
A aplicação inteira fica em `http://localhost:8080`. Para gerar um pacote
único, `./mvnw package` produz `backend/target/backend-0.0.1-SNAPSHOT.jar`,
que roda com `java -jar`. O `application-local.properties` **não** vai para
dentro do `.jar`: na máquina de destino ele fica ao lado do arquivo, ou as
credenciais vêm de variáveis de ambiente (`SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).

## Testes

```bash
cd backend
./mvnw test                    # testes de unidade
./mvnw verify -Pintegracao     # unidade + integração contra PostgreSQL
```
Os testes de integração exigem um banco separado, que eles limpam a cada
caso e se recusam a rodar em qualquer outro:
```sql
CREATE DATABASE astileac_teste;
```

Front-end (Vitest, pelo próprio Angular):
```bash
cd frontend
npx ng test --watch=false      # roda uma vez
npm test                       # fica observando e roda de novo a cada alteração
```

## Modelo de dados

O diagrama entidade-relacionamento do sistema atual está em
`docs/ASTILEAC-BD-atual.mmd` (imagem em `docs/ASTILEAC-BD-atual.png`).
`docs/ASTILEAC-BD.mmd` é o modelo inicial, de agosto, com as nove tabelas
de partida, mantido como registro da evolução.

Os demais diagramas (modelo conceitual, casos de uso, classes, arquitetura,
sequência, estados e navegação) estão em `docs/diagramas/`, com um guia de
uso no TCC. Para regenerar as imagens: `node docs/diagramas/gerar.mjs`.

## Licença

Este projeto é de caráter acadêmico. Ver arquivo `LICENSE`.
