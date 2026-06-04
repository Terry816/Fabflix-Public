package com.fabflix.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping(value = {
            "/",
            "/login",
            "/movies",
            "/movies/{id}",
            "/stars/{id}",
            "/cart",
            "/payment",
            "/confirmation",
            "/employee",
            "/employee/{*path}"
    })
    public String forwardToReact() {
        return "forward:/index.html";
    }
}
