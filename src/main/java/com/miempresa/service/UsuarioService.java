package com.miempresa.service;

import com.miempresa.model.Usuario;
import com.miempresa.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;

public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;   // ← nueva dependencia

    public UsuarioService(UsuarioRepository usuarioRepository,
                          NotificacionService notificacionService,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.auditoriaService   = auditoriaService;
    }

    public Usuario crearUsuario(Usuario usuario) {
        if (usuario.getEmail() == null || !usuario.getEmail().contains("@")) {
            throw new IllegalArgumentException("Email inválido");
        }
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        notificacionService.enviarNotificacionRegistro(usuario);
        auditoriaService.registrarOperacion(
            "CREAR_USUARIO",
            "Usuario creado: " + usuario.getNombre() + " (" + usuario.getEmail() + ")"
        );
        return usuarioGuardado;
    }

    // … tus otros métodos también deberán aceptar auditoriaService si lo usan
}
