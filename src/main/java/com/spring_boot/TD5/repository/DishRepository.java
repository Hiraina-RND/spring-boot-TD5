package com.spring_boot.TD5.repository;

import com.spring_boot.TD5.datasource.DataSource;
import com.spring_boot.TD5.entity.Dish;
import com.spring_boot.TD5.entity.DishIngredient;
import com.spring_boot.TD5.entity.DishTypeEnum;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DishRepository {
    IngredientRepository ingredientRepository = new IngredientRepository();
    public Dish findDishById(Integer id) {
        DataSource dataSource = new DataSource();
        Connection connection = dataSource.getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            select dish.id as dish_id, dish.name as dish_name, dish_type, dish.selling_price as dish_price
                            from dish
                            where dish.id = ?;
                            """);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Dish dish = new Dish();
                dish.setId(resultSet.getInt("dish_id"));
                dish.setName(resultSet.getString("dish_name"));
                dish.setDishType(DishTypeEnum.valueOf(resultSet.getString("dish_type")));
                dish.setPrice(resultSet.getObject("dish_price") == null
                        ? null : resultSet.getDouble("dish_price"));
                dish.setDishIngredients(ingredientRepository.findIngredientByDishId(id));
                return dish;
            }
            dataSource.closeConnection(connection);
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Dish saveDish(Dish toSave) {
        String upsertDishSql = """
                    INSERT INTO dish (id, selling_price, name, dish_type)
                    VALUES (?, ?, ?, ?::dish_type)
                    ON CONFLICT (id) DO UPDATE
                    SET name = EXCLUDED.name,
                        dish_type = EXCLUDED.dish_type,
                        selling_price = EXCLUDED.selling_price
                    RETURNING id
                """;

        try (Connection conn = new DataSource().getConnection()) {
            conn.setAutoCommit(false);
            Integer dishId;
            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                if (toSave.getId() != null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, SequenceHelper.getNextSerialValue(conn, "dish", "id"));
                }
                if (toSave.getPrice() != null) {
                    ps.setDouble(2, toSave.getPrice());
                } else {
                    ps.setNull(2, Types.DOUBLE);
                }
                ps.setString(3, toSave.getName());
                ps.setString(4, toSave.getDishType().name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    dishId = rs.getInt(1);
                }
            }

            List<DishIngredient> newDishIngredients = toSave.getDishIngredients();
            if (newDishIngredients != null && !newDishIngredients.isEmpty()) {
                ingredientRepository.detachIngredients(conn, newDishIngredients);
                ingredientRepository.attachIngredients(conn, newDishIngredients);
            }
            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Dish> findAll() {
        List<Dish> dishes = new ArrayList<>();
        IngredientRepository ingredientRepository = new IngredientRepository();

        try (
                Connection connection = new DataSource().getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT id, name, selling_price, dish_type FROM dish"
                );
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                Dish dish = new Dish();
                dish.setId(rs.getInt("id"));
                dish.setName(rs.getString("name"));
                dish.setPrice(rs.getDouble("selling_price"));
                dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));

                List<DishIngredient> ingredients = ingredientRepository.findIngredientByDishId(dish.getId());
                dish.setDishIngredients(ingredients);

                dishes.add(dish);
            }
            return dishes;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Dish findDishByName(String name) {
        DataSource dataSource = new DataSource();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT id, name, selling_price, dish_type FROM dish WHERE name = ?"
             )) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Dish dish = new Dish();
                    dish.setId(rs.getInt("id"));
                    dish.setName(rs.getString("name"));
                    dish.setPrice(rs.getObject("selling_price") == null ? null : rs.getDouble("selling_price"));
                    dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                    dish.setDishIngredients(ingredientRepository.findIngredientByDishId(dish.getId()));
                    return dish;
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
