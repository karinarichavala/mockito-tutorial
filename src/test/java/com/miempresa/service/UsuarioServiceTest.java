package com.miempresa.service;

import com.miempresa.model.Usuario;
import com.miempresa.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    
    @Mock
    private NotificacionService notificacionService;
    
    @InjectMocks
    private UsuarioService usuarioService;
    
    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;
    
    @Test
    void deberiaCrearUsuarioConExito() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Juan Pérez", "juan@ejemplo.com");
        when(usuarioRepository.save(any(Usuario.class)))
            .thenReturn(usuario);
        
        // Act
        Usuario resultado = usuarioService.crearUsuario(usuario);
        
        // Assert
        assertNotNull(resultado);
        assertEquals("Juan Pérez", resultado.getNombre());
        
        // Verify y capturar argumento
        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(notificacionService).enviarNotificacionRegistro(usuario);
        
        Usuario usuarioCapturado = usuarioCaptor.getValue();
        assertEquals("juan@ejemplo.com", usuarioCapturado.getEmail());
    }
}
