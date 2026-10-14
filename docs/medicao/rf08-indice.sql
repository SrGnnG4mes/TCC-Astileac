-- Medicao do indice idx_profissional_area_area (migration V3) na consulta do RF08.
--
-- RODA SO NUM BANCO DE MEDICAO, NUNCA NO astileac: o script apaga todos os
-- cadastros antes de gerar os dados ficticios. Ele mesmo confere o nome do
-- banco e para se nao for astileac_medicao.
--
-- Uso (o banco precisa ter passado pelas migrations -- suba o back-end uma
-- vez apontando para ele):
--   psql -d astileac_medicao -v n=300 -f rf08-indice.sql
--
-- :n e o numero de profissionais ficticios.

\set ON_ERROR_STOP on

SELECT current_database() = 'astileac_medicao' AS banco_certo \gset
\if :banco_certo
\else
  \echo 'Este script so roda no banco astileac_medicao.'
  \quit
\endif

-- ---------- carga ficticia ----------

TRUNCATE log_acesso, convite, usuario, certificado, formacao, vinculo_empregaticio,
         profissional_area, contato, endereco, profissional RESTART IDENTITY;
DELETE FROM area_atuacao WHERE nome = 'medicao-area-rara';
INSERT INTO area_atuacao (nome, descricao)
VALUES ('medicao-area-rara', 'Area ficticia, so para a medicao: poucos profissionais');

-- Um em cada 50 se chama Maria, para o filtro por nome ter o que achar.
-- Um em cada 10 esta inativo. CPF ficticio, so para satisfazer o UNIQUE.
INSERT INTO profissional (nome, cpf, data_nascimento, data_associacao, ativo)
SELECT CASE WHEN g % 50 = 0 THEN 'Maria ' ELSE 'Profissional ' END || g,
       lpad(g::text, 11, '0'),
       date '1970-01-01' + (g % 10000),
       date '2012-01-01' + (g % 4000),
       g % 10 <> 0
FROM generate_series(1, :n) AS g;

-- Cada uma das cinco areas da carga inicial com ~30% de chance por pessoa:
-- em media 1,5 area por profissional, e cada area cobre ~30% do quadro.
INSERT INTO profissional_area (profissional_id, area_atuacao_id, data_inicio)
SELECT p.id, a.id, date '2020-01-01'
FROM profissional p
JOIN area_atuacao a
  ON a.nome IN ('educacional', 'juridica', 'saude', 'artistico-cultural', 'outras')
WHERE (hashtext(p.id::text || a.nome) & 1023) < 307;

-- Area rara: 1% do quadro. E o caso de uma area especifica criada pela
-- diretoria na tela de manutencao.
INSERT INTO profissional_area (profissional_id, area_atuacao_id, data_inicio)
SELECT p.id, a.id, date '2020-01-01'
FROM profissional p
JOIN area_atuacao a ON a.nome = 'medicao-area-rara'
WHERE p.id % 100 = 0;

ANALYZE;

SELECT count(*) AS profissionais FROM profissional;
SELECT a.nome AS area, count(*) AS profissionais
FROM profissional_area pa JOIN area_atuacao a ON a.id = pa.area_atuacao_id
GROUP BY a.nome ORDER BY 2 DESC;

-- ---------- consultas ----------
-- O SQL abaixo e o que o Hibernate gera para a consulta do RF08
-- (ProfissionalRepository.consultar), capturado no log com
-- logging.level.org.hibernate.SQL=DEBUG, com os parametros ja
-- substituidos. "fetch first 20" e a pagina; o count e a consulta que o
-- Spring Data faz para a paginacao quando o resultado passa de uma pagina.
-- Cada uma roda tres vezes; vale o menor tempo.

\echo '#### COM INDICE'

\echo '## area comum (juridica), pagina'
EXPLAIN (ANALYZE, BUFFERS)
select p1_0.id,p1_0.ativo,p1_0.cpf,p1_0.data_associacao,p1_0.data_exclusao_solicitada,p1_0.data_nascimento,p1_0.lattes_url,p1_0.nome from profissional p1_0 where p1_0.data_exclusao_solicitada is null and (cast(null as varchar) is null or lower(p1_0.nome) like lower(('%'||cast(null as varchar)||'%')) escape '') and (null::boolean is null or p1_0.ativo=null::boolean) and (cast('juridica' as varchar) is null or exists(select 1 from profissional_area pa1_0 join area_atuacao aa1_0 on aa1_0.id=pa1_0.area_atuacao_id where pa1_0.profissional_id=p1_0.id and aa1_0.nome=cast('juridica' as varchar))) order by p1_0.nome fetch first 20 rows only;
\g
\g

\echo '## area comum (juridica), contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(p1_0.id) from profissional p1_0 where p1_0.data_exclusao_solicitada is null and (cast(null as varchar) is null or lower(p1_0.nome) like lower(('%'||cast(null as varchar)||'%')) escape '') and (null::boolean is null or p1_0.ativo=null::boolean) and (cast('juridica' as varchar) is null or exists(select 1 from profissional_area pa1_0 join area_atuacao aa1_0 on aa1_0.id=pa1_0.area_atuacao_id where pa1_0.profissional_id=p1_0.id and aa1_0.nome=cast('juridica' as varchar)));
\g
\g

