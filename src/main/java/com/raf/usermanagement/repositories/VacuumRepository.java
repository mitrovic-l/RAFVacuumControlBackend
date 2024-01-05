package com.raf.usermanagement.repositories;

import com.raf.usermanagement.model.User;
import com.raf.usermanagement.model.Vacuum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VacuumRepository extends JpaRepository<Vacuum, Long> {
    public List<Vacuum> findAllByAddedBy(User user);

//    @Query("SELECT * FROM vacuum WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) AND added_by = :addedBy")
//    public List<Vacuum> findAllBySimilarName(@Param("name") String name, @Param("addedBy") Integer addedBy);
    @Query("SELECT v FROM Vacuum v WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :name, '%')) AND v.addedBy = :addedBy")
    public List<Vacuum> findAllBySimilarName(@Param("name") String name, @Param("addedBy") User addedBy);

    @Query("SELECT v FROM Vacuum v WHERE" +
            "(:name IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:dateFrom IS NULL OR v.dateAdded >= :dateFrom) AND " +
            "(:dateTo IS NULL OR v.dateAdded <= :dateTo) AND v.addedBy = :addedBy AND v.active = true ")
    List<Vacuum> findVacuums(@Param("name") String name,
                             @Param("dateFrom") LocalDate dateFrom,
                             @Param("dateTo") LocalDate dateTo,
                             @Param("addedBy") User addedBy);
}
