package com.example.todosimple.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todosimple.dtos.FuncionariosAtivosDto;
import com.example.todosimple.models.Funcionario;
import com.example.todosimple.models.Servico;
import com.example.todosimple.repositories.FuncionarioRepository;
import com.example.todosimple.repositories.ServicoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class FuncionarioService {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    public Funcionario findById(Long id) {
        Optional<Funcionario> funcionario = this.funcionarioRepository.findById(id);
        return funcionario.orElseThrow(() -> new RuntimeException(
                "Funcionario não encontrado! Id: " + id + ", Type: " + Funcionario.class.getName()));
        // MELHORIA: Criar uma exceção customizada, como `FuncionarioNotFoundException`, 
        // para melhor semântica e controle de erros na camada de controller.
    }

    public List<Funcionario> findAll() {
        return funcionarioRepository.findAll();
        // BOA PRÁTICA: Se a lista for grande, considerar paginação com Pageable.
    }

    @Transactional
    public Funcionario create(Funcionario obj) {
        obj.setId(null); // BOA PRÁTICA: Garante criação de novo registro e evita atualização acidental.
        obj = this.funcionarioRepository.save(obj);
        return obj;
    }

    @Transactional
    public Funcionario update(Funcionario obj) {
        Funcionario newObj = findById(obj.getId());
        newObj.setCpf(obj.getCpf());
        newObj.setDataAdmissao(obj.getDataAdmissao());
        newObj.setTipoCNH(obj.getTipoCNH());
        newObj.setCargo(obj.getCargo());
        newObj.setNome(obj.getNome());
        newObj.setStatus(obj.getStatus());
        newObj.setObservacoes(obj.getObservacoes());
        newObj.setServicos(obj.getServicos());
        // MELHORIA: Para maior legibilidade, pode-se extrair esse bloco de cópia para um método privado auxiliar.
        // MELHORIA: Considere validar se os serviços são válidos antes de associar.
        return this.funcionarioRepository.save(newObj);
    }

    @Transactional
    public void delete(Long id) {
        Funcionario funcionario = findById(id);

        List<Servico> servicos = servicoRepository.findAll();
        for (Servico servico : servicos) {
            if (servico.getFuncionarios().contains(funcionario)) {
                servico.getFuncionarios().remove(funcionario);
                servicoRepository.save(servico);
                // MELHORIA: Pode ser custoso iterar por todos os serviços.
                // Uma query que busca apenas os serviços com esse funcionário seria mais performática.
            }
        }

        try {
            this.funcionarioRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Funcionario não pode ser deletado, pois tem entidades dependentes relacionadas!");
            // MELHORIA: Evite capturar Exception genérica. Use algo como DataIntegrityViolationException
            // para capturar violações de integridade específicas.
        }
    }

    public FuncionariosAtivosDto buscarFuncionariosAtivos(){
        
        FuncionariosAtivosDto dto = new FuncionariosAtivosDto();

        int ativos = funcionarioRepository.countByStatus("Ativo");
        int inativos = funcionarioRepository.countByStatus("Inativo");
        // BOA PRÁTICA: Se esses valores forem usados com frequência, uma query agregada única 
        // (com GROUP BY status) seria mais eficiente que duas separadas.

        dto.setAtivos(ativos);
        dto.setInativos(inativos);

        return dto;
    }
}
