package com.example.todosimple.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.todosimple.models.Funcionario;


@Repository
public interface FuncionarioRepository  extends JpaRepository<Funcionario, Long>{
    
    int countByStatus(String status);

}
