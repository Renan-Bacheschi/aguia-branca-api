package io.github.renanbacheschi.aguiabranca.strategy;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface StrategyRepository extends MongoRepository<StrategyDocument, String> {

    Page<StrategyDocument> findByArchivedFalse(Pageable pageable);

    Page<StrategyDocument> findByArchivedFalseAndStartsOnLessThanEqualAndEndsOnGreaterThanEqual(
            LocalDate startsOn,
            LocalDate endsOn,
            Pageable pageable);

    @Query("{'$or': [{'archived': true}, {'startsOn': {'$gt': ?0}}, {'endsOn': {'$lt': ?0}}]}")
    Page<StrategyDocument> findInactiveOn(LocalDate date, Pageable pageable);
}
