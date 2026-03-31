package com.spring_boot.TD5.repository;

import com.spring_boot.TD5.datasource.DataSource;
import com.spring_boot.TD5.entity.DishOrder;
import com.spring_boot.TD5.entity.Order;

import java.sql.*;
import java.util.List;

public class OrderRepository {
    DishOrderRepository dishOrderRepository = new DishOrderRepository();
    Order findOrderByReference(String reference) {
        DataSource dataSource = new DataSource();
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    select id, reference, creation_datetime from "order" where reference like ?""");
            preparedStatement.setString(1, reference);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Order order = new Order();
                Integer idOrder = resultSet.getInt("id");
                order.setId(idOrder);
                order.setReference(resultSet.getString("reference"));
                order.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());
                order.setDishOrderList(dishOrderRepository.findDishOrderByIdOrder(idOrder));
                return order;
            }
            throw new RuntimeException("Order not found with reference " + reference);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Order saveOrder(Order order) {
        String upsertOrderSql = """
                    INSERT INTO "order" (id, reference, creation_datetime)
                    VALUES (?, ?, ?)
                    ON CONFLICT (id) DO NOTHING
                    RETURNING id
                """;

        //TODO : bug on expected return ID when already exists

        try (Connection conn = new DataSource().getConnection()) {
            conn.setAutoCommit(false);
            Integer orderId;
            try (PreparedStatement ps = conn.prepareStatement(upsertOrderSql)) {
                int nextSerialValue = SequenceHelper.getNextSerialValue(conn, "\"order\"", "id");
                if (order.getId() != null) {
                    ps.setInt(1, order.getId());
                } else {
                    ps.setInt(1, nextSerialValue);
                }
                ps.setString(2, order.getReference());
                ps.setTimestamp(3, Timestamp.from(order.getCreationDatetime()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        orderId = rs.getInt(1);
                    } else {
                        orderId = order.getId() != null ? order.getId() : nextSerialValue;
                    }
                }
            }
            List<DishOrder> dishOrderList = order.getDishOrderList();
            detachOrders(conn, orderId);
            attachOrders(conn, orderId, dishOrderList);

            conn.commit();
            return findOrderByReference(order.getReference());
        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate key value violates unique constraint \"order_reference_unique\"")) {
                throw new RuntimeException("Order already exists with reference " + order.getReference());
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private void attachOrders(Connection conn, Integer orderId, List<DishOrder> dishOrders)
            throws SQLException {

        if (dishOrders == null || dishOrders.isEmpty()) {
            return;
        }
        String attachSql = """
                    insert into dish_order (id, id_order, id_dish, quantity)
                    values (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
            int nextSerialValue = SequenceHelper.getNextSerialValue(conn, "dish_order", "id");
            for (DishOrder dishOrder : dishOrders) {
                ps.setInt(1, nextSerialValue);
                ps.setInt(2, orderId);
                ps.setInt(3, dishOrder.getDish().getId());
                ps.setDouble(4, dishOrder.getQuantity());
                ps.addBatch(); // Can be substitute ps.executeUpdate() but bad performance
                nextSerialValue++;
            }
            ps.executeBatch();
        }
    }

    private void detachOrders(Connection conn, Integer idOrder) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dish_order where id_order = ?")) {
            ps.setInt(1, idOrder);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}