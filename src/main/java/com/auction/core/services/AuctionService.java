package com.auction.core.services;

import com.auction.data.model.*;
import com.auction.data.repositories.*;
import com.auction.dto.*;
import com.auction.utils.enums.AccountType;
import com.auction.utils.enums.AuctionStatus;
import com.auction.utils.enums.AuctionType;
import com.auction.utils.enums.TransactionRole;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import javax.transaction.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.core.context.SecurityContextHolder.*;

@Service
@Transactional
public class AuctionService {
    private BiddingRepository biddingRepository;
    private TransactionAssessmentRepository transactionAssessmentRepository;
    private UserAccountRepository userAccountRepository;
    private AuctionRepository auctionRepository;
    private CategoryRepository categoryRepository;
    private Mapper mapper;
    private PurchaseRepository purchaseRepository;
    private UserService userService;
    @Autowired
    private FileService fileService;

    public AuctionService(BiddingRepository biddingRepository, TransactionAssessmentRepository transactionAssessmentRepository, UserAccountRepository userAccountRepository, AuctionRepository auctionRepository, CategoryRepository categoryRepository, Mapper mapper, PurchaseRepository purchaseRepository, UserService userService) {
        this.biddingRepository = biddingRepository;
        this.transactionAssessmentRepository = transactionAssessmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.auctionRepository = auctionRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
        this.purchaseRepository = purchaseRepository;
        this.userService = userService;
    }

    public List<AuctionDTO> findAllByStatusWithCategory(AuctionStatus status) {
        List<AuctionDTO> auctionDTOList = new ArrayList<>();
        auctionRepository.findAllByStatus(status).forEach(a -> {
            AuctionDTO auctionDTO = mapper.map(a, AuctionDTO.class);
            auctionDTO.setCategoryId(a.getCategory().getId());
            auctionDTO.setSellerId(a.getSeller().getId());
            auctionDTO.setCategoryName(a.getCategory().getName());
            auctionDTO.setBidsNumber(a.getBiddingList().size());
            auctionDTOList.add(auctionDTO);
        });

        return auctionDTOList;
    }

    public List<AuctionDTO> findAllByStatusWithCategorySortedByPromote(AuctionStatus status) {
        List<AuctionDTO> auctionDTOList = new ArrayList<>();
        auctionRepository.findAllByStatusOrderByAuctionTypeDesc(status).forEach(a -> {
            AuctionDTO auctionDTO = mapper.map(a, AuctionDTO.class);
            auctionDTO.setCategoryId(a.getCategory().getId());
            auctionDTO.setSellerId(a.getSeller().getId());
            auctionDTO.setCategoryName(a.getCategory().getName());
            auctionDTO.setBidsNumber(a.getBiddingList().size());
            auctionDTOList.add(auctionDTO);
        });

        return auctionDTOList;
    }

    public List<AuctionDTO> findAllByUsernameAndStatusWithCategorySortedByPromote(Long userId, AuctionStatus status) {
        List<AuctionDTO> auctionDTOList = new ArrayList<>();
        auctionRepository.findAllBySellerIdAndStatusOrderByAuctionTypeDesc(userId, status).forEach(a -> {
            AuctionDTO auctionDTO = mapper.map(a, AuctionDTO.class);
            auctionDTO.setCategoryId(a.getCategory().getId());
            auctionDTO.setSellerId(a.getSeller().getId());
            auctionDTO.setCategoryName(a.getCategory().getName());
            auctionDTO.setBidsNumber(a.getBiddingList().size());
            auctionDTOList.add(auctionDTO);
        });

        return auctionDTOList;
    }


    public List<AuctionDTO> findAllById(Long auctionId) {
        List<AuctionDTO> auctionDTOList = new ArrayList<>();
        auctionRepository.findAllById(auctionId).forEach(a -> {
            AuctionDTO auctionDTO = mapper.map(a, AuctionDTO.class);
            auctionDTO.setCategoryId(a.getCategory().getId());
            auctionDTO.setSellerId(a.getSeller().getId());
            auctionDTO.setCategoryName(a.getCategory().getName());
            auctionDTO.setBidsNumber(a.getBiddingList().size());
            auctionDTOList.add(auctionDTO);
        });
        return auctionDTOList;
    }

    public List<AuctionDTO> findLastAddedAuctions() {
        List<AuctionDTO> auctionDTOList = new ArrayList<>();
        auctionRepository.findTop3ByStatusOrderByDateCreatedDesc(AuctionStatus.PENDING).forEach(a -> {
            AuctionDTO auctionDTO = mapper.map(a, AuctionDTO.class);
            auctionDTOList.add(auctionDTO);
        });
        return auctionDTOList;
    }

    public List<AuctionDTO> findEndingAuctions() {
        List<AuctionDTO> auctionDTOList = new ArrayList<>();
        auctionRepository.findTop3ByStatusOrderByDateEndedAsc(AuctionStatus.PENDING).forEach(a -> {
            AuctionDTO auctionDTO = mapper.map(a, AuctionDTO.class);
            auctionDTOList.add(auctionDTO);
        });
        return auctionDTOList;
    }

