package com.miempresa.service;

import com.miempresa.service.NotificacionService;
import com.miempresa.service.AuditoriaService;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
//import static org.mockito.AdditionalAnswers.RETURNS_DEFAULTS;

import java.util.Collections;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;




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
    
    //VERIFICANDO EL COMPORTAMIENTO DE MÉTODOS
    
    @Test
    void verificacionBasica() {
        // Arrange
        Usuario usuario = new Usuario(1L, "David Ruiz", "david@ejemplo.com");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        
        // Act
        usuarioService.crearUsuario(usuario);
        
        // Verify: comprueba que el método fue llamado exactamente una vez
        verify(usuarioRepository).save(usuario);
        verify(notificacionService).enviarNotificacionRegistro(usuario);
        verify(auditoriaService).registrarOperacion(anyString(), anyString());
    }

    @Test
    void verificacionDeInvocaciones() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Lucía Gómez", "lucia@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        
        // Act: llamamos a varios métodos
        usuarioService.crearUsuario(usuario);
        usuarioService.obtenerUsuario(1L);
        usuarioService.obtenerUsuario(1L);
        
        // Verify con distintas verificaciones de número
        verify(usuarioRepository, times(1)).save(any());
        verify(usuarioRepository, times(2)).findById(1L);
        verify(notificacionService, atLeastOnce()).enviarNotificacionRegistro(any());
        verify(auditoriaService, atMost(3)).registrarOperacion(anyString(), anyString());
        verify(usuarioRepository, never()).delete(anyLong());
    }

    @Test
    void verificacionDeOrden() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Martín Vázquez", "martin@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(anyLong())).thenReturn(Optional.of(usuario));
        
        // Act: realizamos varias operaciones
        usuarioService.crearUsuario(usuario);
        usuarioService.desactivarUsuario(1L);
        
        // Verify: comprobamos que las operaciones ocurrieron en cierto orden
        InOrder orden = inOrder(usuarioRepository, notificacionService);
        
        // Primero debería guardarse el usuario y enviarse la notificación de registro
        orden.verify(usuarioRepository).save(usuario);
        orden.verify(notificacionService).enviarNotificacionRegistro(any());
        
        // Luego debería buscarse por ID, guardarse de nuevo y enviarse notificación de desactivación
        orden.verify(usuarioRepository).findById(1L);
        orden.verify(usuarioRepository).save(any());
        orden.verify(notificacionService).enviarNotificacionDesactivacion(any());
    }

    @Test
    void verificacionDeNoMasInteracciones() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Teresa Blanco", "teresa@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        
        // Act
        usuarioService.crearUsuario(usuario);
        
        // Verify: comprobamos las interacciones esperadas
        verify(usuarioRepository).save(usuario);
        verify(notificacionService).enviarNotificacionRegistro(usuario);
        verify(auditoriaService).registrarOperacion(anyString(), anyString());
        
        // Verify: comprobamos que no hay más interacciones con estos mocks
        verifyNoMoreInteractions(usuarioRepository, notificacionService);
    }

    @Test
    void verificacionConArgumentCaptor() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Sara Fernández", "sara@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        
        // Preparamos captores de argumentos
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<String> operacionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> detallesCaptor = ArgumentCaptor.forClass(String.class);
        
        // Act
        usuarioService.crearUsuario(usuario);
    

}
    
    //Mockeando Métodos Void

    @Test
    void mockearMetodoVoidConDoNothing() {
        // Arrange: configuramos el mock para que no haga nada (comportamiento por defecto)
        doNothing().when(notificacionService).enviarNotificacionRegistro(any(Usuario.class));
        
        // Alternativa: como no hacer nada es el comportamiento por defecto, 
        // podríamos omitir esta configuración
        
        // Act
        Usuario usuario = new Usuario(1L, "Raúl Torres", "raul@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        usuarioService.crearUsuario(usuario);
        
        // Verify: comprobamos que el método fue llamado
        verify(notificacionService).enviarNotificacionRegistro(usuario);
    }

    @Test
    void mockearMetodoVoidConDoThrow() {
        // Arrange: configuramos el mock para que lance una excepción
        doThrow(new RuntimeException("Error en el envío de notificación"))
            .when(notificacionService).enviarNotificacionRegistro(any(Usuario.class));
        
        // Act & Assert: verificamos que la excepción se propaga correctamente
        Usuario usuario = new Usuario(1L, "Isabel Mora", "isabel@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        
        Exception exception = assertThrows(RuntimeException.class, () -> {
            usuarioService.crearUsuario(usuario);
        });
        
        assertTrue(exception.getMessage().contains("Error en el envío"));
        
        // Verify: el usuario debería haberse guardado antes de la excepción
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void mockearMetodoVoidConDoAnswer() {
        // Arrange: configuramos un comportamiento más complejo
        doAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            // Podríamos realizar alguna acción con el usuario
            System.out.println("Simulando envío de notificación a: " + usuario.getEmail());
            // No devolvemos nada ya que el método es void
            return null;
        }).when(notificacionService).enviarNotificacionRegistro(any(Usuario.class));
        
        // Act
        Usuario usuario = new Usuario(1L, "Miguel Castro", "miguel@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        usuarioService.crearUsuario(usuario);
        
        // Verify
        verify(notificacionService).enviarNotificacionRegistro(usuario);
    }

    @Test
    void mockearMetodoVoidSelectivamente() {
        // Configuramos comportamientos diferentes según el argumento
        Usuario usuarioValido = new Usuario(1L, "Paula Lima", "paula@ejemplo.com");
        Usuario usuarioInvalido = new Usuario(2L, "Test Invalido", "test@ejemplo.com");
        
        // El método se comporta normalmente para la mayoría de usuarios
        doNothing().when(notificacionService).enviarNotificacionRegistro(any(Usuario.class));
        
        // Pero lanza excepción para un usuario específico
        doThrow(new RuntimeException("Email bloqueado"))
            .when(notificacionService).enviarNotificacionRegistro(eq(usuarioInvalido));
        
        // Act & Assert: probamos el caso válido
        when(usuarioRepository.save(any())).thenReturn(usuarioValido);
        usuarioService.crearUsuario(usuarioValido); // No debería lanzar excepción
        
        // Act & Assert: probamos el caso inválido
        when(usuarioRepository.save(any())).thenReturn(usuarioInvalido);
        assertThrows(RuntimeException.class, () -> {
            usuarioService.crearUsuario(usuarioInvalido);
        });
    }
    
    //TESTEANDO EXCEPCIONES: PARTE 1
    @Test
    void deberiaLanzarExcepcionConEmailInvalido() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Nombre Inválido", "emailsinarroba.com");
        
        // Act & Assert: verificamos que se lanza la excepción correcta
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.crearUsuario(usuario);
        });
        
        // Podemos verificar también el mensaje de la excepción
        assertEquals("Email inválido", exception.getMessage());
        
        // Verify: comprobamos que no se produjo interacción con los mocks
        verify(usuarioRepository, never()).save(any());
        verify(notificacionService, never()).enviarNotificacionRegistro(any());
        verify(auditoriaService, never()).registrarOperacion(anyString(), anyString());
    }

    @Test
    void deberiaGestionarExcepcionDelRepositorio() {
        // Arrange: configuramos el mock para lanzar una excepción
        when(usuarioRepository.save(any(Usuario.class)))
            .thenThrow(new RuntimeException("Error de base de datos"));
        
        // Act & Assert: verificamos que la excepción se propaga
        Usuario usuario = new Usuario(1L, "Victoria Alonso", "victoria@ejemplo.com");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            usuarioService.crearUsuario(usuario);
        });
        
        assertTrue(exception.getMessage().contains("Error de base de datos"));
        
        // Verify: no debería haberse enviado notificación
        verify(notificacionService, never()).enviarNotificacionRegistro(any());
    }

    @Test
    void deberiaLanzarExcepcionSelectivamente() {
        // Arrange: el repositorio lanza excepción solo para ciertos IDs
        when(usuarioRepository.findById(eq(1L)))
            .thenReturn(Optional.of(new Usuario(1L, "Usuario Normal", "normal@ejemplo.com")));
        
        when(usuarioRepository.findById(eq(99L)))
            .thenThrow(new RuntimeException("Usuario bloqueado por seguridad"));
        
        // Act & Assert: caso normal funciona correctamente
        Optional<Usuario> resultado1 = Optional.empty();
        assertTrue(resultado1.isPresent());
        
        // Act & Assert: caso especial lanza excepción
        Exception exception = assertThrows(RuntimeException.class, () -> {
            usuarioService.obtenerUsuario(99L);
        });
        
        assertEquals("Usuario bloqueado por seguridad", exception.getMessage());
    }

    @Test
    void deberiaLanzarExcepcionDespuesDeVariasLlamadas() {
        // Arrange: el repositorio funciona normalmente las dos primeras veces
        // y luego lanza una excepción
        when(usuarioRepository.findAll())
            .thenReturn(Arrays.asList(new Usuario(1L, "Usuario1", "email1@ejemplo.com")))
            .thenReturn(Arrays.asList(new Usuario(2L, "Usuario2", "email2@ejemplo.com")))
            .thenThrow(new RuntimeException("Error de conexión"));
        
        // Act & Assert: primera llamada funciona
        List<Usuario> resultado1 = usuarioService.obtenerTodosLosUsuarios();
        assertEquals(1, resultado1.size());
        assertEquals("Usuario1", resultado1.get(0).getNombre());
        
        // Act & Assert: segunda llamada funciona
        List<Usuario> resultado2 = usuarioService.obtenerTodosLosUsuarios();
        assertEquals(1, resultado2.size());
        assertEquals("Usuario2", resultado2.get(0).getNombre());
        
        // Act & Assert: tercera llamada lanza excepción
        assertThrows(RuntimeException.class, () -> {
            usuarioService.obtenerTodosLosUsuarios();
        });
    }
    
    //TESTEANDO EXCEPCIONES: PARTE 2
    
    @Test
    void deberiaGestionarExcepcionEnMetodoVoid() {
        // Arrange: configuramos el mock para lanzar una excepción
        doThrow(new RuntimeException("Error de envío"))
            .when(notificacionService).enviarNotificacionRegistro(any(Usuario.class));
        
        // Act & Assert
        Usuario usuario = new Usuario(1L, "Javier Ruiz", "javier@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        
        Exception exception = assertThrows(RuntimeException.class, () -> {
            usuarioService.crearUsuario(usuario);
        });
        
        assertEquals("Error de envío", exception.getMessage());
        
        // Verify: el usuario debería haberse guardado antes de la excepción
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deberiaGestionarMultiplesExcepciones() {
        // Creamos un servicio con comportamiento más complejo
        
        // Modificamos temporalmente UsuarioService para manejar excepciones
        // Supongamos que ahora maneja excepciones de notificación y reintenta
        class UsuarioServiceModificado extends UsuarioService {
            public UsuarioServiceModificado(UsuarioRepository repo, 
                                            NotificacionService notif,
                                            AuditoriaService audit) {
                super(repo, notif, audit);
            }
            
            @Override
            public Usuario crearUsuario(Usuario usuario) {
                if (usuario.getEmail() == null || !usuario.getEmail().contains("@")) {
                    throw new IllegalArgumentException("Email inválido");
                }
                
                Usuario usuarioGuardado = usuarioRepository.save(usuario);
                
                try {
                    notificacionService.enviarNotificacionRegistro(usuario);
                } catch (Exception e) {
                    // Loguear el error pero continuar
                    auditoriaService.registrarOperacion("ERROR", 
                        "Error al enviar notificación: " + e.getMessage());
                }
                
                auditoriaService.registrarOperacion("CREAR_USUARIO", 
                    "Usuario creado: " + usuario.getNombre());
                    
                return usuarioGuardado;
            }
        }
        
        // Ahora probamos este comportamiento
        UsuarioServiceModificado servicioModificado = new UsuarioServiceModificado(
            usuarioRepository, notificacionService, auditoriaService);
        
        // Configuramos mocks
        Usuario usuario = new Usuario(1L, "Carmen González", "carmen@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        
        doThrow(new RuntimeException("Fallo en notificación"))
            .when(notificacionService).enviarNotificacionRegistro(any());
        
        // Act
        Usuario resultado = servicioModificado.crearUsuario(usuario);
        
        // Assert
        assertNotNull(resultado);
        
        // Verify: verificamos el manejo correcto de la excepción
        verify(usuarioRepository).save(usuario);
        verify(notificacionService).enviarNotificacionRegistro(usuario);
        verify(auditoriaService).registrarOperacion(eq("ERROR"), contains("Fallo en notificación"));
        verify(auditoriaService).registrarOperacion(eq("CREAR_USUARIO"), anyString());
    }

    @Test
    void deberiaGestionarExcepcionesEncadenadas() {
        // Arrange: creamos una excepción con causa
        SQLException causaOriginal = new SQLException("Error en la consulta SQL");
        RuntimeException excepcionWrapper = new RuntimeException("Error de persistencia", causaOriginal);
        
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(excepcionWrapper);
        
        // Act & Assert
        Usuario usuario = new Usuario(1L, "Laura Martín", "laura@ejemplo.com");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            usuarioService.crearUsuario(usuario);
        });
        
        // Verificamos la excepción y su causa
        assertEquals("Error de persistencia", exception.getMessage());
        assertTrue(exception.getCause() instanceof SQLException);
        assertEquals("Error en la consulta SQL", exception.getCause().getMessage());
    }

    @Test
    void deberiaGestionarExcepcionesAsincronas() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Nombre Inválido", "emailinvalido");
        
        // Act
        Usuario futuro = usuarioService.crearUsuario(usuario);
        
        // Assert: verificamos que la excepción se propaga correctamente
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            futuro.getId(); // get() propagará la excepción si ocurre durante la ejecución asíncrona
        });
        
        // La causa real debe ser nSuestra IllegalArgumentException
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Email inválido", exception.getCause().getMessage());
    }
    
    //SPY VS MOCK: ENTENDIENDO LAS DIFERENCIAS
    @Test
    void ejemploConMock() {
        // Creamos un mock de la interfaz
        UsuarioRepository mockRepository = mock(UsuarioRepository.class);
        
        // Configuramos el comportamiento del mock
        Usuario usuario = new Usuario(1L, "Nombre Test", "test@ejemplo.com");
        when(mockRepository.findById(1L)).thenReturn(Optional.of(usuario));
        
        // Por defecto, los métodos no configurados devuelven valores por defecto
        assertTrue(mockRepository.findAll().isEmpty());     // Lista vacía por defecto
        assertFalse(mockRepository.existsById(2L));         // false por defecto
        
        // Los métodos configurados devuelven lo especificado
        Optional<Usuario> resultado = mockRepository.findById(1L);
        assertTrue(resultado.isPresent());
        assertEquals("Nombre Test", resultado.get().getNombre());
        
        // Podemos verificar las interacciones
        verify(mockRepository).findById(1L);
    }

    @Test
    void ejemploConSpy() {
        // Creamos un objeto real
        List<String> listaReal = new ArrayList<>();
        listaReal.add("uno");
        listaReal.add("dos");
        
        // Creamos un spy sobre el objeto real
        List<String> listaSpy = spy(listaReal);
        
        // Los métodos no configurados llaman a la implementación real
        assertEquals(2, listaSpy.size());      // Llama al método real size()
        assertEquals("uno", listaSpy.get(0));  // Llama al método real get()
        
        // Podemos modificar el comportamiento de algunos métodos
        // IMPORTANTE: Usar doReturn para evitar llamar al método real durante la configuración
        doReturn(100).when(listaSpy).size();
        
        // Ahora size() devuelve el valor mockeado
        assertEquals(100, listaSpy.size());
        
        // Pero otros métodos siguen llamando a la implementación real
        assertEquals("uno", listaSpy.get(0));
        
        // Podemos añadir elementos y el objeto real se actualiza
        listaSpy.add("tres");
        assertEquals(3, listaReal.size());    // La lista real tiene ahora 3 elementos
        
        // Verificación funciona igual que con mocks
        verify(listaSpy, times(2)).get(0);
    }


    
    


    
}
