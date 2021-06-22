package com.learn.coemall.mallcart.service.impl;

import com.alibaba.fastjson.JSON;
import com.learn.coemall.mallcart.feign.ProductFeignService;
import com.learn.coemall.mallcart.interceptor.CartInterceptor;
import com.learn.coemall.mallcart.service.CartService;
import com.learn.coemall.mallcart.vo.Cart;
import com.learn.coemall.mallcart.vo.CartItem;
import com.learn.coemall.mallcart.vo.SkuInfoVo;
import com.learn.coemall.mallcart.vo.UserInfoTo;
import com.learn.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author coffee
 * @date 2021-06-21 11:17
 */
@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    private final String CART_PREFIX = "coemall:cart";


    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String o = (String) cartOps.get(skuId.toString());
        CartItem cartItem;
        if (!StringUtils.hasLength(o)){
            //远程查询当前要添加的商品信息
            cartItem = new CartItem();
            CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                R r = productFeignService.getSkuInfo(skuId);
                SkuInfoVo info = (SkuInfoVo) r.get("skuInfo");

                //将新商品添加到购物车
                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setImage(info.getSkuDefaultImg());
                cartItem.setTitle(info.getSkuTitle());
                cartItem.setSkuId(skuId);
                cartItem.setPrice(info.getPrice());
            },executor);

            //远程查询sku的组合信息
            CompletableFuture<Void> runAsync1 = CompletableFuture.runAsync(() -> {
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(values);
            }, executor);

            CompletableFuture.allOf(runAsync,runAsync1).get();

            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(),s);
        } else{
            //购物车有当前商品
            cartItem = JSON.parseObject(o, CartItem.class);
            //修改数量
            cartItem.setCount(cartItem.getCount() + num);
            cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));
        }
        return cartItem;
    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String o = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(o,CartItem.class);
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Cart cart = new Cart();
        if (userInfoTo.getUserId() != null){//登录
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            //临时购物车
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems != null){
                //临时购物车有数据进行合并
                for (CartItem item : tempCartItems) {
                    addToCart(item.getSkuId(),item.getCount());
                }
                clearCart(tempCartKey);
            }
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }else {//未登录
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1);

        cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo != null){
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartKey);
            return cartItems.stream().filter(CartItem::getCheck)
                    .map(item -> {
                        R price = productFeignService.getPrice(item.getSkuId());
                        item.setPrice((BigDecimal) price.get("data"));
                        return item;
                    }).collect(Collectors.toList());
        }else {
            return null;
        }
    }

    private BoundHashOperations<String,Object,Object> getCartOps(){
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey;
        if (userInfoTo.getUserId() != null){
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        }else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        return redisTemplate.boundHashOps(cartKey);
    }

    private List<CartItem> getCartItems(String cartKey){
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (!CollectionUtils.isEmpty(values)) {
            return values.stream().map(value -> {
                String str = (String) value;
                return JSON.parseObject(str, CartItem.class);
            }).collect(Collectors.toList());
        }
        return null;
    }
}
