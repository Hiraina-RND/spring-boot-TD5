package com.spring_boot.TD5.repository;

import com.spring_boot.TD5.datasource.DataSource;
import com.spring_boot.TD5.entity.Dish;
import com.spring_boot.TD5.entity.DishOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DishOrderRepository {
    public List<DishOrder> findDishOrderByIdOrder(Integer idOrder) {
        DataSource dataSource = new DataSource();
        Connection connection = dataSource.getConnection();
        List<DishOrder> dishOrders = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                        select id, id_dish, quantity from dish_order where dish_order.id_order = ?
                    """);
            preparedStatement.setInt(1, idOrder);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                DishRepository dishRepository = new DishRepository();
                Dish dish = dishRepository.findDishById(resultSet.getInt("id_dish"));
                DishOrder dishOrder = new DishOrder();
                dishOrder.setId(resultSet.getInt("id"));
                dishOrder.setQuantity(resultSet.getInt("quantity"));
                dishOrder.setDish(dish);
                dishOrders.add(dishOrder);
            }
            dataSource.closeConnection(connection);
            return dishOrders;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
