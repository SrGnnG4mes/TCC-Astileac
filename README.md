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
├── docs/          Documentação do TCC (LaTeX) e diagramas
└── README.md
```

## Como executar

### Pré-requisitos
- JDK 21
- Node.js 20+
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
A API sobe em `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Front-end
```bash
cd frontend
npm install
ng serve
```
A aplicação sobe em `http://localhost:4200`

## Modelo de dados

O diagrama entidade-relacionamento está em `docs/astileac_der.mermaid`.

## Licença

Este projeto é de caráter acadêmico. Ver arquivo `LICENSE`.
