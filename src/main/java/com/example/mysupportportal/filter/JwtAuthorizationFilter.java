package com.example.mysupportportal.filter;

import com.example.mysupportportal.constant.SecurityConstant;
import com.example.mysupportportal.utility.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public JwtAuthorizationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //AKO JE REQUEST METHOD OPTION TERAJ DALJE
        if(request.getMethod().equalsIgnoreCase(SecurityConstant.OPTIONS_HTTP_METHOD)){
            response.setStatus(HttpStatus.OK.value());
        }else{
            //AKO NIJE OPTIONS MORA IMATI bERAER HEDER
            String authorizationHeader=request.getHeader(HttpHeaders.AUTHORIZATION);
            if(authorizationHeader == null || !authorizationHeader.startsWith(SecurityConstant.TOKEN_PREFIX)){
                filterChain.doFilter(request, response);
                return;
            }
            //SKINI BEARER
            String token=authorizationHeader.substring(SecurityConstant.TOKEN_PREFIX.length());
            //DOGRABI USERNAM IZ TOKENA
            String username= jwtTokenProvider.getSubject(token);

            //AKO JE TOKEN VALID I NEMA KORISNIKA U SECURITY CONTEXTU
            //ONDA SETUJ KORISNIKA U CONTEXT
            if(jwtTokenProvider.isTokenValid(username, token) && SecurityContextHolder.getContext().getAuthentication()==null){
                List<GrantedAuthority> authorities=jwtTokenProvider.getAuthorities(token);
                Authentication authentication= jwtTokenProvider.getAuthentication(username, authorities, request);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }else{
                SecurityContextHolder.clearContext();
            }
        }
        //TERAJ DALJE
        filterChain.doFilter(request, response);
    }
}
