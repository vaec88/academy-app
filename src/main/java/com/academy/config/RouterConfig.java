package com.academy.config;

import com.academy.handler.CourseHandler;
import com.academy.handler.EnrollmentHandler;
import com.academy.handler.RoleHandler;
import com.academy.handler.StudentHandler;
import com.academy.handler.UserHandler;
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

    @Bean
    public RouterFunction<ServerResponse> studentRoutes(StudentHandler studentHandler) {
        return route(GET("/v2/students"), studentHandler::findAll)
                .andRoute(GET("/v2/students/{id}"), studentHandler::findById)
                .andRoute(POST("/v2/students"), studentHandler::save)
                .andRoute(PUT("/v2/students/{id}"), studentHandler::update)
                .andRoute(DELETE("/v2/students/{id}"), studentHandler::delete);
    }

    @Bean
    public RouterFunction<ServerResponse> enrollmentRoutes(EnrollmentHandler enrollmentHandler) {
        return route(GET("/v2/enrollments"), enrollmentHandler::findAll)
                .andRoute(GET("/v2/enrollments/{id}"), enrollmentHandler::findById)
                .andRoute(POST("/v2/enrollments"), enrollmentHandler::save)
                .andRoute(PUT("/v2/enrollments/{id}"), enrollmentHandler::update)
                .andRoute(DELETE("/v2/enrollments/{id}"), enrollmentHandler::delete);
    }

    @Bean
    public RouterFunction<ServerResponse> roleRoutes(RoleHandler roleHandler) {
        return route(GET("/v2/roles"), roleHandler::findAll)
                .andRoute(GET("/v2/roles/{id}"), roleHandler::findById)
                .andRoute(POST("/v2/roles"), roleHandler::save)
                .andRoute(PUT("/v2/roles/{id}"), roleHandler::update)
                .andRoute(DELETE("/v2/roles/{id}"), roleHandler::delete);
    }

    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler userHandler) {
        return route(GET("/v2/users"), userHandler::findAll)
                .andRoute(GET("/v2/users/{id}"), userHandler::findById)
                .andRoute(POST("/v2/users"), userHandler::save)
                .andRoute(PUT("/v2/users/{id}"), userHandler::update)
                .andRoute(DELETE("/v2/users/{id}"), userHandler::delete);
    }
}
