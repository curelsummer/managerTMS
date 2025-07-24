package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.system.domain.Department;
import cc.mrbird.febs.system.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {
    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    public List<Department> list() {
        return departmentService.list();
    }

    @PostMapping
    public boolean add(@RequestBody Department department) {
        return departmentService.save(department);
    }

    @PutMapping
    public boolean update(@RequestBody Department department) {
        return departmentService.updateById(department);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return departmentService.removeById(id);
    }
} 