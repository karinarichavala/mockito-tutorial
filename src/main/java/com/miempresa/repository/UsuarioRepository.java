package com.miempresa.repository;

import com.miempresa.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
	
	 Optional<Usuario> findById(Long id);
	 List<Usuario> findAll();
	 Usuario save(Usuario usuario);
	 void delete(Long id);
	 boolean existsById(Long id);

}
