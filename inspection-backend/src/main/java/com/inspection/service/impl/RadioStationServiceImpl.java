package com.inspection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inspection.common.exception.BusinessException;
import com.inspection.common.exception.EntityNotFoundException;
import com.inspection.common.exception.RegulationViolationException;
import com.inspection.dto.StationDetailDTO;
import com.inspection.entity.FrequencyUsage;
import com.inspection.entity.RadioStation;
import com.inspection.mapper.RadioStationMapper;
import com.inspection.service.FrequencyUsageService;
import com.inspection.service.RadioStationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RadioStationServiceImpl extends ServiceImpl<RadioStationMapper, RadioStation> implements RadioStationService {
	private static final Map<String, Set<String>> STATION_CLASS_RULES = Map.of(
			"一类", Set.of("广播电台", "微波站", "雷达站", "静止轨道空间无线电台", "卫星测控（导航）站", "卫星关口站", "卫星国际专线地球站"),
			"二类", Set.of("公众移动通信基站（含室外直放站，不含室内站）"),
			"三类", Set.of("其他地面无线电业务台（站）", "非静止轨道空间无线电台", "其他卫星地球站")
	);

	private final FrequencyUsageService frequencyUsageService;

	public RadioStationServiceImpl(FrequencyUsageService frequencyUsageService) {
		this.frequencyUsageService = frequencyUsageService;
	}

	@Override
	public RadioStation createStation(RadioStation station) throws BusinessException {
		validateStation(station, null);
		// 自动生成 station_id（从 1001 开始递增），仅在前端未传入时生成
		if (station.getStationId() == null) {
			RadioStation last = lambdaQuery()
					.orderByDesc(RadioStation::getStationId)
					.last("LIMIT 1")
					.one();
			station.setStationId(last == null ? 1001L : last.getStationId() + 1);
		}
		save(station);
		return station;
	}

	@Override
	public RadioStation updateStation(Long id, RadioStation station) throws BusinessException {
		if (getById(id) == null) {
			throw new EntityNotFoundException("台站不存在，无法更新");
		}
		station.setId(id);
		validateStation(station, id);
		updateById(station);
		return station;
	}

	@Override
	public StationDetailDTO getStationDetail(Long id) throws BusinessException {
		RadioStation station = getById(id);
		if (station == null) {
			station = getByStationId(id);
		}
		if (station == null) {
			throw new EntityNotFoundException("台站不存在");
		}
		StationDetailDTO dto = new StationDetailDTO();
		dto.setId(station.getId());
		dto.setName(station.getName());
		dto.setCategory(station.getCategory());
		dto.setStationClass(station.getStationClass());
		dto.setStationType(station.getStationType());
		dto.setServiceType(station.getServiceType());
		dto.setStationCode(station.getStationCode());
		dto.setAddress(station.getAddress());
		dto.setLongitude(station.getLongitude());
		dto.setLatitude(station.getLatitude());
		dto.setLicenseNo(station.getLicenseNo());
		dto.setValidUntil(station.getValidUntil());
		dto.setTransmitPower(station.getTransmitPower());
		dto.setOccupiedBandwidth(station.getOccupiedBandwidth());
		dto.setAntennaGain(station.getAntennaGain());
		dto.setAntennaHeight(station.getAntennaHeight());
		dto.setAntennaSize(station.getAntennaSize());
		dto.setPolarization(station.getPolarization());
		dto.setMaxEirp(station.getMaxEirp());
		dto.setOrbitalPosition(station.getOrbitalPosition());
		dto.setTotalBandwidth(station.getTotalBandwidth());
		dto.setReviewStatus(station.getReviewStatus());
		dto.setReviewedBy(station.getReviewedBy());
		dto.setReviewedAt(station.getReviewedAt());
		dto.setCreatedAt(station.getCreatedAt());
		dto.setUpdatedAt(station.getUpdatedAt());

		List<FrequencyUsage> frequencies = frequencyUsageService.lambdaQuery()
				.eq(FrequencyUsage::getStationId, station.getStationId())
				.orderByDesc(FrequencyUsage::getId)
				.list();
		dto.setFrequencies(frequencies);
		return dto;
	}

	@Override
	public RadioStation getByStationId(Long stationId) {
		return lambdaQuery().eq(RadioStation::getStationId, stationId).one();
	}

	private void validateStation(RadioStation station, Long id) throws BusinessException {
		Set<String> allowedTypes = STATION_CLASS_RULES.get(station.getStationClass());
		if (allowedTypes == null) {
			throw new RegulationViolationException("附件分类仅支持一类、二类、三类");
		}
		if (!allowedTypes.contains(station.getStationType())) {
			throw new RegulationViolationException("台站类型与附件分类不匹配，请按附件3填写");
		}
		if (station.getValidUntil() != null && station.getValidUntil().isBefore(LocalDate.now())) {
			throw new RegulationViolationException("台站执照已过期，不能作为在用台站录入");
		}

		long duplicatedCode = lambdaQuery()
				.eq(RadioStation::getStationCode, station.getStationCode())
				.ne(id != null, RadioStation::getId, id)
				.count();
		if (duplicatedCode > 0) {
			throw new RegulationViolationException("台站识别码重复，请核对后再提交");
		}

		if (station.getCategory() == null || station.getCategory().isBlank()) {
			station.setCategory(station.getStationClass());
		}
		if (station.getReviewStatus() == null || station.getReviewStatus().isBlank()) {
			station.setReviewStatus("待审核");
		}
	}
}
