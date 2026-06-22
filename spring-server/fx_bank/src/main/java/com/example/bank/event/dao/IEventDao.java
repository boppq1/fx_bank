package com.example.bank.event.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.bank.event.dto.EventDto;

@Mapper
public interface IEventDao {
    void insertEvent(EventDto event);
    EventDto selectEvent(@Param("userNo") Long userNo);
    void updateLetter(EventDto event);
    void updateIsApplied(@Param("userNo") Long userNo);
    void insertCoupon(@Param("userNo") Long userNo, @Param("eventPk") Long eventPk, @Param("productNo") Long productNo);
}