package io.mosip.kernel.masterdata.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.util.EmptyCheckUtils;
import io.mosip.kernel.masterdata.constant.ApplicationErrorCode;
import io.mosip.kernel.masterdata.constant.HolidayErrorCode;
import io.mosip.kernel.masterdata.constant.MasterDataConstant;
import io.mosip.kernel.masterdata.constant.RegistrationCenterDeviceHistoryErrorCode;
import io.mosip.kernel.masterdata.constant.RegistrationCenterErrorCode;
import io.mosip.kernel.masterdata.dto.HolidayDto;
import io.mosip.kernel.masterdata.dto.PageDto;
import io.mosip.kernel.masterdata.dto.RegistarionCenterReqDto;
import io.mosip.kernel.masterdata.dto.RegistrationCenterDto;
import io.mosip.kernel.masterdata.dto.RegistrationCenterHolidayDto;
import io.mosip.kernel.masterdata.dto.getresponse.RegistrationCenterResponseDto;
import io.mosip.kernel.masterdata.dto.getresponse.ResgistrationCenterStatusResponseDto;
import io.mosip.kernel.masterdata.dto.getresponse.extn.RegistrationCenterExtnDto;
import io.mosip.kernel.masterdata.dto.postresponse.IdResponseDto;
import io.mosip.kernel.masterdata.dto.postresponse.RegistrationCenterPostResponseDto;
import io.mosip.kernel.masterdata.entity.Holiday;
import io.mosip.kernel.masterdata.entity.Location;
import io.mosip.kernel.masterdata.entity.RegistrationCenter;
import io.mosip.kernel.masterdata.entity.RegistrationCenterDevice;
import io.mosip.kernel.masterdata.entity.RegistrationCenterHistory;
import io.mosip.kernel.masterdata.entity.RegistrationCenterMachine;
import io.mosip.kernel.masterdata.entity.RegistrationCenterMachineDevice;
import io.mosip.kernel.masterdata.entity.RegistrationCenterUserMachine;
import io.mosip.kernel.masterdata.entity.id.IdAndLanguageCodeID;
import io.mosip.kernel.masterdata.exception.DataNotFoundException;
import io.mosip.kernel.masterdata.exception.MasterDataServiceException;
import io.mosip.kernel.masterdata.exception.RequestException;
import io.mosip.kernel.masterdata.repository.HolidayRepository;
import io.mosip.kernel.masterdata.repository.RegistrationCenterDeviceRepository;
import io.mosip.kernel.masterdata.repository.RegistrationCenterHistoryRepository;
import io.mosip.kernel.masterdata.repository.RegistrationCenterMachineDeviceRepository;
import io.mosip.kernel.masterdata.repository.RegistrationCenterMachineRepository;
import io.mosip.kernel.masterdata.repository.RegistrationCenterMachineUserRepository;
import io.mosip.kernel.masterdata.repository.RegistrationCenterRepository;
import io.mosip.kernel.masterdata.service.LocationService;
import io.mosip.kernel.masterdata.service.RegistrationCenterHistoryService;
import io.mosip.kernel.masterdata.service.RegistrationCenterService;
import io.mosip.kernel.masterdata.utils.ExceptionUtils;
import io.mosip.kernel.masterdata.utils.MapperUtils;
import io.mosip.kernel.masterdata.utils.MetaDataUtils;

/**
 * This service class contains methods that provides registration centers
 * details based on user provided data.
 * 
 * @author Dharmesh Khandelwal
 * @author Abhishek Kumar
 * @author Urvil Joshi
 * @author Ritesh Sinha
 * @author Sagar Mahapatra
 * @author Sidhant Agarwal
 * @author Uday Kumar
 * @author Megha Tanga
 * @since 1.0.0
 *
 */

@Service
public class RegistrationCenterServiceImpl implements RegistrationCenterService {

	/**
	 * Reference to RegistrationCenterRepository.
	 */
	@Autowired
	private RegistrationCenterRepository registrationCenterRepository;

