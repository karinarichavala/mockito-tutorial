package com.miempresa.service;

import com.miempresa.model.Usuario;
import com.miempresa.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    
    //ARGUMENT MATCHERS: FLEXIBILIDAD EN TESTS
    
    @Test
    void ejemplosDeArgumentMatchers() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Patricia Sánchez", "patricia@ejemplo.com");
        
        // Matchers básicos y de tipo
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(anyLong())).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        
        // Matchers de valor
        when(usuarioRepository.findById(eq(1L))).thenReturn(Optional.of(usuario));
        
        // Combinando matchers con valores exactos
        // IMPORTANTE: No se puede mezclar valores concretos y matchers en una misma llamada
        // Incorrecto: when(service.metodo(eq(1), "valor")).thenReturn(...);
        // Correcto: when(service.metodo(eq(1), eq("valor"))).thenReturn(...);
        
        // Matchers de texto
        doNothing().when(auditoriaService).registrarOperacion(
            contains("USUARIO"),
            matches(".*@ejemplo\\.com.*")
        );
        
        // Matchers personalizados con argThat
        when(usuarioRepository.save(argThat(u -> 
            u.getEmail() != null && u.getEmail().contains("@")
        ))).thenReturn(usuario);
        
        // Matchers personalizados más complejos
        when(usuarioRepository.save(argThat(new ArgumentMatcher<Usuario>() {
            @Override
            public boolean matches(Usuario u) {
                return u.getNombre() != null && 
                       u.getNombre().length() > 3 &&
                       u.getEmail() != null &&
                       u.getEmail().matches(".*@.*\\..*");
            }
            
            @Override
            public String toString() {
                // Mensaje descriptivo para los fallos de test
                return "un usuario con nombre válido y email en formato correcto";
            }
        }))).thenReturn(usuario);
    }
    
    //EL MÉTODO DORETURN: ALTERNATIVA A WHEN THENRETURN
    
    @Test
    void ejemplosConDoReturn() {
        // 1. Para métodos void (este caso usaría doNothing() o doThrow())
        // No funciona: when(notificacionService.enviarNotificacionRegistro(any())).thenReturn(...);
        doNothing().when(notificacionService).enviarNotificacionRegistro(any());

        // 2. Con spies
        List<String> listaSpy = spy(new ArrayList<>());

        // Problema: Esto llama al método real size() que devuelve 0
        // when(listaSpy.size()).thenReturn(10);

        // Solución: usar doReturn() para evitar llamar al método real
        doReturn(10).when(listaSpy).size();

        // 3. Para excepciones (aunque también se puede usar when().thenThrow())
        doThrow(new RuntimeException("Error simulado"))
            .when(usuarioRepository).delete(anyLong());

        // 4. Cuando el método puede tener efectos secundarios
        // Problema: Si get(0) lanza IndexOutOfBoundsException, el test fallará
        // when(listaSpy.get(0)).thenReturn("valor");

        // Solución: doReturn() evita llamar al método real
        doReturn("valor").when(listaSpy).get(0);
    }

    @Test
    void ejemploConDoAnswer() {
        // Usando doAnswer para responder dinámicamente según los argumentos
        doAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            // Simulamos lógica que asigna un ID al guardar
            if (usuario.getId() == null) {
                usuario.setId(1L);
            }
            return usuario;
        }).when(usuarioRepository).save(any(Usuario.class));

        // Uso
        Usuario nuevoUsuario = new Usuario(null, "Nuevo Usuario", "nuevo@ejemplo.com");
        Usuario guardado = usuarioService.crearUsuario(nuevoUsuario);

        // El ID debe haber sido asignado por nuestro doAnswer
        assertEquals(1L, guardado.getId());
    }

    
    

}
