/**
 * Prefixo das chamadas a API.
 *
 * Existe por causa de uma colisao descoberta rodando: o proxy do
 * servidor de desenvolvimento repassa para o back-end TUDO que bate no
 * caminho configurado -- inclusive a navegacao do navegador pedindo o
 * HTML. Com "/profissionais" no proxy, abrir
 * http://localhost:4200/profissionais devolvia o JSON cru da API em vez
 * da aplicacao, quebrando recarga de pagina, favoritos e link direto.
 *
 * O prefixo separa os dois espacos de nome: "/api/..." e API, o resto e
 * rota do Angular. O proxy remove o "/api" antes de encaminhar
 * (pathRewrite), entao o back-end continua publicando "/profissionais"
 * sem prefixo -- nada muda do lado dele.
 *
 * Fica numa constante so para o dia em que a aplicacao for servida pelo
 * proprio back-end, quando o valor passa a ser string vazia.
 */
export const API = '/api';
