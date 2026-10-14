# Medição do índice da consulta por área (RF08)

Medido em 13/10/2026. O registro do projeto dizia "pendente de medição": o
índice `idx_profissional_area_area`, criado na migration V3, só ajuda se o
PostgreSQL decidir usá-lo, e não havia como afirmar ganho sem olhar o plano
de execução.

## Pergunta

O índice sobre `profissional_area.area_atuacao_id` é usado pela consulta do
RF08? E, se for, quanto ele muda o tempo de resposta?

## Como foi medido

- **Consulta medida:** o SQL que o Hibernate de fato gera para
  `ProfissionalRepository.consultar`, capturado no log da aplicação com
  `logging.level.org.hibernate.SQL=DEBUG`. Foram medidas tanto a página
  (`fetch first 20`) quanto a contagem que o Spring Data faz para a
  paginação.
- **Dados:** fictícios, gerados num banco separado (`astileac_medicao`),
  nunca no banco do sistema. Cada uma das cinco áreas da carga inicial cobre
  cerca de 30% do quadro, e há uma área **rara** com 1% dos profissionais,
  que representa uma área específica criada pela diretoria na tela de
  manutenção. Um em cada dez está inativo, e um em cada cinquenta se chama
  Maria, para o filtro por nome ter o que achar.
- **Volumes:** 300 profissionais, estimativa folgada para uma associação
  estadual (o número real do quadro ainda precisa ser confirmado com a
  diretoria), e depois 10 mil e 100 mil, para ver a partir de quando o
  índice importa.
- **Com e sem índice:** o mesmo script remove o índice dentro de uma
  transação e a desfaz no fim. No PostgreSQL o DDL é transacional, então o
  índice volta sozinho.
- **Tempo:** `EXPLAIN (ANALYZE, BUFFERS)`. Cada consulta rodou três vezes e
  vale o menor tempo, para descontar a primeira leitura do disco.
- **Ambiente:** PostgreSQL 18.6, Windows 11, máquina do autor. Os tempos
  absolutos valem para esta máquina; o que se compara é a proporção entre
  as linhas da tabela.

O script está em [`rf08-indice.sql`](rf08-indice.sql), e a saída completa
de cada volume, com todos os planos, em [`resultados/`](resultados/).

## Resultados

Tempo de execução em milissegundos. "Usa o índice" indica se
`idx_profissional_area_area` aparece no plano.

| Consulta | Volume | Com índice | Sem índice | Usa o índice? |
|---|---:|---:|---:|---|
| área comum, página | 300 | 0,36 | 0,35 | não |
| área comum, contagem | 300 | 0,23 | 0,25 | não |
| área rara, página | 300 | 0,17 | 0,19 | não |
| área + nome + ativo | 300 | 0,16 | 0,17 | não |
| área comum, página | 10 mil | 4,0 | 6,0 | sim |
| área comum, contagem | 10 mil | 4,0 | 3,8 | sim |
| área rara, página | 10 mil | 1,3 | 3,7 | sim |
| área + nome + ativo | 10 mil | 2,7 | 4,2 | não |
| área comum, página | 100 mil | 103 | 130 | sim |
| área comum, contagem | 100 mil | 62 | 92 | sim |
| área rara, página | 100 mil | 29 | 66 | sim |
| área + nome + ativo | 100 mil | 70 | 73 | não |

## O que os números dizem

**1. No volume da ASTILEAC, o índice não é usado, e não faz falta.** Com
300 profissionais, o PostgreSQL prefere ler a tabela inteira, que cabe em
poucas páginas de memória: é mais barato que consultar um índice. A
consulta leva menos de meio milissegundo com ou sem ele. **Não se pode
afirmar ganho de desempenho do índice no uso real da associação.**

**2. A partir de cerca de 10 mil cadastros, o planejador passa a usá-lo**, e
o ganho é maior quanto mais rara a área: 2,9 vezes mais rápido na área rara
com 10 mil, 2,3 vezes com 100 mil. Na área comum, que cobre 30% do quadro, o
ganho é pequeno (1,3 vez com 100 mil), porque ainda é preciso ler 30% das
linhas de qualquer jeito.

**3. A dúvida anotada no projeto não se confirmou.** O receio era que, num
`EXISTS` correlacionado, o PostgreSQL preferisse a chave primária composta
`(profissional_id, area_atuacao_id)` e ignorasse o índice da área. O que ele
faz é outra coisa: transforma o `EXISTS` num *hashed SubPlan*. Busca uma vez
só, pelo índice da área, todos os profissionais daquela área, monta uma
tabela de hash e confere cada profissional contra ela. O índice usado é o da
V3, e não a chave composta.

**4. O custo que sobra não é da área.** Com 100 mil, a maior parte do tempo
vai em percorrer a tabela `profissional` e ordenar por nome para montar a
página. O filtro por nome usa `LIKE '%maria%'`, com curinga no início, que
índice B-tree comum não atende. Se um dia o volume justificar, o caminho
seria outro índice (de trigramas, por exemplo), não este.

## Conclusão

O índice da V3 é **correto e barato**, mas não é ele que torna o RF08
rápido no volume real: com algumas centenas de associados, a consulta já é
instantânea sem ele. Ele passa a valer a partir de dezenas de milhares de
cadastros, sobretudo para áreas específicas com poucos profissionais.
Manter o índice não custa quase nada (uma tabela pequena, com uma escrita a
mais por associação de área), e ele protege o sistema caso o volume cresça.
Mas a justificativa honesta para ele é **preparo para crescimento**, não
ganho de desempenho medido no uso atual.
