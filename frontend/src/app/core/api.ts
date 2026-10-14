/**
 * Prefixo das chamadas a API.
 *
 * Existe por causa de uma colisao descoberta rodando: as rotas do Angular
 * tem os mesmos nomes dos recursos da API (/profissionais, /convites,
 * /areas-atuacao). Sem prefixo, abrir http://localhost:4200/profissionais
 * fazia o proxy repassar a navegacao ao back-end, que devolvia o JSON cru
 * da API em vez da aplicacao -- quebrando recarga de pagina, favoritos e
 * link direto.
 *
 * O prefixo separa os dois espacos de nome: "/api/..." e API, o resto e
 * rota do Angular. O back-end publica a API sob o mesmo "/api"
 * (WebConfig.PREFIXO_API), entao o valor e o mesmo no desenvolvimento,
 * com o proxy do ng serve, e na implantacao, com o back-end servindo o
 * Angular compilado.
 *
 * A primeira versao removia o prefixo no proxy e deixava o back-end sem
 * ele, com a ideia de trocar este valor por string vazia quando o
 * back-end servisse a aplicacao. Teria trazido a colisao de volta na
 * implantacao: o problema era o mesmo, so mudaria de servidor.
 */
export const API = '/api';
