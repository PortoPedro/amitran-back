package com.example.todosimple.services;

import java.util.Optional;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.todosimple.models.Servico;
import com.example.todosimple.models.Veiculo;
import com.example.todosimple.repositories.ServicoRepository;
import com.example.todosimple.repositories.VeiculoRepository;

import jakarta.transaction.Transactional;

@Service
public class VeiculoService {

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    public Veiculo findByPlaca(String placa) {
        Optional<Veiculo> veiculo = this.veiculoRepository.findById(placa);
        return veiculo.orElseThrow(() -> new RuntimeException(
                "Veiculo não foi encontrado! Placa: " + placa + ", tipo: " + Veiculo.class.getName()));
        // MELHORIA: Criar exceção customizada como `VeiculoNotFoundException` para controle mais claro e preciso de erros.
    }

    public List<Veiculo> findAllV() {
        return veiculoRepository.findAll();
        // MELHORIA: O nome do método poderia ser `findAll` para seguir convenções e evitar confusão.
        // Se o motivo for diferenciar de outro findAll, considere usar @Qualifier ou nome mais descritivo como `findAllVeiculos`.
        // BOA PRÁTICA: Implementar paginação com Pageable se a base crescer muito.
    }

    @Transactional
    public Veiculo create(Veiculo obj) {
        obj = this.veiculoRepository.save(obj);
        return obj;
        // OBSERVAÇÃO: Seria interessante verificar se já existe veículo com a mesma placa para evitar conflitos.
    }

    @Transactional
    public Veiculo update(Veiculo obj) {
        Veiculo novoObj = findByPlaca(obj.getPlaca());
        novoObj.setPlaca(obj.getPlaca());
        novoObj.setTipoVeiculo(obj.getTipoVeiculo());
        novoObj.setStatus(obj.getStatus());
        novoObj.setModelo(obj.getModelo());
        novoObj.setServicos(obj.getServicos());
        novoObj.setObservacoes(obj.getObservacoes());
        return this.veiculoRepository.save(novoObj);
        // MELHORIA: Extrair a lógica de atualização para um método privado para reduzir duplicação e aumentar clareza.
        // ATENÇÃO: Alterar a placa pode causar problema se for a chave primária. Normalmente placas não devem ser alteradas.
    }

    @Transactional
    public void delete(String placa) {
        Veiculo veiculo = findByPlaca(placa);

        List<Servico> servicos = servicoRepository.findAll();
        for (Servico servico : servicos) {
            if (servico.getVeiculos().contains(veiculo)) {
                servico.getVeiculos().remove(veiculo);
                servicoRepository.save(servico);
            }
        }
        // MELHORIA: Em vez de buscar todos os serviços, fazer uma query específica para buscar apenas serviços vinculados a esse veículo.
        // Isso melhora a performance em grandes volumes de dados.

        try {
            this.veiculoRepository.deleteById(placa);
        } catch (Exception e) {
            throw new RuntimeException("Houve um erro ao deletar o Veículo");
            // MELHORIA: Capturar exceções específicas como DataIntegrityViolationException para tratamento mais adequado.
        }
    }
}
