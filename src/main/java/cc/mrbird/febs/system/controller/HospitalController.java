package cc.mrbird.febs.system.controller;

import cc.mrbird.febs.system.domain.Hospital;
import cc.mrbird.febs.system.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/hospital")
public class HospitalController {
    @Autowired
    private HospitalService hospitalService;

    @GetMapping
    public List<Hospital> list() {
        return hospitalService.list();
    }

    @PostMapping
    public boolean add(@RequestBody Hospital hospital) {
        return hospitalService.save(hospital);
    }

    @PutMapping
    public boolean update(@RequestBody Hospital hospital) {
        return hospitalService.updateById(hospital);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return hospitalService.removeById(id);
    }
} 