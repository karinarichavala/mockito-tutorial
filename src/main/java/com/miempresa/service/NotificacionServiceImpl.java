package com.miempresa.service;

import com.miempresa.model.Usuario;

public class NotificacionServiceImpl implements NotificacionService {
    @Override
    public void enviarNotificacionRegistro(Usuario usuario) {
        // Implementación real (en tests no hace nada o lo mockeas)
        System.out.println("Enviando notificación de registro a " + usuario.getEmail());
    }

    @Override
    public void enviarNotificacionDesactivacion(Usuario usuario) {
        System.out.println("Enviando notificación de desactivación a " + usuario.getEmail());
    }
    
    
}
