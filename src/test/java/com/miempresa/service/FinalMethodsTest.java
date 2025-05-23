package com.miempresa.service;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Clase con métodos final
class ClaseConMetodosFinal {
   public final String metodoFinal() {
       return "Resultado real del método final";
   }
   public final String metodoFinalConParametro(String param) {
       return "Procesando: " + param;
   }
}

public class FinalMethodsTest {

   @Test
   void testMockearMetodoFinal() {
      // 1. Crear el mock
      ClaseConMetodosFinal mock = mock(ClaseConMetodosFinal.class);

      // 2. Stubear el método final
      when(mock.metodoFinal()).thenReturn("Resultado mockeado");
      when(mock.metodoFinalConParametro(anyString())).thenReturn("Parámetro mockeado");

      // 3. Verificar
      assertEquals("Resultado mockeado", mock.metodoFinal());
      assertEquals("Parámetro mockeado", mock.metodoFinalConParametro("test"));

      verify(mock).metodoFinal();
      verify(mock).metodoFinalConParametro("test");
   }
}
