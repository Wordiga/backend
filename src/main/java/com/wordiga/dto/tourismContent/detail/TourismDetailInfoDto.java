package com.wordiga.dto.tourismContent.detail;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TourismDetailInfoDto {

    private String contentId;
    private String contentTypeId;
    private Integer serialNumber;
    private String infoName;
    private String infoText;
    private String fieldType;
    private String subContentId;
    private String subDetailAlt;
    private String subDetailImage;
    private String subDetailOverview;
    private String subName;
    private Integer subNumber;
    private String roomCode;
    private String roomTitle;
    private String roomSizePyeong;
    private String roomCount;
    private String roomBaseCount;
    private String roomMaxCount;
    private String roomOffSeasonWeekdayMinFee;
    private String roomOffSeasonWeekendMinFee;
    private String roomPeakSeasonWeekdayMinFee;
    private String roomPeakSeasonWeekendMinFee;
    private String roomIntro;
    private String roomBathFacility;
    private String roomBath;
    private String roomHomeTheater;
    private String roomAirCondition;
    private String roomTv;
    private String roomPc;
    private String roomCable;
    private String roomInternet;
    private String roomRefrigerator;
    private String roomToiletries;
    private String roomSofa;
    private String roomCook;
    private String roomTable;
    private String roomHairDryer;
    private String roomSizeSquareMeters;
    private List<RoomImageDto> roomImages;
}
