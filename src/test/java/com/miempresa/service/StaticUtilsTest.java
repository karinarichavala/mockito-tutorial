package com.miempresa.service;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Clase con métodos estáticos
class UtilsEstaticos {
   public static String formatearFecha(LocalDate fecha) {
       return fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
   }
   public static boolean esEmailValido(String email) {
       return email != null && email.contains("@") && email.contains(".");
   }
   public static int generarIdUnico() {
       return (int) (System.currentTimeMillis() % 10000);
   }
}

public class StaticUtilsTest {

   @Test
   void testMockearMetodoEstatico() {
       try (MockedStatic<UtilsEstaticos> utilsMock = mockStatic(UtilsEstaticos.class)) {
           // 1. Stubear estáticos
           utilsMock.when(() -> UtilsEstaticos.formatearFecha(any(LocalDate.class)))
                   .thenReturn("01/01/2023");
           utilsMock.when(() -> UtilsEstaticos.esEmailValido("ok@ejemplo.com"))
                   .thenReturn(true);
           utilsMock.when(UtilsEstaticos::generarIdUnico)
                   .thenReturn(12345);

           // 2. Verificar comportamiento mockeado
           assertEquals("01/01/2023", UtilsEstaticos.formatearFecha(LocalDate.now()));
           assertTrue(UtilsEstaticos.esEmailValido("ok@ejemplo.com"));
           assertEquals(12345, UtilsEstaticos.generarIdUnico());

           // 3. Verificar invocaciones
           utilsMock.verify(() -> UtilsEstaticos.formatearFecha(any()));
           utilsMock.verify(() -> UtilsEstaticos.esEmailValido("ok@ejemplo.com"));
           utilsMock.verify(UtilsEstaticos::generarIdUnico);
       }
       // Fuera del try, los métodos vuelven a su comportamiento real
   }
}
