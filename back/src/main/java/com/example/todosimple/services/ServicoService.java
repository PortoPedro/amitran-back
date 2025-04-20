package com.example.todosimple.services;

import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.todosimple.dtos.ServicosPorMesDto;
import com.example.todosimple.models.Funcionario;
import com.example.todosimple.models.Servico;
import com.example.todosimple.models.Veiculo;
import com.example.todosimple.repositories.FuncionarioRepository;
import com.example.todosimple.repositories.ServicoRepository;
import com.example.todosimple.repositories.VeiculoRepository;

import jakarta.transaction.Transactional;

@Service
public class ServicoService {
    
    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    public Servico findById(Long id) {
        Optional<Servico> servico = this.servicoRepository.findById(id);
        return servico.orElseThrow(() -> new RuntimeException(
            "Servico não encontrado para o ID: " + id
        ));
        // BOA PRÁTICA: Exceção customizada como `ServicoNotFoundException` deixaria mais claro o problema.
    }

    public List<Servico> findAll() {
        return servicoRepository.findAll();
    }

    public List<Servico> getServicoPorData(LocalDateTime data) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        String dataInicio = data.minus(1, ChronoUnit.MONTHS).format(formatter);
        String dataTermino = data.plus(5, ChronoUnit.MONTHS).format(formatter);
        // MELHORIA: Ideal seria o repositório aceitar `LocalDateTime` diretamente para evitar conversões desnecessárias.
        return servicoRepository.findByDataInicioBetween(dataInicio, dataTermino);
    }

    @Transactional
    public Servico create(Servico obj) {
        verificarConflitos(obj);
        obj.setId(null); // BOA PRÁTICA: Garante persistência como novo objeto
        obj = this.servicoRepository.save(obj);
        return obj;
    }

    @Transactional
    public Servico update(Servico obj) {
        verificarConflitos(obj);
        Servico newObj = findById(obj.getId());
        newObj.setNomeCliente(obj.getNomeCliente());
        newObj.setEnderecoOrigem(obj.getEnderecoOrigem());
        newObj.setEnderecoEntrega(obj.getEnderecoEntrega());
        newObj.setDataInicio(obj.getDataInicio());
        newObj.setDataTermino(obj.getDataTermino());
        newObj.setValor(obj.getValor());
        newObj.setRegiao(obj.getRegiao());
        newObj.setDescricao(obj.getDescricao());
        newObj.setFuncionarios(obj.getFuncionarios());
        newObj.setVeiculos(obj.getVeiculos());
        // MELHORIA: Separar update em método auxiliar para clareza ou usar mapeamento por DTO.
        return this.servicoRepository.save(newObj);
    }

    private void verificarConflitos(Servico obj) {
        List<Servico> servicosExistentes = servicoRepository.findByDataInicioBetween(
            obj.getDataInicio().toString(),
            obj.getDataTermino().toString()
        );
        // MELHORIA: Assim como mencionado antes, usar LocalDateTime diretamente aqui também.

        for (Veiculo veiculo : obj.getVeiculos()) {
            for (Servico servico : servicosExistentes) {
                if (!servico.getId().equals(obj.getId()) && 
                    servico.getVeiculos().contains(veiculo)) {
                    throw new RuntimeException("Conflito: Veículo " + veiculo.getPlaca() + 
                        " já está alocado para o serviço " + servico.getNomeCliente() + 
                        " no período selecionado.");
                }
                // MELHORIA: Pode haver problema de performance aqui se a lista for grande. 
                // Usar Set ao invés de List em `getVeiculos()` melhoraria performance de `contains()`.
            }
        }

        for (Funcionario funcionario : obj.getFuncionarios()) {
            for (Servico servico : servicosExistentes) {
                if (!servico.getId().equals(obj.getId()) && 
                    servico.getFuncionarios().contains(funcionario)) {
                    throw new RuntimeException("Conflito: Funcionário " + funcionario.getNome() + 
                        " já está alocado para o serviço " + servico.getNomeCliente() + 
                        " no período selecionado.");
                }
                // MELHORIA: Mesmo caso acima. Substituir List por Set nas entidades ajudaria.
            }
        }
        // MELHORIA: Esses dois blocos são muito semelhantes. Poderiam ser extraídos para métodos auxiliares privados
        // para reutilização e clareza.
    }

    public void delete(Long id) {
        findById(id); // BOA PRÁTICA: Garante que o serviço existe antes de tentar deletar
        try {
            this.servicoRepository.deleteById(id);
        } catch(Exception e) {
            throw new RuntimeException("Não é possível excluir o serviço pois há entidades relacionadas!");
            // MELHORIA: Evite capturar Exception genérico. Use algo mais específico como DataIntegrityViolationException.
        }
    }

    public Servico addFuncionarioToServico(Long servicoId, Long funcionarioId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
            .orElseThrow(() -> new RuntimeException("Funcionario não encontrado com ID: " + funcionarioId));
        
        Servico servico = findById(servicoId);
        servico.getFuncionarios().add(funcionario);
        // MELHORIA: Verificar se o funcionário já está adicionado antes de adicionar.
        return servicoRepository.save(servico);
    }

    public Servico addVeiculoToServico(Long servicoId, String veiculoId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
            .orElseThrow(() -> new RuntimeException("Veiculo não encontrado com Placa: " + veiculoId));
        
        Servico servico = findById(servicoId);
        servico.getVeiculos().add(veiculo);
        // MELHORIA: Idem acima — verificar se o veículo já existe antes de adicionar para evitar duplicação.
        return servicoRepository.save(servico);
    }

    public List<ServicosPorMesDto> buscarServicosporMes() {
        List<ServicosPorMesDto> servicosPorMes = new ArrayList<ServicosPorMesDto>();

        List<Object[]> resultados = servicoRepository.countServicosPorMes();
        // BOA PRÁTICA: A consulta já retorna dados agregados e está bem separada aqui.

        for (Object[] resultado : resultados) {
            String mes = (String) resultado[0];  
            Long quantidade = (Long) resultado[1];  
            servicosPorMes.add(new ServicosPorMesDto(mes, quantidade.intValue()));
        }

        return servicosPorMes;
        // MELHORIA: Se resultados estiver vazio, poderia retornar Collections.emptyList() para evitar alocação desnecessária.
    }
}
