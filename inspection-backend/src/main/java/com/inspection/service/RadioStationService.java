package com.inspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.inspection.common.exception.BusinessException;
import com.inspection.dto.StationDetailDTO;
import com.inspection.entity.RadioStation;

public interface RadioStationService extends IService<RadioStation> {
	RadioStation createStation(RadioStation station) throws BusinessException;

	RadioStation updateStation(Long id, RadioStation station) throws BusinessException;

	StationDetailDTO getStationDetail(Long id) throws BusinessException;

	RadioStation getByStationId(Long stationId);
}
