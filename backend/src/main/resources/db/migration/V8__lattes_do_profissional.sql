-- Endereco do curriculo Lattes do associado.
--
-- Pedido da diretoria: quem trabalha na area normalmente tem Lattes, e
-- ele reune formacao e producao num lugar que a propria plataforma
-- mantem. Funciona como complemento ao que o sistema guarda, nao como
-- substituto -- o Lattes e autodeclarado e esta fora do controle da
-- associacao.
--
-- Coluna no PROFISSIONAL, e nao um CONTATO de tipo novo: nao e meio de
-- contato, e referencia a um curriculo publico. Forcar na tabela de
-- contatos faria o tipo EMAIL/TELEFONE perder o sentido.
--
-- Opcional: nem todo associado tem Lattes, e exigir impediria o
-- cadastro de quem nao tem.
ALTER TABLE profissional
    ADD COLUMN lattes_url varchar(255);
