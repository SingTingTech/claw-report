package com.clawreport.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clawreport.common.PageResult;
import com.clawreport.entity.Report;
import com.clawreport.entity.ReportCategory;
import com.clawreport.entity.ReportParam;
import com.clawreport.entity.ReportShare;
import com.clawreport.mapper.ReportCategoryMapper;
import com.clawreport.mapper.ReportMapper;
import com.clawreport.mapper.ReportParamMapper;
import com.clawreport.mapper.ReportShareMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportService {

    @Autowired
    private ReportMapper reportMapper;
    
    @Autowired
    private ReportParamMapper reportParamMapper;
    
    @Autowired
    private ReportCategoryMapper categoryMapper;
    
    @Autowired
    private ReportShareMapper shareMapper;
    
    @Autowired
    private ObjectMapper objectMapper;

    public List<Report> list() {
        return reportMapper.selectList(
            new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, 1)
                .orderByDesc(Report::getCreateTime)
        );
    }

    public Report getById(Long id) {
        return reportMapper.selectById(id);
    }

    public List<ReportParam> getParams(Long reportId) {
        return reportParamMapper.selectList(
            new LambdaQueryWrapper<ReportParam>().eq(ReportParam::getReportId, reportId)
        );
    }
    
    public void saveParams(Long reportId, List<ReportParam> params) {
        // 删除旧的参数
        reportParamMapper.delete(
            new LambdaQueryWrapper<ReportParam>().eq(ReportParam::getReportId, reportId)
        );
        // 保存新参数
        for (ReportParam param : params) {
            param.setReportId(reportId);
            reportParamMapper.insert(param);
        }
    }

    public void create(Report report, Long userId) {
        report.setCreateUserId(userId);
        report.setStatus(1);
        reportMapper.insert(report);
        
        // 保存参数配置
        saveParams(report);
    }

    public void update(Report report) {
        reportMapper.updateById(report);
        saveParams(report);
    }

    private void saveParams(Report report) {
        if (report.getParamsConfig() != null) {
            try {
                List<Map<String, Object>> params = objectMapper.readValue(
                    report.getParamsConfig(), 
                    List.class
                );
                // 删除旧参数
                reportParamMapper.delete(
                    new LambdaQueryWrapper<ReportParam>().eq(ReportParam::getReportId, report.getId())
                );
                // 插入新参数
                for (Map<String, Object> p : params) {
                    ReportParam param = new ReportParam();
                    param.setReportId(report.getId());
                    param.setParamName((String) p.get("name"));
                    param.setParamLabel((String) p.get("label"));
                    param.setParamType((String) p.getOrDefault("type", "TEXT"));
                    param.setDefaultValue((String) p.get("defaultValue"));
                    param.setOptionsSql((String) p.get("optionsSql"));
                    param.setRequired((Integer) p.getOrDefault("required", 0));
                    reportParamMapper.insert(param);
                }
            } catch (Exception e) {
                // 忽略参数解析错误
            }
        }
    }

    public void delete(Long id) {
        reportMapper.deleteById(id);
        // 同时删除关联参数
        reportParamMapper.delete(
            new LambdaQueryWrapper<ReportParam>().eq(ReportParam::getReportId, id)
        );
    }

    // 分类管理
    public List<ReportCategory> getCategories() {
        return categoryMapper.selectList(
            new LambdaQueryWrapper<ReportCategory>()
                .eq(ReportCategory::getDeleted, 0)
                .orderByAsc(ReportCategory::getSortOrder)
        );
    }

    public void createCategory(ReportCategory category) {
        categoryMapper.insert(category);
    }

    public void updateCategory(ReportCategory category) {
        categoryMapper.updateById(category);
    }

    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    // 分享功能
    public String createShare(Long reportId, Long userId, LocalDateTime expiresAt) {
        String token = UUID.randomUUID().toString().replace("-", "");
        
        ReportShare share = new ReportShare();
        share.setReportId(reportId);
        share.setShareToken(token);
        share.setExpiresAt(expiresAt);
        share.setCreatedBy(userId);
        shareMapper.insert(share);
        
        return token;
    }

    public ReportShare getByShareToken(String token) {
        List<ReportShare> shares = shareMapper.selectList(
            new LambdaQueryWrapper<ReportShare>()
                .eq(ReportShare::getShareToken, token)
        );
        if (shares.isEmpty()) {
            return null;
        }
        ReportShare share = shares.get(0);
        // 检查是否过期
        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        return share;
    }
}
