package br.com.criare3d.config;

import br.com.criare3d.dto.RascunhoCalculo;
import br.com.criare3d.service.CalculoRascunhoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Configuration
public class CalculoRascunhoWeb implements WebMvcConfigurer, HandlerMethodArgumentResolver, HandlerInterceptor {
    private static final String ESTADO = CalculoRascunhoWeb.class.getName();
    private final CalculoRascunhoService service;

    public CalculoRascunhoWeb(CalculoRascunhoService service) {
        this.service = service;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == RascunhoCalculo.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer model,
                                  NativeWebRequest web, WebDataBinderFactory binder) {
        var request = web.getNativeRequest(HttpServletRequest.class);
        var estado = estado(request);
        model.addAttribute("rascunhoId", estado.getId());
        return estado;
    }

    private RascunhoCalculo estado(HttpServletRequest request) {
        if (request.getAttribute(ESTADO) instanceof RascunhoCalculo estado) return estado;
        String referencia = request.getParameter("rascunhoId");
        var estado = service.abrir(referencia == null || referencia.isBlank() ? null : UUID.fromString(referencia),
            request.getServletPath().equals("/historico/novo") && "true".equals(request.getParameter("novo")));
        request.setAttribute(ESTADO, estado);
        return estado;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView model) {
        if (model == null) return;
        RascunhoCalculo estado = request.getAttribute(ESTADO) instanceof RascunhoCalculo r ? r : null;
        if (estado == null && request.getParameter("rascunhoId") != null
            && !request.getParameter("rascunhoId").isBlank()) estado = estado(request);
        if (estado == null) return;
        model.addObject("rascunhoId", estado.getId());
        model.addObject("rascunhoFormularioJson", service.formularioJson(estado));
        String view = model.getViewName();
        if (view != null && view.startsWith("redirect:/historico/")) {
            model.setViewName("redirect:" + UriComponentsBuilder.fromUriString(view.substring("redirect:".length()))
                .replaceQueryParam("rascunhoId", estado.getId()).build().toUriString());
        }
    }
}
