package com.miempresa.service;

import com.miempresa.model.Usuario;
import com.miempresa.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Answer.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;



public class UsuarioServiceTest {
	
	@Mock
    private UsuarioRepository usuarioRepository;
    
    @Mock
    private NotificacionService notificacionService;
    
    private UsuarioService usuarioService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usuarioService = new UsuarioService(usuarioRepository, notificacionService);
    }
    
    @Test
    void deberiaCrearUsuarioConExito() {
        // Arrange (Preparación)
        Usuario usuario = new Usuario(1L, "Juan Pérez", "juan@ejemplo.com");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        
        // Act (Acción)
        Usuario resultado = usuarioService.crearUsuario(usuario);
        
        // Assert (Verificación)
        assertNotNull(resultado);
        assertEquals("Juan Pérez", resultado.getNombre());
        assertEquals("juan@ejemplo.com", resultado.getEmail());
        
        // Verify (Comprobación de interacciones)
        verify(usuarioRepository).save(usuario);
        verify(notificacionService).enviarNotificacionRegistro(usuario);
    }
    
    @Test
    void deberiaLanzarExcepcionCuandoEmailEsInvalido() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Juan Pérez", "juanejemplo.com"); // Email sin @
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.crearUsuario(usuario);
        });
        
        assertEquals("Email inválido", exception.getMessage());
        
        // Verify
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(notificacionService, never()).enviarNotificacionRegistro(any(Usuario.class));
    }
    //Mocks programáticos
    @Test
    void creacionProgramaticaDeMocks_metodosBasicos() {
        // Crear un mock de forma programática
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        NotificacionService notificacionService = mock(NotificacionService.class);
        // Inicializar el servicio con los mocks
        UsuarioService usuarioService = new UsuarioService(usuarioRepository, notificacionService);


        // Devolver un usuario específico cuando se busca por ID
        Usuario usuario = new Usuario(1L, "Ana García", "ana@ejemplo.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // Devolver una lista cuando se buscan todos los usuarios
        List<Usuario> usuarios = Arrays.asList(
           new Usuario(1L, "Ana García", "ana@ejemplo.com"),
           new Usuario(2L, "Carlos López", "carlos@ejemplo.com")
        );
        when(usuarioRepository.findAll()).thenReturn(usuarios);

        // Simular que un método lanza una excepción
        when(usuarioRepository.findById(99L)).thenThrow(new RuntimeException("Usuario no encontrado"));

        // Configurar métodos void
        doNothing().when(notificacionService).enviarNotificacionRegistro(any(Usuario.class));
        doThrow(new RuntimeException("Error de conexión"))
           .when(notificacionService).enviarNotificacionDesactivacion(any(Usuario.class));

        // También podemos utilizar matchers para hacer que las configuraciones sean más flexibles:
        // Usar any() para cualquier objeto
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        // Usar eq() para un valor específico
        when(usuarioRepository.findById(eq(1L))).thenReturn(Optional.of(usuario));
        // Combinar matchers
        when(usuarioRepository.save(argThat(u -> u.getEmail().contains("@"))))
           .thenReturn(usuario);

        // A modo de verificación básica (puedes adaptarlo a tu lógica):
        assertTrue(usuarioService.obtenerUsuario(1L).isPresent());
        assertEquals(2, usuarioService.obtenerTodosLosUsuarios().size());
        assertThrows(RuntimeException.class, () -> usuarioService.obtenerUsuario(99L));
    }
    
    //VALORES POR DEFECTO EN LOS MOCKS

    @Test
    void testValoresPorDefecto() {
        // Crear mock sin configurar comportamiento
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        // Verificar valores por defecto
        assertNull(usuarioRepository.save(new Usuario(1L, "test", "test@example.com")));
        assertTrue(usuarioRepository.findAll().isEmpty());
        assertEquals(Optional.empty(), usuarioRepository.findById(1L));
        assertFalse(usuarioRepository.existsById(1L));
    }

    @Test
    void testRespuestasPorDefectoPersonalizadas() {
        // Crear mock con respuestas personalizadas
        UsuarioRepository usuarioRepository = mock(
            UsuarioRepository.class,
            new Answer<Object>() {
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    if (invocation.getMethod().getReturnType().equals(Optional.class)) {
                        return Optional.of(new Usuario(1L, "Usuario Default", "default@ejemplo.com"));
                    }
                    if (invocation.getMethod().getReturnType().equals(List.class)) {
                        return Collections.singletonList(
                            new Usuario(1L, "Usuario Default", "default@ejemplo.com"));
                    }
                    return RETURNS_DEFAULTS.answer(invocation);
                }
            }
        );

        // Ahora todos los métodos que devuelven Optional contendrán un Usuario
        assertTrue(usuarioRepository.findById(999L).isPresent());

        // Y findAll() devolverá una lista con un elemento
        assertEquals(1, usuarioRepository.findAll().size());
    }

    

    
    
    
    
    

}
