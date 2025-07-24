package cc.mrbird.febs.system.service.impl;

import cc.mrbird.febs.system.domain.Department;
import cc.mrbird.febs.system.dao.DepartmentMapper;
import cc.mrbird.febs.system.service.DepartmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service("departmentService")
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {
    // 可扩展自定义方法
} 