package com.noodle.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.noodle.domain.OrderItem;

/**
 * order_itemsテーブルを操作するレポジトリークラス.
 * 
 * @author yuichi
 *
 */
@Repository
public class OrderItemRepository {

	@Autowired
	private NamedParameterJdbcTemplate template;

	private final static RowMapper<OrderItem> ORDER_ITEM_ROW_MAPPER = (rs, i) -> {
		OrderItem orderItem = new OrderItem();
		orderItem.setId(rs.getInt("id"));
		orderItem.setItemId(rs.getInt("item_id"));
		orderItem.setOrderId(rs.getInt("order_id"));
		orderItem.setQuantity(rs.getInt("quantity"));
		// String型からchar型への変換
		char[] size = rs.getString("size").toCharArray();
		orderItem.setSize(size[0]);
		return orderItem;
	};

	/**
	 * 注文商品情報をInsertするメソッド.
	 * 
	 * @param orderItem
	 */
	public void insert(OrderItem orderItem) {
		String sql = "INSERT INTO order_items(item_id,order_id,quantity,size) "
				+ "VALUES(:itemId,:orderId,:quantity,:size)";
		SqlParameterSource param = new BeanPropertySqlParameterSource(orderItem);
		template.update(sql, param);
	}

	/**
	 * 主キー検索を行うメソッド.
	 * 
	 * @param id 注文商品ID
	 * @return 注文商品情報
	 */
	public OrderItem load(Integer id) {
		String sql = "SELECT id,item_id,order_id,quantity,size FROM order_items WHERE id=:id";
		SqlParameterSource param = new MapSqlParameterSource().addValue("id", id);
		return template.queryForObject(sql, param, ORDER_ITEM_ROW_MAPPER);
	}

	/**
	 * 注文IDと商品IDで注文商品IDを取得するメソッド. 1つの注文時に同じ商品が2つある場合はidの降順で先頭のidを取得する
	 * 利用されるクラス:addToCartService
	 * 
	 * @return 注文商品ID
	 */
	public int findIdByItemIdAndOrderId(OrderItem orderItem) {
		String sql = "SELECT id,item_id,order_id,quantity,size FROM order_items "
				+ "WHERE item_id=:itemId AND order_id=:orderId ORDER BY id DESC";
		SqlParameterSource param = new BeanPropertySqlParameterSource(orderItem);
		return template.query(sql, param, ORDER_ITEM_ROW_MAPPER).get(0).getId();
	}

	/**
	 * 注文商品IDで注文商品とそれに関連するトッピングを削除するメソッド.
	 * 
	 * @param id 注文商品ID
	 */
	public void deleteOrderItemAndOrderToppingById(Integer id) {
		String sql = "WITH deleted AS (DELETE FROM order_items WHERE id = :id RETURNING id) "
				+ "DELETE FROM order_toppings WHERE order_item_id IN (SELECT id FROM deleted)";
		SqlParameterSource param = new MapSqlParameterSource().addValue("id", id);
		template.update(sql, param);
	}

	/**
	 * 注文商品の注文IDをログイン前に発行した注文IDから ログイン後に発行した注文IDに書き換えるメソッド. 利用されるクラス:LoginService
	 * 
	 * @param orderId    ログイン後に発行した注文ID
	 * @param preOrderId ログイン前に発行した注文ID
	 */
	public void updateOrderItemId(Integer orderId, Integer preOrderId) {
		String sql = "UPDATE order_items SET order_id = :orderId WHERE order_id = :preOrderId";
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("orderId", orderId).addValue("preOrderId",preOrderId);
		template.update(sql, param);
	}

}
