package com.example.Weather.Assistant.repository;

import com.example.Weather.Assistant.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User,Long> {

}
