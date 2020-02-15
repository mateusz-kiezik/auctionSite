package com.auction.mvc.controllers;

import com.auction.core.services.AuctionService;
import com.auction.core.services.CategoryService;
import com.auction.core.services.UserService;
import com.auction.core.validators.AuctionAddValidator;
import com.auction.dto.AuctionDTO;
import com.auction.dto.LoggedUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;

@Controller
@RequestMapping("/auction-add")
public class AddAuctionController {
    private CategoryService categoryService;
    private AuctionService auctionService;
    private AuctionAddValidator auctionValidator;
    private UserService userService;

    @Autowired
    public AddAuctionController(CategoryService categoryService,
                                AuctionService auctionService,
                                AuctionAddValidator auctionValidator,
                                UserService userService) {
        this.categoryService = categoryService;
        this.auctionService = auctionService;
        this.auctionValidator = auctionValidator;
        this.userService = userService;

    }

    @GetMapping
    public ModelAndView addAuctionInitPage(Model model) {

        model.addAttribute("categories", categoryService.getCategoriesMap());
        return new ModelAndView("add-auction", "newAuction", new AuctionDTO());
    }

    @PostMapping
    public String addNewAuction(@Valid @ModelAttribute("newAuction") AuctionDTO auctionDTO,
                                BindingResult result, Model model) {
        auctionValidator.validate(auctionDTO,result);
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getCategoriesMap());
            return "add-auction";
        }

        String auctionId = auctionService.addAuction(auctionDTO);
        return "redirect:/auction/" + auctionId;
    }
}
