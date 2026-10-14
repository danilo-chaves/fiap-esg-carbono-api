package br.com.fiap.carbonoapi.service;

import br.com.fiap.carbonoapi.dto.request.EmissaoCarbonoRequest;
import br.com.fiap.carbonoapi.dto.response.EmissaoCarbonoResponse;
import br.com.fiap.carbonoapi.entity.EmissaoCarbono;
import br.com.fiap.carbonoapi.entity.Empresa;
import br.com.fiap.carbonoapi.entity.enums.EscopoEmissao;
import br.com.fiap.carbonoapi.exception.ResourceNotFoundException;
import br.com.fiap.carbonoapi.mapper.EmissaoCarbonoMapper;
import br.com.fiap.carbonoapi.repository.EmissaoCarbonoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmissaoCarbonoServiceTest {

    @Mock
    private EmissaoCarbonoRepository emissaoRepository;

    @Mock
    private EmpresaService empresaService;

    @Mock
    private EmissaoCarbonoMapper emissaoMapper;

    @InjectMocks
    private EmissaoCarbonoService emissaoCarbonoService;

    @Test
    @DisplayName("Deve registrar emissão de carbono com sucesso")
    void deveRegistrarEmissaoComSucesso() {
        EmissaoCarbonoRequest request = new EmissaoCarbonoRequest();
        request.setEmpresaId(1L);
        request.setQuantidadeTco2e(new BigDecimal("150.50"));
        request.setEscopo(EscopoEmissao.ESCOPO_1);
        request.setDataEmissao(LocalDate.now());

        Empresa empresa = new Empresa();
        empresa.setId(1L);

        EmissaoCarbono emissao = new EmissaoCarbono();
        emissao.setId(10L);
        emissao.setQuantidadeTco2e(new BigDecimal("150.50"));

        EmissaoCarbonoResponse response = new EmissaoCarbonoResponse();
        response.setId(10L);
        response.setQuantidadeTco2e(new BigDecimal("150.50"));

        when(empresaService.buscarEmpresaOuLancarExcecao(1L)).thenReturn(empresa);
        when(emissaoMapper.toEntity(request, empresa)).thenReturn(emissao);
        when(emissaoRepository.save(emissao)).thenReturn(emissao);
        when(emissaoMapper.toResponse(emissao)).thenReturn(response);

        EmissaoCarbonoResponse resultado = emissaoCarbonoService.registrar(request);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals(new BigDecimal("150.50"), resultado.getQuantidadeTco2e());
        verify(emissaoRepository).save(any(EmissaoCarbono.class));
    }

    @Test
    @DisplayName("Deve buscar emissão por id existente")
    void deveBuscarEmissaoPorIdExistente() {
        Long id = 10L;
        EmissaoCarbono emissao = new EmissaoCarbono();
        emissao.setId(id);

        EmissaoCarbonoResponse response = new EmissaoCarbonoResponse();
        response.setId(id);

        when(emissaoRepository.findById(id)).thenReturn(Optional.of(emissao));
        when(emissaoMapper.toResponse(emissao)).thenReturn(response);

        EmissaoCarbonoResponse resultado = emissaoCarbonoService.buscarPorId(id);

        assertNotNull(resultado);
        assertEquals(id, resultado.getId());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar emissão inexistente")
    void deveLancarExcecaoQuandoEmissaoNaoExiste() {
        Long id = 999L;
        when(emissaoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> emissaoCarbonoService.buscarPorId(id));
    }
}
