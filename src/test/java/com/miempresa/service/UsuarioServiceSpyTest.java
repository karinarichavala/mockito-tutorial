package com.miempresa.service;
import com.miempresa.model.Usuario;
import com.miempresa.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceSpyTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    // Un spy para el servicio de notificaciones  
    @Spy
    private NotificacionServiceImpl notificacionService = new NotificacionServiceImpl();

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void testConSpyComoInyeccion() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Cristina Lago", "cristina@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);

        // Modificamos comportamiento del spy para un método específico
        doThrow(new RuntimeException("Error simulado"))
            .when(notificacionService).enviarNotificacionDesactivacion(any());

        // Act: crearUsuario debería funcionar normalmente
        Usuario resultado = usuarioService.crearUsuario(usuario);
        assertNotNull(resultado);

        // Pero desactivarUsuario debería fallar
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        assertThrows(RuntimeException.class, () -> {
            usuarioService.desactivarUsuario(1L);
        });

        // Verify: el spy permite verificaciones como un mock normal
        verify(notificacionService).enviarNotificacionRegistro(usuario);
    }
}
