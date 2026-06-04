package com.fabflix.cart;

import com.fabflix.common.ApiException;
import com.fabflix.common.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/shopping-cart")
    public Map<String, Object> shoppingCart(HttpServletRequest request) {
        Map<String, Object> response = ApiResponses.success("Cart loaded.");
        response.put("cartItems", cartService.cartItems(userId(request)));
        return response;
    }

    @PostMapping("/add-to-cart")
    public Map<String, Object> addToCart(HttpServletRequest request, @RequestParam("movieId") String movieId) {
        cartService.addToCart(userId(request), movieId);
        Map<String, Object> response = ApiResponses.success("Movie added to cart.");
        response.put("movieId", movieId);
        return response;
    }

    @PostMapping("/update-cart")
    public Map<String, Object> updateCart(
            HttpServletRequest request,
            @RequestParam("movieId") String movieId,
            @RequestParam("quantity") int quantity
    ) {
        cartService.updateCart(userId(request), movieId, quantity);
        Map<String, Object> response = ApiResponses.success("Cart updated.");
        response.put("movieId", movieId);
        response.put("quantity", quantity);
        return response;
    }

    @PostMapping("/place-order")
    public Map<String, Object> placeOrder(
            HttpServletRequest request,
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "cardNumber", required = false) String cardNumber,
            @RequestParam(value = "expiration", required = false) String expiration
    ) {
        cartService.placeOrder(userId(request), new PaymentRequest(firstName, lastName, cardNumber, expiration));
        return ApiResponses.success("Order placed successfully.");
    }

    @GetMapping("/order-summary")
    public Map<String, Object> orderSummary(HttpServletRequest request) {
        Map<String, Object> response = ApiResponses.success("Order summary loaded.");
        response.put("cartItems", cartService.orderSummary(userId(request)));
        return response;
    }

    private int userId(HttpServletRequest request) {
        Object value = request.getAttribute("userId");
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token.");
    }
}