    public List<AuctionDTO> findPromotedAuctions() {
        List<AuctionDTO> auctionDTOList = new ArrayList<>();
        auctionRepository.findTop5ByAuctionTypeAndStatusOrderByDateEndedAsc(AuctionType.PROMOTED, AuctionStatus.PENDING).forEach(a -> {
            AuctionDTO auctionDTO = mapper.map(a, AuctionDTO.class);
            auctionDTOList.add(auctionDTO);
        });
        return auctionDTOList;
    }


    public String addAuction(AuctionDTO auctionDTO) throws IOException {
        LocalDateTime dateNow = LocalDateTime.now();
        Integer auctionDays = auctionDTO.getDays();
        Auction auction = mapper.map(auctionDTO, Auction.class);
        auction.setPhoto(null);
        auction.setActualPrice(auctionDTO.getStartPrice());
        auction.setStatus(AuctionStatus.PENDING);
        if (auctionDTO.getAuctionType() == null) {
            auction.setAuctionType(AuctionType.NORMAL);
        } else {
            auction.setAuctionType(auctionDTO.getAuctionType());
        }
        auction.setDateCreated(dateNow);
        auction.setDateEnded(dateNow.plusDays(auctionDays));
        String sellerLogin =
                getContext().getAuthentication().getName();
        List<Category> allCategoryById = categoryRepository.findAllById(auctionDTO.getCategoryId());
        if (allCategoryById.size() == 1) {
            auction.setCategory(allCategoryById.get(0));
        }
        List<UserAccount> allUsersByUsername = userAccountRepository.findAllByLogin(sellerLogin);
        UserAccount user;
        if (allUsersByUsername.size() == 1) {
            user = allUsersByUsername.get(0);
            auction.setSeller(user);
            return String.valueOf(auctionRepository.save(auction).getId());
        }
        return "";
    }

    public void buyAuctionByUser(Long auctionId) {
        Optional<Auction> auctionOpt = auctionRepository.getOneById(auctionId);
        Optional<UserAccount> userOpt = userAccountRepository
                .getOneByLogin(getContext().getAuthentication().getName());
        if (auctionOpt.isPresent() && userOpt.isPresent()) {
            Auction auction = auctionOpt.get();
            UserAccount user = userOpt.get();
            auction.setStatus(AuctionStatus.SOLD);
            auction.setDateEnded(LocalDateTime.now());
            createPurchase(auction, user, auction.getBuyNowPrice());
        }
    }

    private void createPurchase(Auction auction, UserAccount user, BigDecimal value) {
        Purchase purchase = new Purchase();
        purchase.setAuction(auction);
        purchase.setBuyerUser(user);
        purchase.setAmount(value);
        TransactionAssessment transactionAssessment = new TransactionAssessment();
        user.getPurchases().add(purchase);
        userAccountRepository.save(user);
        auctionRepository.save(auction);
        transactionAssessment.setPurchase(purchaseRepository.save(purchase));
        transactionAssessmentRepository.save(transactionAssessment);
    }

    public boolean makeBid(BidDTO bid) {
        String name = getContext().getAuthentication().getName();

        List<UserAccount> allUsersByLogin = userAccountRepository
                .findAllByLogin(name);
        UserAccount user = allUsersByLogin.get(0);

        Optional<Auction> auctionOptional = auctionRepository.findById(bid.getAuctionId());
        if (auctionOptional.isPresent()) {
            Auction auction = auctionOptional.get();
            Bidding newBid = new Bidding();
            newBid.setAmount(bid.getValue());
            newBid.setBiddingUser(user);
            newBid.setAuction(auction);
            auction.setActualPrice(bid.getValue());
            biddingRepository.save(newBid);
            auctionRepository.save(auction);
            return true;
        }
        return false;
    }

    public boolean isAnyBids(Long auctionId) {
        Optional<Auction> auction = auctionRepository.findById(auctionId);
        return auction.filter(value -> value.getBiddingList().size() > 0).isPresent();
    }

    public BigDecimal getBiggestBid(Long auctionId) {
        return biddingRepository.findBiggestBid(auctionId, PageRequest.of(0, 1)).get(0).getAmount();

    }

    public BigDecimal getStartBid(Long auctionId) {
        Optional<Auction> auction = auctionRepository.findById(auctionId);
        if (auction.isPresent())
            return auction.get().getStartPrice();
        else
            return BigDecimal.valueOf(0L);
    }

    public List<AuctionDTO> findUserObserveList(String username) {
        List<AuctionDTO> observeAuctions = new ArrayList<>();
        List<UserAccount> allByUsername = userAccountRepository.findAllByLogin(username);
        if (allByUsername.size() == 1) {
            UserAccount user = allByUsername.get(0);
            user.getObserveAuctions().forEach(a -> {
                AuctionDTO auctionDTO = mapper.map(a, AuctionDTO.class);
                auctionDTO.setCategoryId(a.getCategory().getId());
                auctionDTO.setCategoryName(a.getCategory().getName());
                observeAuctions.add(auctionDTO);
            });
        }

        return observeAuctions;
    }


