package com.learn.coemall.mallcart.service;

import com.learn.coemall.mallcart.vo.Cart;
import com.learn.coemall.mallcart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author coffee
 * @date 2021-06-21 11:17
 */
public interface CartService {

    /**
     * 将商品添加到购物车
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取购物车中某个购物项
     */
    CartItem getCartItem(Long skuId);

    /**
     * 获取购物车
     */
    Cart getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空购物车
     */
    void clearCart(String cartKey);

    /**
     * 勾选购物项
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 修改购物项数量
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 删除购物项
     */
    void deleteItem(Long skuId);

    /**
     * 获取用户购物车里的购物项
     */
    List<CartItem> getUserCartItems();

}
