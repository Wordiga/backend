package com.wordiga.tourism.service;

import com.wordiga.global.client.dto.*;
import com.wordiga.global.util.KtoUtils;
import com.wordiga.tourism.domain.TourismContentType;
import com.wordiga.tourism.dto.detail.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class TourismDetailMapper {

    public TourismCommonDetailDto toCommon(ContentDetailDto source) {
        return TourismCommonDetailDto.builder()
                .contentId(source.getContentid())
                .contentTypeId(source.getContenttypeid())
                .title(source.getTitle())
                .createdTime(source.getCreatedtime())
                .modifiedTime(source.getModifiedtime())
                .tel(source.getTel())
                .telName(source.getTelname())
                .homepage(source.getHomepage())
                .firstImage(source.getFirstimage())
                .firstImage2(source.getFirstimage2())
                .copyrightTypeCode(source.getCpyrhtDivCd())
                .addr1(source.getAddr1())
                .addr2(source.getAddr2())
                .zipcode(source.getZipcode())
                .mapx(KtoUtils.parseBigDecimal(source.getMapx()))
                .mapy(KtoUtils.parseBigDecimal(source.getMapy()))
                .mapLevel(source.getMlevel())
                .overview(source.getOverview())
                .lDongRegnCd(source.getLDongRegnCd())
                .lDongSignguCd(source.getLDongSignguCd())
                .lclsSystm1(source.getLclsSystm1())
                .lclsSystm2(source.getLclsSystm2())
                .lclsSystm3(source.getLclsSystm3())
                .build();
    }

    public TourismDetailImageDto toImage(DetailImageDto source) {
        return TourismDetailImageDto.builder()
                .imageName(source.getImgname())
                .imageUrl(source.getOriginimgurl())
                .thumbnailUrl(source.getSmallimageurl())
                .copyrightTypeCode(source.getCpyrhtDivCd())
                .serialNumber(KtoUtils.parseInteger(source.getSerialnum()))
                .build();
    }

    public SeasonalImageDto toGallerySeasonalImage(PhotoGalleryItem source) {
        String value = source.getGalPhotographyMonth();
        Integer month = null;
        LocalDate date = null;
        try {
            if (value != null && value.matches("\\d{8}")) {
                date = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
                month = date.getMonthValue();
            } else if (value != null && value.matches("\\d{6}")) {
                month = YearMonth.parse(value, DateTimeFormatter.ofPattern("yyyyMM")).getMonthValue();
            }
        } catch (RuntimeException ignored) {
        }
        return SeasonalImageDto.builder()
                .imageUrl(source.getGalWebImageUrl())
                .thumbnailUrl(source.getGalWebImageUrl())
                .shootingDate(date)
                .season(toSeason(month))
                .matchConfidence(BigDecimal.ONE)
                .build();
    }

    public SeasonalImageDto toFallbackSeasonalImage(DetailImageDto source) {
        return SeasonalImageDto.builder()
                .imageUrl(source.getOriginimgurl())
                .thumbnailUrl(source.getSmallimageurl())
                .shootingDate(null)
                .season(SeasonalImageDto.Season.UNKNOWN)
                .matchConfidence(BigDecimal.ONE)
                .build();
    }

    public TourismIntroDetailDto toIntro(DetailIntroDto source) {
        if (source == null) return null;

        TourismIntroDetailDto.TourismIntroDetailDtoBuilder target = TourismIntroDetailDto.builder()
                .contentId(source.getContentid())
                .contentTypeId(source.getContenttypeid());

        TourismContentType contentType = TourismContentType.fromCode(source.getContenttypeid());
        if (contentType == null) return target.build();

        switch (contentType) {
            case TOURIST_ATTRACTION -> target
                    .accomCount(source.getAccomcount())
                    .checkBabyCarriage(source.getChkbabycarriage())
                    .checkCreditCard(source.getChkcreditcard())
                    .checkPet(source.getChkpet())
                    .experienceAgeRange(source.getExpagerange())
                    .experienceGuide(source.getExpguide())
                    .heritage1(source.getHeritage1())
                    .heritage2(source.getHeritage2())
                    .heritage3(source.getHeritage3())
                    .infoCenter(source.getInfocenter())
                    .openDate(source.getOpendate())
                    .parking(source.getParking())
                    .restDate(source.getRestdate())
                    .useSeason(source.getUseseason())
                    .useTime(source.getUsetime());

            case CULTURAL_FACILITY -> target
                    .accomCount(source.getAccomcountculture())
                    .checkBabyCarriage(source.getChkbabycarriageculture())
                    .checkCreditCard(source.getChkcreditcardculture())
                    .checkPet(source.getChkpetculture())
                    .discountInfo(source.getDiscountinfo())
                    .infoCenter(source.getInfocenterculture())
                    .parking(source.getParkingculture())
                    .parkingFee(source.getParkingfee())
                    .restDate(source.getRestdateculture())
                    .useFee(source.getUsefee())
                    .useTime(source.getUsetimeculture())
                    .scale(source.getScale())
                    .spendTime(source.getSpendtime());

            case FESTIVAL -> target
                    .ageLimit(source.getAgelimit())
                    .bookingPlace(source.getBookingplace())
                    .discountInfo(source.getDiscountinfofestival())
                    .eventEndDate(source.getEventenddate())
                    .eventHomepage(source.getEventhomepage())
                    .eventPlace(source.getEventplace())
                    .eventStartDate(source.getEventstartdate())
                    .festivalGrade(source.getFestivalgrade())
                    .placeInfo(source.getPlaceinfo())
                    .playTime(source.getPlaytime())
                    .program(source.getProgram())
                    .spendTime(source.getSpendtimefestival())
                    .sponsor1(source.getSponsor1())
                    .sponsor1Tel(source.getSponsor1tel())
                    .sponsor2(source.getSponsor2())
                    .sponsor2Tel(source.getSponsor2tel())
                    .subEvent(source.getSubevent())
                    .useFee(source.getUsetimefestival());

            case TRAVEL_COURSE -> target
                    .distance(source.getDistance())
                    .infoCenter(source.getInfocentertourcourse())
                    .schedule(source.getSchedule())
                    .takeTime(source.getTaketime())
                    .theme(source.getTheme());

            case LEPORTS -> target
                    .accomCount(source.getAccomcountleports())
                    .checkBabyCarriage(source.getChkbabycarriageleports())
                    .checkCreditCard(source.getChkcreditcardleports())
                    .checkPet(source.getChkpetleports())
                    .experienceAgeRange(source.getExpagerangeleports())
                    .infoCenter(source.getInfocenterleports())
                    .openPeriod(source.getOpenperiod())
                    .parkingFee(source.getParkingfeeleports())
                    .parking(source.getParkingleports())
                    .reservation(source.getReservation())
                    .restDate(source.getRestdateleports())
                    .scale(source.getScaleleports())
                    .useFee(source.getUsefeeleports())
                    .useTime(source.getUsetimeleports());

            case LODGING -> target
                    .accomCount(source.getAccomcountlodging())
                    .checkInTime(source.getCheckintime())
                    .checkOutTime(source.getCheckouttime())
                    .checkCooking(source.getChkcooking())
                    .foodPlace(source.getFoodplace())
                    .infoCenter(source.getInfocenterlodging())
                    .parking(source.getParkinglodging())
                    .pickup(source.getPickup())
                    .roomCount(source.getRoomcount())
                    .reservation(source.getReservationlodging())
                    .reservationUrl(source.getReservationurl())
                    .roomType(source.getRoomtype())
                    .scale(source.getScalelodging())
                    .subFacility(source.getSubfacility())
                    .barbecue(source.getBarbecue())
                    .beauty(source.getBeauty())
                    .beverage(source.getBeverage())
                    .bicycle(source.getBicycle())
                    .campfire(source.getCampfire())
                    .fitness(source.getFitness())
                    .karaoke(source.getKaraoke())
                    .publicBath(source.getPublicbath())
                    .publicPc(source.getPublicpc())
                    .sauna(source.getSauna())
                    .seminar(source.getSeminar())
                    .sports(source.getSports())
                    .refundRegulation(source.getRefundregulation());

            case SHOPPING -> target
                    .checkBabyCarriage(source.getChkbabycarriageshopping())
                    .checkCreditCard(source.getChkcreditcardshopping())
                    .checkPet(source.getChkpetshopping())
                    .cultureCenter(source.getCulturecenter())
                    .fairDay(source.getFairday())
                    .infoCenter(source.getInfocentershopping())
                    .openDate(source.getOpendateshopping())
                    .openTime(source.getOpentime())
                    .parking(source.getParkingshopping())
                    .restDate(source.getRestdateshopping())
                    .restroom(source.getRestroom())
                    .saleItem(source.getSaleitem())
                    .saleItemCost(source.getSaleitemcost())
                    .scale(source.getScaleshopping())
                    .shopGuide(source.getShopguide());

            case RESTAURANT -> target
                    .checkCreditCard(source.getChkcreditcardfood())
                    .discountInfo(source.getDiscountinfofood())
                    .firstMenu(source.getFirstmenu())
                    .infoCenter(source.getInfocenterfood())
                    .kidsFacility(source.getKidsfacility())
                    .openDate(source.getOpendatefood())
                    .openTime(source.getOpentimefood())
                    .packing(source.getPacking())
                    .parking(source.getParkingfood())
                    .reservation(source.getReservationfood())
                    .restDate(source.getRestdatefood())
                    .scale(source.getScalefood())
                    .seat(source.getSeat())
                    .smoking(source.getSmoking())
                    .treatMenu(source.getTreatmenu())
                    .licenseNumber(source.getLcnsno());

            case OTHER -> {
            }
        }
        return target.build();
    }

    public List<TourismDetailInfoDto> toDetails(List<DetailInfoDto> sources) {
        if (sources == null) return List.of();
        return sources.stream().map(this::toDetail).toList();
    }

    private TourismDetailInfoDto toDetail(DetailInfoDto source) {
        return TourismDetailInfoDto.builder()
                .contentId(source.getContentid())
                .contentTypeId(source.getContenttypeid())
                .serialNumber(KtoUtils.parseInteger(source.getSerialnum()))
                .infoName(source.getInfoname())
                .infoText(source.getInfotext())
                .fieldType(source.getFldgubun())
                .subContentId(source.getSubcontentid())
                .subDetailAlt(source.getSubdetailalt())
                .subDetailImage(source.getSubdetailimg())
                .subDetailOverview(source.getSubdetailoverview())
                .subName(source.getSubname())
                .subNumber(KtoUtils.parseInteger(source.getSubnum()))
                .roomCode(source.getRoomcode())
                .roomTitle(source.getRoomtitle())
                .roomSizePyeong(source.getRoomsize1())
                .roomCount(source.getRoomcount())
                .roomBaseCount(source.getRoombasecount())
                .roomMaxCount(source.getRoommaxcount())
                .roomOffSeasonWeekdayMinFee(source.getRoomoffseasonminfee1())
                .roomOffSeasonWeekendMinFee(source.getRoomoffseasonminfee2())
                .roomPeakSeasonWeekdayMinFee(source.getRoompeakseasonminfee1())
                .roomPeakSeasonWeekendMinFee(source.getRoompeakseasonminfee2())
                .roomIntro(source.getRoomintro())
                .roomBathFacility(source.getRoombathfacility())
                .roomBath(source.getRoombath())
                .roomHomeTheater(source.getRoomhometheater())
                .roomAirCondition(source.getRoomaircondition())
                .roomTv(source.getRoomtv())
                .roomPc(source.getRoompc())
                .roomCable(source.getRoomcable())
                .roomInternet(source.getRoominternet())
                .roomRefrigerator(source.getRoomrefrigerator())
                .roomToiletries(source.getRoomtoiletries())
                .roomSofa(source.getRoomsofa())
                .roomCook(source.getRoomcook())
                .roomTable(source.getRoomtable())
                .roomHairDryer(source.getRoomhairdryer())
                .roomSizeSquareMeters(source.getRoomsize2())
                .roomImages(roomImages(source))
                .build();
    }

    private List<RoomImageDto> roomImages(DetailInfoDto source) {
        List<RoomImageDto> images = new ArrayList<>();
        addRoomImage(images, source.getRoomimg1(), source.getRoomimg1alt(), source.getCpyrhtDivCd1());
        addRoomImage(images, source.getRoomimg2(), source.getRoomimg2alt(), source.getCpyrhtDivCd2());
        addRoomImage(images, source.getRoomimg3(), source.getRoomimg3alt(), source.getCpyrhtDivCd3());
        addRoomImage(images, source.getRoomimg4(), source.getRoomimg4alt(), source.getCpyrhtDivCd4());
        addRoomImage(images, source.getRoomimg5(), source.getRoomimg5alt(), source.getCpyrhtDivCd5());
        return images;
    }

    private void addRoomImage(List<RoomImageDto> images, String url, String alt, String copyrightTypeCode) {
        if (url != null && !url.isBlank()) {
            images.add(RoomImageDto.builder()
                    .imageUrl(url)
                    .alt(alt)
                    .copyrightTypeCode(copyrightTypeCode)
                    .build());
        }
    }

    private SeasonalImageDto.Season toSeason(Integer month) {
        if (month == null) return SeasonalImageDto.Season.UNKNOWN;
        return switch (month) {
            case 3, 4, 5 -> SeasonalImageDto.Season.SPRING;
            case 6, 7, 8 -> SeasonalImageDto.Season.SUMMER;
            case 9, 10, 11 -> SeasonalImageDto.Season.AUTUMN;
            default -> SeasonalImageDto.Season.WINTER;
        };
    }
}