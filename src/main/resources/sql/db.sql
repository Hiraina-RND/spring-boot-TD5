create database "mini_dish_db";

create user "mini_dish_db_manager" with password '123456';

alter database "mini_dish_db" owner to mini_dish_db_manager;