\echo '## area rara, pagina'
EXPLAIN (ANALYZE, BUFFERS)
select p1_0.id,p1_0.ativo,p1_0.cpf,p1_0.data_associacao,p1_0.data_exclusao_solicitada,p1_0.data_nascimento,p1_0.lattes_url,p1_0.nome from profissional p1_0 where p1_0.data_exclusao_solicitada is null and (cast(null as varchar) is null or lower(p1_0.nome) like lower(('%'||cast(null as varchar)||'%')) escape '') and (null::boolean is null or p1_0.ativo=null::boolean) and (cast('medicao-area-rara' as varchar) is null or exists(select 1 from profissional_area pa1_0 join area_atuacao aa1_0 on aa1_0.id=pa1_0.area_atuacao_id where pa1_0.profissional_id=p1_0.id and aa1_0.nome=cast('medicao-area-rara' as varchar))) order by p1_0.nome fetch first 20 rows only;
\g
\g

\echo '## area comum + nome + ativo, pagina'
EXPLAIN (ANALYZE, BUFFERS)
select p1_0.id,p1_0.ativo,p1_0.cpf,p1_0.data_associacao,p1_0.data_exclusao_solicitada,p1_0.data_nascimento,p1_0.lattes_url,p1_0.nome from profissional p1_0 where p1_0.data_exclusao_solicitada is null and (cast('maria' as varchar) is null or lower(p1_0.nome) like lower(('%'||cast('maria' as varchar)||'%')) escape '') and (true is null or p1_0.ativo=true) and (cast('juridica' as varchar) is null or exists(select 1 from profissional_area pa1_0 join area_atuacao aa1_0 on aa1_0.id=pa1_0.area_atuacao_id where pa1_0.profissional_id=p1_0.id and aa1_0.nome=cast('juridica' as varchar))) order by p1_0.nome fetch first 20 rows only;
\g
\g

-- DROP INDEX dentro de transacao desfeita no fim: no PostgreSQL o DDL e
-- transacional, entao o indice volta sozinho.
BEGIN;
DROP INDEX idx_profissional_area_area;

\echo '#### SEM INDICE'

\echo '## area comum (juridica), pagina'
EXPLAIN (ANALYZE, BUFFERS)
select p1_0.id,p1_0.ativo,p1_0.cpf,p1_0.data_associacao,p1_0.data_exclusao_solicitada,p1_0.data_nascimento,p1_0.lattes_url,p1_0.nome from profissional p1_0 where p1_0.data_exclusao_solicitada is null and (cast(null as varchar) is null or lower(p1_0.nome) like lower(('%'||cast(null as varchar)||'%')) escape '') and (null::boolean is null or p1_0.ativo=null::boolean) and (cast('juridica' as varchar) is null or exists(select 1 from profissional_area pa1_0 join area_atuacao aa1_0 on aa1_0.id=pa1_0.area_atuacao_id where pa1_0.profissional_id=p1_0.id and aa1_0.nome=cast('juridica' as varchar))) order by p1_0.nome fetch first 20 rows only;
\g
\g

\echo '## area comum (juridica), contagem'
EXPLAIN (ANALYZE, BUFFERS)
select count(p1_0.id) from profissional p1_0 where p1_0.data_exclusao_solicitada is null and (cast(null as varchar) is null or lower(p1_0.nome) like lower(('%'||cast(null as varchar)||'%')) escape '') and (null::boolean is null or p1_0.ativo=null::boolean) and (cast('juridica' as varchar) is null or exists(select 1 from profissional_area pa1_0 join area_atuacao aa1_0 on aa1_0.id=pa1_0.area_atuacao_id where pa1_0.profissional_id=p1_0.id and aa1_0.nome=cast('juridica' as varchar)));
\g
\g

\echo '## area rara, pagina'
EXPLAIN (ANALYZE, BUFFERS)
select p1_0.id,p1_0.ativo,p1_0.cpf,p1_0.data_associacao,p1_0.data_exclusao_solicitada,p1_0.data_nascimento,p1_0.lattes_url,p1_0.nome from profissional p1_0 where p1_0.data_exclusao_solicitada is null and (cast(null as varchar) is null or lower(p1_0.nome) like lower(('%'||cast(null as varchar)||'%')) escape '') and (null::boolean is null or p1_0.ativo=null::boolean) and (cast('medicao-area-rara' as varchar) is null or exists(select 1 from profissional_area pa1_0 join area_atuacao aa1_0 on aa1_0.id=pa1_0.area_atuacao_id where pa1_0.profissional_id=p1_0.id and aa1_0.nome=cast('medicao-area-rara' as varchar))) order by p1_0.nome fetch first 20 rows only;
\g
\g

\echo '## area comum + nome + ativo, pagina'
EXPLAIN (ANALYZE, BUFFERS)
select p1_0.id,p1_0.ativo,p1_0.cpf,p1_0.data_associacao,p1_0.data_exclusao_solicitada,p1_0.data_nascimento,p1_0.lattes_url,p1_0.nome from profissional p1_0 where p1_0.data_exclusao_solicitada is null and (cast('maria' as varchar) is null or lower(p1_0.nome) like lower(('%'||cast('maria' as varchar)||'%')) escape '') and (true is null or p1_0.ativo=true) and (cast('juridica' as varchar) is null or exists(select 1 from profissional_area pa1_0 join area_atuacao aa1_0 on aa1_0.id=pa1_0.area_atuacao_id where pa1_0.profissional_id=p1_0.id and aa1_0.nome=cast('juridica' as varchar))) order by p1_0.nome fetch first 20 rows only;
\g
\g

ROLLBACK;
