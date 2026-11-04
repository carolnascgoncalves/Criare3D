
package br.com.criare3d.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TurboPostRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletResponseWrapper responseWrapper =
                new HttpServletResponseWrapper(response) {

                    @Override
                    public void sendRedirect(String location) throws IOException {
                        resetBuffer();
                        setStatus(HttpServletResponse.SC_SEE_OTHER);
                        setHeader("Location", encodeRedirectURL(location));
                    }
                };

        filterChain.doFilter(request, responseWrapper);
    }
}
