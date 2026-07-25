package ec.edu.ups.icc.academicevents.categories.controllers;

import ec.edu.ups.icc.academicevents.categories.dtos.CategoryResponse;
import ec.edu.ups.icc.academicevents.categories.services.CategoryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(
            CategoryService categoryService
    ) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>>
    findAllActive() {

        return ResponseEntity.ok(
                categoryService.findAllActive()
        );
    }
}