package com.miempresa.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Spy;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Clase de ejemplo exclusiva para mostrar @Spy
@ExtendWith(MockitoExtension.class)
class SpiesAnotacionesTest {

    // Spy de ArrayList vacía (Mockito creará una instancia por nosotros)
    @Spy
    private List<String> listaSpy = new ArrayList<>();

    // Alternativa: Mockito creará la instancia usando el constructor sin argumentos
    @Spy
    private ArrayList<Integer> numerosSpy;

    @Test
    void testSpyConAnotaciones() {
        // Los métodos no mockeados llaman a la implementación real
        listaSpy.add("uno");
        assertEquals(1, listaSpy.size());
        assertEquals("uno", listaSpy.get(0));

        // Podemos modificar el comportamiento de algunos métodos
        doReturn(100).when(listaSpy).size();
        assertEquals(100, listaSpy.size());

        // Y seguir utilizando el comportamiento real para otros
        listaSpy.add("dos");
        assertEquals("dos", listaSpy.get(1));
    }
}
