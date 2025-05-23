package com.miempresa.service;

import com.miempresa.model.Usuario;
import com.miempresa.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private AuditoriaService auditoriaService;  // ← tercer mock

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deberiaInteractuarConTodosLosServicios() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Elena Martínez", "elena@ejemplo.com");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        // Act
        Usuario resultado = usuarioService.crearUsuario(usuario);

        // Assert
        assertNotNull(resultado);

        // Verify: comprobamos la interacción con los tres mocks
        verify(usuarioRepository).save(usuario);
        verify(notificacionService).enviarNotificacionRegistro(usuario);
        verify(auditoriaService).registrarOperacion(
            eq("CREAR_USUARIO"),
            contains("Elena Martínez")
        );
    }

    @Test
    void noDeberiaInteractuarConServiciosCuandoHayError() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Elena Martínez", "emailinvalido");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.crearUsuario(usuario);
        });

        // Verify: comprobamos que no hubo interacción con ningún servicio
        verify(usuarioRepository, never()).save(any());
        verify(notificacionService, never()).enviarNotificacionRegistro(any());
        verify(auditoriaService, never()).registrarOperacion(anyString(), anyString());
    }

    @Test
    void deberiaOrquestarCorrectamenteTodasLasDependencias() {
        // Arrange: configuramos comportamiento de todos los mocks
        Usuario usuario = new Usuario(1L, "Carmen Jiménez", "carmen@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        doNothing().when(notificacionService).enviarNotificacionRegistro(any());
        doNothing().when(auditoriaService).registrarOperacion(anyString(), anyString());

        // Act
        usuarioService.crearUsuario(usuario);

        // Verify: comprobamos el orden de las interacciones
        InOrder inOrder = inOrder(usuarioRepository, notificacionService, auditoriaService);
        inOrder.verify(usuarioRepository).save(any());
        inOrder.verify(notificacionService).enviarNotificacionRegistro(any());
        inOrder.verify(auditoriaService).registrarOperacion(anyString(), anyString());
    }
}
