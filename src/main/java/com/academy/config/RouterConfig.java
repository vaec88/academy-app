package com.academy.config;

import com.academy.handler.CourseHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> courseRoutes(CourseHandler courseHandler) {
        return route(GET("/v2/courses"), courseHandler::findAll)
                .andRoute(GET("/v2/courses/{id}"), courseHandler::findById)
                .andRoute(POST("/v2/courses"), courseHandler::save)
                .andRoute(PUT("/v2/courses/{id}"), courseHandler::update)
                .andRoute(DELETE("/v2/courses/{id}"), courseHandler::delete);
    }
}