    public List<AuctionDTO> findAllByUserLoginAndStatus(String login, AuctionStatus... auctionStatus) {
        List<AuctionDTO> myAuctions = new ArrayList<>();
        for (AuctionStatus status : auctionStatus) {
            auctionRepository.findAllByLoginAndAuctionStatus(login, status).forEach(a -> {
                AuctionDTO auctionDTO = mapper.map(a, AuctionDTO.class);
                auctionDTO.setCategoryId(a.getCategory().getId());
                auctionDTO.setCategoryName(a.getCategory().getName());
                auctionDTO.setBidsNumber(a.getBiddingList().size());
                myAuctions.add(auctionDTO);
            });
        }
        return myAuctions;
    }

    public List<AuctionDTO> doPendingAuctionFilter(FilterAuctionDTO filter) {
        List<Auction> collect;
        List<Auction> sorted;
        Sort sort;
        if (filter.getSort() != null) {
            switch (filter.getSort()) {
                case priceASC:
                    if (filter.getOnlyBuyNow()) {
                        sort = Sort.by(Sort.Direction.ASC, "buyNowPrice");
                    } else {
                        sort = Sort.by(Sort.Direction.ASC, "actualPrice");
                    }
                    break;
                case priceDES:
                    if (filter.getOnlyBuyNow()) {
                        sort = Sort.by(Sort.Direction.DESC, "buyNowPrice");
                    } else {
                        sort = Sort.by(Sort.Direction.DESC, "actualPrice");
                    }
                    break;
                case timeASC:
                    sort = Sort.by(Sort.Direction.DESC, "dateCreated");
                    break;
                case timeDES:
                    sort = Sort.by(Sort.Direction.ASC, "dateEnded");
                    break;
                default:
                    sort = Sort.unsorted();
            }
        } else {
            sort = Sort.unsorted();
        }

        if (filter.getCategoryId().contains(0L) || filter.getCategoryId().size() == 0) {
            sorted = auctionRepository.findAllByStatus(AuctionStatus.PENDING, sort);
        } else {
            sorted = auctionRepository.findAllByStatusAndCategory(AuctionStatus.PENDING,
                    filter.getCategoryId(), sort);
        }

        if (filter.getOnlyBuyNow()) {
            collect = sorted.stream().filter(x ->
                    x.getBiddingList().size() == 0).collect(Collectors.toList());
        } else {
            collect = sorted;
        }


        List<AuctionDTO> auctionsDTO = new ArrayList<>();
        collect.forEach(a -> {
            AuctionDTO auctionDTO = mapper.map(a, AuctionDTO.class);
            auctionDTO.setCategoryId(a.getCategory().getId());
            auctionDTO.setSellerId(a.getSeller().getId());
            auctionDTO.setCategoryName(a.getCategory().getName());
            BigDecimal actualPrice;

            if (a.getBiddingList().size() > 0) {
                actualPrice = Collections.max(a.getBiddingList().stream()
                        .map(Bidding::getAmount).collect(Collectors.toList()));
            } else {
                actualPrice = a.getStartPrice();
            }
            auctionDTO.setActualPrice(actualPrice);
            auctionDTO.setBidsNumber(a.getBiddingList().size());
            auctionsDTO.add(auctionDTO);
        });

        return auctionsDTO;
    }

    public List<Long> getAllAuctionsId() {
        return auctionRepository.findAll().stream().map(Auction::getId).collect(Collectors.toList());
    }

    public void finishedAuctions() {
        List<Auction> finishedAuction = auctionRepository.findAllFinishedAuction(LocalDateTime.now(),
                AuctionStatus.PENDING);
        finishedAuction.forEach(this::finishedAuction);

    }

    private void finishedAuction(Auction auction) {
        int bidingSize = auction.getBiddingList().size();
        if (bidingSize > 0) {
            auction.setStatus(AuctionStatus.SOLD);
            Bidding bidding = biddingRepository.findBiggestBid(auction.getId(),
                    PageRequest.of(0, 1)).get(0);
            createPurchase(auction, bidding.getBiddingUser(), bidding.getAmount());

        } else {
            auction.setStatus(AuctionStatus.CLOSE);
        }
        auctionRepository.save(auction);
    }

    public void finishedAuction(Long auctionId) {
        Optional<Auction> auctionOpt = auctionRepository.getOneById(auctionId);
        auctionOpt.ifPresent(this::finishedAuction);
    }

    public void promoteAuction(Long auctionId) {
        Auction auction = auctionRepository.findAllById(auctionId).get(0);
        auction.setAuctionType(AuctionType.PROMOTED);
        auctionRepository.save(auction);
    }

    public String getAuctionTitleFromPurchase(Long purchaseId) {
        return purchaseRepository.getOne(purchaseId).getAuction().getTitle();
    }

    public void removeMyObserveAuction(Long auctionId) {
        String login = getContext().getAuthentication().getName();
        Optional<UserAccount> oneByLogin = userAccountRepository.getOneByLogin(login);
        if (oneByLogin.isPresent()) {
            UserAccount userAccount = oneByLogin.get();
            userAccount.getObserveAuctions().remove(auctionRepository.getOne(auctionId));
            userAccountRepository.save(userAccount);
        }
    }
}

