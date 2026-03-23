package com.clawreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clawreport.entity.Report;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}
