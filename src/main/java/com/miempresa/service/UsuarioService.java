package com.miempresa.service;

import com.miempresa.model.Usuario;
import com.miempresa.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;

public class UsuarioService {
	
	private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    public UsuarioService(UsuarioRepository usuarioRepository, 
                          NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }
    public Usuario crearUsuario(Usuario usuario) {
        if (usuario.getEmail() == null || !usuario.getEmail().contains("@")) {
            throw new IllegalArgumentException("Email inválido");
        }
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        notificacionService.enviarNotificacionRegistro(usuario);
        return usuarioGuardado;
    }
    public Optional<Usuario> obtenerUsuario(Long id) {
        return usuarioRepository.findById(id);
    }
    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }
    public void desactivarUsuario(Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
            notificacionService.enviarNotificacionDesactivacion(usuario);
        }
    }

}
