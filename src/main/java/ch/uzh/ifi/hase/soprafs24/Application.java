package ch.uzh.ifi.hase.soprafs24;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RestController
@SpringBootApplication
@EnableAsync
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @GetMapping("/")
    @ResponseStatus(HttpStatus.FOUND) // 302 Redirect
    public void redirectToSwagger(HttpServletResponse response) throws IOException {
        response.sendRedirect("/swagger-ui/index.html"); // or "/swagger-ui.html" depending on your setup
    }
}
