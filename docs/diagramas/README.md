# Diagramas do TCC

Fontes em Mermaid (`.mmd`) e imagens geradas (`.png`, no triplo da resolução
natural). Todos foram conferidos contra o código e as migrations em
14/10/2026: cada seta, rótulo e cardinalidade corresponde a algo que o
sistema faz, com a classe ou a migration indicada nos comentários do fonte.

O DER completo continua em `docs/ASTILEAC-BD-atual.mmd`, fonte de verdade do
modelo, como antes.

## Quais são e onde entram no texto

A coluna "letra" é o tamanho aproximado do texto da figura **impresso** na
largura sugerida (o corpo do texto do TCC tem 12 pt). No PDF, com zoom, todas
ficam nítidas.

| Figura | Arquivo | Seção do capítulo | Largura sugerida | Letra |
|---|---|---|---|---|
| Casos de uso: visitante e associado | `casos-de-uso-associado` | Implementação dos requisitos | `0.8\textwidth` | 7 pt |
| Casos de uso: diretoria e rotina agendada | `casos-de-uso-diretoria` | Implementação dos requisitos | `0.6\textwidth` | 6 pt |
| Modelo conceitual (notação de Chen) | `modelo-conceitual` | Modelagem do banco | `0.85\textwidth` | 7,5 pt |
| DER, vista do cadastro do profissional | `der-cadastro` | Modelagem do banco | página inteira | 5 pt |
| DER, vista de acesso e proteção de dados | `der-acesso` | Modelagem do banco ou Segurança | `\textwidth` | 6 pt |
| DER completo, 11 tabelas | `../ASTILEAC-BD-atual` | Apêndice, em página deitada | página inteira | 3 pt |
| Arquitetura em camadas | `arquitetura` | Arquitetura da aplicação | `0.65\textwidth` | 6,5 pt |
| Classes do domínio | `classes-dominio` | Arquitetura da aplicação | página inteira | 5,5 pt |
| Sequência: login com CSRF | `sequencia-login` | Segurança, ou Testes (o defeito do 403) | `\textwidth` | 5,5 pt |
| Sequência: cadastro por convite | `sequencia-convite` | Implementação (RF12) | página inteira | 5,5 pt |
| Sequência: consulta por área | `sequencia-consulta-area` | Implementação (RF08) | `\textwidth` | 5,5 pt |
| Estados do cadastro (desativar × eliminar) | `estados-cadastro` | Segurança e proteção de dados (RF10) | `0.5\textwidth` | 6,5 pt |
| Estados do documento do certificado | `estados-certificado` | Implementação (RF13) | `\textwidth` | 6 pt |
| Estados do convite | `estados-convite` | Implementação (RF12) | `0.8\textwidth` | 6,5 pt |
| Mapa de navegação das telas | `navegacao` | Implementação (front-end) | `\textwidth` | 5 pt |

Não é preciso usar todas. As que sustentam argumento do capítulo são o modelo
conceitual (a N:N com atributo), a sequência do convite (o uso único com
`UPDATE` condicional), os estados do cadastro (desativar não é eliminar) e os
casos de uso, que o projeto de pesquisa prometeu no quadro de ferramentas.

### Por que o DER tem vistas parciais

Com as 11 tabelas e todas as colunas, o DER não cabe numa página A4 com letra
legível impressa: em retrato sai com cerca de 3 pt. Por isso ele foi dividido
em duas vistas para o corpo do texto, e o completo vai para um apêndice em
página deitada. As vistas são **geradas** pelo `gerar.mjs`, copiando as
tabelas do DER completo, para nunca divergirem dele.

## Notação e escolhas (para defender na banca)

- **Modelo conceitual em notação de Chen**, com cardinalidade (mínima,
  máxima) junto a cada entidade. O relacionamento "atua em" é N:N e tem
  atributo próprio (data de início): é exatamente por isso que, no modelo
  lógico, ele vira a tabela `PROFISSIONAL_AREA`. O registro de acesso aparece
  sem relacionamento, de propósito.
- **DER (modelo lógico) com os tipos do PostgreSQL.** Linha cheia é
  relacionamento identificador (a chave da filha contém a do pai), o que só
  acontece em `PROFISSIONAL_AREA`; linha tracejada é não identificador.
  `LOG_ACESSO` fica sem ligação porque `registro_id` não é chave estrangeira.
  O modelo físico é o SQL das migrations `V1` a `V8`.
- **Casos de uso.** O Mermaid não tem esse diagrama; ele foi desenhado como
  fluxograma com a notação da UML (boneco = ator, forma arredondada = caso de
  uso, tracejado = `«include»`/`«extend»`). A moldura do sistema, opcional na
  UML, foi omitida porque o Mermaid desorganiza os atores quando ela existe.
  O membro da diretoria também é associado: os casos do associado valem para
  ele sobre o próprio cadastro.
- **Classes.** As associações são unidirecionais (a classe filha conhece
  `Profissional`, e `Profissional` não tem coleções), como no código.
  `Usuario.perfil` é `String` no mapeamento, com os valores do enum `Perfil`;
  daí a dependência tracejada.
- **Sequências** têm os passos numerados, para o texto citar ("passo 18 da
  Figura X"). Na consulta por área, o Spring Security aparece como nota e o
  repositório no rótulo da consulta ao banco, para a figura caber na página.
- **Estados** são derivados de colunas (`ativo`, `data_exclusao_solicitada`,
  `arquivo_path`, `validado_em`, `data_consumo`): o banco não tem campo de
  situação, e os nomes dos estados são os que a tela mostra.

## Como regenerar as imagens

Depois de alterar um `.mmd`, na raiz do repositório:

```bash
node docs/diagramas/gerar.mjs
```

Precisa de Node e de um Chrome instalado. A configuração comum (tema neutro,
Arial, espaçamentos) está em `mermaid-config.json`.

## Como inserir no LaTeX

Do `docs/tcc/`, no mesmo padrão das figuras do capítulo:

```latex
\begin{figure}[!htb]
\centering
\caption{Diagrama de casos de uso do visitante e do associado.}
\label{fig:casos-de-uso-associado}
\includegraphics[width=0.8\textwidth]{../diagramas/casos-de-uso-associado.png}\\
{\footnotesize Fonte: Elaborado pelo autor.}
\end{figure}
```

Para as de página inteira, limite também a altura, senão a legenda não cabe:

```latex
\includegraphics[width=\textwidth,height=0.85\textheight,keepaspectratio]{../diagramas/der-cadastro.png}
```

O DER completo, em página deitada (`\usepackage{pdflscape}` no preâmbulo):

```latex
\begin{landscape}
\begin{figure}[p]
\centering
\caption{Diagrama entidade-relacionamento completo do sistema.}
\label{fig:der-completo}
\includegraphics[width=\linewidth,height=0.85\textheight,keepaspectratio]{../ASTILEAC-BD-atual.png}\\
{\footnotesize Fonte: Elaborado pelo autor.}
\end{figure}
\end{landscape}
```