	@Autowired
	private RegistrationCenterHistoryRepository registrationCenterHistoryRepository;

	@Autowired
	RegistrationCenterMachineRepository registrationCenterMachineRepository;

	@Autowired
	RegistrationCenterMachineUserRepository registrationCenterMachineUserRepository;

	@Autowired
	RegistrationCenterMachineDeviceRepository registrationCenterMachineDeviceRepository;

	@Autowired
	RegistrationCenterHistoryService registrationCenterHistoryService;

	@Autowired
	RegistrationCenterDeviceRepository registrationCenterDeviceRepository;

	/**
	 * Reference to HolidayRepository.
	 */
	@Autowired
	private HolidayRepository holidayRepository;

	@Autowired
	private LocationService locationService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * getRegistrationCenterHolidays(java.lang.String, int, java.lang.String)
	 */
	@Override
	public RegistrationCenterHolidayDto getRegistrationCenterHolidays(String registrationCenterId, int year,
			String langCode) {
		List<RegistrationCenterDto> registrationCenters;
		List<RegistrationCenter> registrationCenterEntity = new ArrayList<>();
		RegistrationCenterHolidayDto registrationCenterHolidayResponse = null;
		RegistrationCenterDto registrationCenterDto = null;
		RegistrationCenter registrationCenter = null;
		List<HolidayDto> holidayDto = null;
		List<Holiday> holidays = null;
		String holidayLocationCode = "";

		Objects.requireNonNull(registrationCenterId);
		Objects.requireNonNull(year);
		Objects.requireNonNull(langCode);
		try {
			registrationCenter = registrationCenterRepository.findByIdAndLangCode(registrationCenterId, langCode);
		} catch (DataAccessException | DataAccessLayerException dataAccessException) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(dataAccessException));
		}
		if (registrationCenter == null) {
			throw new DataNotFoundException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
		} else {
			registrationCenterEntity.add(registrationCenter);
			registrationCenters = MapperUtils.mapAll(registrationCenterEntity, RegistrationCenterDto.class);
			registrationCenterDto = registrationCenters.get(0);
			try {
				holidayLocationCode = registrationCenterDto.getHolidayLocationCode();
				holidays = holidayRepository.findAllByLocationCodeYearAndLangCode(holidayLocationCode, langCode, year);
				if (holidayLocationCode != null)
					holidays = holidayRepository.findAllByLocationCodeYearAndLangCode(holidayLocationCode, langCode,
							year);
			} catch (DataAccessException | DataAccessLayerException dataAccessException) {
				throw new MasterDataServiceException(HolidayErrorCode.HOLIDAY_FETCH_EXCEPTION.getErrorCode(),
						HolidayErrorCode.HOLIDAY_FETCH_EXCEPTION.getErrorMessage());

			}
			if (holidays != null)
				holidayDto = MapperUtils.mapHolidays(holidays);
		}
		registrationCenterHolidayResponse = new RegistrationCenterHolidayDto();
		registrationCenterHolidayResponse.setRegistrationCenter(registrationCenterDto);
		registrationCenterHolidayResponse.setHolidays(holidayDto);

		return registrationCenterHolidayResponse;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * getRegistrationCentersByCoordinates(double, double, int, java.lang.String)
	 */
	@Override
	public RegistrationCenterResponseDto getRegistrationCentersByCoordinates(double longitude, double latitude,
			int proximityDistance, String langCode) {
		List<RegistrationCenter> centers = null;
		try {
			centers = registrationCenterRepository.findRegistrationCentersByLat(latitude, longitude,
					proximityDistance * MasterDataConstant.METERTOMILECONVERSION, langCode);
		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(e));
		}
		if (centers.isEmpty()) {
			throw new DataNotFoundException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
		}
		List<RegistrationCenterDto> registrationCenters = null;
		registrationCenters = MapperUtils.mapAll(centers, RegistrationCenterDto.class);
		RegistrationCenterResponseDto registrationCenterResponseDto = new RegistrationCenterResponseDto();
		registrationCenterResponseDto.setRegistrationCenters(registrationCenters);
		return registrationCenterResponseDto;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * getRegistrationCentersByLocationCodeAndLanguageCode(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public RegistrationCenterResponseDto getRegistrationCentersByLocationCodeAndLanguageCode(String locationCode,
			String langCode) {
		List<RegistrationCenter> registrationCentersList = null;
		try {
			registrationCentersList = registrationCenterRepository.findByLocationCodeAndLangCode(locationCode,
					langCode);

		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(e));
		}
		if (registrationCentersList.isEmpty()) {
			throw new DataNotFoundException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
		}
		List<RegistrationCenterDto> registrationCentersDtoList = null;
		registrationCentersDtoList = MapperUtils.mapAll(registrationCentersList, RegistrationCenterDto.class);
		RegistrationCenterResponseDto registrationCenterResponseDto = new RegistrationCenterResponseDto();
		registrationCenterResponseDto.setRegistrationCenters(registrationCentersDtoList);
		return registrationCenterResponseDto;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * getRegistrationCentersByIDAndLangCode(java.lang.String, java.lang.String)
	 */
	@Override
	public RegistrationCenterResponseDto getRegistrationCentersByIDAndLangCode(String registrationCenterId,
			String langCode) {
		List<RegistrationCenterDto> registrationCenters = new ArrayList<>();

		RegistrationCenter registrationCenter = null;
		try {
			registrationCenter = registrationCenterRepository.findByIdAndLangCode(registrationCenterId, langCode);
		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(e));
		}
		if (registrationCenter == null) {
			throw new DataNotFoundException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
		}

		RegistrationCenterDto registrationCenterDto = MapperUtils.map(registrationCenter, RegistrationCenterDto.class);
		registrationCenters.add(registrationCenterDto);
		RegistrationCenterResponseDto response = new RegistrationCenterResponseDto();
		response.setRegistrationCenters(registrationCenters);
		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * getAllRegistrationCenters()
	 */
	@Override
	public RegistrationCenterResponseDto getAllRegistrationCenters() {
		List<RegistrationCenter> registrationCentersList = null;
		try {
			registrationCentersList = registrationCenterRepository.findAllByIsDeletedFalseOrIsDeletedIsNull();

		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorMessage());
		}

		if (registrationCentersList.isEmpty()) {
			throw new DataNotFoundException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
		}

		List<RegistrationCenterDto> registrationCenters = null;
		registrationCenters = MapperUtils.mapAll(registrationCentersList, RegistrationCenterDto.class);
		RegistrationCenterResponseDto registrationCenterResponseDto = new RegistrationCenterResponseDto();
		registrationCenterResponseDto.setRegistrationCenters(registrationCenters);
		return registrationCenterResponseDto;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * findRegistrationCenterByHierarchyLevelandTextAndLanguageCode(java.lang.
	 * String, java.lang.String, java.lang.String)
	 */
	@Override
	public RegistrationCenterResponseDto findRegistrationCenterByHierarchyLevelandTextAndLanguageCode(
			String languageCode, Short hierarchyLevel, String text) {
		List<RegistrationCenter> registrationCentersList = null;
		try {
			Set<String> codes = getLocationCode(
					locationService.getLocationByLangCodeAndHierarchyLevel(languageCode, hierarchyLevel),
					hierarchyLevel, text);
			if (!EmptyCheckUtils.isNullEmpty(codes)) {
				registrationCentersList = registrationCenterRepository.findRegistrationCenterByListOfLocationCode(codes,
						languageCode);
			} else {
				throw new DataNotFoundException(
						RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
						RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
			}

		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(e));
		}
		if (registrationCentersList.isEmpty()) {
			throw new DataNotFoundException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
		}
		List<RegistrationCenterDto> registrationCentersDtoList = null;
		registrationCentersDtoList = MapperUtils.mapAll(registrationCentersList, RegistrationCenterDto.class);

		RegistrationCenterResponseDto registrationCenterResponseDto = new RegistrationCenterResponseDto();
		registrationCenterResponseDto.setRegistrationCenters(registrationCentersDtoList);
		return registrationCenterResponseDto;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * createRegistrationCenter(io.mosip.kernel.masterdata.dto.RequestDto)
	 */
	@Override
	public IdResponseDto createRegistrationCenter(RegistrationCenterDto registrationCenterDto) {
		try {
			if (!EmptyCheckUtils.isNullEmpty(registrationCenterDto.getLatitude())
					&& !EmptyCheckUtils.isNullEmpty(registrationCenterDto.getLongitude())) {
				Float.parseFloat(registrationCenterDto.getLatitude());
				Float.parseFloat(registrationCenterDto.getLongitude());
			}
		} catch (NullPointerException | NumberFormatException latLongParseException) {
			throw new RequestException(ApplicationErrorCode.APPLICATION_REQUEST_EXCEPTION.getErrorCode(),
					ApplicationErrorCode.APPLICATION_REQUEST_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(latLongParseException));
		}
		RegistrationCenter registrationCenterEntity = new RegistrationCenter();
		registrationCenterEntity = MetaDataUtils.setCreateMetaData(registrationCenterDto,
				registrationCenterEntity.getClass());
		RegistrationCenterHistory registrationCenterHistoryEntity = MetaDataUtils
				.setCreateMetaData(registrationCenterDto, RegistrationCenterHistory.class);
		registrationCenterHistoryEntity.setEffectivetimes(registrationCenterEntity.getCreatedDateTime());
		registrationCenterHistoryEntity.setCreatedDateTime(registrationCenterEntity.getCreatedDateTime());
		RegistrationCenter registrationCenter;
		try {
			registrationCenter = registrationCenterRepository.create(registrationCenterEntity);
			registrationCenterHistoryRepository.create(registrationCenterHistoryEntity);
		} catch (DataAccessLayerException | DataAccessException exception) {
			throw new MasterDataServiceException(ApplicationErrorCode.APPLICATION_INSERT_EXCEPTION.getErrorCode(),
					ApplicationErrorCode.APPLICATION_INSERT_EXCEPTION.getErrorMessage() + " "
							+ ExceptionUtils.parseException(exception));
		}
		IdResponseDto idResponseDto = new IdResponseDto();
		idResponseDto.setId(registrationCenter.getId());
		return idResponseDto;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * validateTimestampWithRegistrationCenter(java.lang.String, java.lang.String)
	 */
	@Override
	public ResgistrationCenterStatusResponseDto validateTimeStampWithRegistrationCenter(String id, String langCode,
			String timestamp) {
		LocalDateTime localDateTime = null;
		try {
			localDateTime = MapperUtils.parseToLocalDateTime(timestamp);
		} catch (DateTimeParseException ex) {
			throw new RequestException(
					RegistrationCenterDeviceHistoryErrorCode.INVALIDE_EFFECTIVE_DATE_TIME_FORMATE_EXCEPTION
							.getErrorCode(),
					RegistrationCenterDeviceHistoryErrorCode.INVALIDE_EFFECTIVE_DATE_TIME_FORMATE_EXCEPTION
							.getErrorMessage() + ExceptionUtils.parseException(ex));
		}
		LocalDate localDate = localDateTime.toLocalDate();
		ResgistrationCenterStatusResponseDto resgistrationCenterStatusResponseDto = new ResgistrationCenterStatusResponseDto();
		try {
			/**
			 * a query is written in RegistrationCenterRepository which would check if the
			 * date is not a holiday for that center
			 *
			 */
			RegistrationCenter registrationCenter = registrationCenterRepository.findByIdAndLangCode(id, langCode);
			if (registrationCenter == null) {
				throw new DataNotFoundException(
						RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
						RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
			}
			boolean isTrue = registrationCenterRepository.validateDateWithHoliday(localDate,
					registrationCenter.getHolidayLocationCode());
			if (isTrue) {
				resgistrationCenterStatusResponseDto.setStatus(MasterDataConstant.INVALID);
			} else {

				resgistrationCenterStatusResponseDto.setStatus(MasterDataConstant.VALID);
			}

		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(e));
		}

		return resgistrationCenterStatusResponseDto;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * updateRegistrationCenter(io.mosip.kernel.masterdata.dto.RequestDto)
	 */
	@Transactional
	@Override
	public IdAndLanguageCodeID updateRegistrationCenter(RequestWrapper<RegistrationCenterDto> registrationCenter) {
		RegistrationCenter updRegistrationCenter = null;
		try {

			RegistrationCenter renRegistrationCenter = registrationCenterRepository.findByIdAndLangCodeAndIsDeletedTrue(
					registrationCenter.getRequest().getId(), registrationCenter.getRequest().getLangCode());
			if (renRegistrationCenter != null) {
				MetaDataUtils.setUpdateMetaData(registrationCenter.getRequest(), renRegistrationCenter, false);
				updRegistrationCenter = registrationCenterRepository.update(renRegistrationCenter);

				RegistrationCenterHistory registrationCenterHistory = new RegistrationCenterHistory();
				MapperUtils.map(updRegistrationCenter, registrationCenterHistory);
				MapperUtils.setBaseFieldValue(updRegistrationCenter, registrationCenterHistory);
				registrationCenterHistory.setEffectivetimes(updRegistrationCenter.getUpdatedDateTime());
				registrationCenterHistory.setUpdatedDateTime(updRegistrationCenter.getUpdatedDateTime());
				registrationCenterHistoryService.createRegistrationCenterHistory(registrationCenterHistory);

			} else {
				throw new RequestException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
						RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
			}
		} catch (DataAccessLayerException | DataAccessException exception) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_UPDATE_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_UPDATE_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(exception));
		}

		IdAndLanguageCodeID idAndLanguageCodeID = new IdAndLanguageCodeID();
		MapperUtils.map(updRegistrationCenter, idAndLanguageCodeID);
		return idAndLanguageCodeID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * deleteRegistrationCenter(java.lang.String)
	 */
	@Override
	@Transactional
	public IdResponseDto deleteRegistrationCenter(String id) {
		RegistrationCenter delRegistrationCenter = null;
		try {
			List<RegistrationCenter> renRegistrationCenterList = registrationCenterRepository
					.findByRegIdAndIsDeletedFalseOrNull(id);
			if (!renRegistrationCenterList.isEmpty()) {
				for (RegistrationCenter renRegistrationCenter : renRegistrationCenterList) {

					List<RegistrationCenterMachine> registrationCenterMachineList = registrationCenterMachineRepository
							.findByMachineIdAndIsDeletedFalseOrIsDeletedIsNull(renRegistrationCenter.getId());
					List<RegistrationCenterUserMachine> registrationCenterMachineUser = registrationCenterMachineUserRepository
							.findByMachineIdAndIsDeletedFalseOrIsDeletedIsNull(renRegistrationCenter.getId());
					List<RegistrationCenterMachineDevice> registrationCenterMachineDevice = registrationCenterMachineDeviceRepository
							.findByMachineIdAndIsDeletedFalseOrIsDeletedIsNull(renRegistrationCenter.getId());
					List<RegistrationCenterDevice> registrationCenterDeviceList = registrationCenterDeviceRepository
							.findByDeviceIdAndIsDeletedFalseOrIsDeletedIsNull(renRegistrationCenter.getId());

					if (registrationCenterMachineList.isEmpty() && registrationCenterMachineUser.isEmpty()
							&& registrationCenterMachineDevice.isEmpty() && registrationCenterDeviceList.isEmpty()) {
						MetaDataUtils.setDeleteMetaData(renRegistrationCenter);
						delRegistrationCenter = registrationCenterRepository.update(renRegistrationCenter);

						RegistrationCenterHistory registrationCenterHistory = new RegistrationCenterHistory();
						MapperUtils.map(delRegistrationCenter, registrationCenterHistory);
						MapperUtils.setBaseFieldValue(delRegistrationCenter, registrationCenterHistory);

						registrationCenterHistory.setEffectivetimes(delRegistrationCenter.getDeletedDateTime());
						registrationCenterHistory.setDeletedDateTime(delRegistrationCenter.getDeletedDateTime());
						registrationCenterHistoryService.createRegistrationCenterHistory(registrationCenterHistory);
					} else {
						throw new RequestException(RegistrationCenterErrorCode.DEPENDENCY_EXCEPTION.getErrorCode(),
								RegistrationCenterErrorCode.DEPENDENCY_EXCEPTION.getErrorMessage());
					}
				}
			} else {
				throw new RequestException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
						RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
			}

		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_DELETE_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_DELETE_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(e));
		}

		IdResponseDto idResponseDto = new IdResponseDto();
		idResponseDto.setId(id);
		return idResponseDto;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * findRegistrationCenterByHierarchyLevelAndListTextAndlangCode(java.lang.
	 * String, java.lang.Integer, java.util.List)
	 */
	@Override
	public RegistrationCenterResponseDto findRegistrationCenterByHierarchyLevelAndListTextAndlangCode(
			String languageCode, Short hierarchyLevel, List<String> names) {
		List<RegistrationCenterDto> registrationCentersDtoList = null;
		List<RegistrationCenter> registrationCentersList = null;
		Set<String> uniqueLocCode = new TreeSet<>();
		try {
			Map<Short, List<Location>> parLocCodeToListOfLocation = locationService
					.getLocationByLangCodeAndHierarchyLevel(languageCode, hierarchyLevel);
			Set<String> codes = getListOfLocationCode(parLocCodeToListOfLocation, hierarchyLevel, names);
			uniqueLocCode.addAll(codes);
			if (!EmptyCheckUtils.isNullEmpty(uniqueLocCode)) {
				registrationCentersList = registrationCenterRepository
						.findRegistrationCenterByListOfLocationCode(uniqueLocCode, languageCode);
			} else {
				throw new DataNotFoundException(
						RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
						RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
			}

		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(e));
		}
		if (registrationCentersList.isEmpty()) {
			throw new DataNotFoundException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
		}
		registrationCentersDtoList = MapperUtils.mapAll(registrationCentersList, RegistrationCenterDto.class);

		RegistrationCenterResponseDto registrationCenterResponseDto = new RegistrationCenterResponseDto();
		registrationCenterResponseDto.setRegistrationCenters(registrationCentersDtoList);
		return registrationCenterResponseDto;
	}

	private Set<String> getLocationCode(Map<Short, List<Location>> levelToListOfLocationMap, Short hierarchyLevel,
			String text) {
		validateLocationName(levelToListOfLocationMap, hierarchyLevel, text);
		Set<String> uniqueLocCode = new TreeSet<>();
		boolean isParent = false;
		for (Entry<Short, List<Location>> data : levelToListOfLocationMap.entrySet()) {
			if (!isParent) {
				for (Location location : data.getValue()) {
					if (text.trim().equalsIgnoreCase(location.getName().trim())) {
						uniqueLocCode.add(location.getCode());
						isParent = true;
						break;// parent code set
					}
				}
			} else if (data.getKey() > hierarchyLevel) {
				for (Location location : data.getValue()) {
					if (uniqueLocCode.contains(location.getParentLocCode())) {
						uniqueLocCode.add(location.getCode());
					}
				}
			}
		}
		return uniqueLocCode;
	}

	private Set<String> getListOfLocationCode(Map<Short, List<Location>> levelToListOfLocationMap, Short hierarchyLevel,
			List<String> texts) {

		List<String> validLocationName = validateListOfLocationName(levelToListOfLocationMap, hierarchyLevel, texts);
		Set<String> uniqueLocCode = new TreeSet<>();
		if (!validLocationName.isEmpty()) {
			for (String text : validLocationName) {
				boolean isParent = false;
				for (Entry<Short, List<Location>> data : levelToListOfLocationMap.entrySet()) {
					if (!isParent) {
						for (Location location : data.getValue()) {
							if (text.trim().equalsIgnoreCase(location.getName().trim())) {
								uniqueLocCode.add(location.getCode());
								isParent = true;
								break;// parent code set
							}
						}
					} else if (data.getKey() > hierarchyLevel) {
						for (Location location : data.getValue()) {
							if (uniqueLocCode.contains(location.getParentLocCode())) {
								uniqueLocCode.add(location.getCode());
							}
						}
					}
				}
			}
		} else {
			throw new DataNotFoundException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
		}
		return uniqueLocCode;
	}

	private void validateLocationName(Map<Short, List<Location>> levelToListOfLocationMap, Short hierarchyLevel,
			String text) {
		List<Location> rootLocation = levelToListOfLocationMap.get(hierarchyLevel);
		boolean isRootLocation = false;
		for (Location location : rootLocation) {
			if (location.getName().trim().equalsIgnoreCase(text)) {
				isRootLocation = true;
			}
		}
		if (!isRootLocation) {
			throw new DataNotFoundException(RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
		}
	}

	private List<String> validateListOfLocationName(Map<Short, List<Location>> levelToListOfLocationMap,
			Short hierarchyLevel, List<String> texts) {
		List<String> locationNames = new ArrayList<>();
		List<Location> rootLocation = levelToListOfLocationMap.get(hierarchyLevel);
		for (String text : texts) {
			for (Location location : rootLocation) {
				if (location.getName().trim().equalsIgnoreCase(text)) {
					locationNames.add(text);
				}
			}
		}
		return locationNames;
	}

	@Override
	public PageDto<RegistrationCenterExtnDto> getAllExistingRegistrationCenters(int pageNumber, int pageSize,
			String sortBy, String orderBy) {
		List<RegistrationCenterExtnDto> registrationCenters = null;
		PageDto<RegistrationCenterExtnDto> registrationCenterPages = null;
		try {
			Page<RegistrationCenter> pageData = registrationCenterRepository
					.findAll(PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(orderBy), sortBy)));
			if (pageData != null && pageData.getContent() != null && !pageData.getContent().isEmpty()) {
				registrationCenters = MapperUtils.mapAll(pageData.getContent(), RegistrationCenterExtnDto.class);
				registrationCenterPages = new PageDto<RegistrationCenterExtnDto>(pageData.getNumber(), 0, null,
						pageData.getTotalPages(), (int) pageData.getTotalElements(), registrationCenters);
			} else {
				throw new DataNotFoundException(
						RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorCode(),
						RegistrationCenterErrorCode.REGISTRATION_CENTER_NOT_FOUND.getErrorMessage());
			}
		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorMessage());
		}
		return registrationCenterPages;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.masterdata.service.RegistrationCenterService#
	 * createRegistrationCenterAdmin(io.mosip.kernel.masterdata.dto.
	 * RegistrationCenterDto)
	 */
	@Override
	@Transactional
	public RegistrationCenterPostResponseDto createRegistrationCenterAdmin(
			RegistarionCenterReqDto<RegistrationCenterDto> registarionCenterReqDto) {
		RegistrationCenter registrationCenterEntity = new RegistrationCenter();
		RegistrationCenterHistory registrationCenterHistoryEntity = new RegistrationCenterHistory();
		List<RegistrationCenter> registrationCenterList = new ArrayList<>();
		List<RegistrationCenterExtnDto> registrationCenterDtoList = new ArrayList<>();
			
		try {
			for (RegistrationCenterDto registrationCenterDto : registarionCenterReqDto.getRequest()) {
				registrationCenterEntity = MetaDataUtils.setCreateMetaData(registrationCenterDto,
						registrationCenterEntity.getClass());
				registrationCenterHistoryEntity = MetaDataUtils.setCreateMetaData(registrationCenterDto,
						RegistrationCenterHistory.class);
				registrationCenterHistoryEntity.setEffectivetimes(registrationCenterEntity.getCreatedDateTime());
				registrationCenterHistoryEntity.setCreatedDateTime(registrationCenterEntity.getCreatedDateTime());

				RegistrationCenter registrationCenter;

				registrationCenter = registrationCenterRepository.create(registrationCenterEntity);
				registrationCenterList.add(registrationCenter);
				registrationCenterHistoryRepository.create(registrationCenterHistoryEntity);
			}

		} catch (DataAccessLayerException | DataAccessException exception) {
			throw new MasterDataServiceException(ApplicationErrorCode.APPLICATION_INSERT_EXCEPTION.getErrorCode(),
					ApplicationErrorCode.APPLICATION_INSERT_EXCEPTION.getErrorMessage() + " "
							+ ExceptionUtils.parseException(exception));
		}

		
		RegistrationCenterPostResponseDto registrationCenterPostResponseDto = new RegistrationCenterPostResponseDto();
		registrationCenterDtoList = MapperUtils.mapAll(registrationCenterList, RegistrationCenterExtnDto.class);
		registrationCenterPostResponseDto.setRegistrationCenters(registrationCenterDtoList);
		return registrationCenterPostResponseDto;

	}

	@Transactional
	@Override
	public RegistrationCenterPostResponseDto updateRegistrationCenterAdmin(
			RegistarionCenterReqDto<RegistrationCenterDto> registarionCenterReqDto) {
		RegistrationCenter updRegistrationCenter = null;
		RegistrationCenter updRegistrationCenterEntity = null;
		List<RegistrationCenterExtnDto> registrationCenterDtoList = null;
		List<RegistrationCenterDto> notUpdRegistrationCenterList = new ArrayList<>();
		List<RegistrationCenter> updRegistrationCenterList = new ArrayList<>();

		try {
			for (RegistrationCenterDto registrationCenterDto : registarionCenterReqDto.getRequest()) {

				RegistrationCenter renRegistrationCenter = registrationCenterRepository
						.findByIdAndLangCodeAndIsDeletedTrue(registrationCenterDto.getId(),
								registrationCenterDto.getLangCode());
				if (renRegistrationCenter != null) {
					// updating reg center
					updRegistrationCenterEntity = MetaDataUtils.setUpdateMetaData(registrationCenterDto,
							renRegistrationCenter, false);
					updRegistrationCenter = registrationCenterRepository.update(updRegistrationCenterEntity);
					// creating reg center history
					RegistrationCenterHistory registrationCenterHistory = new RegistrationCenterHistory();
					MapperUtils.map(updRegistrationCenter, registrationCenterHistory);
					MapperUtils.setBaseFieldValue(updRegistrationCenter, registrationCenterHistory);
					registrationCenterHistory.setEffectivetimes(updRegistrationCenter.getUpdatedDateTime());
					registrationCenterHistory.setUpdatedDateTime(updRegistrationCenter.getUpdatedDateTime());
					registrationCenterHistoryRepository.create(registrationCenterHistory);
					// adding into updated list
					updRegistrationCenterList.add(updRegistrationCenter);
				} else {
					// adding into non-updated list
					notUpdRegistrationCenterList.add(registrationCenterDto);
				}
			}

		} catch (DataAccessLayerException | DataAccessException exception) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_UPDATE_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_UPDATE_EXCEPTION.getErrorMessage()
							+ ExceptionUtils.parseException(exception));
		}

		RegistrationCenterPostResponseDto registrationCenterPostResponseDto = new RegistrationCenterPostResponseDto();
		registrationCenterDtoList = MapperUtils.mapAll(updRegistrationCenterList, RegistrationCenterExtnDto.class);
		registrationCenterPostResponseDto.setRegistrationCenters(registrationCenterDtoList);
		registrationCenterPostResponseDto.setNotUpdatedRegCenters(notUpdRegistrationCenterList);
		return registrationCenterPostResponseDto;
	}

}