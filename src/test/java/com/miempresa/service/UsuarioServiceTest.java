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
import java.util.concurrent.atomic.AtomicInteger;

import com.miempresa.model.Usuario;
import com.miempresa.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
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
    
    //MOCKS CON MÚLTIPLES RETURNS
    @Test
    void multipleReturnsSecuenciales() {
        // Configuramos respuestas secuenciales
        when(usuarioRepository.findAll())
            .thenReturn(Arrays.asList(new Usuario(1L, "Usuario1", "email1@ejemplo.com")))  // Primera llamada
            .thenReturn(Arrays.asList(
                new Usuario(2L, "Usuario2", "email2@ejemplo.com"),
                new Usuario(3L, "Usuario3", "email3@ejemplo.com")))  // Segunda llamada
            .thenReturn(Collections.emptyList());  // Tercera llamada y siguientes

        // Primera llamada
        List<Usuario> resultado1 = usuarioService.obtenerTodosLosUsuarios();
        assertEquals(1, resultado1.size());
        assertEquals("Usuario1", resultado1.get(0).getNombre());

        // Segunda llamada
        List<Usuario> resultado2 = usuarioService.obtenerTodosLosUsuarios();
        assertEquals(2, resultado2.size());
        assertEquals("Usuario2", resultado2.get(0).getNombre());
        assertEquals("Usuario3", resultado2.get(1).getNombre());

        // Tercera llamada
        List<Usuario> resultado3 = usuarioService.obtenerTodosLosUsuarios();
        assertTrue(resultado3.isEmpty());

        // Cuarta llamada (sigue devolviendo el último valor configurado)
        List<Usuario> resultado4 = usuarioService.obtenerTodosLosUsuarios();
        assertTrue(resultado4.isEmpty());
    }

    @Test
    void returnsYThrows() {
        // Configuramos una secuencia que eventualmente lanza una excepción
        when(usuarioRepository.findById(eq(1L)))
            .thenReturn(Optional.of(new Usuario(1L, "Usuario Inicial", "inicial@ejemplo.com")))
            .thenReturn(Optional.of(new Usuario(1L, "Usuario Actualizado", "actualizado@ejemplo.com")))
            .thenThrow(new RuntimeException("Base de datos no disponible"));

        // Primera llamada - devuelve valor
        Optional<Usuario> resultado1 = usuarioService.obtenerUsuario(1L);
        assertTrue(resultado1.isPresent());
        assertEquals("Usuario Inicial", resultado1.get().getNombre());

        // Segunda llamada - devuelve otro valor
        Optional<Usuario> resultado2 = usuarioService.obtenerUsuario(1L);
        assertTrue(resultado2.isPresent());
        assertEquals("Usuario Actualizado", resultado2.get().getNombre());

        // Tercera llamada - lanza excepción
        Exception exception = assertThrows(RuntimeException.class, () -> {
            usuarioService.obtenerUsuario(1L);
        });
        assertEquals("Base de datos no disponible", exception.getMessage());
    }

    @Test
    void respuestasDinamicas() {
        // Usamos un contador atómico para llevar la cuenta de las llamadas
        AtomicInteger contador = new AtomicInteger(0);

        // Configuramos un comportamiento dinámico basado en el contador
        when(usuarioRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            int numLlamada = contador.getAndIncrement();

            if (numLlamada == 0) {
                return Optional.of(new Usuario(id, "Primera llamada", "primera@ejemplo.com"));
            } else if (numLlamada == 1) {
                return Optional.of(new Usuario(id, "Segunda llamada", "segunda@ejemplo.com"));
            } else if (numLlamada < 5) {
                return Optional.of(new Usuario(id, "Llamada intermedia", "intermedia@ejemplo.com"));
            } else {
                throw new RuntimeException("Demasiadas consultas");
            }
        });

        // Probamos el comportamiento dinámico
        for (int i = 0; i < 5; i++) {
            Optional<Usuario> resultado = usuarioService.obtenerUsuario(5L);
            assertTrue(resultado.isPresent());
            if (i == 0) {
                assertEquals("Primera llamada", resultado.get().getNombre());
            } else if (i == 1) {
                assertEquals("Segunda llamada", resultado.get().getNombre());
            } else {
                assertEquals("Llamada intermedia", resultado.get().getNombre());
            }
        }

        // La sexta llamada debería lanzar excepción
        assertThrows(RuntimeException.class, () -> {
            usuarioService.obtenerUsuario(5L);
        });
    }

    @Test
    void returnsPorArgumentos() {
        // Configuramos diferentes respuestas según el ID
        when(usuarioRepository.findById(eq(1L)))
            .thenReturn(Optional.of(new Usuario(1L, "Admin", "admin@ejemplo.com")));

        when(usuarioRepository.findById(eq(2L)))
            .thenReturn(Optional.of(new Usuario(2L, "Usuario", "usuario@ejemplo.com")));

        when(usuarioRepository.findById(eq(3L)))
            .thenReturn(Optional.empty());

        when(usuarioRepository.findById(argThat(id -> id > 1000)))
            .thenThrow(new RuntimeException("ID fuera de rango"));

        // Probamos diferentes IDs
        assertTrue(usuarioService.obtenerUsuario(1L).isPresent());
        assertEquals("Admin", usuarioService.obtenerUsuario(1L).get().getNombre());

        assertTrue(usuarioService.obtenerUsuario(2L).isPresent());
        assertEquals("Usuario", usuarioService.obtenerUsuario(2L).get().getNombre());

        assertFalse(usuarioService.obtenerUsuario(3L).isPresent());

        assertThrows(RuntimeException.class, () -> {
            usuarioService.obtenerUsuario(1001L);
        });
    }
    
    //VERIFICANDO MÉTODOS VOID
    
    @Test
    void verificacionBasicaMetodoVoid() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Pedro García", "pedro@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        
        // Act: llamamos a métodos que ejecutan operaciones void
        usuarioService.crearUsuario(usuario);
        usuarioService.desactivarUsuario(1L);
        
        // Verify: verificamos que los métodos void fueron llamados
        verify(notificacionService).enviarNotificacionRegistro(usuario);
        verify(notificacionService).enviarNotificacionDesactivacion(any());
    }

    @Test
    void verificarNumeroLlamadasVoid() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Sandra Ruiz", "sandra@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(anyLong())).thenReturn(Optional.of(usuario));
        
        // Act: llamamos los métodos múltiples veces
        usuarioService.crearUsuario(usuario);
        usuarioService.desactivarUsuario(1L);
        usuarioService.desactivarUsuario(2L);
        usuarioService.desactivarUsuario(3L);
        
        // Verify: comprobamos el número exacto de llamadas
        verify(notificacionService, times(1)).enviarNotificacionRegistro(any());
        verify(notificacionService, times(3)).enviarNotificacionDesactivacion(any());
        
        // Alternativas para verificar número de llamadas
        verify(notificacionService, atLeastOnce()).enviarNotificacionRegistro(any());
        verify(notificacionService, atLeast(2)).enviarNotificacionDesactivacion(any());
        verify(notificacionService, atMost(5)).enviarNotificacionDesactivacion(any());
        verify(usuarioRepository, never()).delete(anyLong());
    }

    @Test
    void capturarArgumentosMetodoVoid() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Roberto Núñez", "roberto@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        
        // Preparamos captores de argumentos
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<String> operacionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> detallesCaptor = ArgumentCaptor.forClass(String.class);
        
        // Act
        usuarioService.crearUsuario(usuario);
        usuarioService.desactivarUsuario(1L);
        
        // Verify: capturamos y analizamos los argumentos
        verify(notificacionService, times(2)).enviarNotificacionRegistro(usuarioCaptor.capture());
        verify(auditoriaService, atLeastOnce()).registrarOperacion(
            operacionCaptor.capture(), detallesCaptor.capture());
        
        // Obtenemos todos los valores capturados
        List<Usuario> usuariosCapturados = usuarioCaptor.getAllValues();
        List<String> operacionesCapturadas = operacionCaptor.getAllValues();
        
        // Analizamos los valores capturados
        assertEquals(2, usuariosCapturados.size());
        assertEquals("Roberto Núñez", usuariosCapturados.get(0).getNombre());
        assertTrue(operacionesCapturadas.contains("CREAR_USUARIO"));
    }

    @Test
    void verificarNoLlamadaMetodoVoid() {
        // Arrange: configuramos el repositorio para devolver Optional.empty()
        when(usuarioRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // Act: intentamos desactivar un usuario que no existe
        usuarioService.desactivarUsuario(99L);
        
        // Verify: no debería enviarse ninguna notificación ni guardarse nada
        verify(notificacionService, never()).enviarNotificacionDesactivacion(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void verificarOrdenLlamadasVoid() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Carolina Silva", "carolina@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        
        // Act: realizamos operaciones en cierto orden
        usuarioService.crearUsuario(usuario);
        usuarioService.desactivarUsuario(1L);
        
        // Verify: verificamos que las operaciones ocurrieron en el orden esperado
        InOrder orden = inOrder(notificacionService, auditoriaService);
        
        // Primera debería enviarse la notificación de registro
        orden.verify(notificacionService).enviarNotificacionRegistro(any());
        
        // Luego la notificación de desactivación
        orden.verify(notificacionService).enviarNotificacionDesactivacion(any());
        
        // Y finalmente debería registrarse en auditoría (asumiendo que esto ocurre al final)
        orden.verify(auditoriaService, atLeastOnce()).registrarOperacion(anyString(), anyString());
    }

    //ARGUMENT CAPTURE: ANÁLISIS DETALLADO DE ARGUMENTOS   
    @Test
    void capturaBasicaDeArgumentos() {
        // Creamos un captor para el tipo Usuario
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // Simulamos una operación
        Usuario usuario = new Usuario(1L, "Marta López", "marta@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        usuarioService.crearUsuario(usuario);

        // Verificamos la llamada y capturamos el argumento
        verify(usuarioRepository).save(usuarioCaptor.capture());

        // Accedemos al valor capturado
        Usuario usuarioCapturado = usuarioCaptor.getValue();

        // Realizamos verificaciones sobre el valor capturado
        assertEquals("Marta López", usuarioCapturado.getNombre());
        assertEquals("marta@ejemplo.com", usuarioCapturado.getEmail());
        assertTrue(usuarioCapturado.isActivo());
    }

    @Test
    void capturaMultiplesArgumentos() {
        // Captores para diferentes argumentos
        ArgumentCaptor<String> tipoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> detallesCaptor = ArgumentCaptor.forClass(String.class);
        // Simulamos varias operaciones
        Usuario usuario1 = new Usuario(1L, "Ana Gil", "ana@ejemplo.com");
        Usuario usuario2 = new Usuario(2L, "Mario Ros", "mario@ejemplo.com");
        when(usuarioRepository.save(any()))
            .thenReturn(usuario1)
            .thenReturn(usuario2);
        when(usuarioRepository.findById(anyLong()))
            .thenReturn(Optional.of(usuario1));

        usuarioService.crearUsuario(usuario1);
        usuarioService.crearUsuario(usuario2);
        usuarioService.desactivarUsuario(1L);
        // Verificamos y capturamos todas las llamadas
        verify(auditoriaService, times(3)).registrarOperacion(
            tipoCaptor.capture(), detallesCaptor.capture());
        // Obtenemos todas las capturas
        List<String> tipos = tipoCaptor.getAllValues();
        List<String> detalles = detallesCaptor.getAllValues();
        // Verificamos los valores capturados
        assertEquals(3, tipos.size());
        assertEquals(3, detalles.size());
        // Verificamos el contenido específico
        assertTrue(tipos.contains("CREAR_USUARIO"));
        assertTrue(tipos.contains("DESACTIVAR_USUARIO"));
        boolean encontradoAna = detalles.stream().anyMatch(d -> d.contains("Ana Gil"));
        boolean encontradoMario = detalles.stream().anyMatch(d -> d.contains("Mario Ros"));
        assertTrue(encontradoAna, "Debería encontrarse Ana en los detalles");
        assertTrue(encontradoMario, "Debería encontrarse Mario en los detalles");
    }

    @Test
    void verificacionesAvanzadasConCaptura() {
        // Simulamos una implementación adicional
        class UsuarioServiceExtendido extends UsuarioService {
            public UsuarioServiceExtendido(UsuarioRepository repo,
                                           NotificacionService notif,
                                           AuditoriaService audit) {
                super(repo, notif, audit);
            }

            public List<Usuario> crearUsuariosEnLote(List<Usuario> usuarios) {
                List<Usuario> resultado = new ArrayList<>();
                for (Usuario u : usuarios) {
                    if (u.getEmail() != null && u.getEmail().contains("@")) {
                        Usuario guardado = usuarioRepository.save(u);
                        notificacionService.enviarNotificacionRegistro(guardado);
                        resultado.add(guardado);
                    }
                }
                auditoriaService.registrarOperacion("CREAR_LOTE",
                    "Creados " + resultado.size() + " usuarios en lote");
                return resultado;
            }
        }

        // Creamos una instancia del servicio extendido
        UsuarioServiceExtendido servicioExtendido = new UsuarioServiceExtendido(
            usuarioRepository, notificacionService, auditoriaService);

        // Preparamos datos y captores
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        List<Usuario> loteUsuarios = Arrays.asList(
            new Usuario(1L, "User1", "user1@ejemplo.com"),
            new Usuario(2L, "User2", "user2@ejemplo.com"),
            new Usuario(3L, "User3", "emailinvalido"),  // Email inválido
            new Usuario(4L, "User4", "user4@ejemplo.com")
        );

        // Configuramos el mock para devolver el mismo usuario que recibe
        when(usuarioRepository.save(any()))
            .thenAnswer(i -> i.getArgument(0));

        // Act
        List<Usuario> resultado = servicioExtendido.crearUsuariosEnLote(loteUsuarios);

        // Verify: capturamos todos los usuarios guardados
        verify(usuarioRepository, times(3)).save(usuarioCaptor.capture());

        // Obtenemos los valores capturados
        List<Usuario> usuariosGuardados = usuarioCaptor.getAllValues();

        // Verificaciones avanzadas
        assertEquals(3, usuariosGuardados.size());
        assertEquals(3, resultado.size());

        // Verificamos que no se guardó el usuario con email inválido
        boolean encontradoInvalido = usuariosGuardados.stream()
            .anyMatch(u -> "User3".equals(u.getNombre()));
        assertFalse(encontradoInvalido, "No debería guardarse el usuario con email inválido");

        // Verificamos que se enviaron las notificaciones correctas
        verify(notificacionService, times(3)).enviarNotificacionRegistro(any());

        // Y verificamos la auditoría
        verify(auditoriaService)
            .registrarOperacion(eq("CREAR_LOTE"), contains("3 usuarios"));
    }

    @ExtendWith(MockitoExtension.class)
    class ArgumentCaptorAnotacionesTest {
        @Mock private UsuarioRepository usuarioRepository;
        @Mock private NotificacionService notificacionService;
        @Mock private AuditoriaService auditoriaService;
        @InjectMocks private UsuarioService usuarioService;
        @Captor private ArgumentCaptor<Usuario> usuarioCaptor;
        @Captor private ArgumentCaptor<String> stringCaptor;

        @Test
        void testCapturadorConAnotaciones() {
            // Arrange
            Usuario usuario = new Usuario(1L, "Jaime Vega", "jaime@ejemplo.com");
            when(usuarioRepository.save(any())).thenReturn(usuario);

            // Act
            usuarioService.crearUsuario(usuario);

            // Verify con los captores ya inyectados
            verify(usuarioRepository).save(usuarioCaptor.capture());
            verify(auditoriaService).registrarOperacion(eq("CREAR_USUARIO"), stringCaptor.capture());

            // Accedemos a los valores capturados
            Usuario usuarioCaptado = usuarioCaptor.getValue();
            String detallesCaptados = stringCaptor.getValue();

            // Verificamos
            assertEquals("Jaime Vega", usuarioCaptado.getNombre());
            assertTrue(detallesCaptados.contains("Jaime Vega"));
        }
    }

    



    
    


    
}
