package br.com.fiap.carbonoapi.service;

import br.com.fiap.carbonoapi.dto.request.EmpresaRequest;
import br.com.fiap.carbonoapi.dto.response.EmpresaResponse;
import br.com.fiap.carbonoapi.entity.Empresa;
import br.com.fiap.carbonoapi.exception.ConflictException;
import br.com.fiap.carbonoapi.exception.ResourceNotFoundException;
import br.com.fiap.carbonoapi.mapper.EmpresaMapper;
import br.com.fiap.carbonoapi.repository.EmpresaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private EmpresaMapper empresaMapper;

    @InjectMocks
    private EmpresaService empresaService;

    @Test
    @DisplayName("Deve cadastrar empresa com sucesso quando CNPJ não existir")
    void deveCadastrarEmpresaComSucesso() {
        EmpresaRequest request = new EmpresaRequest();
        request.setNome("Eco Tech Sustentável");
        request.setCnpj("12.345.678/0001-99");
        request.setSetor("Tecnologia");
        request.setPais("Brasil");

        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNome("Eco Tech Sustentável");
        empresa.setCnpj("12.345.678/0001-99");

        EmpresaResponse responseEsperada = new EmpresaResponse();
        responseEsperada.setId(1L);
        responseEsperada.setNome("Eco Tech Sustentável");
        responseEsperada.setCnpj("12.345.678/0001-99");

        when(empresaRepository.existsByCnpj(request.getCnpj())).thenReturn(false);
        when(empresaMapper.toEntity(request)).thenReturn(empresa);
        when(empresaRepository.save(empresa)).thenReturn(empresa);
        when(empresaMapper.toResponse(empresa)).thenReturn(responseEsperada);

        EmpresaResponse resultado = empresaService.cadastrar(request);

        assertNotNull(resultado);
        assertEquals("12.345.678/0001-99", resultado.getCnpj());
        assertEquals("Eco Tech Sustentável", resultado.getNome());
        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    @DisplayName("Deve lançar ConflictException ao tentar cadastrar empresa com CNPJ duplicado")
    void deveLancarConflictExceptionQuandoCnpjJaExiste() {
        EmpresaRequest request = new EmpresaRequest();
        request.setCnpj("12.345.678/0001-99");

        when(empresaRepository.existsByCnpj(request.getCnpj())).thenReturn(true);

        assertThrows(ConflictException.class, () -> empresaService.cadastrar(request));
        verify(empresaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar empresa por id existente")
    void deveBuscarEmpresaPorIdExistente() {
        Long id = 1L;
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setNome("Empresa Verde");

        EmpresaResponse responseEsperada = new EmpresaResponse();
        responseEsperada.setId(id);
        responseEsperada.setNome("Empresa Verde");

        when(empresaRepository.findById(id)).thenReturn(Optional.of(empresa));
        when(empresaMapper.toResponse(empresa)).thenReturn(responseEsperada);

        EmpresaResponse resultado = empresaService.buscarPorId(id);

        assertNotNull(resultado);
        assertEquals(id, resultado.getId());
        assertEquals("Empresa Verde", resultado.getNome());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar empresa com id inexistente")
    void deveLancarResourceNotFoundExceptionQuandoIdNaoExiste() {
        Long id = 99L;
        when(empresaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> empresaService.buscarPorId(id));
    }

    @Test
    @DisplayName("Deve listar empresas paginadas")
    void deveListarEmpresas() {
        PageRequest pageable = PageRequest.of(0, 10);
        Empresa empresa = new Empresa();
        empresa.setId(1L);

        EmpresaResponse response = new EmpresaResponse();
        response.setId(1L);

        when(empresaRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(empresa)));
        when(empresaMapper.toResponse(empresa)).thenReturn(response);

        Page<EmpresaResponse> resultado = empresaService.listarEmpresas(pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
    }
}
