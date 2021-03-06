package com.smartcity.dao;

import com.smartcity.domain.Task;
import com.smartcity.exceptions.DbOperationException;
import com.smartcity.exceptions.NotFoundException;
import com.smartcity.mapper.TaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskDaoImpl implements TaskDao {

    private JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(TaskDaoImpl.class);
    private TaskMapper mapper;

    @Autowired
    public TaskDaoImpl(DataSource dataSource, TaskMapper mapper) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.mapper = mapper;
    }

    public Task create(Task task) {
        LocalDateTime localDateTime = LocalDateTime.now();
        try {
            GeneratedKeyHolder holder = new GeneratedKeyHolder();
            task.setCreatedAt(localDateTime);
            task.setUpdatedAt(localDateTime);
            this.jdbcTemplate.update(con -> createStatement(task, con), holder);
            task.setId(Optional.ofNullable(holder.getKey()).map(Number::longValue)
                    .orElseThrow(() -> new DbOperationException("Create Task Dao method error: AutoGeneratedKey = null")));
            return task;
        } catch (Exception e) {
            logger.error("Create Task Dao method error: ", task, e);
            throw new DbOperationException("Create Task Dao method error: " + task + e);
        }
    }


    public Task findById(Long id) {
        try {
            return this.jdbcTemplate.queryForObject(Queries.SQL_GET_BY_ID,
                    mapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw loggedNotFoundException(id);
        } catch (Exception e) {
            logger.error("Can't get Task by id = {} Task Dao get method error: ", id, e);
            throw new DbOperationException("Can't get Task by id = " + id + " Task Dao findById method error: " + e);
        }
    }

    public List<Task> findByOrganizationId(Long id) {
        try {
            return this.jdbcTemplate.query(Queries.SQL_GET_BY_ORGANIZATION_ID, mapper, id);
        } catch (Exception e) {
            logger.error("Can't get Task by Org id = {} Task Dao findByOrgId method error: ", id, e);
            throw new DbOperationException("Can't get Task by Org id = " + id + " Task Dao findByOrgId method error: " + e);
        }
    }

    public List<Task> findByUserId(Long id) {
        try {
            return this.jdbcTemplate.query(Queries.SQL_GET_BY_USER_ID, mapper, id);
        } catch (Exception e) {
            logger.error("Can't get Task by User id = {} Task Dao findByUserId method error: ", id, e);
            throw new DbOperationException("Can't get Task by User id = " + id + " Task Dao findByUserId method error: " + e);
        }
    }


    public List<Task> findAll() {
        try {
            return this.jdbcTemplate.query(Queries.SQL_GET_ALL_TASKS, mapper);
        } catch (Exception e) {
            logger.error("Can't get all Tasks. Task Dao findAll method error: ", e);
            throw new DbOperationException("Can't get all Tasks. Task Dao findAll method error: " + e);
        }
    }

    public List<Task> findByDate(Long id, LocalDateTime from, LocalDateTime to) {
        try {
            return this.jdbcTemplate.query(Queries.SQL_GET_BY_DATE, mapper, from, to, id);
        } catch (Exception e) {
            logger.error("Can't get Task by such dates = {}, {} Task Dao findByDate method error: ", from, to, e);
            throw new DbOperationException("Can't get Task by dates from = " + from + ", " + to
                    + " Task Dao findByDate method error: " + e);
        }
    }

    public Task update(Task task) {
        int rowsAffected;
        try {
            task.setUpdatedAt(LocalDateTime.now());
            rowsAffected = this.jdbcTemplate.update(Queries.SQL_UPDATE,
                    task.getTitle(), task.getDescription(),
                    Date.valueOf(task.getDeadlineDate().toLocalDate()), task.getTaskStatus(),
                    task.getBudget(), task.getApprovedBudget(), task.getUpdatedAt(),
                    task.getUsersOrganizationsId(), task.getId());
        } catch (Exception e) {
            logger.error("Update Task Dao method error: " + task.toString() + "  " + e);
            throw new DbOperationException("Update Task Dao method error: " + task.toString() + "  " + e);
        }
        if (rowsAffected < 1) {
            throw loggedNotFoundException(task.getId());
        } else return task;
    }

    public boolean delete(Long id) {
        int rowsAffected;
        try {
            rowsAffected = this.jdbcTemplate.update(Queries.SQL_DELETE, id);
        } catch (Exception e) {
            logger.error("Delete Task Dao method error: " + e);
            throw new DbOperationException("Delete Task Dao method error: " + e);
        }
        if (rowsAffected < 1) {
            throw loggedNotFoundException(id);
        } else return true;
    }

    private PreparedStatement createStatement(Task task, Connection con) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                Queries.SQL_CREATE, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, task.getTitle());
        ps.setString(2, task.getDescription());
        ps.setObject(3, task.getDeadlineDate());
        ps.setString(4, task.getTaskStatus());
        ps.setLong(5, task.getBudget());
        ps.setLong(6, task.getApprovedBudget());
        ps.setObject(7, task.getCreatedAt());
        ps.setObject(8, task.getUpdatedAt());
        ps.setLong(9, task.getUsersOrganizationsId());
        return ps;
    }

    private NotFoundException loggedNotFoundException(Long id) {
        NotFoundException notFoundException = new NotFoundException("Task not found.Id = " + id);
        logger.error("Runtime exception. Task by id = {} not found. Message: {}",
                id, notFoundException.getMessage());
        return notFoundException;
    }


    class Queries {
        static final String SQL_CREATE = "Insert into Tasks(title, description, " +
                "deadline_date, task_status,  budget,  " +
                "approved_budget, created_date, updated_date," +
                "users_organizations_id) values (?,?,?,?,?,?,?,?,?);";

        static final String SQL_GET_BY_ID = "Select * from Tasks where id = ?;";

        static final String SQL_GET_BY_USER_ID = "Select * from Tasks where users_organizations_id IN " +
                "(Select id from Users_organizations Where user_id = ?);";

        static final String SQL_GET_BY_ORGANIZATION_ID = "Select * from Tasks where users_organizations_id IN " +
                "(Select id from Users_organizations where organization_id = ?);";

        static final String SQL_GET_ALL_TASKS = "Select * from Tasks;";

        static final String SQL_UPDATE = "Update Tasks set title = ? , description = ?, " +
                "deadline_date = ?, task_status = ?, budget = ?, " +
                "approved_budget = ?, updated_date = ?,  users_organizations_id = ? where id = ?;";

        static final String SQL_DELETE = "Delete from Tasks where id = ?;";

        static final String SQL_GET_BY_DATE = "Select * from Tasks where created_date between ? and ? " +
                "and users_organizations_id IN (Select id from Users_organizations where organization_id = ?) order by created_date;";
    }
}

