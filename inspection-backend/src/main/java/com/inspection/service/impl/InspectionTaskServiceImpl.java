package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.common.enums.TaskStatus;
import com.inspection.common.exception.BusinessException;
import com.inspection.common.exception.EntityNotFoundException;
import com.inspection.common.exception.RegulationViolationException;
import com.inspection.entity.InspectionTask;
import com.inspection.mapper.InspectionTaskMapper;
import com.inspection.service.InspectionTaskService;
import com.inspection.service.RadioStationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class InspectionTaskServiceImpl extends ServiceImpl<InspectionTaskMapper, InspectionTask> implements InspectionTaskService {
	private static final Set<String> INSPECTION_MODES = Set.of("日常检查", "重点检查");
	private static final Set<String> FREQUENCY_CATEGORIES = Set.of("地面频率使用", "卫星频率使用", "卫星通信网频率");
	private static final Set<String> STATION_CATEGORIES = Set.of("地面台站", "空间电台", "卫星地球站");

	private final RadioStationService stationService;

	public InspectionTaskServiceImpl(RadioStationService stationService) {
		this.stationService = stationService;
	}

	@Override
	public InspectionTask createTask(InspectionTask task) throws BusinessException {
		validateTask(task, null);
		save(task);
		return task;
	}

	@Override
	public InspectionTask updateTask(Long id, InspectionTask task) throws BusinessException {
		if (getById(id) == null) {
			throw new EntityNotFoundException("检查任务不存在，无法更新");
		}
		task.setId(id);
		validateTask(task, id);
		updateById(task);
		return task;
	}

	private void validateTask(InspectionTask task, Long id) throws BusinessException {
		if (!INSPECTION_MODES.contains(task.getInspectionMode())) {
			throw new RegulationViolationException("检查模式必须是日常检查或重点检查");
		}
		if (task.getStationId() == null || stationService.getByStationId(task.getStationId()) == null) {
			throw new EntityNotFoundException("任务绑定的台站不存在");
		}
		// 验证多站ID列表（如果提供）
		if (task.getStationIds() != null && !task.getStationIds().isBlank()) {
			try {
				List<Long> stationIdList = com.alibaba.fastjson2.JSON.parseArray(task.getStationIds(), Long.class);
				for (Long sid : stationIdList) {
					if (stationService.getByStationId(sid) == null) {
						throw new EntityNotFoundException("任务绑定的台站ID " + sid + " 不存在");
					}
				}
			} catch (Exception e) {
				if (e instanceof EntityNotFoundException) throw e;
				throw new RegulationViolationException("stationIds 格式必须为合法的JSON数组");
			}
		}
		if ("频率检查任务".equals(task.getTaskType()) && task.getCheckCategory() != null
				&& !FREQUENCY_CATEGORIES.contains(task.getCheckCategory())) {
			throw new RegulationViolationException("频率检查任务的检查细分类只能是地面频率使用、卫星频率使用或卫星通信网频率");
		}
		if ("台站检查任务".equals(task.getTaskType()) && task.getCheckCategory() != null
				&& !STATION_CATEGORIES.contains(task.getCheckCategory())) {
			throw new RegulationViolationException("台站检查任务的检查细分类只能是地面台站、空间电台或卫星地球站");
		}

		if (task.getStatus() == null || task.getStatus().isBlank()) {
			task.setStatus(TaskStatus.PENDING_REVIEW.getLabel());
		}

		if (task.getAssignee() == null || task.getAssignee().isBlank()) {
			throw new RegulationViolationException("新建任务时必须从检查员列表中选择责任人员");
		}

		LocalDate dueDate = task.getDueDate() == null ? LocalDate.now() : task.getDueDate();
		int year = dueDate.getYear();
		if ("日常检查".equals(task.getInspectionMode())) {
			long duplicated = lambdaQuery()
					.eq(InspectionTask::getStationId, task.getStationId())
					.eq(InspectionTask::getInspectionMode, "日常检查")
					.ge(InspectionTask::getDueDate, LocalDate.of(year, 1, 1))
					.le(InspectionTask::getDueDate, LocalDate.of(year, 12, 31))
					.ne(id != null, InspectionTask::getId, id)
					.count();
			if (duplicated > 0) {
				throw new RegulationViolationException("同一台站同一年内已存在日常检查任务（第二十三条）");
			}
		}

		if (task.getTaskType() == null || task.getTaskType().isBlank()) {
			task.setTaskType(task.getInspectionMode());
		}
		if (task.getCheckCategory() == null || task.getCheckCategory().isBlank()) {
			task.setCheckCategory("频率检查任务".equals(task.getTaskType()) ? "地面频率使用" : "地面台站");
		}
	}
}
