package ec.edu.ups.icc.academicevents.categories.controllers;

import ec.edu.ups.icc.academicevents.categories.dtos.CategoryResponse;
import ec.edu.ups.icc.academicevents.categories.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@Tag(name = "Categories", description = "Consulta de categorías disponibles para clasificar eventos académicos")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(
            CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Listar categorías activas", description = "Devuelve las categorías activas ordenadas alfabéticamente por nombre.")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAllActive() {

        return ResponseEntity.ok(
                categoryService.findAllActive());
    }
}