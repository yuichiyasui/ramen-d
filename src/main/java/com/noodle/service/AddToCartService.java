package com.noodle.service;

import java.math.BigInteger;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noodle.domain.Order;
import com.noodle.domain.OrderItem;
import com.noodle.domain.OrderTopping;
import com.noodle.form.OrderItemForm;
import com.noodle.repository.OrderItemRepository;
import com.noodle.repository.OrderRepository;
import com.noodle.repository.OrderToppingRepository;
import com.noodle.repository.ToppingRepository;

/**
 * カートに追加する処理を行うサービスクラス.
 * 
 * @author yuichi
 *
 */
@Service
@Transactional
public class AddToCartService {

	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private OrderItemRepository orderItemRepository;
	@Autowired
	private OrderToppingRepository orderToppingRepository;
	@Autowired
	private ToppingRepository toppingRepository;
	@Autowired
	private HttpSession session;

	/** ロギング処理 */
	private static final Logger LOGGER
	= LoggerFactory.getLogger(AddToCartService.class);
	
	/**
	 * オーダー情報を作成するメソッド.
	 * @param form item_detail.htmlから受け取ったリクエストパラメータ
	 * @return このオーダーのユーザーID
	 */
	public int addToCart(OrderItemForm form) {
		// セッションIDを10進数に変換
		BigInteger decSessionId = new BigInteger(session.getId(), 16);
		// int型に変換(ハッシュコードに変換)
		int preUserId = decSessionId.hashCode();
		
		// UserIdで検索して注文前のオーダーが作成されているか確認
		Order order;
		if(orderRepository.findByUserIdAndStatus(preUserId) == null) {
			// オーダーがなかったらOrderオブジェクトを生成
			order = new Order();
			order.setUserId(preUserId);
			order.setStatus(0);
			order.setTotalPrice(0);
			orderRepository.insert(order);
			LOGGER.info("注文が存在しなかったので新規に作成しました");
		}else {
			// オーダーが既に存在したらそれをとってくる
			order = orderRepository.findByUserIdAndStatus(preUserId);
			LOGGER.info("注文が既に存在したので既存の注文に追加します");
		}
		
		// OrderItemオブジェクトを生成
		OrderItem orderItem = new OrderItem();
		orderItem.setItemId(form.getItemId());
		// 注文IDを取得してセット
		int orderId = orderRepository.findIdByUserIdAndStatus(order.getUserId());
		orderItem.setOrderId(orderId);
		orderItem.setQuantity(form.getQuantity());
		orderItem.setSize(form.getSize());
		orderItemRepository.insert(orderItem);
		LOGGER.info("注文商品情報をDBに追加しました");
		
		// OrderToppingオブジェクトを生成
		OrderTopping orderTopping;
		// OrderToppingListの中身があればINSERT
		if(form.getOrderToppingList() != null ) {
			for(Integer toppingId : form.getOrderToppingList()) {
				orderTopping = new OrderTopping();
				orderTopping.setToppingId(toppingId);
				// order_itemsテーブルから主キーをとってくる
				int orderItemId = orderItemRepository.findIdByItemIdAndOrderId(orderItem);
				orderTopping.setOrderItemId(orderItemId);
				orderTopping.setTopping(toppingRepository.load(toppingId));
				orderToppingRepository.insert(orderTopping);
				LOGGER.info("注文トッピング情報をDBに追加しました");
			}
		}
		return order.getUserId();
	}
	
}