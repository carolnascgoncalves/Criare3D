package br.com.criare3d.service.interfaces;

import org.springframework.security.core.userdetails.UserDetails;

public interface UsuarioDetailsService {
    public UserDetails loadUserByUsername(String email);
}
