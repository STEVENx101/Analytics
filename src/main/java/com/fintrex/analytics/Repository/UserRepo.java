package com.fintrex.analytics.Repository;

import com.fintrex.analytics.Entity.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepo {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public Map<String, Object> getUserById(Integer id) {

        String sql = "SELECT * FROM hris_new.`users` WHERE `id` = :id";
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        try {
            return jdbcTemplate.queryForMap(sql, params);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Map<String, Object> getUserByUsernameAndPassword(String username) {
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);

        boolean isHod = false;
        String role2 = null;
        String department = null;

        

        // 3. Default role2 for normal users
        if (role2 == null) {
            role2 = "USER";
        }

        // 4. Fetch main user data
        String sql = """
       SELECT 
           eu.username, 
           eu.user_type_id,
           eu.app_support, 
           ut.commentToggle,
           ut.internalComment,
                       ut.assign,
                       ut.status_change,
           ut.name AS Tname,
           eu.name AS callname, 
           eu.id AS user_id
       FROM analytics.user eu
       LEFT JOIN user_type ut ON ut.id = eu.user_type_id
       WHERE eu.email = :username
    """;

        try {
            Map<String, Object> userData = jdbcTemplate.queryForMap(sql, params);

            // Add roles
            if (isHod) {
                userData.put("role", "HOD");
                userData.put("department", department);
            }
            userData.put("role2", role2);

            return userData;
        } catch (EmptyResultDataAccessException e) {
            return null; // user not found
        }
    }

    public String removeUser(String user) {
        System.out.println("Repository: " + user);
        String sql = "DELETE FROM admin WHERE username = :user";  // Named parameter correctly used

        // Use a map to set the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);

        // Use the update method with named parameters
        int rowsAffected = jdbcTemplate.update(sql, params);

        if (rowsAffected > 0) {
            return "User with username " + user + " has been removed successfully.";
        } else {
            return "No user found with username " + user + ".";
        }
    }

}
