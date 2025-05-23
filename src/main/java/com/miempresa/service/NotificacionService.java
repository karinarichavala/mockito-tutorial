package com.miempresa.service;

import com.miempresa.model.Usuario;

public interface NotificacionService {
	
	void enviarNotificacionRegistro(Usuario usuario);
    void enviarNotificacionDesactivacion(Usuario usuario);

}
