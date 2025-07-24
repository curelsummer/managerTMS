package cc.mrbird.febs.system.dao;

import cc.mrbird.febs.system.domain.Department;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository("departmentMapper")     
public interface DepartmentMapper extends BaseMapper<Department> {
    /**
     * 通过科室ID查找医院ID
     */
    @Select("SELECT hospital_id FROM t_department WHERE department_id = #{departmentId}")
    Long findHospitalIdByDepartmentId(@Param("departmentId") Long departmentId);
} 