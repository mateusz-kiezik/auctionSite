package com.auction.data.repositories;

import com.auction.data.model.Auction;
import com.auction.data.model.UserAccount;
import com.auction.dto.AuctionDTO;
import com.auction.utils.enums.AuctionStatus;
import com.auction.utils.enums.AuctionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {


    List<Auction> findAllById(Long auctionId);

    List<Auction> findAllByStatus(AuctionStatus status);

    List<Auction> findAllByStatusOrderByAuctionTypeDesc(AuctionStatus status);

    List<Auction> findAllBySellerIdAndStatusOrderByAuctionTypeDesc(Long id, AuctionStatus status);

    List<Auction> findTop3ByStatusOrderByDateCreatedDesc(AuctionStatus status);

    List<Auction> findTop3ByStatusOrderByDateEndedAsc(AuctionStatus status);

    List<Auction> findTop5ByAuctionTypeAndStatusOrderByDateEndedAsc(AuctionType auctionType, AuctionStatus status);

    Optional<Auction> getOneById(Long auctionId);

    @Query(value = "SELECT a FROM Auction a WHERE a.seller.login = ?1 AND a.status=?2")
    List<Auction> findAllByLoginAndAuctionStatus(String login, AuctionStatus auctionStatus);

    @Query(value = "SELECT a FROM Auction a WHERE a.status = ?1 ORDER BY a.auctionType DESC, a.dateEnded ASC")
    List<Auction> findAllByStatus(AuctionStatus status, Sort sort);

    @Query(value = "SELECT a FROM Auction a WHERE a.status = ?1 AND a.category.id in ?2 ORDER BY a.auctionType DESC, a.dateEnded ASC")
    List<Auction> findAllByStatusAndCategory(AuctionStatus pending, List<Long> categoryId, Sort sort);

    @Query(value = "SELECT a FROM Auction a WHERE a.dateEnded < ?1 AND a.status = ?2")
    List<Auction> findAllFinishedAuction(LocalDateTime currTime, AuctionStatus status);



}