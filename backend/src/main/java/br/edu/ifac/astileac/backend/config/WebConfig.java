package br.edu.ifac.astileac.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Fixa o formato JSON das respostas paginadas.
 *
 * Por padrao o Spring serializa o objeto Page interno (PageImpl) do
 * jeito que ele e, e a propria biblioteca avisa no log que a estrutura
 * resultante nao tem estabilidade garantida entre versoes. Como o
 * front-end Angular (Passo 7) vai depender desse formato, VIA_DTO
 * troca a serializacao por um contrato estavel e documentado.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig {

}
