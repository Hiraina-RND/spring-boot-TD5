package com.spring_boot.TD5.repository;

import com.spring_boot.TD5.datasource.DataSource;
import com.spring_boot.TD5.entity.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockMovementRepository {
    public void insertIngredientStockMovements(Connection conn, Ingredient ingredient) {
        List<StockMovement> stockMovementList = ingredient.getStockMovementList();
        String sql = """
                insert into stock_movement(id, id_ingredient, quantity, type, unit, creation_datetime)
                values (?, ?, ?, ?::movement_type, ?::unit, ?)
                on conflict (id) do nothing
                """;
        try {
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            for (StockMovement stockMovement : stockMovementList) {
                if (ingredient.getId() != null) {
                    preparedStatement.setInt(1, ingredient.getId());
                } else {
                    preparedStatement.setInt(1, SequenceHelper.getNextSerialValue(conn, "stock_movement", "id"));
                }
                preparedStatement.setInt(2, ingredient.getId());
                preparedStatement.setDouble(3, stockMovement.getValue().getQuantity());
                preparedStatement.setObject(4, stockMovement.getType());
                preparedStatement.setObject(5, stockMovement.getValue().getUnit());
                preparedStatement.setTimestamp(6, Timestamp.from(stockMovement.getCreationDatetime()));
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<StockMovement> findStockMovementsByIngredientId(Integer id) {

        DataSource dbConnection = new DataSource();
        Connection connection = dbConnection.getConnection();
        List<StockMovement> stockMovementList = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            select id, quantity, unit, type, creation_datetime
                            from stock_movement
                            where stock_movement.id_ingredient = ?;
                            """);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                StockMovement stockMovement = new StockMovement();
                stockMovement.setId(resultSet.getInt("id"));
                stockMovement.setType(MovementTypeEnum.valueOf(resultSet.getString("type")));
                stockMovement.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());

                StockValue stockValue = new StockValue();
                stockValue.setQuantity(resultSet.getDouble("quantity"));
                stockValue.setUnit(Unit.valueOf(resultSet.getString("unit")));
                stockMovement.setValue(stockValue);

                stockMovementList.add(stockMovement);
            }
            dbConnection.closeConnection(connection);
            return stockMovementList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
